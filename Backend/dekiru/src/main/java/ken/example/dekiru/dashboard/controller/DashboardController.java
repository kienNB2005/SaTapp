package ken.example.dekiru.dashboard.controller;

import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.dashboard.dto.DashboardResponse;
import ken.example.dekiru.dashboard.service.DashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * GET /api/v1/dashboard
 * GET /api/v1/dashboard?semesterId=2   ← FE dropdown chọn học kỳ
 *
 * DashboardResponse đã chuyển sang package dto —
 * Controller chỉ còn nhận HTTP request và trả ResponseEntity.
 */
@RestController
@RequestMapping("/api/v1/lecturers/me/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    private final DashboardService service;
    private final SecurityUtils securityUtils;


    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false) Long semesterId) {

        Long lecturerId = securityUtils.getCurrentLecturerId();
        return ResponseEntity.ok(service.getDashboard(lecturerId, semesterId));
    }
}
