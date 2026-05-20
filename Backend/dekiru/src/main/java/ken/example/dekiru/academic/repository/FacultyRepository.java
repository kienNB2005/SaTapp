package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    boolean existsByCode(String code);
    Optional<Faculty> findByCode(String code);
    List<Faculty> findAllByCodeIn(List<String> codes);
}