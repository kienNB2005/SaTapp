package ken.example.dekiru.academic.controller;

import ken.example.dekiru.academic.dto.CreateFacultyRequest;
import ken.example.dekiru.academic.dto.UpdateFacultyRequest;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.dto.FacultyResponse;
import ken.example.dekiru.academic.entity.Faculty;
import ken.example.dekiru.academic.service.FacultyService;
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
@RequestMapping("/api/v1/faculties")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FacultyController {

    FacultyService facultyService;

    @GetMapping
    public ApiResponse<List<FacultyResponse>> getAllFaculties() {
        List<FacultyResponse> faculties = facultyService.getAllFaculties();
        return ApiResponse.success(faculties, "Lấy danh sách khoa thành công");
    }

    @GetMapping("/{id}")
    public ApiResponse<FacultyResponse> getFacultyById(@PathVariable Long id) {
        FacultyResponse faculty = facultyService.getFacultyById(id);
        return ApiResponse.success(faculty, "Lấy thông tin khoa thành công");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FacultyResponse> createFaculty(@RequestBody CreateFacultyRequest request) {
        FacultyResponse faculty = facultyService.createFaculty(request);
        return ApiResponse.success(faculty, "Thêm mới khoa thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FacultyResponse> updateFaculty(@PathVariable Long id, @RequestBody UpdateFacultyRequest request) {
        FacultyResponse faculty = facultyService.updateFaculty(id, request);
        return ApiResponse.success(faculty, "Cập nhật khoa thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteFaculty(@PathVariable Long id) {
        facultyService.deleteFaculty(id);
        return ApiResponse.success(null, "Xóa khoa thành công");
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResponse> importFaculties(@RequestParam("file") MultipartFile file) {
        // Validate định dạng file .xlsx
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        // Service giờ trả về ImportResponse thay vì List<Faculty>
        ImportResponse report = facultyService.importExcel(file);

        // Trả về báo cáo chi tiết cho người dùng
        return ApiResponse.success(report, "Đã xử lý xong file dữ liệu.");
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        // Trỏ đường dẫn tới file Excel nằm trong thư mục resources
        Resource resource = new ClassPathResource("templates/Template_Import_Khoa.xlsx");

        // Kiểm tra xem có lỡ tay xóa mất file không
        if (!resource.exists()) {
            // Bạn có thể throw AppException ở đây nếu muốn
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        // Trả file về cho trình duyệt tải xuống
        return ResponseEntity.ok()
                // Header này báo cho trình duyệt biết đây là file đính kèm để tải về
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_Khoa.xlsx")
                // Khai báo định dạng file Excel
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}