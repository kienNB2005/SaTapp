package ken.example.dekiru.attendance.repository;
import ken.example.dekiru.attendance.entity.Attendance;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.schedule.entity.Schedule;

import jakarta.persistence.LockModeType;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.attendance.dto.ClassSessionListDto;
import org.springframework.data.jpa.repository.EntityGraph;
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
        cs.actualPeriodStart,
        cs.actualPeriodEnd,
        r.code,
        SUM(CASE WHEN a.status = ken.example.dekiru.attendance.entity.Attendance.Status.present THEN 1L ELSE 0L END),
        COUNT(a.id),
        SUM(CASE WHEN a.isLate = true THEN 1L ELSE 0L END),
        cs.status,
        cs.makeupFor.id,
        (SELECT COUNT(m.id) FROM ClassSession m WHERE m.makeupFor.id = cs.id AND m.status <> ken.example.dekiru.attendance.entity.ClassSession.Status.cancelled),
        s.semester.endDate
    )
    FROM ClassSession cs
    JOIN cs.schedule s
    JOIN cs.actualRoom r
    LEFT JOIN Attendance a ON a.classSession.id = cs.id
    WHERE s.adminClass.id = :adminClassId
      AND s.subject.id = :subjectId
      AND cs.actualLecturer.id = :lecturerId
      AND s.semester.isActive = true
      AND NOT (cs.status = ken.example.dekiru.attendance.entity.ClassSession.Status.cancelled AND cs.makeupFor.id IS NOT NULL)
    GROUP BY cs.id, cs.sessionNumber, s.totalSessions,
             cs.sessionDate, cs.actualPeriodStart, cs.actualPeriodEnd, r.code, cs.status, cs.makeupFor.id, s.semester.endDate
    ORDER BY cs.sessionDate ASC, cs.actualPeriodStart ASC
""")
    List<ClassSessionListDto> findClassSessionsListForAdminClassAndSubject(
            @Param("adminClassId") Long adminClassId, 
            @Param("subjectId") Long subjectId,
            @Param("lecturerId") Long lecturerId
    );

    boolean existsByMakeupFor_IdAndStatusNot(Long originalSessionId, ClassSession.Status status);

    boolean existsByScheduleIdAndSessionDateAndActualPeriodStart(Long scheduleId, java.time.LocalDate sessionDate, Byte actualPeriodStart);

    List<ClassSession> findBySchedule_AdminClass_IdAndSessionDateBetweenAndStatusNot(Long adminClassId, java.time.LocalDate startDate, java.time.LocalDate endDate, ClassSession.Status status);

    List<ClassSession> findByActualLecturer_IdAndSessionDateBetweenAndStatusNot(Long lecturerId, java.time.LocalDate startDate, java.time.LocalDate endDate, ClassSession.Status status);
    
    @EntityGraph(attributePaths = {"schedule", "schedule.subject", "schedule.adminClass", "actualRoom", "makeupFor"})
    List<ClassSession> findByActualLecturer_IdAndSessionDateBetween(Long lecturerId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    @EntityGraph(attributePaths = {"schedule", "schedule.subject", "schedule.adminClass", "actualRoom", "actualLecturer", "actualLecturer.user"})
    List<ClassSession> findBySessionDate(java.time.LocalDate sessionDate);

    List<ClassSession> findBySchedule_IdAndSessionDateBetween(Long scheduleId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    @Query("""
        SELECT COUNT(cs.id)
        FROM ClassSession cs 
        WHERE cs.status <> ken.example.dekiru.attendance.entity.ClassSession.Status.cancelled
          AND cs.sessionDate = :sessionDate
          AND cs.actualPeriodStart <= :periodEnd
          AND cs.actualPeriodEnd >= :periodStart
          AND cs.schedule.adminClass.id = :adminClassId
    """)
    long countConflictForClass(
        @Param("sessionDate") java.time.LocalDate sessionDate,
        @Param("periodStart") Byte periodStart,
        @Param("periodEnd") Byte periodEnd,
        @Param("adminClassId") Long adminClassId
    );

    @Query("""
        SELECT COUNT(cs.id)
        FROM ClassSession cs
        WHERE cs.status <> ken.example.dekiru.attendance.entity.ClassSession.Status.cancelled
          AND cs.sessionDate = :sessionDate
          AND cs.actualPeriodStart <= :periodEnd
          AND cs.actualPeriodEnd >= :periodStart
          AND cs.actualRoom.id = :roomId
    """)
    long countConflictForRoom(
        @Param("sessionDate") java.time.LocalDate sessionDate,
        @Param("periodStart") Byte periodStart,
        @Param("periodEnd") Byte periodEnd,
        @Param("roomId") Long roomId
    );

    @Query("""
        SELECT COUNT(cs.id)
        FROM ClassSession cs
        WHERE cs.status <> ken.example.dekiru.attendance.entity.ClassSession.Status.cancelled
          AND cs.sessionDate = :sessionDate
          AND cs.actualPeriodStart <= :periodEnd
          AND cs.actualPeriodEnd >= :periodStart
          AND cs.actualLecturer.id = :lecturerId
    """)
    long countConflictForLecturer(
        @Param("sessionDate") java.time.LocalDate sessionDate,
        @Param("periodStart") Byte periodStart,
        @Param("periodEnd") Byte periodEnd,
        @Param("lecturerId") Long lecturerId
    );
}
