package ken.example.dekiru.report.dto;

public interface AdminSchoolReportRowProjection {
    Long getScheduleId();
    String getClassName();
    String getSubjectName();
    String getLecturerName();
    Integer getTotalStudents();
    Integer getCompletedSessions();
    Integer getTotalSessions();
    Double getAttendanceRate();
}
