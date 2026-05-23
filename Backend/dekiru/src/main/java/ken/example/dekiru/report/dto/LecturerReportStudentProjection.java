package ken.example.dekiru.report.dto;

public interface LecturerReportStudentProjection {
    String getStudentCode();
    String getStudentName();
    Integer getPresentCount();
    Integer getAbsentCount();
    Integer getExcusedCount();
    Integer getLateCount();
    Integer getLeftEarlyCount();
    Integer getTotalSessions();
    Double getAttendanceRate();
}
