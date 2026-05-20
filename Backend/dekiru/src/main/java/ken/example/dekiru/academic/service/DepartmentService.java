package ken.example.dekiru.academic.service;

import ken.example.dekiru.academic.dto.CreateDepartmentRequest;
import ken.example.dekiru.academic.dto.UpdateDepartmentRequest;
import ken.example.dekiru.academic.dto.DepartmentResponse;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.entity.Department;
import ken.example.dekiru.academic.entity.Faculty;
import ken.example.dekiru.academic.mapper.DepartmentMapper;
import ken.example.dekiru.academic.repository.DepartmentRepository;
import ken.example.dekiru.academic.repository.AdministrativeClassRepository;
import ken.example.dekiru.academic.repository.FacultyRepository;
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
public class DepartmentService {

    DepartmentRepository departmentRepository;
    FacultyRepository facultyRepository;
    AdministrativeClassRepository administrativeClassRepository;
    DepartmentMapper departmentMapper;

    // Lấy danh sách tất cả các bộ môn
    public List<DepartmentResponse> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departmentMapper.toDepartmentResponseList(departments);
    }

    // Lấy thông tin bộ môn theo ID
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_EXISTED));
        return departmentMapper.toDepartmentResponse(department);
    }

    @Transactional
    public ImportResponse importExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_IS_EMPTY);
        }

        int successCount = 0;
        int updateCount = 0;
        int errorCount = 0;
        List<String> errorDetails = new ArrayList<>();
        List<Department> departmentsToSave = new ArrayList<>();
        Set<String> depCodesInCurrentFile = new HashSet<>();
        int totalRows = 0; // ✅ Declare ở ngoài try-catch

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum();

            // ✅ Validate headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }
            List<String> expectedHeaders = List.of("Mã ngành học", "Tên ngành học", "Mã khoa");
            if (!validateHeaders(headerRow, expectedHeaders)) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }

            // 🔥 FIX N+1: Lấy tất cả mã code từ Excel
            List<String> depCodesInFile = new ArrayList<>();
            List<String> facCodesInFile = new ArrayList<>();
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String depCode = getCellValue(row.getCell(0));
                String facCode = getCellValue(row.getCell(2));
                if (!depCode.isEmpty()) {
                    depCodesInFile.add(depCode);
                }
                if (!facCode.isEmpty()) {
                    facCodesInFile.add(facCode);
                }
            }

            // 🔥 FIX N+1: 2 queries batch
            Map<String, Department> existingDepartmentsMap = new HashMap<>();
            if (!depCodesInFile.isEmpty()) {
                List<Department> existingDeps = departmentRepository.findAllByCodeIn(depCodesInFile);
                for (Department dep : existingDeps) {
                    existingDepartmentsMap.put(dep.getCode(), dep);
                }
            }

            Map<String, Faculty> existingFacultiesMap = new HashMap<>();
            if (!facCodesInFile.isEmpty()) {
                List<Faculty> existingFacs = facultyRepository.findAllByCodeIn(facCodesInFile);
                for (Faculty fac : existingFacs) {
                    existingFacultiesMap.put(fac.getCode(), fac);
                }
            }

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String facultyCode = getCellValue(row.getCell(2));

                    if (code.isEmpty() && name.isEmpty()) {
                        continue;
                    }

                    if (code.isEmpty() || name.isEmpty() || facultyCode.isEmpty()) {
                        throw new RuntimeException("Mã bộ môn, Tên bộ môn, Mã khoa không được để trống");
                    }

                    // 🔥 FIX DUPLICATE
                    if (depCodesInCurrentFile.contains(code)) {
                        throw new RuntimeException("Mã bộ môn '" + code + "' bị trùng trong file Excel");
                    }
                    depCodesInCurrentFile.add(code);

                    Faculty faculty = existingFacultiesMap.get(facultyCode);
                    if (faculty == null) {
                        throw new RuntimeException("Khoa với mã '" + facultyCode + "' không tồn tại");
                    }

                    Department existingDepartment = existingDepartmentsMap.get(code);

                    if (existingDepartment != null) {
                        if (!existingDepartment.getName().equals(name) || !existingDepartment.getFaculty().getId().equals(faculty.getId())) {
                            existingDepartment.setName(name);
                            existingDepartment.setFaculty(faculty);
                            departmentsToSave.add(existingDepartment);
                            updateCount++;
                        }
                    } else {
                        Department newDepartment = Department.builder()
                                .code(code)
                                .name(name)
                                .faculty(faculty)
                                .build();
                        departmentsToSave.add(newDepartment);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorDetails.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_PROCESSING_ERROR);
        }

        // PHASE 2: Lưu Database (ngoài try-catch)
        if (!departmentsToSave.isEmpty()) {
            departmentRepository.saveAll(departmentsToSave);
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

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    // ✅ Sửa bộ môn (chỉ sửa facultyId - khóa ngoại)
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_EXISTED));
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            department.setName(request.getName());
        }
        
        // Sửa Faculty nếu có facultyId
        if (request.getFacultyId() != null) {
            Faculty faculty = facultyRepository.findById(request.getFacultyId())
                    .orElseThrow(() -> new AppException(ErrorCode.FACULTY_NOT_EXISTED));
            department.setFaculty(faculty);
        }
        
        Department updated = departmentRepository.save(department);
        return departmentMapper.toDepartmentResponse(updated);
    }

    // ✅ Xóa bộ môn
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_EXISTED));
        
        // Check: Bộ môn có lớp hành chính không?
        long classCount = administrativeClassRepository.countByDepartmentId(id);
        if (classCount > 0) {
            throw new AppException(ErrorCode.DEPARTMENT_HAS_ADMINISTRATIVE_CLASSES);
        }
        
        departmentRepository.deleteById(id);
    }

    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.DEPARTMENT_EXISTED);
        }
        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new AppException(ErrorCode.FACULTY_NOT_EXISTED));
        Department department = Department.builder()
                .code(request.getCode())
                .name(request.getName())
                .faculty(faculty)
                .build();
        Department saved = departmentRepository.save(department);
        return departmentMapper.toDepartmentResponse(saved);
    }
}
