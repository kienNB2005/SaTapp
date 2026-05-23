package ken.example.dekiru.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerReportResponse {
    private LecturerReportSummaryDto summary;
    private List<LecturerReportStudentDto> students;
}
