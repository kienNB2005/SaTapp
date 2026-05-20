package ken.example.dekiru.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrTokenResponse {
    private String qrCodeData;
    private LocalDateTime qrExpiresAt;
    private Long sessionId;
    private String type;
}
