package ken.example.dekiru.academic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @Column(length = 20, nullable = false, unique = true)
    private String code;

    @Column(length = 100)
    private String building;

    @Column(nullable = false)
    @Builder.Default
    private Short capacity = 50;

    /** NULL = chưa nhập GPS — GPS check bị bỏ qua khi NULL */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /** SV phải đứng trong bán kính này (mét) khi quét QR */
    @Column(name = "gps_radius_m", nullable = false)
    @Builder.Default
    private Short gpsRadiusM = 50;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}