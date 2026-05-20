package ken.example.dekiru.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Map thẳng lên DB view: v_attendance_summary
 *
 * Dùng cho thẻ "Chuyên cần TB".
 * View không có cột id đơn → dùng @IdClass với composite PK.
 */
@Getter
@Entity
@Immutable
@Table(name = "v_attendance_summary")
@IdClass(VAttendanceSummaryId.class)
public class VAttendanceSummary {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Id
    @Column(name = "semester_id")
    private Long semesterId;

    @Id
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "semester_name")
    private String semesterName;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "present_count")
    private Integer presentCount;

    @Column(name = "absent_count")
    private Integer absentCount;

    @Column(name = "excused_count")
    private Integer excusedCount;

    @Column(name = "late_count")
    private Integer lateCount;

    @Column(name = "left_early_count")
    private Integer leftEarlyCount;

    @Column(name = "attendance_rate")
    private Double attendanceRate;
}
