package ken.example.dekiru.student.repository;

import ken.example.dekiru.student.entity.StudentAttendanceOverview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentAttendanceOverviewRepository
        extends JpaRepository<StudentAttendanceOverview, Long> {

    /**
     * Lấy tổng quan chuyên cần của sinh viên trong học kỳ đang active.
     * View đã tự filter is_active = 1 nên chỉ cần truyền studentId.
     */
    @Query(value = """
        SELECT *
        FROM v_student_attendance_overview
        WHERE student_id = :studentId
          AND (:semesterId IS NULL OR semester_id = :semesterId)
          AND (:semesterId IS NOT NULL OR semester_id = (SELECT id FROM semester WHERE is_active = 1 LIMIT 1))
        """, nativeQuery = true)
    Optional<StudentAttendanceOverview> findByStudentIdAndSemesterId(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId);
}