package ken.example.dekiru.student.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Entity
@Immutable
@Table(name = "v_student_schedule")
public class StudentSchedule {
    @Id
    @Column(name = "class_session_id")
    private Long classSessionId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "lecturer_name")
    private String lecturerName;

    @Column(name = "room_code")
    private String roomCode;

    @Column(name = "period_start_time")
    private LocalTime periodStartTime;

    @Column(name = "period_end_time")
    private LocalTime periodEndTime;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "session_status")
    private String sessionStatus;

    @Column(name = "attendance_status")
    private String attendanceStatus;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "original_session_date")
    private LocalDate originalSessionDate;
}