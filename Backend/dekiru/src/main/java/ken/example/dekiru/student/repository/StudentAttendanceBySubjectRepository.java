package ken.example.dekiru.student.repository;

import ken.example.dekiru.student.entity.StudentAttendanceBySubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentAttendanceBySubjectRepository
        extends JpaRepository<StudentAttendanceBySubject, Long> {

    /**
     * Lấy danh sách chuyên cần từng môn của sinh viên trong học kỳ đang active.
     * Thứ tự: danger → warning → safe, sau đó theo tên môn.
     * Frontend nhận list đã sort sẵn, không cần sort thêm.
     */
    @Query(value = """
        SELECT *
        FROM v_student_attendance_by_subject
        WHERE student_id = :studentId
          AND (:semesterId IS NULL OR semester_id = :semesterId)
          AND (:semesterId IS NOT NULL OR semester_id = (SELECT id FROM semester WHERE is_active = 1 LIMIT 1))
        ORDER BY
            FIELD(attendance_status, 'danger', 'warning', 'safe'),
            subject_name
        """, nativeQuery = true)
    List<StudentAttendanceBySubject> findByStudentIdAndSemesterId(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId);

    /**
     * Lọc riêng các môn đang trong ngưỡng nguy hiểm (cảnh báo cấm thi).
     * Dùng cho badge thông báo hoặc widget cảnh báo nhanh.
     */
    @Query(value = """
            SELECT *
            FROM v_student_attendance_by_subject
            WHERE student_id = :studentId
              AND is_danger = 1
            ORDER BY absent_count DESC
            """, nativeQuery = true)
    List<StudentAttendanceBySubject> findDangerSubjectsByStudentId(
            @Param("studentId") Long studentId);
}