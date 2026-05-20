package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepartmentResponse {
    Long id;
    Long facultyId;
    String facultyCode;
    String facultyName;
    String code;
    String name;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

