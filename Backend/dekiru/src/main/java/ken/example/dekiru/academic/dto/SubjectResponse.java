package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectResponse {
    Long id;
    String code;
    String name;
    Byte credits;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

