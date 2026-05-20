package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateDepartmentRequest {
    String code;
    String name;
    Long facultyId;
}
