package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    boolean existsByName(String name);
    Optional<Semester> findByName(String name);
    Optional<Semester> findByIsActiveTrue();
    @Query("""
        SELECT DISTINCT sch.semester 
        FROM Student st, Schedule sch
        WHERE st.id = :studentId
          AND st.adminClass.id = sch.adminClass.id
          AND sch.semester.startDate <= :today
        ORDER BY sch.semester.startDate DESC
    """)
    List<Semester> findStartedSemestersByStudentId(
            @Param("studentId") Long studentId,
            @Param("today") LocalDate today
    );
}

