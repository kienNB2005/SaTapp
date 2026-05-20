package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    boolean existsByCode(String code);
    Optional<Subject> findByCode(String code);
    List<Subject> findAllByCodeIn(List<String> codes);
}

