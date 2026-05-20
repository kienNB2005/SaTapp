package ken.example.dekiru.student.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO cho thẻ "Tổng quan" đầu màn hình chuyên cần.
 * Dùng @Builder để MapStruct sinh code qua builder pattern.
 */
@Getter
@Builder
public class AttendanceOverviewResponse {

    private String studentCode;
    private String fullName;
    private String adminClassCode;
    private String semesterName;

    private int totalPassed;
    private int totalPresent;
    private int totalAbsent;
    private int totalExcused;
    private int totalLate;
    private int totalLeaveEarly;
    private double attendanceRatePct;
}