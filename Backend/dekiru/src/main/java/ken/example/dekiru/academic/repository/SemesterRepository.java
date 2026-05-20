package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    boolean existsByName(String name);
    Optional<Semester> findByName(String name);
    Optional<Semester> findByIsActiveTrue();
}

