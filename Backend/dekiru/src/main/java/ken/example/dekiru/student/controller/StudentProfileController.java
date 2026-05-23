package ken.example.dekiru.student.controller;

import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.student.dto.StudentProfileResponse;
import ken.example.dekiru.student.service.StudentProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAuthority('ROLE_STUDENT')")
public class StudentProfileController {
    StudentProfileService studentProfileService;

    @GetMapping
    public ApiResponse<StudentProfileResponse> getProfile() {
        return ApiResponse.success(
                studentProfileService.getProfile(),
                "Lấy thông tin cá nhân sinh viên thành công"
        );
    }
}
