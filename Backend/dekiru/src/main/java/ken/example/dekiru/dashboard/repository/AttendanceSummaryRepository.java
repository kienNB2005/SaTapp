package ken.example.dekiru.dashboard.repository;
import ken.example.dekiru.schedule.entity.Schedule;

import ken.example.dekiru.dashboard.entity.VAttendanceSummary;
import ken.example.dekiru.dashboard.entity.VAttendanceSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceSummaryRepository
        extends JpaRepository<VAttendanceSummary, VAttendanceSummaryId> {

    @Query("""
            SELECT ROUND(AVG(v.attendanceRate), 1)
            FROM VAttendanceSummary v
            WHERE v.semesterId = :semesterId
              AND v.subjectId IN (
                  SELECT DISTINCT sc.subject.id
                  FROM Schedule sc
                  WHERE sc.lecturer.id = :lecturerId
                    AND sc.semester.id = :semesterId
              )
            """)
    Double avgAttendanceRateForLecturer(
            @Param("lecturerId") Long lecturerId,
            @Param("semesterId") Long semesterId);
}