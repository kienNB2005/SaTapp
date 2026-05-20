package ken.example.dekiru.attendance.entity;
import ken.example.dekiru.security.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkout_event")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CheckoutEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    /** User (role = lecturer) đã nhấn "Yêu cầu check-out" */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triggered_by", nullable = false)
    private User triggeredBy;

    @CreationTimestamp
    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    /**
     * Deadline SV phải quét (thường triggered_at + 3–10 phút).
     * Scheduler so sánh NOW() để đóng event và đánh left_early.
     */
    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(length = 300)
    private String note;

    /**
     * NULL = event đang mở.
     * Ghi bởi Scheduler khi deadline_at < NOW() hoặc GV đóng thủ công.
     */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}