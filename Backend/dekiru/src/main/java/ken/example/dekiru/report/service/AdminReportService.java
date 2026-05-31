package ken.example.dekiru.report.service;

import ken.example.dekiru.report.dto.*;
import ken.example.dekiru.report.repository.AdminReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final AdminReportRepository adminReportRepository;

    public AdminSchoolReportResponse getSchoolWideReport(Long semesterId, Long facultyId, Long departmentId, Double absentLimit, String search) {
        // Prepare search term
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Fetch projections
        List<AdminSchoolReportRowProjection> rowProjections = adminReportRepository.getReportRows(semesterId, facultyId, departmentId, searchTerm);
        List<AdminSchoolReportStatsProjection> statsProjections = adminReportRepository.getReportStats(semesterId, facultyId, departmentId, absentLimit);

        AdminSchoolReportStatsProjection statsProjection = statsProjections.isEmpty() ? null : statsProjections.get(0);

        // Map Stats
        AdminSchoolReportSummaryDto summaryDto = AdminSchoolReportSummaryDto.builder()
                .totalClasses(statsProjection != null ? statsProjection.getTotalClasses() : 0)
                .avgAttendanceRate(statsProjection != null && statsProjection.getAvgAttendanceRate() != null 
                        ? Math.round(statsProjection.getAvgAttendanceRate() * 10.0) / 10.0 
                        : 100.0)
                .underThresholdCount(statsProjection != null ? statsProjection.getUnderThresholdCount() : 0)
                .totalSessionsTaught(statsProjection != null ? statsProjection.getTotalSessionsTaught() : 0)
                .build();

        // Map Rows
        List<AdminSchoolReportRowDto> rowDtos = rowProjections.stream().map(row -> {
            boolean isWarning = row.getAttendanceRate() != null && row.getAttendanceRate() < absentLimit;
            String status = isWarning ? "Nguy cơ" : "Ổn định";

            return AdminSchoolReportRowDto.builder()
                    .scheduleId(row.getScheduleId())
                    .className(row.getClassName())
                    .subject(row.getSubjectName())
                    .lecturer(row.getLecturerName())
                    .totalStudents(row.getTotalStudents())
                    .completedSessions(row.getCompletedSessions() + "/" + row.getTotalSessions())
                    .attendanceRate(row.getAttendanceRate() != null ? row.getAttendanceRate() : 100.0)
                    .status(status)
                    .warning(isWarning)
                    .build();
        }).collect(Collectors.toList());

        return AdminSchoolReportResponse.builder()
                .summary(summaryDto)
                .rows(rowDtos)
                .build();
    }
}
