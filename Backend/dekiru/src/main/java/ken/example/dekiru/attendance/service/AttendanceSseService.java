package ken.example.dekiru.attendance.service;

import ken.example.dekiru.attendance.entity.Attendance;
import ken.example.dekiru.attendance.dto.AttendanceSummaryDto;
import ken.example.dekiru.attendance.repository.AttendanceRepository;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.common.config.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceSseService {

    @Value("${app.sse.timeout-ms:1800000}")
    Long sseTimeout;

    // sessionId -> danh sách emitter của các GV đang theo dõi buổi đó
    final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    final AttendanceRepository attendanceRepository;
    final ClassSessionRepository classSessionRepository;
    final SecurityUtils securityUtils;

    /**
     * GV subscribe để nhận update real-time.
     * Ngay khi subscribe, server gửi luôn snapshot hiện tại.
     */
    public SseEmitter subscribe(Long sessionId) {
        Long lecturerId = securityUtils.getCurrentLecturerId();

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        if (!session.getActualLecturer().getId().equals(lecturerId)) {
            throw new AppException(ErrorCode.NO_PERMISSION_ON_SESSION);
        }

        // Sử dụng timeout từ config (mặc định 30p nếu ko có config)
        SseEmitter emitter = new SseEmitter(sseTimeout);

        // Đăng ký emitter vào map
        emitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Dọn emitter khi kết nối kết thúc / lỗi
        Runnable cleanup = () -> removeEmitter(sessionId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            String msg = e.getMessage();
            // Nếu là lỗi ngắt kết nối bình thường từ phía Client, chỉ log DEBUG thay vì WARN
            if (msg != null && (msg.contains("disconnected client") || msg.contains("Broken pipe") || msg.contains("Connection reset"))) {
                log.debug("SSE disconnected client sessionId={}", sessionId);
            } else {
                log.warn("SSE error sessionId={}: {}", sessionId, e.getMessage());
            }
            cleanup.run();
        });

        // Gửi ngay snapshot hiện tại để client không phải chờ event đầu tiên
        try {
            List<AttendanceSummaryDto> snapshot = buildSnapshot(sessionId);
            emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(snapshot));
        } catch (IOException | IllegalStateException e) {
            log.warn("Failed to send initial snapshot to sessionId={}", sessionId);
            // Gửi lỗi về client nếu có thể trước khi hoàn thành
            try {
                emitter.send(SseEmitter.event().name("error").data("Lỗi khởi tạo dữ liệu"));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Gửi heartbeat định kỳ để giữ kết nối không bị timeout bởi Proxy/Load Balancer (Nginx)
     * và dọn dẹp các emitter đã hỏng.
     */
    @Scheduled(fixedDelayString = "${app.sse.heartbeat-interval-ms:25000}")
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        log.debug("Sending SSE heartbeats for {} sessions", emitters.size());
        emitters.forEach((sessionId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("ping")
                            .comment("keep-alive"));
                } catch (IOException | IllegalStateException e) {
                    log.debug("Removing broken emitter for session {}: {}", sessionId, e.getMessage());
                    removeEmitter(sessionId, emitter);
                }
            }
        });
    }

    /**
     * Gọi sau mỗi lần SV quét QR thành công (từ ClassSessionService).
     * Đẩy bản ghi điểm danh đã cập nhật tới tất cả GV đang subscribe.
     */
    public void pushAttendanceUpdate(Long sessionId, Attendance attendance) {
        CopyOnWriteArrayList<SseEmitter> sessionEmitters = emitters.get(sessionId);
        if (sessionEmitters == null || sessionEmitters.isEmpty()) return;

        AttendanceSummaryDto dto = toDto(attendance);

        for (SseEmitter emitter : sessionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("attendance-update")  // client lắng nghe event tên này
                        .data(dto));
            } catch (IOException | IllegalStateException e) {
                log.warn("Failed to push update to emitter, removing. sessionId={}", sessionId);
                removeEmitter(sessionId, emitter);
            }
        }
    }

    /**
     * Gọi khi GV đóng buổi học — báo client kết thúc stream.
     */
    public void pushSessionClosed(Long sessionId) {
        CopyOnWriteArrayList<SseEmitter> sessionEmitters = emitters.get(sessionId);
        if (sessionEmitters == null) return;

        for (SseEmitter emitter : sessionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("session-closed")
                        .data("Buổi học đã kết thúc"));
                emitter.complete();
            } catch (IOException | IllegalStateException e) {
                log.warn("Failed to send session-closed event. sessionId={}", sessionId);
                removeEmitter(sessionId, emitter);
            }
        }
        emitters.remove(sessionId);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private List<AttendanceSummaryDto> buildSnapshot(Long sessionId) {
        return attendanceRepository.findAttendanceSummaryBySessionId(sessionId);
    }

    private AttendanceSummaryDto toDto(Attendance a) {
        return new AttendanceSummaryDto(
                a.getId(),
                a.getStudent().getStudentCode(),
                a.getStudent().getUser().getFullName(),
                a.getStatus(),
                a.getScannedAt(),
                a.getIsLate(),
                a.getLateMinutes() != null ? (short) a.getLateMinutes() : null,
                a.getCheckedOutAt(),
                a.getLeftEarly(),
                a.getGpsVerified()
        );
    }

    private void removeEmitter(Long sessionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(sessionId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(sessionId);
        }
    }

    /**
     * Gọi từ CheckoutEventScheduler sau khi markLeftEarlyForSession().
     * Nhiều SV bị cập nhật cùng lúc nên gửi lại toàn bộ snapshot thay vì từng record.
     */
    public void pushLeftEarlyUpdate(Long sessionId) {
        CopyOnWriteArrayList<SseEmitter> sessionEmitters = emitters.get(sessionId);
        if (sessionEmitters == null || sessionEmitters.isEmpty()) return;

        List<AttendanceSummaryDto> snapshot = buildSnapshot(sessionId);

        for (SseEmitter emitter : sessionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("left-early-update")   // client phân biệt với "attendance-update"
                        .data(snapshot));
            } catch (IOException | IllegalStateException e) {
                log.warn("Failed to push left-early-update. sessionId={}", sessionId);
                removeEmitter(sessionId, emitter);
            }
        }
    }
}