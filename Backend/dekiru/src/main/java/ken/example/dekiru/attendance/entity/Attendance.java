package ken.example.dekiru.attendance.entity;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.student.entity.Student;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_att_session_student",
                columnNames = {"class_session_id", "student_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Attendance {

    public enum Status {
        present, absent, excused
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * NULL khi pre-insert (mặc định absent toàn lớp lúc mở buổi).
     * Điền khi SV thực sự quét QR.
     */
    @Column(name = "device_id", length = 255)
    private String deviceId;

    // --- Trạng thái ---

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('present','absent','excused')", nullable = false)
    @Builder.Default
    private Status status = Status.absent;

    // --- Check-in ---

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    /** TRUE nếu scanned_at - period_start_time > late_threshold_min */
    @Column(name = "is_late", nullable = false)
    @Builder.Default
    private Boolean isLate = false;

    /**
     * Số phút muộn lúc check-in.
     * 0 = đúng giờ, NULL = vắng.
     */
    @Column(name = "late_minutes")
    private Short lateMinutes;

    // --- GPS check-in ---

    @Column(name = "scan_lat", precision = 10, scale = 7)
    private BigDecimal scanLat;

    @Column(name = "scan_lng", precision = 10, scale = 7)
    private BigDecimal scanLng;

    @Column(name = "distance_m")
    private Short distanceM;

    /**
     * TRUE = trong bán kính hợp lệ.
     * FALSE = ngoài bán kính nhưng GV đã override.
     * NULL = GPS bị tắt cho buổi này.
     */
    @Column(name = "gps_verified")
    private Boolean gpsVerified;

    // --- Check-out ---

    /**
     * FK sang checkout_event.id — event mà SV đã quét.
     * NULL = SV chưa check-out.
     * ON DELETE SET NULL — xóa event không mất bản ghi điểm danh.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_event_id")
    private CheckoutEvent checkoutEvent;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "checkout_lat", precision = 10, scale = 7)
    private BigDecimal checkoutLat;

    @Column(name = "checkout_lng", precision = 10, scale = 7)
    private BigDecimal checkoutLng;

    /**
     * TRUE = có checkout_event nhưng SV không quét trước deadline.
     * Không thay đổi status (vẫn present).
     */
    @Column(name = "left_early", nullable = false)
    @Builder.Default
    private Boolean leftEarly = false;

    // --- Chỉnh sửa thủ công ---

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** NULL = chưa bị sửa tay */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by")
    private User editedBy;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}