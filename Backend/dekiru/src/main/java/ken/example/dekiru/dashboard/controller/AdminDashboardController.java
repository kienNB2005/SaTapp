package ken.example.dekiru.dashboard.controller;

import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.dashboard.dto.AdminDashboardResponse;
import ken.example.dekiru.dashboard.service.AdminDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardController {

    AdminDashboardService adminDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public ApiResponse<AdminDashboardResponse> getAdminDashboard() {
        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();
        return ApiResponse.success(response, "Lấy dữ liệu dashboard thành công");
    }
}
