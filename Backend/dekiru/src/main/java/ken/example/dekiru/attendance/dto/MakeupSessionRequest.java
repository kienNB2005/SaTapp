package ken.example.dekiru.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakeupSessionRequest {
    private LocalDate sessionDate;
    private Byte periodStart;
    private Byte periodEnd;
    private Long roomId;
}
