package ken.example.dekiru.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLecturerRequest {
    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean isActive;

    @NotNull(message = "Khoa không được để trống")
    private Long facultyId;

    private String phoneNumber;
    private String gender;
    private java.time.LocalDate birthday;
    private String birthPlace;
}

