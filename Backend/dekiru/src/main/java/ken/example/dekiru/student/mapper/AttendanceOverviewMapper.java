package ken.example.dekiru.student.mapper;

import ken.example.dekiru.student.dto.AttendanceOverviewResponse;
import ken.example.dekiru.student.entity.StudentAttendanceOverview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper: StudentAttendanceOverview (view entity) → AttendanceOverviewResponse (DTO)
 *
 * Xử lý 2 vấn đề đặc thù của view:
 *   1. View có thể trả NULL trên các cột COUNT khi SV chưa có buổi nào
 *      → defaultExpression chuyển về 0
 *   2. attendance_rate_pct NULL (chưa có buổi nào closed)
 *      → default về 100.0 (chưa có dữ liệu = chưa vắng buổi nào)
 */
@Mapper(componentModel = "spring")
public interface AttendanceOverviewMapper {

    @Mapping(target = "totalPassed",      source = "totalPassed",      qualifiedByName = "nullToZeroInt")
    @Mapping(target = "totalPresent",     source = "totalPresent",     qualifiedByName = "nullToZeroInt")
    @Mapping(target = "totalAbsent",      source = "totalAbsent",      qualifiedByName = "nullToZeroInt")
    @Mapping(target = "totalExcused",     source = "totalExcused",     qualifiedByName = "nullToZeroInt")
    @Mapping(target = "totalLate",        source = "totalLate",        qualifiedByName = "nullToZeroInt")
    @Mapping(target = "totalLeaveEarly",  source = "totalLeaveEarly",  qualifiedByName = "nullToZeroInt")
    @Mapping(target = "attendanceRatePct",source = "attendanceRatePct",qualifiedByName = "nullToHundredDouble")
    AttendanceOverviewResponse toResponse(StudentAttendanceOverview entity);

    @Named("nullToZeroInt")
    static int nullToZeroInt(Integer value) {
        return value != null ? value : 0;
    }

    @Named("nullToHundredDouble")
    static double nullToHundredDouble(Double value) {
        return value != null ? value : 100.0;
    }
}