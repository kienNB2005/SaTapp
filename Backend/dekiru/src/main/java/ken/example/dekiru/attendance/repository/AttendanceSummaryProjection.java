package ken.example.dekiru.attendance.repository;

public interface AttendanceSummaryProjection {
    Long getAttendanceId();
    String getStudentCode();
    String getFullName();
    Integer getPresentCount();
    Integer getAbsentCount();
    Integer getExcusedCount();
    Integer getLateCount();
    String getStatus();    // Status từ DB
    Boolean getIsLate();   // Cờ đi muộn từ DB
    Integer getLateMinutes(); // Sửa Short -> Integer
    Boolean getLeftEarly();
    String getNote();
}