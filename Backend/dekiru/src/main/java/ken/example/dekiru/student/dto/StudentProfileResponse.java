package ken.example.dekiru.student.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentProfileResponse {
    String fullName;
    String studentCode;
    String email;
    String adminClassName;
    String cohortYear;
    String departmentName;
    String facultyName;
    String homeroomTeacherName;
    String phoneNumber;
    String gender;
    LocalDate birthday;
    String birthPlace;
}
