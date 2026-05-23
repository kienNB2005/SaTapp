package ken.example.dekiru.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Map thẳng lên DB view: v_lecturer_week
 *
 * View đã WHERE session_date BETWEEN T2..CN tuần hiện tại sẵn rồi.
 * Dùng cho:
 *  - Thẻ "Tuần này": sessions_this_week, subjects_this_week
 *  - Panel "Lịch tuần này" bên phải dashboard
 */
@Entity
@Table(name = "v_lecturer_week")
@org.hibernate.annotations.Immutable
@Getter
public class VLecturerWeek {

    @Id
    @Column(name = "class_session_id")
    private Long classSessionId;

    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "status")
    private String status;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "period_start")
    private Integer periodStart;

    @Column(name = "period_end")
    private Integer periodEnd;

    @Column(name = "period_start_time")
    private LocalTime periodStartTime;

    @Column(name = "period_end_time")
    private LocalTime periodEndTime;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "class_code")
    private String classCode;

    @Column(name = "class_name")
    private String className;

    @Column(name = "room_code")
    private String roomCode;

    @Column(name = "lecturer_id")
    private Long lecturerId;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "makeup_for_id")
    private Long makeupForId;

    @Column(name = "original_session_date")
    private LocalDate originalSessionDate;

//
//    @Transient
//    @Column(name = "total_students")
//    private Integer totalStudents;

}
