package ken.example.dekiru.security.dto;

import ken.example.dekiru.common.enums.Gender;
import ken.example.dekiru.security.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String email;
    String fullName;
    User.Role role;
    Boolean isActive;
    String phoneNumber;
    Gender gender;
    LocalDate birthday;
    String birthPlace;
}

