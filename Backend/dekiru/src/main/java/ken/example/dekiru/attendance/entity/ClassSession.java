package ken.example.dekiru.attendance.entity;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.academic.entity.Room;
import ken.example.dekiru.schedule.entity.Schedule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_session",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_session_schedule_date_period",
                columnNames = {"schedule_id", "session_date", "actual_period_start"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClassSession {

    public enum Status {
        scheduled, open, closed, cancelled
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    /**
     * Copy từ schedule khi Stored Procedure sinh buổi.
     * Có thể thay đổi khi đổi phòng hoặc tạo buổi bù.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actual_room_id", nullable = false)
    private Room actualRoom;

    /**
     * Copy từ schedule khi Stored Procedure sinh buổi.
     * Có thể khác GV gốc trong trường hợp dạy thay.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actual_lecturer_id", nullable = false)
    private Lecturer actualLecturer;

    @Column(name = "actual_period_start", nullable = false)
    private Byte actualPeriodStart;

    @Column(name = "actual_period_end", nullable = false)
    private Byte actualPeriodEnd;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    /**
     * Thứ tự buổi trong môn (1..N).
     * Short thay TINYINT để tránh tràn.
     */
    @Column(name = "session_number", nullable = false)
    private Byte sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('scheduled','open','closed','cancelled')", nullable = false)
    @Builder.Default
    private Status status = Status.scheduled;

    // --- QR Token ---

    /** HMAC-SHA256. Server ghi đè mỗi 60s. Chỉ có giá trị khi status = open. */
    @Column(name = "qr_token", length = 255)
    private String qrToken;

    @Column(name = "qr_expires_at")
    private LocalDateTime qrExpiresAt;

    // --- GPS ---

    /** GV có thể tắt khi tín hiệu yếu */
    @Column(name = "gps_enabled", nullable = false)
    @Builder.Default
    private Boolean gpsEnabled = true;

    // --- Thời gian mở / đóng ---

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // --- Hủy buổi ---

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // --- Buổi bù (self-referencing) ---

    /**
     * NULL = buổi học thường.
     * Có giá trị = buổi dạy bù, trỏ về ClassSession bị hủy.
     * ON DELETE SET NULL — xóa buổi gốc không xóa buổi bù.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_for_id")
    private ClassSession makeupFor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}