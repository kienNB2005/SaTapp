package ken.example.dekiru.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Maps to view: v_student_attendance_by_subject
 * 1 dòng = 1 sinh viên × 1 môn học × học kỳ đang active
 * Cấp dữ liệu cho danh sách thẻ môn học bên dưới màn hình chuyên cần.
 *
 * Composite PK (student_id, subject_id) — dùng @IdClass vì Hibernate
 * yêu cầu @Id duy nhất trên view entity. Ở đây dùng subject_id làm @Id
 * vì mỗi query đã WHERE student_id = ? nên không bao giờ trùng key trong context.
 */
@Getter
@Entity
@Immutable
@Table(name = "v_student_attendance_by_subject")
public class StudentAttendanceBySubject {

    @Id
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "lecturer_name")
    private String lecturerName;

    /** Tổng buổi của môn (không tính cancelled) */
    @Column(name = "total_sessions")
    private Integer totalSessions;

    /** Số buổi đã kết thúc (closed) */
    @Column(name = "passed_sessions")
    private Integer passedSessions;

    /** Số buổi chưa đến (scheduled / open) */
    @Column(name = "remaining_sessions")
    private Integer remainingSessions;

    @Column(name = "present_count")
    private Integer presentCount;

    @Column(name = "absent_count")
    private Integer absentCount;

    @Column(name = "excused_count")
    private Integer excusedCount;

    @Column(name = "late_count")
    private Integer lateCount;

    @Column(name = "leave_early_count")
    private Integer leaveEarlyCount;

    /** Tỉ lệ chuyên cần % theo môn */
    @Column(name = "attendance_rate_pct")
    private Double attendanceRatePct;

    /** Ngưỡng vắng tối đa = FLOOR(total_sessions * 0.2) */
    @Column(name = "max_absent_allowed")
    private Integer maxAbsentAllowed;

    /** 1 nếu absent_count > max_absent_allowed → hiện banner cảnh báo đỏ */
    @Column(name = "is_danger")
    private Boolean isDanger;

    /**
     * Nhãn tổng kết: "safe" | "warning" | "danger"
     * safe    : rate >= 80%
     * warning : 60% <= rate < 80%
     * danger  : rate < 60% hoặc vượt ngưỡng vắng
     */
    @Column(name = "attendance_status")
    private String attendanceStatus;
}