package ken.example.dekiru.common.config;
import ken.example.dekiru.security.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    // Hàm lấy đối tượng Jwt (thay vì UserPrincipal)
    private Jwt getJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem user đã đăng nhập bằng JWT chưa
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    // Lấy ID Giảng viên
    public Long getCurrentLecturerId() {
        Jwt jwt = getJwtToken();
        if (jwt == null || !jwt.hasClaim("lecturerId")) {
            throw new RuntimeException("Access Denied: Không tìm thấy ID Giảng viên trong Token!");
        }
        // Ép kiểu Object sang Long an toàn
        return ((Number) jwt.getClaim("lecturerId")).longValue();
    }

    // Lấy ID Sinh viên
    public Long getCurrentStudentId() {
        Jwt jwt = getJwtToken();
        if (jwt == null || !jwt.hasClaim("studentId")) {
            throw new RuntimeException("Access Denied: Không tìm thấy ID Sinh viên trong Token!");
        }
        return ((Number) jwt.getClaim("studentId")).longValue();
    }

    // Lấy ID User gốc (nếu bạn có claim "userId")
    public Long getCurrentUserId() {
        Jwt jwt = getJwtToken();
        if (jwt != null && jwt.hasClaim("userId")) {
            return ((Number) jwt.getClaim("userId")).longValue();
        }
        return null;
    }
}