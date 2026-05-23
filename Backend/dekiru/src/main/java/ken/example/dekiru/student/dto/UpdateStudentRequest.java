package ken.example.dekiru.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStudentRequest {
    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean isActive;

    private String phoneNumber;
    private String gender;
    private java.time.LocalDate birthday;
    private String birthPlace;
}

