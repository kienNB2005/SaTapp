package ken.example.dekiru.student.repository;
import ken.example.dekiru.student.entity.StudentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudentScheduleRepository extends JpaRepository<StudentSchedule, Long> {

    // Lấy theo tuần, mặc định tuần hiện tại truyền từ Service
    @Query(value = """
        SELECT * FROM v_student_schedule
        WHERE student_id = :studentId
        AND session_date BETWEEN :startDate AND :endDate
        ORDER BY session_date, period_start_time
        """, nativeQuery = true)
    List<StudentSchedule> findByStudentIdAndWeek(
        @Param("studentId") Long studentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}