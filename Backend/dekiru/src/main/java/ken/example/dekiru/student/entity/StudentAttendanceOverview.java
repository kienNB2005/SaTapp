package ken.example.dekiru.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Maps to view: v_student_attendance_overview
 * 1 dòng = 1 sinh viên × học kỳ đang active (is_active = 1)
 * Cấp dữ liệu cho thẻ "Tổng quan" đầu màn hình chuyên cần.
 */
@Getter
@Entity
@Immutable
@Table(name = "v_student_attendance_overview")
public class StudentAttendanceOverview {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "admin_class_code")
    private String adminClassCode;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "semester_name")
    private String semesterName;

    /** Tổng buổi học đã kết thúc (status = 'closed'), không tính cancelled */
    @Column(name = "total_passed")
    private Integer totalPassed;

    /** Số buổi có mặt (status = 'present', kể cả đi muộn) */
    @Column(name = "total_present")
    private Integer totalPresent;

    /** Số buổi vắng không phép (status = 'absent') */
    @Column(name = "total_absent")
    private Integer totalAbsent;

    /** Số buổi vắng có phép (status = 'excused') */
    @Column(name = "total_excused")
    private Integer totalExcused;

    /** Số buổi đi muộn (present + is_late = 1) */
    @Column(name = "total_late")
    private Integer totalLate;

    /** Số buổi về sớm (present + left_early = 1) */
    @Column(name = "total_leave_early")
    private Integer totalLeaveEarly;

    /**
     * Tỉ lệ chuyên cần % = (present + excused) / total_passed * 100
     * excused được tính hợp lệ — không bị phạt chuyên cần
     */
    @Column(name = "attendance_rate_pct")
    private Double attendanceRatePct;
}