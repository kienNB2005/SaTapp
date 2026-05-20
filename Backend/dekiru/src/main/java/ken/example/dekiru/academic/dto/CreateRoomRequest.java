package ken.example.dekiru.academic.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRoomRequest {
    String code;
    String building;
    Short capacity;
    BigDecimal latitude;
    BigDecimal longitude;
    Short gpsRadiusM;
}
