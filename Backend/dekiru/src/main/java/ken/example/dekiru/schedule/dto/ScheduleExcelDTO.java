package ken.example.dekiru.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleExcelDTO {
    Integer rowIndex;

    @NotBlank(message = "Mã lớp hành chính không được để trống")
    String adminClassCode;

    @NotBlank(message = "Mã môn học không được để trống")
    String subjectCode;

    @NotBlank(message = "Mã giảng viên không được để trống")
    String lecturerCode;

    @NotBlank(message = "Mã phòng không được để trống")
    String roomCode;

    @NotNull(message = "Thứ học không được để trống")
    @Min(value = 2, message = "Thứ học phải từ 2 (Thứ 2) đến 8 (Chủ nhật)")
    @Max(value = 8, message = "Thứ học phải từ 2 (Thứ 2) đến 8 (Chủ nhật)")
    Integer dayOfWeek;

    @NotNull(message = "Tiết bắt đầu không được để trống")
    @Min(value = 1, message = "Tiết bắt đầu phải từ 1 đến 15")
    @Max(value = 15, message = "Tiết bắt đầu phải từ 1 đến 15")
    Integer periodStart;

    @NotNull(message = "Tiết kết thúc không được để trống")
    @Min(value = 1, message = "Tiết kết thúc phải từ 1 đến 15")
    @Max(value = 15, message = "Tiết kết thúc phải từ 1 đến 15")
    Integer periodEnd;

    @NotNull(message = "Tuần bắt đầu không được để trống")
    @Min(value = 1, message = "Tuần bắt đầu phải từ 1")
    Integer weekStart;

    @NotNull(message = "Tuần kết thúc không được để trống")
    @Min(value = 1, message = "Tuần kết thúc phải từ 1")
    Integer weekEnd;
}
