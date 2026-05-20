package ken.example.dekiru.attendance.dto;

import ken.example.dekiru.attendance.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDto {
    private Long attendanceId;
    private String studentCode;
    private String fullName;
    private Attendance.Status status;   // present | absent | excused
    private LocalDateTime scannedAt;    // thời điểm check-in
    private Boolean isLate;
    private Short lateMinutes;
    private LocalDateTime checkedOutAt; // thời điểm check-out, null nếu chưa
    private Boolean leftEarly;          // true nếu không quét checkout kịp deadline
    private Boolean gpsVerified;        // null = không bật GPS
}