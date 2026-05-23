package ken.example.dekiru.academic.controller;

import ken.example.dekiru.academic.dto.CreateSubjectRequest;
import ken.example.dekiru.academic.dto.UpdateSubjectRequest;
import ken.example.dekiru.academic.dto.SubjectResponse;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.service.SubjectService;
import ken.example.dekiru.attendance.service.ClassSessionService;
import ken.example.dekiru.attendance.dto.DropdownOption;
import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectController {

    SubjectService subjectService;
    ClassSessionService classSessionService;

    @GetMapping("/filter-by-session")
    public ApiResponse<List<DropdownOption>> getSubjectsFilter(
            @RequestParam(required = false) Long adminClassId) {
        List<DropdownOption> subjects;
        if (adminClassId != null) {
            subjects = classSessionService.getSubjectsForLecturerAndClass(adminClassId);
        } else {
            subjects = classSessionService.getSubjectsForLecturer();
        }
        return ApiResponse.success(subjects, "Lấy danh sách môn học thành công");
    }

    @GetMapping
    public ApiResponse<List<SubjectResponse>> getAllSubjects() {
        List<SubjectResponse> subjects = subjectService.getAllSubjects();
        return ApiResponse.success(subjects, "Lấy danh sách môn học thành công");
    }

    @GetMapping("/{id}")
    public ApiResponse<SubjectResponse> getSubjectById(@PathVariable Long id) {
        SubjectResponse subject = subjectService.getSubjectById(id);
        return ApiResponse.success(subject, "Lấy thông tin môn học thành công");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> createSubject(@RequestBody CreateSubjectRequest request) {
        SubjectResponse subject = subjectService.createSubject(request);
        return ApiResponse.success(subject, "Thêm mới môn học thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> updateSubject(@PathVariable Long id, @RequestBody UpdateSubjectRequest request) {
        SubjectResponse subject = subjectService.updateSubject(id, request);
        return ApiResponse.success(subject, "Cập nhật môn học thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ApiResponse.success(null, "Xóa môn học thành công");
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResponse> importSubjects(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }
        ImportResponse report = subjectService.importExcel(file);
        return ApiResponse.success(report, "Đã xử lý xong file dữ liệu.");
    }
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        // Trỏ đường dẫn tới file Excel nằm trong thư mục resources
        Resource resource = new ClassPathResource("templates/Template_Import_MonHoc.xlsx");

        // Kiểm tra xem có lỡ tay xóa mất file không
        if (!resource.exists()) {
            // Bạn có thể throw AppException ở đây nếu muốn
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        // Trả file về cho trình duyệt tải xuống
        return ResponseEntity.ok()
                // Header này báo cho trình duyệt biết đây là file đính kèm để tải về
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_MonHoc.xlsx")
                // Khai báo định dạng file Excel
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
