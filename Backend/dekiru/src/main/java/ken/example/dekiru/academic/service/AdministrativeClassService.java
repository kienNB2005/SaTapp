package ken.example.dekiru.academic.service;

import ken.example.dekiru.academic.dto.CreateAdministrativeClassRequest;
import ken.example.dekiru.academic.dto.UpdateAdministrativeClassRequest;
import ken.example.dekiru.academic.dto.AdministrativeClassResponse;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.entity.AdministrativeClass;
import ken.example.dekiru.academic.entity.Department;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.academic.mapper.AdministrativeClassMapper;
import ken.example.dekiru.academic.repository.AdministrativeClassRepository;
import ken.example.dekiru.academic.repository.DepartmentRepository;
import ken.example.dekiru.academic.repository.LecturerRepository;
import ken.example.dekiru.schedule.repository.ScheduleRepository;
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
public class AdministrativeClassService {

    AdministrativeClassRepository administrativeClassRepository;
    DepartmentRepository departmentRepository;
    LecturerRepository lecturerRepository;
    AdministrativeClassMapper administrativeClassMapper;
    ScheduleRepository scheduleRepository;

    // Lấy danh sách tất cả các lớp hành chính
    public List<AdministrativeClassResponse> getAllAdministrativeClasses() {
        List<AdministrativeClass> classes = administrativeClassRepository.findAll();
        return administrativeClassMapper.toAdministrativeClassResponseList(classes);
    }

