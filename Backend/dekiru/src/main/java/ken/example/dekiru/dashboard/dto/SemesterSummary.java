package ken.example.dekiru.dashboard.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO tổng hợp 4 thẻ đầu Dashboard.
 * Được build trong Service từ dữ liệu đã query.
 *
 *  Thẻ 1 — Buổi hôm nay  : sessionsToday, openCount, upcomingCount
 *  Thẻ 2 — Tuần này      : sessionsThisWeek, subjectsThisWeek
 *  Thẻ 3 — Học kỳ này    : totalSessions, closedSessions, remainingSessions
 *  Thẻ 4 — Chuyên cần TB : avgAttendanceRate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SemesterSummary {

    Long   semesterId;
    String semesterName;

    // Thẻ 1
    int    sessionsToday;
    int    openCount;
    private int    upcomingCount;

    // Thẻ 2
    int    sessionsThisWeek;
    int    subjectsThisWeek;

    // Thẻ 3
    int    totalSessions;
    int    closedSessions;
    int    remainingSessions;

    // Thẻ 4
    Double avgAttendanceRate;


}
