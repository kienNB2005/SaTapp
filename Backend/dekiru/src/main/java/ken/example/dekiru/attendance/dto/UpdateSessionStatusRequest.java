package ken.example.dekiru.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionStatusRequest {
    private String status; // "OPEN", "CLOSED", "CANCELLED", "CHECKING_OUT"
    private String reason; // Cho trường hợp CANCELLED
    private Integer checkoutMinutes; // Cho trường hợp CHECKING_OUT
}
