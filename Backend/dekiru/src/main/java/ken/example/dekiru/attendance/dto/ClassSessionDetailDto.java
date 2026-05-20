package ken.example.dekiru.attendance.dto;
import ken.example.dekiru.schedule.entity.PeriodTime;

import ken.example.dekiru.attendance.entity.ClassSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionDetailDto {
    private Long sessionId;
    private ClassSession.Status status;

    // Môn học & lớp
    private String subjectName;
    private String subjectCode;
    private String className;           // adminClass.code, ví dụ "K22A"

    // Phòng
    private String roomCode;
    private String building;

    // Tiết học
    private Byte periodStart;
    private Byte periodEnd;
    private LocalTime periodStartTime;  // từ PeriodTime entity
    private LocalTime periodEndTime;

    // Buổi thứ mấy
    private Integer sessionNumber;
    private Long totalSessions;         // đếm từ repository

    // Ngày học
    private LocalDate sessionDate;

    // Thời gian mở/đóng
    private LocalDateTime openedAt;

    // GPS
    private Boolean gpsEnabled;

    // QR hiện tại (nếu session đang open)
    private String qrCodeData;
    private LocalDateTime qrExpiresAt;
    private String qrType;             // CHECK_IN | CHECK_OUT
}