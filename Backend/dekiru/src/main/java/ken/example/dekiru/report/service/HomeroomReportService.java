package ken.example.dekiru.report.service;

import ken.example.dekiru.academic.entity.AdministrativeClass;
import ken.example.dekiru.academic.repository.AdministrativeClassRepository;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.report.dto.LecturerReportResponse;
import ken.example.dekiru.report.dto.LecturerReportStudentDto;
import ken.example.dekiru.report.dto.LecturerReportStudentProjection;
import ken.example.dekiru.report.dto.LecturerReportSummaryDto;
import ken.example.dekiru.report.repository.HomeroomReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeroomReportService {

    private final HomeroomReportRepository homeroomReportRepository;
    private final AdministrativeClassRepository administrativeClassRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public LecturerReportResponse getReportData(Long semesterId, Long subjectId, Long adminClassId, Double absentLimitPct) {
        verifyHomeroomTeacherAccess(adminClassId);

        List<LecturerReportStudentProjection> rawData = homeroomReportRepository.getStudentReportData(semesterId, subjectId, adminClassId);
        List<Object[]> sessionStats = homeroomReportRepository.getSessionStats(semesterId, subjectId, adminClassId);

        int totalSessions = 0;
        int finishedSessions = 0;
        if (!sessionStats.isEmpty() && sessionStats.get(0) != null) {
            Object[] stat = sessionStats.get(0);
            totalSessions = stat[0] != null ? ((Number) stat[0]).intValue() : 0;
            finishedSessions = stat[1] != null ? ((Number) stat[1]).intValue() : 0;
        }

        double threshold = absentLimitPct != null ? absentLimitPct : 20.0;
        int underThresholdCount = 0;
        double totalAttendanceRate = 0.0;
        int validStudentsForRate = 0;
        
        List<LecturerReportStudentDto> students = rawData.stream().map(proj -> {
            boolean isDanger = false;
            double absentPct = 0.0;
            if (proj.getTotalSessions() != null && proj.getTotalSessions() > 0) {
                absentPct = (proj.getAbsentCount().doubleValue() / proj.getTotalSessions()) * 100.0;
            }
            if (absentPct >= threshold) {
                isDanger = true;
            }

            double rate = proj.getAttendanceRate() != null ? proj.getAttendanceRate() : 100.0;

            return LecturerReportStudentDto.builder()
                    .studentCode(proj.getStudentCode())
                    .fullName(proj.getStudentName())
                    .presentCount(proj.getPresentCount())
                    .absentCount(proj.getAbsentCount())
                    .excusedCount(proj.getExcusedCount())
                    .lateCount(proj.getLateCount())
                    .leftEarlyCount(proj.getLeftEarlyCount())
                    .attendanceRate(rate)
                    .isDanger(isDanger)
                    .build();
        }).collect(Collectors.toList());

        for (LecturerReportStudentDto student : students) {
            if (student.getIsDanger()) {
                underThresholdCount++;
            }
            totalAttendanceRate += student.getAttendanceRate();
            validStudentsForRate++;
        }

        double avgAttendanceRate = validStudentsForRate == 0 ? 0.0 : totalAttendanceRate / validStudentsForRate;

        LecturerReportSummaryDto summary = LecturerReportSummaryDto.builder()
                .totalStudents(students.size())
                .avgAttendanceRate(avgAttendanceRate)
                .underThresholdCount(underThresholdCount)
                .finishedSessions(finishedSessions)
                .totalSessions(totalSessions)
                .build();

        return LecturerReportResponse.builder()
                .summary(summary)
                .students(students)
                .build();
    }

    public ByteArrayInputStream exportExcel(Long semesterId, Long subjectId, Long adminClassId, Double absentLimitPct) {
        LecturerReportResponse data = getReportData(semesterId, subjectId, adminClassId, absentLimitPct);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BaoCaoChuNhiem");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "Mã sinh viên", "Họ tên", "Số buổi có mặt", "Số buổi vắng mặt", "Số buổi vắng có phép", "Số lần đi muộn", "Số lần về sớm", "Tỉ lệ đi học (%)", "Trạng thái"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int stt = 1;
            for (LecturerReportStudentDto student : data.getStudents()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(student.getStudentCode());
                row.createCell(2).setCellValue(student.getFullName());
                row.createCell(3).setCellValue(student.getPresentCount());
                row.createCell(4).setCellValue(student.getAbsentCount());
                row.createCell(5).setCellValue(student.getExcusedCount());
                row.createCell(6).setCellValue(student.getLateCount());
                row.createCell(7).setCellValue(student.getLeftEarlyCount());
                
                Cell rateCell = row.createCell(8);
                rateCell.setCellValue(student.getAttendanceRate());
                
                Cell statusCell = row.createCell(9);
                statusCell.setCellValue(student.getIsDanger() ? "Cảnh báo vắng nhiều" : "Bình thường");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel", e);
        }
    }

    private void verifyHomeroomTeacherAccess(Long adminClassId) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        AdministrativeClass adminClass = administrativeClassRepository.findById(adminClassId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp hành chính."));
        
        if (adminClass.getHomeroomTeacher() == null || !adminClass.getHomeroomTeacher().getId().equals(lecturerId)) {
            throw new RuntimeException("Access Denied: Bạn không phải là giáo viên chủ nhiệm của lớp này.");
        }
    }
}
