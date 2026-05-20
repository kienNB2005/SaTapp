package ken.example.dekiru.student.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateStudentRequest {
    String fullName;
    String email;
    String studentCode;
    Long adminClassId;
}
