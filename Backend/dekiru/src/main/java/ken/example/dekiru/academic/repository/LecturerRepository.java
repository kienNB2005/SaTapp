package ken.example.dekiru.academic.repository;

import ken.example.dekiru.academic.entity.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long>, JpaSpecificationExecutor<Lecturer> {
    Optional<Lecturer> findByLecturerCode(String lecturerCode);
    List<Lecturer> findAllByLecturerCodeIn(List<String> lecturerCodes);

    // THÊM DÒNG NÀY: Lấy danh sách Giảng viên theo mã Khoa
    List<Lecturer> findAllByFaculty_Code(String facultyCode);

    Optional<Lecturer> findByUserId(Long userId);
}

