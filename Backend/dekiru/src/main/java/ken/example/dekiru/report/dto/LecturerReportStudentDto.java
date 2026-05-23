package ken.example.dekiru.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerReportStudentDto {
    private String studentCode;
    private String fullName;
    private Integer presentCount;
    private Integer absentCount;
    private Integer excusedCount;
    private Integer lateCount;
    private Integer leftEarlyCount;
    private Double attendanceRate;
    private Boolean isDanger;
}
