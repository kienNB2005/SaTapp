package ken.example.dekiru.report.dto;

public interface AdminSchoolReportStatsProjection {
    Integer getTotalClasses();
    Double getAvgAttendanceRate();
    Integer getUnderThresholdCount();
    Integer getTotalSessionsTaught();
}
