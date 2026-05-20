package ken.example.dekiru.academic.service;

import ken.example.dekiru.academic.dto.CreateFacultyRequest;
import ken.example.dekiru.academic.dto.UpdateFacultyRequest;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.dto.FacultyResponse;
import ken.example.dekiru.academic.entity.Faculty;
import ken.example.dekiru.academic.mapper.FacultyMapper;
import ken.example.dekiru.academic.repository.FacultyRepository;
import ken.example.dekiru.academic.repository.DepartmentRepository;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FacultyService {

    FacultyRepository facultyRepository;
    DepartmentRepository departmentRepository;
    FacultyMapper facultyMapper;

    @Transactional
    public ImportResponse importExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_IS_EMPTY);
        }

        int successCount = 0;
        int updateCount = 0;
        int errorCount = 0;
        List<String> errorDetails = new ArrayList<>();
        List<Faculty> facultiesToSave = new ArrayList<>();
        Set<String> codesInCurrentFile = new HashSet<>();
        int totalRows = 0;

        // PHASE 1: Đọc file Excel (catch lỗi file)
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum();

            // ✅ Validate headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }
            List<String> expectedHeaders = List.of("Mã khoa", "Tên khoa");
            if (!validateHeaders(headerRow, expectedHeaders)) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }

            // 🔥 FIX N+1: Batch load
            List<String> codesInFile = new ArrayList<>();
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String code = getCellValue(row.getCell(0));
                if (!code.isEmpty()) {
                    codesInFile.add(code);
                }
            }

            Map<String, Faculty> existingFacultiesMap = new HashMap<>();
            if (!codesInFile.isEmpty()) {
                List<Faculty> existingFaculties = facultyRepository.findAllByCodeIn(codesInFile);
                for (Faculty faculty : existingFaculties) {
                    existingFacultiesMap.put(faculty.getCode(), faculty);
                }
            }

            // Process từng dòng
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));

                    if (code.isEmpty() && name.isEmpty()) {
                        continue;
                    }

                    if (code.isEmpty() || name.isEmpty()) {
                        throw new RuntimeException("Mã khoa hoặc Tên khoa không được để trống");
                    }

                    // 🔥 FIX DUPLICATE
                    if (codesInCurrentFile.contains(code)) {
                        throw new RuntimeException("Mã khoa '" + code + "' bị trùng trong file Excel");
                    }
                    codesInCurrentFile.add(code);

                    Faculty existingFaculty = existingFacultiesMap.get(code);

                    if (existingFaculty != null) {
                        if (!existingFaculty.getName().equals(name)) {
                            existingFaculty.setName(name);
                            facultiesToSave.add(existingFaculty);
                            updateCount++;
                        }
                    } else {
                        Faculty newFaculty = Faculty.builder()
                                .code(code)
                                .name(name)
                                .build();
                        facultiesToSave.add(newFaculty);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorDetails.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            // ❌ Chỉ catch lỗi file Excel (IOException, XSSFException, ...)
            throw new AppException(ErrorCode.FILE_PROCESSING_ERROR);
        }

        // PHASE 2: Lưu Database (NGOÀI khối try-catch của file)
        // ✅ Nếu lỗi DB, sẽ throw ra đúng, không bị mask
        if (!facultiesToSave.isEmpty()) {
            facultyRepository.saveAll(facultiesToSave);
        }

        return ImportResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .updateCount(updateCount)
                .errorCount(errorCount)
                .errors(errorDetails)
                .build();
    }

    // ✅ Helper: Validate headers
    private boolean validateHeaders(Row headerRow, List<String> expectedHeaders) {
        for (int i = 0; i < expectedHeaders.size(); i++) {
            Cell cell = headerRow.getCell(i);
            String headerValue = getCellValue(cell).trim();
            if (!headerValue.equalsIgnoreCase(expectedHeaders.get(i))) {
                return false;
            }
        }
        return true;
    }

    // Hàm tiện ích: Lấy dữ liệu ô an toàn
    // Tự động ép kiểu (để nếu user nhập mã khoa là số 123, app cũng không báo lỗi)
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    // Lấy danh sách tất cả các khoa
    public List<FacultyResponse> getAllFaculties() {
        List<Faculty> faculties = facultyRepository.findAll();
        return facultyMapper.toFacultyResponseList(faculties);
    }

    // Lấy thông tin khoa theo ID
    public FacultyResponse getFacultyById(Long id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FACULTY_NOT_EXISTED));
        return facultyMapper.toFacultyResponse(faculty);
    }

    // ✅ Sửa khoa (chỉ sửa name)
    public FacultyResponse updateFaculty(Long id, UpdateFacultyRequest request) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FACULTY_NOT_EXISTED));
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            faculty.setName(request.getName());
        }
        
        Faculty updated = facultyRepository.save(faculty);
        return facultyMapper.toFacultyResponse(updated);
    }

    // ✅ Xóa khoa
    public void deleteFaculty(Long id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FACULTY_NOT_EXISTED));
        
        // Check: Khoa có ngành không? Nếu có, không cho xóa
        long departmentCount = departmentRepository.countByFacultyId(id);
        if (departmentCount > 0) {
            throw new AppException(ErrorCode.FACULTY_HAS_DEPARTMENTS);
        }
        
        facultyRepository.deleteById(id);
    }

    public FacultyResponse createFaculty(CreateFacultyRequest request) {
        if (facultyRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.FACULTY_EXISTED);
        }
        Faculty faculty = Faculty.builder()
                .code(request.getCode())
                .name(request.getName())
                .build();
        Faculty saved = facultyRepository.save(faculty);
        return facultyMapper.toFacultyResponse(saved);
    }
}
