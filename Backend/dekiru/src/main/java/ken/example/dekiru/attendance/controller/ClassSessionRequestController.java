package ken.example.dekiru.attendance.controller;

import ken.example.dekiru.attendance.dto.ApproveSessionRequest;
import ken.example.dekiru.attendance.dto.CancelSessionRequest;
import ken.example.dekiru.attendance.dto.MakeupSessionRequest;
import ken.example.dekiru.attendance.entity.ClassSessionRequest;
import ken.example.dekiru.attendance.service.ClassSessionRequestService;
import ken.example.dekiru.common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/session-requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassSessionRequestController {

    ClassSessionRequestService requestService;

    // --- LECTURER API ---
    @PostMapping("/{sessionId}/cancel")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<ClassSessionRequest> createCancelRequest(
            @PathVariable Long sessionId,
            @RequestBody CancelSessionRequest request) {
        ClassSessionRequest result = requestService.createCancelRequest(sessionId, request);
        return ApiResponse.success(result, "Đã gửi yêu cầu hủy buổi học");
    }

    @PostMapping("/{originalSessionId}/makeup")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<ClassSessionRequest> createMakeupRequest(
            @PathVariable Long originalSessionId,
            @RequestBody MakeupSessionRequest request) {
        ClassSessionRequest result = requestService.createMakeupRequest(originalSessionId, request);
        return ApiResponse.success(result, "Đã gửi yêu cầu tạo lịch học bù");
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<List<ClassSessionRequest>> getMyRequests() {
        List<ClassSessionRequest> result = requestService.getMyRequests();
        return ApiResponse.success(result, "Lấy danh sách thành công");
    }

    // --- ADMIN API ---
    @GetMapping("/admin/pending-cancels")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ClassSessionRequest>> getPendingCancelRequests() {
        List<ClassSessionRequest> result = requestService.getPendingCancelRequests();
        return ApiResponse.success(result, "Lấy danh sách thành công");
    }

    @GetMapping("/admin/pending-makeups")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ClassSessionRequest>> getPendingMakeupRequests() {
        List<ClassSessionRequest> result = requestService.getPendingMakeupRequests();
        return ApiResponse.success(result, "Lấy danh sách thành công");
    }

    @PostMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassSessionRequest> approveRequest(
            @PathVariable Long id,
            @RequestParam String type) { // type = "cancel" or "makeup"
        ClassSessionRequest result = requestService.approveRequest(id, type);
        return ApiResponse.success(result, "Phê duyệt thành công");
    }

    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassSessionRequest> rejectRequest(
            @PathVariable Long id,
            @RequestParam String type, // type = "cancel" or "makeup"
            @RequestBody ApproveSessionRequest request) {
        ClassSessionRequest result = requestService.rejectRequest(id, type, request);
        return ApiResponse.success(result, "Đã từ chối yêu cầu");
    }
}
