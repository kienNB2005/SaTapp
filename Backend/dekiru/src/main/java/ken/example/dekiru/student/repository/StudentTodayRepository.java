package ken.example.dekiru.student.repository;

import ken.example.dekiru.student.entity.StudentToday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentTodayRepository extends JpaRepository<StudentToday, Long> {
    List<StudentToday> findByStudentId(Long studentId);
}