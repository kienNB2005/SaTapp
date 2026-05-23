package ken.example.dekiru.security.controller;

import ken.example.dekiru.academic.dto.LecturerExcelDTO;
import ken.example.dekiru.student.dto.StudentExcelDTO;
import ken.example.dekiru.student.dto.CreateStudentRequest;
import ken.example.dekiru.academic.dto.CreateLecturerRequest;
import ken.example.dekiru.academic.dto.UpdateLecturerRequest;
import ken.example.dekiru.student.dto.UpdateStudentRequest;
import ken.example.dekiru.academic.dto.LecturerPreviewResponse;
import ken.example.dekiru.academic.dto.LecturerResponse;
import ken.example.dekiru.student.dto.StudentPreviewResponse;
import ken.example.dekiru.student.dto.StudentResponse;
import ken.example.dekiru.security.service.UserService;
import ken.example.dekiru.common.response.ApiResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @GetMapping ("/students")
    public ApiResponse<Page<StudentResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(userService.getAllStudents(search, classId, isActive, pageable));
    }

    @GetMapping("/students/template")
    public ResponseEntity<Resource> downloadTemplateStudy() {
        // Trỏ đường dẫn tới file Excel nằm trong thư mục resources
        Resource resource = new ClassPathResource("templates/Template_Import_SinhVien.xlsx");

        // Kiểm tra xem có lỡ tay xóa mất file không
        if (!resource.exists()) {
            // Bạn có thể throw AppException ở đây nếu muốn
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        // Trả file về cho trình duyệt tải xuống
        return ResponseEntity.ok()
                // Header này báo cho trình duyệt biết đây là file đính kèm để tải về
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_SinhVien.xlsx")
                // Khai báo định dạng file Excel
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/students/import/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StudentPreviewResponse>> previewImportStudent(@RequestParam("file") MultipartFile file) {
        List<StudentPreviewResponse> response = userService.previewImportStudent(file);
        return ApiResponse.success(response, "Preview danh sách import sinh viên");
    }

    @PostMapping("/students/import/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> confirmImportStudent(@RequestBody List<StudentExcelDTO> students) {
        userService.confirmImportStudent(students);
        return ApiResponse.success(null, "Import sinh viên thành công");
    }

    @PostMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> createStudent(@RequestBody CreateStudentRequest request) {
        StudentResponse response = userService.createStudent(request);
        return ApiResponse.success(response, "Thêm mới sinh viên thành công");
    }

    @PutMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> updateStudent(@PathVariable("id") Long id, @RequestBody UpdateStudentRequest request) {
        StudentResponse response = userService.updateStudent(id, request);
        return ApiResponse.success(response, "Cập nhật sinh viên thành công");
    }

    @GetMapping ("/lecturers")
    public ApiResponse<Page<LecturerResponse>> getAllLecturers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(userService.getAllLecturers(search, departmentId, isActive, pageable));
    }

    @GetMapping("/lecturer/template")
    public ResponseEntity<Resource> downloadTemplateLecture() {
        // Trỏ đường dẫn tới file Excel nằm trong thư mục resources
        Resource resource = new ClassPathResource("templates/Template_Import_GiangVien.xlsx");

        // Kiểm tra xem có lỡ tay xóa mất file không
        if (!resource.exists()) {
            // Bạn có thể throw AppException ở đây nếu muốn
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        // Trả file về cho trình duyệt tải xuống
        return ResponseEntity.ok()
                // Header này báo cho trình duyệt biết đây là file đính kèm để tải về
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_GiangVien.xlsx")
                // Khai báo định dạng file Excel
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/lecturers/import/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LecturerPreviewResponse>> previewImportLecturer(@RequestParam("file") MultipartFile file) {
        List<LecturerPreviewResponse> response = userService.previewImportLecturer(file);
        return ApiResponse.success(response, "Preview danh sách import giảng viên");
    }

    @PostMapping("/lecturers/import/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> confirmImportLecturer(@RequestBody List<LecturerExcelDTO> lecturers) {
        userService.confirmImportLecturer(lecturers);
        return ApiResponse.success(null, "Import giảng viên thành công");
    }

    @PostMapping("/lecturers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LecturerResponse> createLecturer(@RequestBody CreateLecturerRequest request) {
        LecturerResponse response = userService.createLecturer(request);
        return ApiResponse.success(response, "Thêm mới giảng viên thành công");
    }

    @PutMapping("/lecturers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LecturerResponse> updateLecturer(@PathVariable("id") Long id, @RequestBody UpdateLecturerRequest request) {
        LecturerResponse response = userService.updateLecturer(id, request);
        return ApiResponse.success(response, "Cập nhật giảng viên thành công");
    }
}
