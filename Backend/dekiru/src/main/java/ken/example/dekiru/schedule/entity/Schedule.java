package ken.example.dekiru.schedule.entity;
import ken.example.dekiru.academic.entity.Subject;
import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.academic.entity.Room;
import ken.example.dekiru.academic.entity.AdministrativeClass;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule")
@NamedStoredProcedureQuery(
    name = "generate_sessions_for_schedule",
    procedureName = "generate_sessions_for_schedule",
    parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_schedule_id", type = Long.class)
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_class_id", nullable = false)
    private AdministrativeClass adminClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /**
     * Thứ học: 2=T2 ... 7=T7, 8=CN.
     * CHECK (day_of_week BETWEEN 2 AND 8) enforce ở DB.
     */
    @Column(name = "day_of_week", nullable = false)
    private Byte dayOfWeek;

    /** Tiết bắt đầu (1–15) */
    @Column(name = "period_start", nullable = false)
    private Byte periodStart;

    /** Tiết kết thúc (1–15) */
    @Column(name = "period_end", nullable = false)
    private Byte periodEnd;

    /** Tuần bắt đầu học trong học kỳ */
    @Column(name = "week_start", nullable = false)
    @Builder.Default
    private Byte weekStart = 1;

    /** Tuần kết thúc học trong học kỳ */
    @Column(name = "week_end", nullable = false)
    private Byte weekEnd;

    /**
     * Cập nhật bởi Stored Procedure sau khi sinh ClassSession.
     * Dùng Short tránh tràn TINYINT.
     */
    @Column(name = "total_sessions", nullable = false)
    @Builder.Default
    private Byte totalSessions = 0;

    /** SV muộn ≤ N phút → present + is_late = true */
    @Column(name = "late_threshold_min", nullable = false)
    @Builder.Default
    private Byte lateThresholdMin = 15;

    /** SV muộn > N phút → server từ chối (TOO_LATE) */
    @Column(name = "max_late_min", nullable = false)
    @Builder.Default
    private Byte maxLateMin = 30;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
