package ken.example.dekiru.dashboard.dto;



import ken.example.dekiru.dashboard.entity.VLecturerToday;
import ken.example.dekiru.dashboard.entity.VLecturerWeek;
import ken.example.dekiru.dashboard.entity.VScheduleProgress;

import java.util.List;

/**
 * DTO trả về từ GET /api/v1/dashboard
 *
 * Tách ra package dto để tránh vòng phụ thuộc
 * Service → Controller (anti-pattern).
 */
public record DashboardResponse(
        SemesterSummary semesterSummary,  // 4 thẻ tổng quan
        List<VLecturerToday>    todaySessions,    // panel "Buổi học hôm nay"
        List<VLecturerWeek>     weekSessions,     // panel "Lịch tuần này"
        List<VScheduleProgress> progress          // panel "Tiến độ học kỳ"
) {}
