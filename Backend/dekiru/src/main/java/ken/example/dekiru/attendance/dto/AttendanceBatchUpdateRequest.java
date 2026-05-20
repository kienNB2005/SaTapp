package ken.example.dekiru.attendance.dto;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceBatchUpdateRequest {
    private List<UpdateItem> items;

    @Data
    public static class UpdateItem {
        private Long attendanceId;
        private String uiStatus;    // PRESENT | ABSENT | EXCUSED | LATE
        private Short lateMinutes;
        private Boolean leftEarly;
        private String note;
    }
}