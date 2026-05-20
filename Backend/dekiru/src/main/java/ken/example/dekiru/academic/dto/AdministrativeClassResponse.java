package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdministrativeClassResponse {
    Long id;
    Long departmentId;
    String departmentCode;
    String departmentName;
    Long homeroomTeacherId;
    String homeroomTeacherName;
    String homeroomTeacherCode;
    String code;
    String name;
    String cohortYear;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

