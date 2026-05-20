package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAdministrativeClassRequest {
    String code;
    String name;
    String cohortYear;
    Long departmentId;
    String homeroomTeacherCode;
}
