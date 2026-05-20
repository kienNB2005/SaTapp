package ken.example.dekiru.dashboard.repository;

import ken.example.dekiru.dashboard.entity.VLecturerToday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LecturerTodayRepository
        extends JpaRepository<VLecturerToday, Long> {

    List<VLecturerToday> findByLecturerId(Long lecturerId);

    List<VLecturerToday> findByLecturerIdAndStatus(Long lecturerId, String status);
}