package ken.example.dekiru.student.repository;

import ken.example.dekiru.student.entity.Student;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    Optional<Student> findByStudentCode(String studentCode);
    List<Student> findAllByStudentCodeIn(List<String> studentCodes);
    // THÊM DÒNG NÀY: Lấy danh sách Sinh viên theo mã Lớp hành chính
    List<Student> findAllByAdminClass_Code(String adminClassCode);
    List<Student> findAllByAdminClass_Id(Long adminClassId);

    @NonNull // Khẳng định kết quả trả về không bao giờ null
    @EntityGraph(attributePaths = {
            "user",
            "adminClass",
            "adminClass.homeroomTeacher"
    })
    Page<Student> findAll(@Nullable Specification<Student> spec, @NonNull Pageable pageable); // Khẳng định pageable không null

    Optional<Student> findByUserId(Long userId);

    @EntityGraph(attributePaths = {
            "user",
            "adminClass",
            "adminClass.department",
            "adminClass.department.faculty",
            "adminClass.homeroomTeacher",
            "adminClass.homeroomTeacher.user"
    })
    Optional<Student> findProfileById(Long id);
}

