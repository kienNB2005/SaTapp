package ken.example.dekiru.student.service;

import ken.example.dekiru.academic.dto.SemesterResponse;
import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.academic.mapper.SemesterMapper;
import ken.example.dekiru.academic.repository.SemesterRepository;
import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.student.dto.AttendanceOverviewResponse;
import ken.example.dekiru.student.dto.AttendanceScreenResponse;
import ken.example.dekiru.student.dto.AttendanceSubjectResponse;
import ken.example.dekiru.student.entity.StudentAttendanceOverview;
import ken.example.dekiru.student.mapper.AttendanceOverviewMapper;
import ken.example.dekiru.student.mapper.AttendanceSubjectMapper;
import ken.example.dekiru.student.repository.StudentAttendanceBySubjectRepository;
import ken.example.dekiru.student.repository.StudentAttendanceOverviewRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentAttendanceService {

      StudentAttendanceOverviewRepository overviewRepository;
      StudentAttendanceBySubjectRepository bySubjectRepository;
      AttendanceOverviewMapper overviewMapper;
      AttendanceSubjectMapper subjectMapper;
      SecurityUtils securityUtils;
      SemesterRepository semesterRepository;
      SemesterMapper semesterMapper;
    public List<SemesterResponse> getSemestersForFilter() {
        // Tự động lấy studentId từ token đăng nhập
        Long studentId = securityUtils.getCurrentStudentId();

        List<Semester> semesters = semesterRepository.findStartedSemestersByStudentId(
                studentId,
                LocalDate.now()
        );
        return semesterMapper.toSemesterResponseList(semesters);
    }
    /**
     * Trả toàn bộ dữ liệu màn hình chuyên cần trong 1 lần gọi.
     * View tự filter học kỳ active — không cần truyền semesterId.
     */
    public AttendanceScreenResponse getAttendanceScreen(Long semesterId) {
        Long studentId = securityUtils.getCurrentStudentId();

        // Overview — ném lỗi rõ ràng nếu không tìm thấy
        // (xảy ra khi chưa có học kỳ active hoặc SV chưa có lịch)
        StudentAttendanceOverview overviewEntity = overviewRepository
                .findByStudentIdAndSemesterId(studentId, semesterId)
                .orElseThrow(() -> new AppException(ErrorCode.NO_SEMESTER_EXISTS));

        AttendanceOverviewResponse overview = overviewMapper.toResponse(overviewEntity);

        // Danh sách môn — đã sort ở DB: danger → warning → safe → tên môn
        // toResponseList() là method sinh sẵn bởi MapStruct, không cần .stream().map()
        List<AttendanceSubjectResponse> subjects = subjectMapper.toResponseList(
                bySubjectRepository.findByStudentIdAndSemesterId(studentId, semesterId)
        );

        return AttendanceScreenResponse.builder()
                .overview(overview)
                .subjects(subjects)
                .build();
    }

    /**
     * Lấy riêng danh sách môn đang nguy hiểm (vượt ngưỡng cấm thi).
     * Dùng cho widget cảnh báo nhanh hoặc push notification.
     */
    public List<AttendanceSubjectResponse> getDangerSubjects() {
        Long studentId = securityUtils.getCurrentStudentId();
        return subjectMapper.toResponseList(
                bySubjectRepository.findDangerSubjectsByStudentId(studentId)
        );
    }
}