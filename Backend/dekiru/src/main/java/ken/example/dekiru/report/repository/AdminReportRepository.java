package ken.example.dekiru.report.repository;

import ken.example.dekiru.report.dto.AdminSchoolReportRowProjection;
import ken.example.dekiru.report.dto.AdminSchoolReportStatsProjection;
import ken.example.dekiru.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReportRepository extends JpaRepository<Schedule, Long> {

    @Query(value = """
        SELECT 
            schedule_id AS scheduleId,
            class_name AS className,
            subject_name AS subjectName,
            lecturer_name AS lecturerName,
            total_students AS totalStudents,
            completed_sessions AS completedSessions,
            total_sessions AS totalSessions,
            attendance_rate AS attendanceRate
        FROM v_admin_school_report
        WHERE semester_id = :semesterId
          AND (:facultyId IS NULL OR faculty_id = :facultyId)
          AND (:departmentId IS NULL OR department_id = :departmentId)
          AND (:search IS NULL OR class_name LIKE CONCAT('%', :search, '%') OR lecturer_name LIKE CONCAT('%', :search, '%') OR subject_name LIKE CONCAT('%', :search, '%'))
        ORDER BY class_name ASC
        """, nativeQuery = true)
    List<AdminSchoolReportRowProjection> getReportRows(
            @Param("semesterId") Long semesterId,
            @Param("facultyId") Long facultyId,
            @Param("departmentId") Long departmentId,
            @Param("search") String search
    );

    @Query(value = """
        SELECT 
            COUNT(schedule_id) AS totalClasses,
            COALESCE(AVG(attendance_rate), 100.0) AS avgAttendanceRate,
            COALESCE(SUM(CASE WHEN attendance_rate < :absentLimit THEN 1 ELSE 0 END), 0) AS underThresholdCount,
            COALESCE(SUM(completed_sessions), 0) AS totalSessionsTaught
        FROM v_admin_school_report
        WHERE semester_id = :semesterId
          AND (:facultyId IS NULL OR faculty_id = :facultyId)
          AND (:departmentId IS NULL OR department_id = :departmentId)
        """, nativeQuery = true)
    List<AdminSchoolReportStatsProjection> getReportStats(
            @Param("semesterId") Long semesterId,
            @Param("facultyId") Long facultyId,
            @Param("departmentId") Long departmentId,
            @Param("absentLimit") Double absentLimit
    );
}
