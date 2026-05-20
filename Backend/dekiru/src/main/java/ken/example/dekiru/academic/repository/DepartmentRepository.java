package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface    DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByCode(String code);
    Optional<Department> findByCode(String code);
    @EntityGraph(attributePaths = {"faculty"})
    List<Department> findAllByCodeIn(List<String> codes);
    long countByFacultyId(Long facultyId); // Check has Administrative Classes via Department
    @EntityGraph(attributePaths = {"faculty"})
    List<Department> findAll();
}

