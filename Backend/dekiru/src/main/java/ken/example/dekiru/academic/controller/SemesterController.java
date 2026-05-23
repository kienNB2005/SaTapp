package ken.example.dekiru.academic.controller;

import ken.example.dekiru.academic.dto.CreateSemesterRequest;
import ken.example.dekiru.academic.dto.UpdateSemesterRequest;
import ken.example.dekiru.academic.dto.SemesterResponse;
import ken.example.dekiru.academic.service.SemesterService;
import ken.example.dekiru.common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SemesterController {

    SemesterService semesterService;

    @GetMapping
    public ApiResponse<List<SemesterResponse>> getAllSemesters() {
        List<SemesterResponse> semesters = semesterService.getAllSemesters();
        return ApiResponse.success(semesters, "Lấy danh sách học kỳ thành công");
    }

    @GetMapping("/{id}")
    public ApiResponse<SemesterResponse> getSemesterById(@PathVariable Long id) {
        SemesterResponse semester = semesterService.getSemesterById(id);
        return ApiResponse.success(semester, "Lấy thông tin học kỳ thành công");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SemesterResponse> createSemester(@RequestBody CreateSemesterRequest request) {
        SemesterResponse response = semesterService.createSemester(request);
        return ApiResponse.success(response, "Tạo học kỳ thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SemesterResponse> updateSemester(@PathVariable Long id, @RequestBody UpdateSemesterRequest request) {
        SemesterResponse response = semesterService.updateSemester(id, request);
        return ApiResponse.success(response, "Cập nhật học kỳ thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ApiResponse.success(null, "Xóa học kỳ thành công");
    }
}
