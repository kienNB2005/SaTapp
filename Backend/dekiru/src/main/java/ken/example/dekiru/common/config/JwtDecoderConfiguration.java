    package ken.example.dekiru.common.config;

    import ken.example.dekiru.security.service.JWTService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
    import org.springframework.security.oauth2.jwt.Jwt;
    import org.springframework.security.oauth2.jwt.JwtDecoder;
    import org.springframework.security.oauth2.jwt.JwtException;
    import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
    import org.springframework.security.oauth2.jwt.BadJwtException; // Thêm cái này
    import org.springframework.stereotype.Component;

    import javax.crypto.SecretKey;
    import javax.crypto.spec.SecretKeySpec;
    import java.nio.charset.StandardCharsets;
    import java.util.Objects;

    @Component
    @RequiredArgsConstructor
    public class JwtDecoderConfiguration implements JwtDecoder {

        @Value("${jwt.secret-key}")
        private String secretKey;

        private final JWTService jwtService;
        private NimbusJwtDecoder nimbusJwtDecoder = null;

        @Override
        public Jwt decode(String token) throws JwtException {
            try {
                // Bước 1: Kiểm tra định dạng token thô trước (Tránh lỗi Missing part delimiters)
                // Token hợp lệ phải có 2 dấu chấm (Header.Payload.Signature)
                if (token == null || token.split("\\.").length < 3) {
                    throw new BadJwtException("Invalid token format: Missing part delimiters");
                }

                // Bước 2: Gọi service để kiểm tra logic (hết hạn, chữ ký...)
                // Lưu ý: Nếu verifyToken bên trong ném ParseException, nó sẽ rơi xuống catch bên dưới
                boolean isValid = jwtService.verifyToken(token);
                if (!isValid) {
                    throw new BadJwtException("Token invalid or expired");
                }

                // Bước 3: Khởi tạo decoder nếu chưa có
                if (Objects.isNull(nimbusJwtDecoder)) {
                    SecretKey secretKeySpec = new SecretKeySpec(
                            secretKey.getBytes(StandardCharsets.UTF_8), "HS512");
                    nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                            .macAlgorithm(MacAlgorithm.HS512)
                            .build();
                }

                return nimbusJwtDecoder.decode(token);

            } catch (Exception e) {
                // CỰC KỲ QUAN TRỌNG:
                // Không được ném RuntimeException.
                // Phải ném JwtException hoặc BadJwtException để Spring Security
                // có thể xử lý permitAll() cho các endpoint public.
                throw new BadJwtException(e.getMessage());
            }
        }
    }