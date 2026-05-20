package ken.example.dekiru.student.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO cho mỗi thẻ môn học trong màn hình chuyên cần.
 * Dùng @Builder để MapStruct sinh code qua builder pattern.
 */
@Getter
@Builder
public class AttendanceSubjectResponse {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private int credits;
    private String lecturerName;

    private int totalSessions;
    private int passedSessions;
    private int remainingSessions;

    private int presentCount;
    private int absentCount;
    private int excusedCount;
    private int lateCount;
    private int leaveEarlyCount;

    private double attendanceRatePct;
    private int maxAbsentAllowed;

    /** true → hiện banner cảnh báo đỏ "Sắp vượt ngưỡng cấm thi" */
    private boolean danger;

    /**
     * "safe" | "warning" | "danger"
     * Frontend dùng để chọn màu badge % và icon trạng thái
     */
    private String attendanceStatus;
}