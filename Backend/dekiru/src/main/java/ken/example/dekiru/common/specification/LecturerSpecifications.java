package ken.example.dekiru.common.specification;

import ken.example.dekiru.academic.entity.Lecturer;
import org.springframework.data.jpa.domain.Specification;

public class LecturerSpecifications {
    public static Specification <Lecturer> hasSearchText(String searchText) {
        return (root, query, cb) -> {
            if (searchText == null || searchText.isBlank()) return null;
            if (query.getResultType() == Long.class) return null; // skip khi COUNT

            String pattern = "%" + searchText.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("lecturerCode")), pattern),
                    cb.like(cb.lower(root.get("user").get("fullName")), pattern),
                    cb.like(cb.lower(root.get("user").get("email")), pattern)
            );
        };
    }

    public static Specification<Lecturer> hasStatus(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return cb.equal(root.get("user").get("isActive"), isActive); // đổi join → get
        };
    }

    public static Specification <Lecturer> hasFacultyId(Long facultyId) {
        return (root, query, cb) -> {
            if (facultyId == null) return null;
            return cb.equal(root.get("faculty").get("id"), facultyId);
        };
    }
}
