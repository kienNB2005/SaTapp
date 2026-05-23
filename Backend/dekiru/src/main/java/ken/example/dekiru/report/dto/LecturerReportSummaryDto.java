package ken.example.dekiru.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerReportSummaryDto {
    private Integer totalStudents;
    private Double avgAttendanceRate;
    private Integer underThresholdCount;
    private Integer finishedSessions;
    private Integer totalSessions;
}
