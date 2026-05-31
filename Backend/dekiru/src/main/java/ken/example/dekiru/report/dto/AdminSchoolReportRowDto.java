package ken.example.dekiru.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSchoolReportRowDto {
    private Long scheduleId;
    private String className;
    private String subject;
    private String lecturer;
    private Integer totalStudents;
    private String completedSessions;
    private Double attendanceRate;
    private String status;
    private Boolean warning;
}
