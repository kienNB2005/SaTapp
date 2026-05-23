package ken.example.dekiru.attendance.dto;

import ken.example.dekiru.attendance.entity.ClassSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionListDto {
    private Long id;
    private Byte sessionNumber;
    private Byte totalSessions;
    private LocalDate sessionDate;
    private Byte periodStart;
    private Byte periodEnd;
    private String roomCode;
    private Long presentCount;
    private Long totalCount;
    private Long lateCount;
    private ClassSession.Status status;
    private Long makeupForId;
    private Long activeMakeupCount;
    private LocalDate semesterEndDate;
}

