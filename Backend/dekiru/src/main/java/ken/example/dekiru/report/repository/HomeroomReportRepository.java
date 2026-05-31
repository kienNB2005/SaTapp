package ken.example.dekiru.report.repository;

import ken.example.dekiru.report.dto.LecturerReportStudentProjection;
import ken.example.dekiru.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeroomReportRepository extends JpaRepository<Student, Long> {

    @Query(value = """
        SELECT 
            s.student_code AS studentCode,
            u.full_name AS studentName,
            COALESCE(SUM(vas.present_count), 0) AS presentCount,
            COALESCE(SUM(vas.absent_count), 0) AS absentCount,
            COALESCE(SUM(vas.excused_count), 0) AS excusedCount,
            COALESCE(SUM(vas.late_count), 0) AS lateCount,
            COALESCE(SUM(vas.left_early_count), 0) AS leftEarlyCount,
            COALESCE(SUM(vas.total_sessions), 0) AS totalSessions,
            ROUND(
                (COALESCE(SUM(vas.present_count), 0) + COALESCE(SUM(vas.excused_count), 0)) * 100.0 / 
                NULLIF(SUM(vas.finished_sessions), 0)
            , 1) AS attendanceRate
        FROM student s
        JOIN user u ON s.user_id = u.id
        LEFT JOIN v_attendance_summary vas ON vas.student_id = s.id 
            AND vas.semester_id = :semesterId
            AND (:subjectId IS NULL OR vas.subject_id = :subjectId)
        WHERE s.admin_class_id = :adminClassId
        GROUP BY s.id, s.student_code, u.full_name
        ORDER BY s.student_code ASC
        """, nativeQuery = true)
    List<LecturerReportStudentProjection> getStudentReportData(
            @Param("semesterId") Long semesterId,
            @Param("subjectId") Long subjectId,
            @Param("adminClassId") Long adminClassId
    );

    @Query(value = """
        SELECT 
            SUM(CASE WHEN cs.makeup_for_id IS NULL THEN 1 ELSE 0 END) AS totalSessions,
            SUM(CASE WHEN cs.status = 'closed' THEN 1 ELSE 0 END) AS finishedSessions
        FROM class_session cs
        JOIN schedule sc ON cs.schedule_id = sc.id
        WHERE sc.semester_id = :semesterId
          AND (:subjectId IS NULL OR sc.subject_id = :subjectId)
          AND sc.admin_class_id = :adminClassId
        """, nativeQuery = true)
    List<Object[]> getSessionStats(
            @Param("semesterId") Long semesterId,
            @Param("subjectId") Long subjectId,
            @Param("adminClassId") Long adminClassId
    );
}
