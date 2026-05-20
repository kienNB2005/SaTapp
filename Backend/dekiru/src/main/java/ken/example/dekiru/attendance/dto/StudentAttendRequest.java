package ken.example.dekiru.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendRequest {
    private String token;
    private String deviceId;
    
    @Builder.Default
    private double lat = 0.0;
    
    @Builder.Default
    private double lng = 0.0;
}

