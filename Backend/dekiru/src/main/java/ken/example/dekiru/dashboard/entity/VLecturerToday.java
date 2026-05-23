package ken.example.dekiru.dashboard.entity;
import jakarta.persistence.*;
import ken.example.dekiru.attendance.entity.ClassSession;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Map thẳng lên DB view: v_lecturer_today
 *
 * Chỉ dùng @Getter — view là readonly, không cần @Setter.
 * @Immutable báo Hibernate không bao giờ INSERT/UPDATE/DELETE.
 */
@Getter
@Entity
@Immutable
@Table(name = "v_lecturer_today")
public class VLecturerToday {

    @Id
    @Column(name = "class_session_id")
    private Long classSessionId;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ClassSession.Status status;                // scheduled | open | closed | cancelled

    @Column(name = "period_start")
    private Integer periodStart;

    @Column(name = "period_end")
    private Integer periodEnd;

    @Column(name = "period_start_time")
    private LocalTime periodStartTime;

    @Column(name = "period_end_time")
    private LocalTime periodEndTime;

    @Column(name = "late_threshold_min")
    private Integer lateThresholdMin;

    @Column(name = "max_late_min")
    private Integer maxLateMin;

    @Column(name = "total_sessions")        // "Buổi 5/15" — lấy từ schedule
    private Integer totalSessions;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "class_name")
    private String className;

    @Column(name = "room_code")
    private String roomCode;

    @Column(name = "building")
    private String building;

    @Column(name = "gps_radius_m")
    private Integer gpsRadiusM;

    @Column(name = "lecturer_id")
    private Long lecturerId;

    @Column(name = "total_students")
    private Integer totalStudents;

    @Column(name = "present_count")
    private Integer presentCount;

    @Column(name = "late_count")
    private Integer lateCount;

    @Column(name = "left_early_count")
    private Integer leftEarlyCount;

    @Column(name = "makeup_for_id")
    private Long makeupForId;

    @Column(name = "original_session_date")
    private LocalDate originalSessionDate;

}