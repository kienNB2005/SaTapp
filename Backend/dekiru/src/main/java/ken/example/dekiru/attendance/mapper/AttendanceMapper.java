package ken.example.dekiru.attendance.mapper;

import ken.example.dekiru.attendance.dto.AttendanceListDto;
import ken.example.dekiru.attendance.repository.AttendanceSummaryProjection;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    // MapStruct sẽ tự động map các trường trùng tên
    @Mapping(target = "uiStatus", ignore = true) // Bỏ qua, sẽ map thủ công bên dưới
    AttendanceListDto toDto(AttendanceSummaryProjection projection);

    // Hàm này sẽ tự động chạy sau khi MapStruct đã map xong các trường cơ bản
    @AfterMapping
    default void calculateUiStatus(AttendanceSummaryProjection projection, @MappingTarget AttendanceListDto dto) {
        // 1. Xử lý null an toàn cho phần thống kê (nếu sinh viên mới đi học buổi đầu)
        if (dto.getPresentCount() == null) dto.setPresentCount(0);
        if (dto.getAbsentCount() == null) dto.setAbsentCount(0);
        if (dto.getExcusedCount() == null) dto.setExcusedCount(0);
        if (dto.getLateCount() == null) dto.setLateCount(0);

        // 2. Logic dịch trạng thái DB (status + is_late) sang uiStatus cho Frontend
        String dbStatus = projection.getStatus();
        Boolean isLate = projection.getIsLate();

        if ("absent".equalsIgnoreCase(dbStatus)) {
            dto.setUiStatus("ABSENT");
        } else if ("excused".equalsIgnoreCase(dbStatus)) {
            dto.setUiStatus("EXCUSED");
        } else if (Boolean.TRUE.equals(isLate)) {
            dto.setUiStatus("LATE");
        } else {
            dto.setUiStatus("PRESENT");
        }
    }
}