package ken.example.dekiru.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Map thẳng lên DB view: v_schedule_progress
 *
 * Dùng cho thanh tiến độ "Tiến độ học kỳ" và thẻ "Học kỳ này".
 */
@Getter
@Entity
@Immutable
@Table(name = "v_schedule_progress")
public class VScheduleProgress {

    @Id
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "class_name")
    private String className;

    @Column(name = "lecturer_id")
    private Long lecturerId;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "generated_sessions")
    private Integer generatedSessions;

    @Column(name = "closed_sessions")
    private Integer closedSessions;

    @Column(name = "open_sessions")
    private Integer openSessions;

    @Column(name = "upcoming_sessions")
    private Integer upcomingSessions;
}
