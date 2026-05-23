package ken.example.dekiru.academic.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LecturerResponse {
    Long id;
    String fullName;
    String email;
    String lecturerCode;
    String facultyCode;
    String facultyName;
    Boolean isActive;
    String phoneNumber;
    String gender;
    java.time.LocalDate birthday;
    String birthPlace;
}

