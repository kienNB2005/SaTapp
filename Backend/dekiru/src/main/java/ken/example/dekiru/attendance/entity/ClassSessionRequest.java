package ken.example.dekiru.attendance.entity;

import jakarta.persistence.*;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.academic.entity.Room;
import ken.example.dekiru.security.entity.User;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_session_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSessionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_status", nullable = false)
    @Builder.Default
    private RequestStatus cancelStatus = RequestStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "makeup_status")
    private RequestStatus makeupStatus;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "makeup_date")
    private LocalDate makeupDate;

    @Column(name = "makeup_period_start")
    private Integer makeupPeriodStart;

    @Column(name = "makeup_period_end")
    private Integer makeupPeriodEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_room_id")
    private Room makeupRoom;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
}
