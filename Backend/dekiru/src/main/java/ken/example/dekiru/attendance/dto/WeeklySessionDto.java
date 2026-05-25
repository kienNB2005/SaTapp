package ken.example.dekiru.attendance.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class WeeklySessionDto {
    private Long id;
    private Long classSessionId;
    private String subjectName;
    private String className;
    private String roomCode;
    private LocalDate sessionDate;
    private String status;
    private Byte sessionNumber;
    private Long makeupForId;
    private LocalDate originalSessionDate;
    private Byte periodStart;
    private Byte periodEnd;
    private Integer dayOfWeek;
}
