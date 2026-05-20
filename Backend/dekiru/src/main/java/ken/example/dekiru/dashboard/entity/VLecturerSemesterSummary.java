package ken.example.dekiru.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Map thẳng lên DB view: v_lecturer_semester_summary
 *
 * View tổng hợp sẵn tất cả số liệu cho 4 thẻ dashboard theo
 * (lecturer_id, semester_id) — 1 query duy nhất thay vì 4 query riêng.
 *
 * PK tổ hợp: lecturer_id + semester_id
 */
@Getter
@Entity
@Table(name = "v_lecturer_semester_summary")
@IdClass(VLecturerSemesterSummary.PK.class)
@org.hibernate.annotations.Immutable
public class VLecturerSemesterSummary {

    // ─── Composite PK ───────────────────────────────────────────

    public static class PK implements Serializable {
        private Long lecturerId;
        private Long semesterId;

        public PK() {}
        public PK(Long lecturerId, Long semesterId) {
            this.lecturerId = lecturerId;
            this.semesterId = semesterId;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PK that)) return false;
            return Objects.equals(lecturerId, that.lecturerId)
                && Objects.equals(semesterId, that.semesterId);
        }

        @Override
        public int hashCode() { return Objects.hash(lecturerId, semesterId); }
    }

    // ─── Fields ─────────────────────────────────────────────────

    @Id
    @Column(name = "lecturer_id")
    private Long lecturerId;

    @Id
    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "semester_name")
    private String semesterName;

    /** Thẻ "Tuần này" — số buổi trong tuần hiện tại */
    @Column(name = "sessions_this_week")
    private Integer sessionsThisWeek;

    /** Thẻ "Tuần này" — số môn khác nhau trong tuần */
    @Column(name = "subjects_this_week")
    private Integer subjectsThisWeek;

    /** Thẻ "Học kỳ này" — tổng buổi toàn học kỳ */
    @Column(name = "total_sessions_semester")
    private Integer totalSessionsSemester;

    /** Thẻ "Học kỳ này" — số buổi đã xong (status = closed) */
    @Column(name = "closed_sessions")
    private Integer closedSessions;

    /** Thẻ "Học kỳ này" — số buổi còn lại (status = scheduled) */
    @Column(name = "remaining_sessions")
    private Integer remainingSessions;

    /** Thẻ "Chuyên cần TB" — % trung bình toàn lớp */
    @Column(name = "avg_attendance_rate")
    private Double avgAttendanceRate;

    // ─── Getters ────────────────────────────────────────────────
}
