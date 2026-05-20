package ken.example.dekiru.schedule.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "period_time")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PeriodTime {

    /**
     * PK tự nhiên = số tiết (1–15), không auto-increment.
     */
    @Id
    @Column(name = "period_number")
    private Byte periodNumber;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
