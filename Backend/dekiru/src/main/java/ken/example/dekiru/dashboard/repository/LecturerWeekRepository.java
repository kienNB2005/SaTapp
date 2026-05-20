package ken.example.dekiru.dashboard.repository;

import ken.example.dekiru.dashboard.entity.VLecturerWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LecturerWeekRepository
        extends JpaRepository<VLecturerWeek, Long> {

    // Lấy toàn bộ buổi trong tuần của GV — Service tự đếm trên memory
    List<VLecturerWeek> findByLecturerIdOrderBySessionDateAscPeriodStartAsc(Long lecturerId);
}
