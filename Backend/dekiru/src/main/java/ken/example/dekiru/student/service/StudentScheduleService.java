package ken.example.dekiru.student.service;

import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.academic.repository.SemesterRepository;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.student.entity.StudentSchedule;
import ken.example.dekiru.student.entity.StudentToday;
import ken.example.dekiru.student.repository.StudentScheduleRepository;
import ken.example.dekiru.student.repository.StudentTodayRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentScheduleService {
      StudentTodayRepository studentTodayRepository;
      SemesterRepository semesterRepository;
      StudentScheduleRepository studentScheduleRepository;
      SecurityUtils securityUtils;
    public List<StudentToday> getTodaySchedule() {
        Long studentId = securityUtils.getCurrentStudentId();
        return studentTodayRepository.findByStudentId(studentId);
    }

    public List<StudentSchedule> getWeekSchedule( Integer weekNumber) {
        Long studentId = securityUtils.getCurrentStudentId();
        Semester semester = semesterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NO_SEMESTER_EXISTS));

        LocalDate start = semester.getStartDate();
        LocalDate anchorMonday = start.minusDays(start.getDayOfWeek().getValue() - 1);

        // Nếu không truyền weekNumber thì tự tính tuần hiện tại
        if (weekNumber == 0) {
            weekNumber = (int) ChronoUnit.WEEKS.between(anchorMonday, LocalDate.now()) + 1;
        }

        int relativeWeek = weekNumber - (semester.getStartWeek() != null ? semester.getStartWeek() : 1);
        LocalDate weekStart = anchorMonday.plusWeeks(relativeWeek);
        LocalDate weekEnd = weekStart.plusDays(6);

        return studentScheduleRepository
                .findByStudentIdAndWeek(studentId, weekStart, weekEnd);
    }
}