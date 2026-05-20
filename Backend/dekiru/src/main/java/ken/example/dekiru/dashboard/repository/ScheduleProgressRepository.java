package ken.example.dekiru.dashboard.repository;

import ken.example.dekiru.dashboard.entity.VScheduleProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleProgressRepository
        extends JpaRepository<VScheduleProgress, Long> {

    List<VScheduleProgress> findByLecturerIdAndSemesterId(Long lecturerId, Long semesterId);
}