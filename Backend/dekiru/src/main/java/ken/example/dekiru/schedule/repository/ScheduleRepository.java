package ken.example.dekiru.schedule.repository;
import ken.example.dekiru.attendance.entity.ClassSession;

import ken.example.dekiru.schedule.entity.Schedule;
import ken.example.dekiru.academic.entity.AdministrativeClass;
import ken.example.dekiru.academic.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {
    List<Schedule> findAllBySemesterId(Long semesterId);
    long countBySemesterId(Long semesterId);
    boolean existsBySemesterId(Long semesterId);
    long countByAdminClassId(Long administrativeClassId);
    long countBySubjectId(Long subjectId);
    long countSchedulesByRoomId(Long roomId);
    boolean existsByRoom_Id(Long roomId); // Đổi countSchedulesByRoomId thành exists

    @Query("""
        SELECT DISTINCT s.adminClass
        FROM Schedule s
        WHERE s.lecturer.id = :lecturerId
          AND s.semester.isActive = true
        ORDER BY s.adminClass.code ASC
    """)
    List<AdministrativeClass> findDistinctAdminClassesByLecturer(@Param("lecturerId") Long lecturerId);

    @Query("""
        SELECT DISTINCT s.subject
        FROM Schedule s
        WHERE s.lecturer.id = :lecturerId
          AND s.adminClass.id = :adminClassId
          AND s.semester.isActive = true
        ORDER BY s.subject.code ASC
    """)
    List<Subject> findDistinctSubjectsByLecturerAndAdminClass(
            @Param("lecturerId") Long lecturerId,
            @Param("adminClassId") Long adminClassId
    );

    @Query("""
        SELECT DISTINCT s.subject
        FROM Schedule s
        WHERE s.lecturer.id = :lecturerId
          AND s.semester.isActive = true
        ORDER BY s.subject.code ASC
    """)
    List<Subject> findDistinctSubjectsByLecturer(@Param("lecturerId") Long lecturerId);

    /**
     * Gọi stored procedure sinh ClassSession cho 1 Schedule.
     * Procedure đã có sẵn trong MySQL: generate_sessions_for_schedule(p_schedule_id)
     */
    @Procedure(procedureName = "generate_sessions_for_schedule")
    void generateSessions(@Param("p_schedule_id") Long scheduleId);
}
