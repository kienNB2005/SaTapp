package ken.example.dekiru.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSchoolReportSummaryDto {
    private Integer totalClasses;
    private Double avgAttendanceRate;
    private Integer underThresholdCount;
    private Integer totalSessionsTaught;
}
