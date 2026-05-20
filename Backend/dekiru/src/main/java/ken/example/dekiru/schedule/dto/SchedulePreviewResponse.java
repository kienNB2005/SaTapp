package ken.example.dekiru.schedule.dto;

import ken.example.dekiru.schedule.dto.ScheduleExcelDTO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SchedulePreviewResponse {
    ScheduleExcelDTO schedule;
    boolean isValid;
    @Builder.Default
    List<String> errors = new ArrayList<>();

    /** Tên môn học (để hiển thị trên bảng preview) */
    String subjectName;
    /** Tên giảng viên */
    String lecturerName;
    /** Tên lớp */
    String adminClassName;
    /** Số buổi học dự kiến (weekEnd - weekStart + 1) */
    Integer estimatedSessions;
}
