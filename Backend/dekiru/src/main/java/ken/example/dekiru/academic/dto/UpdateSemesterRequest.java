package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateSemesterRequest {
    String name;
    Boolean isActive;
    Byte startWeek;
}

