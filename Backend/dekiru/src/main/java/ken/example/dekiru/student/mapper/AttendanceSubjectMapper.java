package ken.example.dekiru.student.mapper;

import ken.example.dekiru.student.dto.AttendanceSubjectResponse;
import ken.example.dekiru.student.entity.StudentAttendanceBySubject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper: StudentAttendanceBySubject (view entity) → AttendanceSubjectResponse (DTO)
 *
 * Xử lý 3 vấn đề đặc thù:
 *   1. Các cột COUNT có thể NULL → về 0
 *   2. attendance_rate_pct NULL → về 100.0
 *   3. is_danger kiểu Boolean (nullable) → boolean primitive (non-null)
 *   4. attendance_status NULL → về "safe" (trường hợp SV chưa có buổi nào)
 */
@Mapper(componentModel = "spring")
public interface AttendanceSubjectMapper {

    @Mapping(target = "totalSessions",      source = "totalSessions",      qualifiedByName = "nullToZeroInt")
    @Mapping(target = "passedSessions",     source = "passedSessions",     qualifiedByName = "nullToZeroInt")
    @Mapping(target = "remainingSessions",  source = "remainingSessions",  qualifiedByName = "nullToZeroInt")
    @Mapping(target = "presentCount",       source = "presentCount",       qualifiedByName = "nullToZeroInt")
    @Mapping(target = "absentCount",        source = "absentCount",        qualifiedByName = "nullToZeroInt")
    @Mapping(target = "excusedCount",       source = "excusedCount",       qualifiedByName = "nullToZeroInt")
    @Mapping(target = "lateCount",          source = "lateCount",          qualifiedByName = "nullToZeroInt")
    @Mapping(target = "leaveEarlyCount",    source = "leaveEarlyCount",    qualifiedByName = "nullToZeroInt")
    @Mapping(target = "maxAbsentAllowed",   source = "maxAbsentAllowed",   qualifiedByName = "nullToZeroInt")
    @Mapping(target = "attendanceRatePct",  source = "attendanceRatePct",  qualifiedByName = "nullToHundredDouble")
    @Mapping(target = "danger",             source = "isDanger",           qualifiedByName = "nullableBooleanToFalse")
    @Mapping(target = "attendanceStatus",   source = "attendanceStatus",   qualifiedByName = "nullToSafe")
    AttendanceSubjectResponse toResponse(StudentAttendanceBySubject entity);

    List<AttendanceSubjectResponse> toResponseList(List<StudentAttendanceBySubject> entities);

    @Named("nullToZeroInt")
    static int nullToZeroInt(Integer value) {
        return value != null ? value : 0;
    }

    @Named("nullToHundredDouble")
    static double nullToHundredDouble(Double value) {
        return value != null ? value : 100.0;
    }

    @Named("nullableBooleanToFalse")
    static boolean nullableBooleanToFalse(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    @Named("nullToSafe")
    static String nullToSafe(String value) {
        return value != null ? value : "safe";
    }
}