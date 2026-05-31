package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSemesterRequest {
    String name;
    LocalDate startDate;
    LocalDate endDate;
    Byte startWeek;
}

