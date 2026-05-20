package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomResponse {
    Long id;
    String code;
    String building;
    Short capacity;
    BigDecimal latitude;
    BigDecimal longitude;
    Short gpsRadiusM;
    LocalDateTime createdAt;
}

