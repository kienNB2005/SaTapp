package ken.example.dekiru.common.specification;

import ken.example.dekiru.schedule.entity.Schedule;
import org.springframework.data.jpa.domain.Specification;

public class ScheduleSpecifications {

    /**
     * Lọc theo học kỳ (bắt buộc)
     */
    public static Specification<Schedule> hasSemesterId(Long semesterId) {
        return (root, query, cb) -> {
            if (semesterId == null) return null;
            return cb.equal(root.get("semester").get("id"), semesterId);
        };
    }

    /**
     * Tìm kiếm theo: tên môn, mã môn, tên GV, mã lớp, tên lớp, mã phòng
     */
    public static Specification<Schedule> hasSearchText(String searchText) {
        return (root, query, cb) -> {
            if (searchText == null || searchText.isBlank()) return null;
            String pattern = "%" + searchText.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("subject").get("code")), pattern),
                    cb.like(cb.lower(root.get("subject").get("name")), pattern),
                    cb.like(cb.lower(root.get("lecturer").get("lecturerCode")), pattern),
                    cb.like(cb.lower(root.get("lecturer").get("user").get("fullName")), pattern),
                    cb.like(cb.lower(root.get("adminClass").get("code")), pattern),
                    cb.like(cb.lower(root.get("adminClass").get("name")), pattern),
                    cb.like(cb.lower(root.get("room").get("code")), pattern)
            );
        };
    }

    /**
     * Lọc theo lớp hành chính
     */
    public static Specification<Schedule> hasAdminClassId(Long adminClassId) {
        return (root, query, cb) -> {
            if (adminClassId == null) return null;
            return cb.equal(root.get("adminClass").get("id"), adminClassId);
        };
    }

    /**
     * Lọc theo giảng viên
     */
    public static Specification<Schedule> hasLecturerId(Long lecturerId) {
        return (root, query, cb) -> {
            if (lecturerId == null) return null;
            return cb.equal(root.get("lecturer").get("id"), lecturerId);
        };
    }

    /**
     * Lọc theo thứ trong tuần (2-8)
     */
    public static Specification<Schedule> hasDayOfWeek(Integer dayOfWeek) {
        return (root, query, cb) -> {
            if (dayOfWeek == null) return null;
            return cb.equal(root.get("dayOfWeek"), dayOfWeek.byteValue());
        };
    }
}
