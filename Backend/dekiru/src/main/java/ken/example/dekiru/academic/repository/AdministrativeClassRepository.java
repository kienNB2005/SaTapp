package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.AdministrativeClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministrativeClassRepository extends JpaRepository<AdministrativeClass, Long> {
    boolean existsByCode(String code);
    Optional<AdministrativeClass> findByCode(String code);
    @EntityGraph(attributePaths = {"department", "homeroomTeacher"})
    List<AdministrativeClass> findAllByCodeIn(List<String> codes);
    @EntityGraph(attributePaths = {"department", "homeroomTeacher"})
    List<AdministrativeClass> findAll();
    long countByDepartmentId(Long departmentId); // Check if department has classes
}

