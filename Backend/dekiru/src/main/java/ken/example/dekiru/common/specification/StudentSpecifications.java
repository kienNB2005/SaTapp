package ken.example.dekiru.common.specification;

import jakarta.persistence.criteria.Join;
import ken.example.dekiru.student.entity.Student;
import ken.example.dekiru.security.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class StudentSpecifications {

    public static Specification<Student> hasSearchText(String searchText) {
        return (root, query, cb) -> {
            if (searchText == null || searchText.isBlank()) return null;
            if (query.getResultType() == Long.class) return null; // skip khi COUNT

            String pattern = "%" + searchText.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("studentCode")), pattern),
                    cb.like(cb.lower(root.get("user").get("fullName")), pattern),
                    cb.like(cb.lower(root.get("user").get("email")), pattern)
            );
        };
    }

    public static Specification<Student> hasClassId(Long classId) {
        return (root, query, cb) -> {
            if (classId == null) return null;
            return cb.equal(root.get("adminClass").get("id"), classId);
        };
    }

    public static Specification<Student> hasStatus(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return cb.equal(root.get("user").get("isActive"), isActive); // đổi join → get
        };
    }
}