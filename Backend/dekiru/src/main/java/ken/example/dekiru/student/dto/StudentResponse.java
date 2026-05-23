package ken.example.dekiru.student.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentResponse {
    Long id;
    String fullName;
    String email;
    String studentCode;
    String adminClassCode;
    String adminClassName;
    Boolean isActive;
    String phoneNumber;
    String gender;
    java.time.LocalDate birthday;
    String birthPlace;
}

