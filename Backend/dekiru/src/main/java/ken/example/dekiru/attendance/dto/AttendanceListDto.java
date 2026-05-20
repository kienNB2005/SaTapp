package ken.example.dekiru.attendance.dto;

import lombok.Data;

@Data
public class AttendanceListDto {
    private Long attendanceId;
    private String studentCode;
    private String fullName;
    
    // Nhóm thống kê (Lấy từ View v_attendance_summary)
    private Integer presentCount; // Số buổi đi học
    private Integer absentCount;  // Số buổi nghỉ không phép
    private Integer excusedCount; // Số buổi có phép
    private Integer lateCount;    // Số buổi đi muộn
    
    // Nhóm Trạng thái hôm nay (Lấy từ bảng attendance)
    private String uiStatus;      // FE cần 4 trạng thái: PRESENT, ABSENT, EXCUSED, LATE
    private Integer lateMinutes;    // null hoặc số phút
    private Boolean leftEarly;    // true/false
    private String note;
}