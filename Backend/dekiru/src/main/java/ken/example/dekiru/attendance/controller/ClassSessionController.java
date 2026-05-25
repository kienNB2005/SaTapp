package ken.example.dekiru.attendance.controller;

import ken.example.dekiru.attendance.dto.*;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.attendance.service.AttendanceService;
import ken.example.dekiru.attendance.service.AttendanceSseService;
import ken.example.dekiru.attendance.service.ClassSessionService;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
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
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassSessionController {

    ClassSessionService classSessionService;
    AttendanceSseService attendanceSseService;
    AttendanceService attendanceService;

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateSessionStatus(@PathVariable Long id, @RequestBody ken.example.dekiru.attendance.dto.UpdateSessionStatusRequest request) {
        String status = request.getStatus();
        if ("OPEN".equals(status)) {
            QrTokenResponse response = classSessionService.openClassSession(id);
            return ApiResponse.success(response, "Mở buổi học thành công, bắt đầu điểm danh (Check-in)");
        } else if ("CHECKING_OUT".equals(status)) {
            int mins = request.getCheckoutMinutes() != null ? request.getCheckoutMinutes() : 5;
            QrTokenResponse response = classSessionService.startCheckOutQr(id, mins);
            return ApiResponse.success(response, "Đã chuyển sang mã QR Check-out");
        } else if ("CLOSED".equals(status)) {
            classSessionService.closeClassSession(id);
            return ApiResponse.success(null, "Đóng buổi học thành công");
        }
        throw new AppException(ErrorCode.INVALID_SESSION_STATUS);
    }

    @PostMapping("/{id}/qr/refresh")
    public ApiResponse<QrTokenResponse> refreshQrToken(@PathVariable Long id) {
        QrTokenResponse response = classSessionService.refreshQrToken(id);
        return ApiResponse.success(response, "Làm mới mã QR thành công (" + response.getType() + ")");
    }



//    @GetMapping("/{id}/suggested-slots")
//    public ApiResponse<List<SuggestedSlotDto>> getSuggestedSlots(
//            @PathVariable Long id,
//            @RequestParam(required = false, defaultValue = "2") Integer weeks) {
//        List<SuggestedSlotDto> suggestions = classSessionService.getSuggestedSlots(id, weeks);
//        return ApiResponse.success(suggestions, "Lấy danh sách slot gợi ý thành công");
//    }

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

    @GetMapping
    public ApiResponse<List<ClassSessionListDto>> getSessionList(
            @RequestParam Long adminClassId,   // bắt buộc
            @RequestParam Long subjectId) // bắt buộc
    {
        List<ClassSessionListDto> list = classSessionService.getSessionListForClassAndSubject(adminClassId, subjectId);
        return ApiResponse.success(list, "Lấy danh sách buổi học thành công");
    }

    @GetMapping("/weekly")
    public ApiResponse<List<WeeklySessionDto>> getWeeklySessions(
            @RequestParam Long semesterId,
            @RequestParam Integer weekNumber) {
        List<WeeklySessionDto> result = classSessionService.getLecturerSessionsByWeek(semesterId, weekNumber);
        return ApiResponse.success(result, "Lấy thời khóa biểu tuần thành công");
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
