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
public class JwtInfo {
    String jwtId;
    Date issueTime;
    Date expiredTime;
    Long lecturerId;
}
