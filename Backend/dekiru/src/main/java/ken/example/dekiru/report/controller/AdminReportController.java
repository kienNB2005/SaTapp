package ken.example.dekiru.report.controller;

import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.report.dto.AdminSchoolReportResponse;
import ken.example.dekiru.report.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/admin/school-wide")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public ApiResponse<AdminSchoolReportResponse> getSchoolWideReport(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "75.0") Double absentLimit,
            @RequestParam(required = false) String search
    ) {
        AdminSchoolReportResponse response = adminReportService.getSchoolWideReport(semesterId, facultyId, departmentId, absentLimit, search);
        return ApiResponse.success(response, "Lấy báo cáo thành công");
    }
}
