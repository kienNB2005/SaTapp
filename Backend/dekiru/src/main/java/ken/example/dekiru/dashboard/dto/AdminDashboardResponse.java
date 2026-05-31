package ken.example.dekiru.dashboard.dto;

import java.util.List;

public record AdminDashboardResponse(
        Stats stats,
        TodayOverview todayOverview,
        List<ActiveSessionDto> activeSessions,
        List<PendingRequestDto> pendingRequests
) {
    public record Stats(
            String activeSemesterName,
            long totalStudents,
            long totalLecturers,
            long totalClasses
    ) {}

    public record TodayOverview(
            long totalSessions,
            long activeSessions,
            long completedSessions
    ) {}

    public record ActiveSessionDto(
            Long id,
            String lecturerName,
            String lecturerInitial,
            String subjectName,
            String className,
            String roomCode,
            String periodText,
            String openedAt
    ) {}

    public record PendingRequestDto(
            Long requestId,
            String lecturerName,
            String type, // MAKEUP, CANCEL
            String typeName, // Đề xuất dạy bù, Yêu cầu nghỉ dạy
            String subjectName,
            String className,
            String details,
            String time,
            String status // pending, approved, rejected
    ) {}
}
