package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SemesterResponse {
    Long id;
    String name;
    LocalDate startDate;
    LocalDate endDate;
    Boolean isActive;
    Byte startWeek;
    LocalDateTime createdAt;
}

