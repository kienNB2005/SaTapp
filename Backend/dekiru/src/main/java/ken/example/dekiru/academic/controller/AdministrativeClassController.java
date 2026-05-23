package ken.example.dekiru.academic.controller;

import ken.example.dekiru.academic.dto.CreateAdministrativeClassRequest;
import ken.example.dekiru.academic.dto.UpdateAdministrativeClassRequest;
import ken.example.dekiru.academic.dto.AdministrativeClassResponse;
import ken.example.dekiru.academic.service.AdministrativeClassService;
import ken.example.dekiru.attendance.service.ClassSessionService;
import ken.example.dekiru.attendance.dto.DropdownOption;
import ken.example.dekiru.common.dto.ImportResponse;
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
@RequestMapping("/api/v1/administrative-classes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdministrativeClassController {

    AdministrativeClassService administrativeClassService;
    ClassSessionService classSessionService;

    @GetMapping("/filter-by-session")
    public ApiResponse<List<DropdownOption>> getAdminClassesFilter() {
        List<DropdownOption> classes = classSessionService.getAdminClassesForLecturer();
        return ApiResponse.success(classes, "Lấy danh sách lớp hành chính thành công");
    }

    @GetMapping
    public ApiResponse<List<AdministrativeClassResponse>> getAllAdministrativeClasses() {
        List<AdministrativeClassResponse> classes = administrativeClassService.getAllAdministrativeClasses();
        return ApiResponse.success(classes, "Lấy danh sách lớp hành chính thành công");
    }

    @GetMapping("/{id}")
    public ApiResponse<AdministrativeClassResponse> getAdministrativeClassById(@PathVariable Long id) {
        AdministrativeClassResponse administrativeClass = administrativeClassService.getAdministrativeClassById(id);
        return ApiResponse.success(administrativeClass, "Lấy thông tin lớp hành chính thành công");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdministrativeClassResponse> createAdministrativeClass(@RequestBody CreateAdministrativeClassRequest request) {
        AdministrativeClassResponse administrativeClass = administrativeClassService.createAdministrativeClass(request);
        return ApiResponse.success(administrativeClass, "Thêm mới lớp hành chính thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdministrativeClassResponse> updateAdministrativeClass(@PathVariable Long id, @RequestBody UpdateAdministrativeClassRequest request) {
        AdministrativeClassResponse administrativeClass = administrativeClassService.updateAdministrativeClass(id, request);
        return ApiResponse.success(administrativeClass, "Cập nhật lớp hành chính thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteAdministrativeClass(@PathVariable Long id) {
        administrativeClassService.deleteAdministrativeClass(id);
        return ApiResponse.success(null, "Xóa lớp hành chính thành công");
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResponse> importAdministrativeClasses(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }
        ImportResponse report = administrativeClassService.importExcel(file);
        return ApiResponse.success(report, "Đã xử lý xong file dữ liệu.");
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        // Trỏ đường dẫn tới file Excel nằm trong thư mục resources
        Resource resource = new ClassPathResource("templates/Template_Import_LopHanhChinh.xlsx");

        // Kiểm tra xem có lỡ tay xóa mất file không
        if (!resource.exists()) {
            // Bạn có thể throw AppException ở đây nếu muốn
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        // Trả file về cho trình duyệt tải xuống
        return ResponseEntity.ok()
                // Header này báo cho trình duyệt biết đây là file đính kèm để tải về
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_LopHanhChinh.xlsx")
                // Khai báo định dạng file Excel
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
