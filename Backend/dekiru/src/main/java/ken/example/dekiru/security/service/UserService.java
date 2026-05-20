package ken.example.dekiru.security.service;
import ken.example.dekiru.student.dto.CreateStudentRequest;
import ken.example.dekiru.academic.dto.CreateLecturerRequest;
import ken.example.dekiru.student.dto.StudentExcelDTO;
import ken.example.dekiru.academic.dto.LecturerExcelDTO;
import ken.example.dekiru.student.dto.UpdateStudentRequest;
import ken.example.dekiru.academic.dto.UpdateLecturerRequest;
import ken.example.dekiru.student.dto.StudentPreviewResponse;
import ken.example.dekiru.student.dto.StudentResponse;
import ken.example.dekiru.security.dto.UserResponse;
import ken.example.dekiru.academic.dto.LecturerPreviewResponse;
import ken.example.dekiru.academic.dto.LecturerResponse;
import ken.example.dekiru.academic.entity.AdministrativeClass;
import ken.example.dekiru.student.entity.Student;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.academic.entity.Faculty;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.security.mapper.UserMapper;
import ken.example.dekiru.academic.repository.AdministrativeClassRepository;
import ken.example.dekiru.student.repository.StudentRepository;
import ken.example.dekiru.security.repository.UserRepository;
import ken.example.dekiru.academic.repository.FacultyRepository;
import ken.example.dekiru.academic.repository.LecturerRepository;
import ken.example.dekiru.common.specification.LecturerSpecifications;
import ken.example.dekiru.common.specification.StudentSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.data.domain.Pageable;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults (level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class UserService  {
    UserRepository userRepository;
    StudentRepository studentRepository;
    AdministrativeClassRepository administrativeClassRepository;
    Validator validator;
    UserMapper userMapper;
    FacultyRepository facultyRepository;
    LecturerRepository lecturerRepository;

    public List<UserResponse> getAllUsers() {
        return userMapper.toUserResponseList(userRepository.findAll());
    }

    public Page<StudentResponse> getAllStudents(String search, Long classId, Boolean isActive, Pageable pageable) {
        // Kết hợp các điều kiện lọc
        Specification<Student> spec = Specification.where(StudentSpecifications.hasSearchText(search))
                .and(StudentSpecifications.hasClassId(classId))
                .and(StudentSpecifications.hasStatus(isActive));

        // Truy vấn dữ liệu phân trang
        Page<Student> studentPage = studentRepository.findAll(spec, pageable);

        // Map sang DTO (Mapper của bạn cần xử lý việc lấy fullName từ User
        // và className từ AdministrativeClass)
        return studentPage.map(userMapper::toStudentResponse);
    }

    public Page<LecturerResponse> getAllLecturers(String search, Long facultyId, Boolean isActive, Pageable pageable) {
        // Kết hợp các điều kiện lọc
        Specification<Lecturer> spec = Specification.where(LecturerSpecifications.hasSearchText(search))
                .and(LecturerSpecifications.hasFacultyId(facultyId))
                .and(LecturerSpecifications.hasStatus(isActive));

        // Truy vấn dữ liệu phân trang
        Page<Lecturer> lecturerPage = lecturerRepository.findAll(spec, pageable);

        // Map sang DTO (Mapper của bạn cần xử lý việc lấy fullName từ User
        // và facultyName từ Faculty)
        return lecturerPage.map(userMapper::toLecturerResponse);
    }

    // 2. Lấy sinh viên theo lớp
    public List<StudentResponse> getStudentsByAdminClass(String adminClassCode) {
        return userMapper.toStudentResponseList(studentRepository.findAllByAdminClass_Code(adminClassCode));
    }

    // 3. Lấy tất cả giảng viên
    public List<LecturerResponse> getAllLecturers() {
        return userMapper.toLecturerResponseList(lecturerRepository.findAll());
    }

    // 4. Lấy giảng viên theo khoa
    public List<LecturerResponse> getLecturersByFaculty(String facultyCode) {
        return userMapper.toLecturerResponseList(lecturerRepository.findAllByFaculty_Code(facultyCode));
    }


    @Transactional
    public List<LecturerPreviewResponse> previewImportLecturer(MultipartFile file) {

        List<LecturerPreviewResponse> result = new ArrayList<>();
        List<LecturerExcelDTO> rawList = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("File Excel không có dữ liệu");
            }
            String[] expectedHeaders = {"Họ và tên", "Email", "Mã giảng viên", "Mã khoa"};
            for (int i = 0; i < expectedHeaders.length; i++) {
                if (!expectedHeaders[i].equalsIgnoreCase(getCellValue(headerRow.getCell(i)))) {
                    throw new RuntimeException("Sai định dạng file mẫu. Cột " + (i + 1) + " phải là '" + expectedHeaders[i] + "'");
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                LecturerExcelDTO dto = LecturerExcelDTO.builder()
                        .rowIndex(i + 1)
                        .fullName(getCellValue(row.getCell(0)))
                        .email(getCellValue(row.getCell(1)))
                        .lecturerCode(getCellValue(row.getCell(2)))
                        .facultyCode(getCellValue(row.getCell(3)))
                        .build();
                rawList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage());
        }
        List<String> emails = rawList.stream().map(LecturerExcelDTO::getEmail).toList();
        List<String> lecturerCodes = rawList.stream().map(LecturerExcelDTO::getLecturerCode).toList();
        List<String> facultyCodes = rawList.stream().map(LecturerExcelDTO::getFacultyCode).toList();

        Set<String> existingEmails = userRepository.findAllByEmailIn(emails).stream().map(User::getEmail).collect(Collectors.toSet());
        Set<String> existingFacultyCodes = facultyRepository.findAllByCodeIn(facultyCodes).stream()
                .map(Faculty::getCode).collect(Collectors.toSet());
        Set<String> existingLecturerCodes = lecturerRepository.findAllByLecturerCodeIn(lecturerCodes).stream()
                .map(Lecturer::getLecturerCode).collect(Collectors.toSet());
        Set<String> currentFileEmails = new HashSet<>();
        Set<String> currentFileLecturerCodes = new HashSet<>();
        for (LecturerExcelDTO dto : rawList) {
            LecturerPreviewResponse response = LecturerPreviewResponse.builder()
                    .lecturer(dto)
                    .isValid(true)
                    .errors(new ArrayList<>())
                    .build();

            Set<ConstraintViolation<LecturerExcelDTO>> violations = validator.validate(dto); // trả về danh sách lỗi nếu có
            if (!violations.isEmpty()) {
                response.setValid(false);
                violations.forEach(v -> response.getErrors().add(v.getMessage())); //trả về lỗi theo annotation đã đặt trong DTO
            }

            if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                if (existingEmails.contains(dto.getEmail())) {
                    response.setValid(false);
                    response.getErrors().add("Email đã tồn tại trong hệ thống");
                }
                if (!currentFileEmails.add(dto.getEmail())) {
                    response.setValid(false);
                    response.getErrors().add("Email bị trùng lặp trong file");
                }
            }

            if (dto.getLecturerCode() != null && !dto.getLecturerCode().isBlank()) {
                if (existingLecturerCodes.contains(dto.getLecturerCode())) {
                    response.setValid(false);
                    response.getErrors().add("Mã giảng viên đã tồn tại");
                }
                if (!currentFileLecturerCodes.add(dto.getLecturerCode())) {
                    response.setValid(false);
                    response.getErrors().add("Mã giảng viên bị trùng lặp trong file");
                }
            }

            if (dto.getFacultyCode() != null && !dto.getFacultyCode().isBlank()) {
                if (!existingFacultyCodes.contains(dto.getFacultyCode())) {
                    response.setValid(false);
                    response.getErrors().add("Mã khoa không tồn tại");
                }
            }

            result.add(response);
        }

        return result; // Placeholder
    }

    @Transactional
    public void confirmImportLecturer(List<LecturerExcelDTO> lecturers) {
        List<String> facultyCodes = lecturers.stream().map(LecturerExcelDTO::getFacultyCode).toList();
        Map<String, Faculty> mappedFaculties = facultyRepository.findAllByCodeIn(facultyCodes)
                .stream().collect(Collectors.toMap(Faculty::getCode, f -> f));

        List<User> newUsers = new ArrayList<>();
        List<Lecturer> newLecturers = new ArrayList<>();

        for (LecturerExcelDTO dto : lecturers) {
            User user = User.builder()
                    .email(dto.getEmail())
                    .fullName(dto.getFullName())
                    .role(User.Role.lecturer)
                    .isActive(true)
                    .build();

            Faculty faculty = mappedFaculties.get(dto.getFacultyCode());

            Lecturer lecturer = Lecturer.builder()
                    .user(user)
                    .lecturerCode(dto.getLecturerCode())
                    .faculty(faculty)
                    .build();

            newUsers.add(user);
            newLecturers.add(lecturer);
        }

        userRepository.saveAll(newUsers);
        lecturerRepository.saveAll(newLecturers);
    }

    @Transactional
    public List<StudentPreviewResponse> previewImportStudent(MultipartFile file) {
        List<StudentPreviewResponse> result = new ArrayList<>();
        List<StudentExcelDTO> rawList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("File Excel không có dữ liệu");
            }
            String[] expectedHeaders = {"Họ và tên", "Email", "Mã sinh viên", "Mã lớp hành chính"};
            for (int i = 0; i < expectedHeaders.length; i++) {
                if (!expectedHeaders[i].equalsIgnoreCase(getCellValue(headerRow.getCell(i)))) {
                    throw new RuntimeException("Sai định dạng file mẫu. Cột " + (i + 1) + " phải là '" + expectedHeaders[i] + "'");
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                StudentExcelDTO dto = StudentExcelDTO.builder()
                        .rowIndex(i + 1)
                        .fullName(getCellValue(row.getCell(0)))
                        .email(getCellValue(row.getCell(1)))
                        .studentCode(getCellValue(row.getCell(2)))
                        .adminClassCode(getCellValue(row.getCell(3)))
                        .build();
                rawList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage());
        }

        List<String> emails = rawList.stream().map(StudentExcelDTO::getEmail).toList();
        List<String> studentCodes = rawList.stream().map(StudentExcelDTO::getStudentCode).toList();
        List<String> adminClassCodes = rawList.stream().map(StudentExcelDTO::getAdminClassCode).toList();

        Set<String> existingEmails = userRepository.findAllByEmailIn(emails).stream().map(User::getEmail).collect(Collectors.toSet());
        Set<String> existingAdminClassCodes = administrativeClassRepository.findAllByCodeIn(adminClassCodes).stream()
                .map(AdministrativeClass::getCode).collect(Collectors.toSet());
        Set<String> existingStudentCodes = studentRepository.findAllByStudentCodeIn(studentCodes).stream()
                .map(Student::getStudentCode).collect(Collectors.toSet());
        
        Set<String> currentFileEmails = new HashSet<>();
        Set<String> currentFileStudentCodes = new HashSet<>();

        for (StudentExcelDTO dto : rawList) {
            StudentPreviewResponse response = StudentPreviewResponse.builder()
                    .student(dto)
                    .isValid(true)
                    .errors(new ArrayList<>())
                    .build();

            Set<ConstraintViolation<StudentExcelDTO>> violations = validator.validate(dto); // trả về danh sách lỗi nếu có
            if (!violations.isEmpty()) {
                response.setValid(false);
                violations.forEach(v -> response.getErrors().add(v.getMessage())); //trả về lỗi theo annotation đã đặt trong DTO
            }

            if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                if (existingEmails.contains(dto.getEmail())) {
                    response.setValid(false);
                    response.getErrors().add("Email đã tồn tại trong hệ thống");
                }
                if (!currentFileEmails.add(dto.getEmail())) {
                    response.setValid(false);
                    response.getErrors().add("Email bị trùng lặp trong file");
                }
            }

            if (dto.getStudentCode() != null && !dto.getStudentCode().isBlank()) {
                if (existingStudentCodes.contains(dto.getStudentCode())) {
                    response.setValid(false);
                    response.getErrors().add("Mã sinh viên đã tồn tại");
                }
                if (!currentFileStudentCodes.add(dto.getStudentCode())) {
                    response.setValid(false);
                    response.getErrors().add("Mã sinh viên bị trùng lặp trong file");
                }
            }
            
            if (dto.getAdminClassCode() != null && !dto.getAdminClassCode().isBlank()) {
                if (!existingAdminClassCodes.contains(dto.getAdminClassCode())) {
                    response.setValid(false);
                    response.getErrors().add("Mã lớp hành chính không tồn tại");
                }
            }

            result.add(response);
        }

        return result;
    }

    @Transactional
    public void confirmImportStudent(List<StudentExcelDTO> students) {
        List<String> adminClassCodes = students.stream().map(StudentExcelDTO::getAdminClassCode).toList();
        Map<String, AdministrativeClass> mappedAdminClasses = administrativeClassRepository.findAllByCodeIn(adminClassCodes)
                .stream().collect(Collectors.toMap(AdministrativeClass::getCode, c -> c));

        List<User> newUsers = new ArrayList<>();
        List<Student> newStudents = new ArrayList<>();

        for (StudentExcelDTO dto : students) {
            User user = User.builder()
                    .email(dto.getEmail())
                    .fullName(dto.getFullName())
                    .role(User.Role.student)
                    .isActive(true)
                    .build();

            AdministrativeClass adminClass = mappedAdminClasses.get(dto.getAdminClassCode());
            
            Student student = Student.builder()
                    .user(user)
                    .studentCode(dto.getStudentCode())
                    .adminClass(adminClass)
                    .build();
            
            newUsers.add(user);
            newStudents.add(student);
        }

        userRepository.saveAll(newUsers);
        studentRepository.saveAll(newStudents);
    }

    @Transactional
    public StudentResponse updateStudent(Long studentId, UpdateStudentRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));

        User user = student.getUser();
        // Check if email changed and already exists
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        userMapper.updateUserFromStudentRequest(request, user);
        
        userRepository.save(user);
        return userMapper.toStudentResponse(student);
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        if (studentRepository.findByStudentCode(request.getStudentCode()).isPresent()) {
            throw new RuntimeException("Mã sinh viên đã tồn tại");
        }

        AdministrativeClass adminClass = administrativeClassRepository.findById(request.getAdminClassId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp hành chính"));

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(User.Role.student)
                .isActive(true)
                .build();
        userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .studentCode(request.getStudentCode())
                .adminClass(adminClass)
                .build();
        studentRepository.save(student);

        return userMapper.toStudentResponse(student);
    }

    @Transactional
    public LecturerResponse updateLecturer(Long lecturerId, UpdateLecturerRequest request) {
        Lecturer lecturer = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên"));

        User user = lecturer.getUser();
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        userMapper.updateUserFromLecturerRequest(request, user);
        userRepository.save(user);

        if (!lecturer.getFaculty().getId().equals(request.getFacultyId())) {
            Faculty faculty = facultyRepository.findById(request.getFacultyId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khoa"));
            lecturer.setFaculty(faculty);
            lecturerRepository.save(lecturer);
        }

        return userMapper.toLecturerResponse(lecturer);
    }

    @Transactional
    public LecturerResponse createLecturer(CreateLecturerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        if (lecturerRepository.findByLecturerCode(request.getLecturerCode()).isPresent()) {
            throw new RuntimeException("Mã giảng viên đã tồn tại");
        }

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khoa"));

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(User.Role.lecturer)
                .isActive(true)
                .build();
        userRepository.save(user);

        Lecturer lecturer = Lecturer.builder()
                .user(user)
                .lecturerCode(request.getLecturerCode())
                .faculty(faculty)
                .build();
        lecturerRepository.save(lecturer);

        return userMapper.toLecturerResponse(lecturer);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return "";
    }
}
