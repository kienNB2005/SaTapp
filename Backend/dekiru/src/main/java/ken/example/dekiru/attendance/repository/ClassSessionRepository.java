package ken.example.dekiru.attendance.repository;
import ken.example.dekiru.attendance.entity.Attendance;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.schedule.entity.Schedule;

import jakarta.persistence.LockModeType;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.attendance.dto.ClassSessionListDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
     long countByScheduleId(Long scheduleId);
     boolean existsByScheduleId(Long scheduleId);
    boolean existsBySchedule_SemesterIdAndStatusIn(Long semesterId, Collection<ClassSession.Status> statuses);
    long countByActualRoom_Id(Long roomId);
    boolean existsByActualRoom_Id(Long roomId); // Đổi count thành exists
    
    boolean existsByActualLecturer_IdAndStatus(Long lecturerId, ClassSession.Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassSession c WHERE c.id = :id")
    Optional<ClassSession> findByIdWithLock(@Param("id") Long id);

    List<ClassSession> findByStatusAndSessionDateBefore(ClassSession.Status status, java.time.LocalDate date);

    // Kéo từ ClassSession -> Schedule -> Lecturer để check quyền chính chủ
    boolean existsByIdAndSchedule_Lecturer_Id(Long id, Long lecturerId);
    @Query("""
    SELECT new ken.example.dekiru.attendance.dto.ClassSessionListDto(
        cs.id,
        cs.sessionNumber,
        s.totalSessions,
        cs.sessionDate,
        s.periodStart,
        s.periodEnd,
        r.code,
        SUM(CASE WHEN a.status = ken.example.dekiru.attendance.entity.Attendance.Status.present THEN 1L ELSE 0L END),
        COUNT(a.id),
        SUM(CASE WHEN a.isLate = true THEN 1L ELSE 0L END),
        cs.status
    )
    FROM ClassSession cs
    JOIN cs.schedule s
    JOIN cs.actualRoom r
    LEFT JOIN Attendance a ON a.classSession.id = cs.id
    WHERE s.adminClass.id = :adminClassId
      AND s.subject.id = :subjectId
      AND cs.actualLecturer.id = :lecturerId
      AND s.semester.isActive = true
    GROUP BY cs.id, cs.sessionNumber, s.totalSessions,
             cs.sessionDate, s.periodStart, s.periodEnd, r.code, cs.status
    ORDER BY cs.sessionNumber ASC
""")
    List<ClassSessionListDto> findClassSessionsListForAdminClassAndSubject(
            @Param("adminClassId") Long adminClassId, 
            @Param("subjectId") Long subjectId,
            @Param("lecturerId") Long lecturerId
    );
}
