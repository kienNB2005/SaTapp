package ken.example.dekiru.student.controller;

import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.student.entity.StudentSchedule;
import ken.example.dekiru.student.entity.StudentToday;
import ken.example.dekiru.student.service.StudentScheduleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me/schedules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentScheduleController {
      StudentScheduleService studentScheduleService;

    @GetMapping("/today")
    public ApiResponse<List<StudentToday>> getToday(){
        return ApiResponse.success(
            studentScheduleService.getTodaySchedule()
        );
    }

    @GetMapping
    public ApiResponse<List<StudentSchedule>> getSchedule(
            @RequestParam(required = false, defaultValue = "0") Integer weekNumber) {
        return ApiResponse.success(
                studentScheduleService.getWeekSchedule( weekNumber));
    }
}