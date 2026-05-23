package ken.example.dekiru.academic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class LecturerExcelDTO {
    Integer rowIndex;

    @NotBlank(message = "Họ và tên không được để trống")
    String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng chung")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@gmail\\.com$", message = "Email bắt buộc phải có đuôi @gmail.com")
    String email;

    @NotBlank(message = "Mã giảng viên không được để trống")
    String lecturerCode;

    @NotBlank(message = "Mã khoa không được để trống")
    String facultyCode;

    String phoneNumber;
    String gender;
    java.time.LocalDate birthday;
    String birthPlace;
}

