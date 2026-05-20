package ken.example.dekiru.attendance.controller;

import ken.example.dekiru.attendance.dto.*;
import ken.example.dekiru.attendance.service.AttendanceService;
import ken.example.dekiru.attendance.service.AttendanceSseService;
import ken.example.dekiru.attendance.service.ClassSessionService;
import ken.example.dekiru.common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassSessionController {

    ClassSessionService classSessionService;
    AttendanceSseService attendanceSseService;
    AttendanceService attendanceService;

    @PostMapping("/attend")
    public ApiResponse<Void> studentAttend(@RequestBody StudentAttendRequest request) {
        classSessionService.studentAttend(request);
        return ApiResponse.success(null, "Điểm danh thành công");
    }
    @PostMapping("/{id}/open")
    public ApiResponse<QrTokenResponse> openSession(@PathVariable Long id) {
        QrTokenResponse response = classSessionService.openClassSession(id);
        return ApiResponse.success(response, "Mở buổi học thành công, bắt đầu điểm danh (Check-in)");
    }

    @PostMapping("/{id}/checkout/start")
    public ApiResponse<QrTokenResponse> startCheckOutQr(@PathVariable Long id, @RequestParam(required = false, defaultValue = "5") int checkoutMinutes) {
        QrTokenResponse response = classSessionService.startCheckOutQr(id, checkoutMinutes);
        return ApiResponse.success(response, "Đã chuyển sang mã QR Check-out");
    }

    @PutMapping("/{id}/qr/refresh")
    public ApiResponse<QrTokenResponse> refreshQrToken(@PathVariable Long id) {
        QrTokenResponse response = classSessionService.refreshQrToken(id);
        return ApiResponse.success(response, "Làm mới mã QR thành công (" + response.getType() + ")");
    }

    @PostMapping("/{id}/close")
    public ApiResponse<Void> closeSession(@PathVariable Long id) {
        classSessionService.closeClassSession(id);
        return ApiResponse.success(null, "Đóng buổi học thành công");
    }


    /**
     * GV subscribe SSE để nhận danh sách điểm danh real-time.
     *
     * GET /api/v1/sessions/{id}/attendances/stream
     *
     * Event types client nhận:
     *   - "snapshot"          → List<AttendanceSummaryDto> toàn bộ khi mới kết nối
     *   - "attendance-update" → AttendanceSummaryDto của SV vừa quét
     *   - "session-closed"    → String báo buổi học kết thúc, client đóng kết nối
     */
    @GetMapping(value = "/{id}/attendances/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAttendances(@PathVariable Long id) {
        return attendanceSseService.subscribe(id);
    }
    @GetMapping("/{id}")
    public ApiResponse<ClassSessionDetailDto> getSessionDetail(@PathVariable Long id) {
        ClassSessionDetailDto detail = classSessionService.getSessionDetail(id);
        return ApiResponse.success(detail, "Lấy thông tin buổi học thành công");
    }

    @GetMapping("/list")
    public ApiResponse<List<ClassSessionListDto>> getSessionList(
            @RequestParam Long adminClassId,   // bắt buộc
            @RequestParam Long subjectId) // bắt buộc
    {
        List<ClassSessionListDto> list = classSessionService.getSessionListForClassAndSubject(adminClassId, subjectId);
        return ApiResponse.success(list, "Lấy danh sách buổi học thành công");
    }

    @GetMapping("/filter/admin-classes")
    public ApiResponse<List<DropdownOption>> getAdminClassesFilter() {
        List<DropdownOption> classes = classSessionService.getAdminClassesForLecturer();
        return ApiResponse.success(classes, "Lấy danh sách lớp hành chính thành công");
    }

    @GetMapping("/filter/subjects")
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


    @GetMapping("/{sessionId}/attendances")
    public ApiResponse<Page<AttendanceListDto>> getManualAttendanceList(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String uiStatus,
            // Cấu hình phân trang mặc định: 40sv/trang, sắp xếp theo mã SV tăng dần
            @PageableDefault(size = 40) Pageable pageable) {

        Page<AttendanceListDto> result = attendanceService.getAttendanceListForEdit(sessionId, search, uiStatus, pageable);
        return ApiResponse.success(result, "Lấy danh sách điểm danh thành công");
    }

    @PutMapping("/{sessionId}/attendances")
    public ApiResponse<Void> updateAttendanceBatch(
            @PathVariable Long sessionId,
            @RequestBody AttendanceBatchUpdateRequest request) {
        attendanceService.updateAttendanceBatch(sessionId, request);
        return ApiResponse.success(null, "Cập nhật điểm danh thành công");
    }
}
