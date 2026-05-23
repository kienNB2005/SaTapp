package ken.example.dekiru.report.controller;

import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.report.dto.FilterItemDto;
import ken.example.dekiru.report.dto.LecturerReportResponse;
import ken.example.dekiru.report.service.LecturerReportService;
import ken.example.dekiru.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports/lecturer")
@RequiredArgsConstructor
public class LecturerReportController {

    private final ScheduleRepository scheduleRepository;
    private final LecturerReportService lecturerReportService;
    private final SecurityUtils securityUtils;

    @GetMapping("/semesters")
    public ApiResponse<List<FilterItemDto>> getSemesters() {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        List<FilterItemDto> res = scheduleRepository.findDistinctSemestersByLecturer(lecturerId)
                .stream()
                .map(s -> new FilterItemDto(s.getId(), s.getName(), s.getName()))
                .collect(Collectors.toList());
        return ApiResponse.<List<FilterItemDto>>builder()
                .code(1000)
                .message("Lấy danh sách học kỳ thành công")
                .result(res)
                .build();
    }

    @GetMapping("/classes")
    public ApiResponse<List<FilterItemDto>> getClasses(@RequestParam("semesterId") Long semesterId) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        List<FilterItemDto> res = scheduleRepository.findDistinctAdminClassesByLecturerAndSemester(lecturerId, semesterId)
                .stream()
                .map(c -> new FilterItemDto(c.getId(), c.getCode(), c.getName()))
                .collect(Collectors.toList());
        return ApiResponse.<List<FilterItemDto>>builder()
                .code(1000)
                .message("Lấy danh sách lớp thành công")
                .result(res)
                .build();
    }

    @GetMapping("/subjects")
    public ApiResponse<List<FilterItemDto>> getSubjects(
            @RequestParam("semesterId") Long semesterId,
            @RequestParam("adminClassId") Long adminClassId) {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        List<FilterItemDto> res = scheduleRepository.findDistinctSubjectsByLecturerAndClassAndSemester(lecturerId, adminClassId, semesterId)
                .stream()
                .map(s -> new FilterItemDto(s.getId(), s.getCode(), s.getName()))
                .collect(Collectors.toList());
        return ApiResponse.<List<FilterItemDto>>builder()
                .code(1000)
                .message("Lấy danh sách môn học thành công")
                .result(res)
                .build();
    }

    @GetMapping("/data")
    public ApiResponse<LecturerReportResponse> getReportData(
            @RequestParam("semesterId") Long semesterId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("adminClassId") Long adminClassId,
            @RequestParam(value = "absentLimitPct", required = false, defaultValue = "20.0") Double absentLimitPct) {
        
        LecturerReportResponse data = lecturerReportService.getReportData(semesterId, subjectId, adminClassId, absentLimitPct);
        return ApiResponse.<LecturerReportResponse>builder()
                .code(1000)
                .message("Lấy dữ liệu báo cáo thành công")
                .result(data)
                .build();
    }

    @GetMapping("/export/excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam("semesterId") Long semesterId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("adminClassId") Long adminClassId,
            @RequestParam(value = "absentLimitPct", required = false, defaultValue = "20.0") Double absentLimitPct) {

        ByteArrayInputStream in = lecturerReportService.exportExcel(semesterId, subjectId, adminClassId, absentLimitPct);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=BaoCaoDiemDanh.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
