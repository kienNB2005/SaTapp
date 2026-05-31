package ken.example.dekiru.report.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminSchoolReportResponse {
    private AdminSchoolReportSummaryDto summary;
    private List<AdminSchoolReportRowDto> rows;
}
