package ken.example.dekiru.report.controller;

import ken.example.dekiru.academic.repository.AdministrativeClassRepository;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.report.dto.FilterItemDto;
import ken.example.dekiru.report.dto.LecturerReportResponse;
import ken.example.dekiru.report.service.HomeroomReportService;
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
@RequestMapping("/api/v1/reports/homeroom")
@RequiredArgsConstructor
public class HomeroomReportController {

    private final AdministrativeClassRepository administrativeClassRepository;
    private final ScheduleRepository scheduleRepository;
    private final HomeroomReportService homeroomReportService;
    private final SecurityUtils securityUtils;

    @GetMapping("/classes")
    public ApiResponse<List<FilterItemDto>> getClasses() {
        Long lecturerId = securityUtils.getCurrentLecturerId();
        List<FilterItemDto> res = administrativeClassRepository.findByHomeroomTeacher_Id(lecturerId)
                .stream()
                .map(c -> new FilterItemDto(c.getId(), c.getCode(), c.getName()))
                .collect(Collectors.toList());
        return ApiResponse.<List<FilterItemDto>>builder()
                .code(1000)
                .message("Lấy danh sách lớp chủ nhiệm thành công")
                .result(res)
                .build();
    }

    @GetMapping("/semesters")
    public ApiResponse<List<FilterItemDto>> getSemesters(@RequestParam("adminClassId") Long adminClassId) {
        List<FilterItemDto> res = scheduleRepository.findDistinctSemestersByAdminClassId(adminClassId)
                .stream()
                .map(s -> new FilterItemDto(s.getId(), s.getName(), s.getName()))
                .collect(Collectors.toList());
        return ApiResponse.<List<FilterItemDto>>builder()
                .code(1000)
                .message("Lấy danh sách học kỳ thành công")
                .result(res)
                .build();
    }

    @GetMapping("/subjects")
    public ApiResponse<List<FilterItemDto>> getSubjects(
            @RequestParam("adminClassId") Long adminClassId,
            @RequestParam("semesterId") Long semesterId) {
        List<FilterItemDto> res = scheduleRepository.findDistinctSubjectsByAdminClassIdAndSemesterId(adminClassId, semesterId)
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
            @RequestParam("adminClassId") Long adminClassId,
            @RequestParam("semesterId") Long semesterId,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "absentLimitPct", required = false, defaultValue = "20.0") Double absentLimitPct) {
        
        LecturerReportResponse data = homeroomReportService.getReportData(semesterId, subjectId, adminClassId, absentLimitPct);
        return ApiResponse.<LecturerReportResponse>builder()
                .code(1000)
                .message("Lấy dữ liệu báo cáo chủ nhiệm thành công")
                .result(data)
                .build();
    }

    @GetMapping("/export/excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam("adminClassId") Long adminClassId,
            @RequestParam("semesterId") Long semesterId,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "absentLimitPct", required = false, defaultValue = "20.0") Double absentLimitPct) {

        ByteArrayInputStream in = homeroomReportService.exportExcel(semesterId, subjectId, adminClassId, absentLimitPct);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=BaoCaoChuNhiem.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
