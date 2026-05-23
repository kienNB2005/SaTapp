package ken.example.dekiru.schedule.controller;

import ken.example.dekiru.schedule.dto.ScheduleExcelDTO;
import ken.example.dekiru.schedule.dto.SchedulePreviewResponse;
import ken.example.dekiru.schedule.dto.ScheduleResponse;
import ken.example.dekiru.schedule.service.ScheduleService;
import ken.example.dekiru.common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;

import ken.example.dekiru.common.config.SecurityUtils;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduleController {

    ScheduleService scheduleService;
    SecurityUtils securityUtils;

    /**
     * Lấy danh sách TKB cá nhân của giảng viên đang đăng nhập.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<Page<ScheduleResponse>> getMySchedules(
            @RequestParam(required = false) Long semesterId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        Page<ScheduleResponse> page = scheduleService.getSchedules(
                semesterId, null, null, lecturerId, null, pageable);
        return ApiResponse.success(page);
    }

    /**
     * Lấy danh sách TKB — phân trang, tìm kiếm, lọc.
     * Nếu không truyền semesterId → tự lấy học kỳ đang active.
     * Frontend: nếu totalElements == 0 → hiện import, nếu > 0 → hiện danh sách.
     */
    @GetMapping
    public ApiResponse<Page<ScheduleResponse>> getSchedules(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long adminClassId,
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(required = false) Integer dayOfWeek,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ScheduleResponse> page = scheduleService.getSchedules(
                semesterId, search, adminClassId, lecturerId, dayOfWeek, pageable);
        return ApiResponse.success(page);
    }

    /**
     * Tải file mẫu Excel cho TKB
     */
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        Resource resource = new ClassPathResource("templates/Template_Import_TKB.xlsx");

        if (!resource.exists()) {
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_TKB.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    /**
     * Bước 1: Preview — Upload file Excel, validate và trả về danh sách xem trước
     */
    @PostMapping("/import/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SchedulePreviewResponse>> previewImport(@RequestParam("file") MultipartFile file) {
        List<SchedulePreviewResponse> response = scheduleService.previewImportSchedule(file);
        return ApiResponse.success(response, "Preview danh sách import thời khóa biểu");
    }

    /**
     * Bước 2: Confirm — Lưu các dòng hợp lệ vào DB
     */
    @PostMapping("/import/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> confirmImport(
            @RequestParam("semesterId") Long semesterId,
            @RequestBody List<ScheduleExcelDTO> schedules) {
        Map<String, Object> stats = scheduleService.confirmImportSchedule(semesterId, schedules);
        return ApiResponse.success(stats, "Import thời khóa biểu thành công");
    }
}
