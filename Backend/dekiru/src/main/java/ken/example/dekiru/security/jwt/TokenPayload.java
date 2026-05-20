package ken.example.dekiru.security.jwt;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenPayload {
    String token;
    String jwtId;
    Date expiredTime;
}
