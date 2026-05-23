package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateLecturerRequest {
    String fullName;
    String email;
    String lecturerCode;
    Long facultyId;
    String phoneNumber;
    String gender;
    java.time.LocalDate birthday;
    String birthPlace;
}
