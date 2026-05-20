package ken.example.dekiru.attendance.repository;

import jakarta.persistence.LockModeType;
import ken.example.dekiru.attendance.dto.AttendanceSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import ken.example.dekiru.attendance.entity.Attendance;
import ken.example.dekiru.attendance.entity.CheckoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByClassSession_Id(Long classSessionId);

    @Query("""
    SELECT a FROM Attendance a 
    JOIN FETCH a.student s 
    JOIN FETCH s.user u 
    LEFT JOIN FETCH a.editedBy 
    WHERE a.classSession.id = :classSessionId
    ORDER BY s.studentCode ASC
    """)
    List<Attendance> findByClassSession_Id(@Param("classSessionId") Long classSessionId);

    @Query("""
    SELECT new ken.example.dekiru.attendance.dto.AttendanceSummaryDto(
        a.id, s.studentCode, u.fullName, a.status, a.scannedAt,
        a.isLate, a.lateMinutes, a.checkedOutAt, a.leftEarly, a.gpsVerified
    )
    FROM Attendance a
    JOIN a.student s
    JOIN s.user u
    WHERE a.classSession.id = :classSessionId
    """)
    List<AttendanceSummaryDto> findAttendanceSummaryBySessionId(@org.springframework.data.repository.query.Param("classSessionId") Long classSessionId);
    Optional<Attendance> findByClassSession_IdAndStudent_Id(Long classSessionId, Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Attendance a WHERE a.classSession.id = :classSessionId AND a.student.id = :studentId")
    Optional<Attendance> findByClassSessionIdAndStudentIdWithLock(
            @Param("classSessionId") Long classSessionId,
            @Param("studentId") Long studentId
    );
    boolean existsByClassSession_IdAndDeviceIdAndStatusAndStudent_IdNot(Long classSessionId, String deviceId, Attendance.Status status, Long studentId);

    @Modifying
    @Query("""
    UPDATE Attendance a
    SET a.leftEarly = true,
        a.checkoutEvent = :event
    WHERE a.classSession.id = :sessionId
      AND a.status = 'present'
      AND a.checkedOutAt IS NULL
    """)
    void markLeftEarlyForSession(
            @Param("sessionId") Long sessionId,
            @Param("event") CheckoutEvent event
    );

    @Query(value = "SELECT " +
            "a.id AS attendanceId, " +
            "s.student_code AS studentCode, " +
            "u.full_name AS fullName, " +
            "vas.present_count AS presentCount, " +
            "vas.absent_count AS absentCount, " +
            "vas.excused_count AS excusedCount, " +
            "vas.late_count AS lateCount, " +
            "a.status AS status, " +
            "a.is_late AS isLate, " +
            "a.late_minutes AS lateMinutes, " +
            "a.left_early AS leftEarly, " +
            "a.note AS note " +
            "FROM attendance a " +
            "JOIN student s ON a.student_id = s.id " +
            "JOIN user u ON s.user_id = u.id " +
            "JOIN class_session cs ON a.class_session_id = cs.id " +
            "JOIN schedule sc ON cs.schedule_id = sc.id " +
            "LEFT JOIN v_attendance_summary vas " +
            "    ON vas.student_id = s.id " +
            "    AND vas.subject_id = sc.subject_id " +
            "    AND vas.semester_id = sc.semester_id " + // Đã fix lỗi duplicate sinh viên
            "WHERE a.class_session_id = :sessionId " +
            // --- CÁC ĐIỀU KIỆN LỌC ĐỘNG ---
            "AND (:search IS NULL OR s.student_code LIKE CONCAT('%', :search, '%') OR u.full_name LIKE CONCAT('%', :search, '%')) " +
            "AND (:dbStatus IS NULL OR a.status = :dbStatus) " +
            "AND (:isLate IS NULL OR a.is_late = :isLate) ",
            // --- CÂU ĐẾM TỔNG ĐỂ CHIA TRANG ---
            countQuery = "SELECT COUNT(a.id) " +
                    "FROM attendance a " +
                    "JOIN student s ON a.student_id = s.id " +
                    "JOIN user u ON s.user_id = u.id " +
                    "WHERE a.class_session_id = :sessionId " +
                    "AND (:search IS NULL OR s.student_code LIKE CONCAT('%', :search, '%') OR u.full_name LIKE CONCAT('%', :search, '%')) " +
                    "AND (:dbStatus IS NULL OR a.status = :dbStatus) " +
                    "AND (:isLate IS NULL OR a.is_late = :isLate)" +
                    "ORDER BY s.student_name ASC",
            nativeQuery = true)
    Page<AttendanceSummaryProjection> findManualAttendancePaginated(
            @Param("sessionId") Long sessionId,
            @Param("search") String search,
            @Param("dbStatus") String dbStatus,
            @Param("isLate") Boolean isLate,
            Pageable pageable);
}