    // Lấy thông tin lớp hành chính theo ID
    public AdministrativeClassResponse getAdministrativeClassById(Long id) {
        AdministrativeClass administrativeClass = administrativeClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADMINISTRATIVE_CLASS_NOT_EXISTED));
        return administrativeClassMapper.toAdministrativeClassResponse(administrativeClass);
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
        List<AdministrativeClass> classesToSave = new ArrayList<>();
        Set<String> codesInCurrentFile = new HashSet<>();
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
            List<String> expectedHeaders = List.of("Mã lớp", "Tên lớp", "Năm khoá", "Mã ngành", "Mã giáo viên");
            if (!validateHeaders(headerRow, expectedHeaders)) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }

            // 🔥 FIX N+1: Batch load tất cả codes từ Excel
            List<String> classCodesInFile = new ArrayList<>();
            List<String> depCodesInFile = new ArrayList<>();
            List<String> lecturerCodesInFile = new ArrayList<>();

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String classCode = getCellValue(row.getCell(0));
                String depCode = getCellValue(row.getCell(3));
                String lecCode = getCellValue(row.getCell(4));
                
                if (!classCode.isEmpty()) classCodesInFile.add(classCode);
                if (!depCode.isEmpty()) depCodesInFile.add(depCode);
                if (!lecCode.isEmpty()) lecturerCodesInFile.add(lecCode);
            }

            // 🔥 FIX N+1: 3 queries batch
            Map<String, AdministrativeClass> existingClassesMap = new HashMap<>();
            if (!classCodesInFile.isEmpty()) {
                List<AdministrativeClass> existing = administrativeClassRepository.findAllByCodeIn(classCodesInFile);
                for (AdministrativeClass cls : existing) {
                    existingClassesMap.put(cls.getCode(), cls);
                }
            }

            Map<String, Department> departmentsMap = new HashMap<>();
            if (!depCodesInFile.isEmpty()) {
                List<Department> deps = departmentRepository.findAllByCodeIn(depCodesInFile);
                for (Department dep : deps) {
                    departmentsMap.put(dep.getCode(), dep);
                }
            }

            Map<String, Lecturer> lecturersMap = new HashMap<>();
            if (!lecturerCodesInFile.isEmpty()) {
                List<Lecturer> lecturers = lecturerRepository.findAllByLecturerCodeIn(lecturerCodesInFile);
                for (Lecturer lec : lecturers) {
                    lecturersMap.put(lec.getLecturerCode(), lec);
                }
            }

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String cohortYear = getCellValue(row.getCell(2));
                    String departmentCode = getCellValue(row.getCell(3));
                    String lecturerCode = getCellValue(row.getCell(4));

                    if (code.isEmpty() && name.isEmpty()) {
                        continue;
                    }

                    if (code.isEmpty() || name.isEmpty() || cohortYear.isEmpty() || departmentCode.isEmpty()) {
                        throw new RuntimeException("Mã lớp, Tên lớp, Năm khoá, Mã ngành không được để trống");
                    }

                    // 🔥 FIX DUPLICATE
                    if (codesInCurrentFile.contains(code)) {
                        throw new RuntimeException("Mã lớp '" + code + "' bị trùng trong file Excel");
                    }
                    codesInCurrentFile.add(code);

                    // 🔥 Từ Map, không query
                    Department department = departmentsMap.get(departmentCode);
                    if (department == null) {
                        throw new RuntimeException("Ngành với mã '" + departmentCode + "' không tồn tại");
                    }

                    Lecturer lecturer = null;
                    if (!lecturerCode.isEmpty()) {
                        lecturer = lecturersMap.get(lecturerCode);
                        if (lecturer == null) {
                            throw new RuntimeException("Giáo viên với mã '" + lecturerCode + "' không tồn tại");
                        }
                    }

                    AdministrativeClass existingClass = existingClassesMap.get(code);

                    if (existingClass != null) {
                        // Chỉ tính update nếu thực sự có thay đổi
                        boolean changed = !existingClass.getName().equals(name) ||
                                !existingClass.getCohortYear().equals(cohortYear) ||
                                !existingClass.getDepartment().getId().equals(department.getId()) ||
                                (lecturer == null ? existingClass.getHomeroomTeacher() != null : 
                                 !lecturer.getId().equals(existingClass.getHomeroomTeacher() != null ? existingClass.getHomeroomTeacher().getId() : null));

                        if (changed) {
                            existingClass.setName(name);
                            existingClass.setCohortYear(cohortYear);
                            existingClass.setDepartment(department);
                            existingClass.setHomeroomTeacher(lecturer);
                            classesToSave.add(existingClass);
                            updateCount++;
                        }
                    } else {
                        AdministrativeClass newClass = AdministrativeClass.builder()
                                .code(code)
                                .name(name)
                                .cohortYear(cohortYear)
                                .department(department)
                                .homeroomTeacher(lecturer)
                                .build();
                        classesToSave.add(newClass);
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
        if (!classesToSave.isEmpty()) {
            administrativeClassRepository.saveAll(classesToSave);
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
            if (!headerValue.equals(expectedHeaders.get(i))) {
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

    // ✅ Sửa lớp hành chính (sửa name, cohortYear, lecturerId)
    public AdministrativeClassResponse updateAdministrativeClass(Long id, UpdateAdministrativeClassRequest request) {
        AdministrativeClass administrativeClass = administrativeClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADMINISTRATIVE_CLASS_NOT_EXISTED));
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            administrativeClass.setName(request.getName());
        }
        
        if (request.getCohortYear() != null && !request.getCohortYear().trim().isEmpty()) {
            administrativeClass.setCohortYear(request.getCohortYear());
        }
        
        // Sửa giảng viên chủ nhiệm nếu có lecturerCode
        if (request.getLecturerCode() != null) {
            if (request.getLecturerCode().isBlank()) {
                administrativeClass.setHomeroomTeacher(null);
            } else {
                Lecturer lecturer = lecturerRepository.findByLecturerCode(request.getLecturerCode())
                        .orElseThrow(() -> new AppException(ErrorCode.LECTURER_NOT_EXISTED));
                administrativeClass.setHomeroomTeacher(lecturer);
            }
        }
        
        AdministrativeClass updated = administrativeClassRepository.save(administrativeClass);
        return administrativeClassMapper.toAdministrativeClassResponse(updated);
    }

    // ✅ Xóa lớp hành chính
    public void deleteAdministrativeClass(Long id) {
        AdministrativeClass administrativeClass = administrativeClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADMINISTRATIVE_CLASS_NOT_EXISTED));

         long scheduleCount = scheduleRepository.countByAdminClassId(id);
         if (scheduleCount > 0) {
             throw new AppException(ErrorCode.ADMINISTRATIVE_CLASS_HAS_SCHEDULES);
         }
        
        administrativeClassRepository.deleteById(id);
    }

    public AdministrativeClassResponse createAdministrativeClass(CreateAdministrativeClassRequest request) {
        if (administrativeClassRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.ADMINISTRATIVE_CLASS_EXISTED);
        }
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_EXISTED));
        
        Lecturer lecturer = null;
        if (request.getHomeroomTeacherCode() != null && !request.getHomeroomTeacherCode().isBlank()) {
            lecturer = lecturerRepository.findByLecturerCode(request.getHomeroomTeacherCode())
                    .orElseThrow(() -> new AppException(ErrorCode.LECTURER_NOT_EXISTED));
        }

        AdministrativeClass administrativeClass = AdministrativeClass.builder()
                .code(request.getCode())
                .name(request.getName())
                .cohortYear(request.getCohortYear())
                .department(department)
                .homeroomTeacher(lecturer)
                .build();
        AdministrativeClass saved = administrativeClassRepository.save(administrativeClass);
        return administrativeClassMapper.toAdministrativeClassResponse(saved);
    }
}
