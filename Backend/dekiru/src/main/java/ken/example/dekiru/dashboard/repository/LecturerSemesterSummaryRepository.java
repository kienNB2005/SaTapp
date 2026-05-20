package ken.example.dekiru.dashboard.repository;

import ken.example.dekiru.dashboard.entity.VLecturerSemesterSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LecturerSemesterSummaryRepository
        extends JpaRepository<VLecturerSemesterSummary, VLecturerSemesterSummary.PK> {

    Optional<VLecturerSemesterSummary> findByLecturerIdAndSemesterId(
            Long lecturerId, Long semesterId);
}
