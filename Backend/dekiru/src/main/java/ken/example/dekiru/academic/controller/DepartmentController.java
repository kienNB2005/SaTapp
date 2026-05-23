package ken.example.dekiru.academic.controller;

import ken.example.dekiru.academic.dto.CreateDepartmentRequest;
import ken.example.dekiru.academic.dto.UpdateDepartmentRequest;
import ken.example.dekiru.academic.dto.DepartmentResponse;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepartmentController {

    DepartmentService departmentService;

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> getAllDepartments() {
        List<DepartmentResponse> departments = departmentService.getAllDepartments();
        return ApiResponse.success(departments, "Lấy danh sách bộ môn thành công");
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse department = departmentService.getDepartmentById(id);
        return ApiResponse.success(department, "Lấy thông tin bộ môn thành công");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> createDepartment(@RequestBody CreateDepartmentRequest request) {
        DepartmentResponse department = departmentService.createDepartment(request);
        return ApiResponse.success(department, "Thêm mới bộ môn thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> updateDepartment(@PathVariable Long id, @RequestBody UpdateDepartmentRequest request) {
        DepartmentResponse department = departmentService.updateDepartment(id, request);
        return ApiResponse.success(department, "Cập nhật bộ môn thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ApiResponse.success(null, "Xóa bộ môn thành công");
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResponse> importDepartments(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }
        ImportResponse report = departmentService.importExcel(file);
        return ApiResponse.success(report, "Đã xử lý xong file dữ liệu.");
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        // Trỏ đường dẫn tới file Excel nằm trong thư mục resources
        Resource resource = new ClassPathResource("templates/Template_Import_Nganh.xlsx");

        // Kiểm tra xem có lỡ tay xóa mất file không
        if (!resource.exists()) {
            // Bạn có thể throw AppException ở đây nếu muốn
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        // Trả file về cho trình duyệt tải xuống
        return ResponseEntity.ok()
                // Header này báo cho trình duyệt biết đây là file đính kèm để tải về
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_Nganh.xlsx")
                // Khai báo định dạng file Excel
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
