package ken.example.dekiru.schedule.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleResponse {
    Long id;
    String adminClassCode;
    String adminClassName;
    String subjectCode;
    String subjectName;
    String lecturerCode;
    String lecturerName;
    String roomCode;
    Byte dayOfWeek;
    Byte periodStart;
    Byte periodEnd;
    Byte weekStart;
    Byte weekEnd;
    Byte totalSessions;
}
