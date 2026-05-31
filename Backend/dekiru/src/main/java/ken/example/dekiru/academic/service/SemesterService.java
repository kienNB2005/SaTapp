package ken.example.dekiru.academic.service;

import ken.example.dekiru.academic.dto.CreateSemesterRequest;
import ken.example.dekiru.academic.dto.UpdateSemesterRequest;
import ken.example.dekiru.academic.dto.SemesterResponse;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.academic.mapper.SemesterMapper;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import ken.example.dekiru.schedule.repository.ScheduleRepository;
import ken.example.dekiru.academic.repository.SemesterRepository;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SemesterService {

    SemesterRepository semesterRepository;
    SemesterMapper semesterMapper;
    private final ScheduleRepository scheduleRepository;
    private final ClassSessionRepository classSessionRepository;

    // Lấy danh sách tất cả các học kỳ
    public List<SemesterResponse> getAllSemesters() {
        List<Semester> semesters = semesterRepository.findAll();
        return semesterMapper.toSemesterResponseList(semesters);
    }

    // Lấy thông tin học kỳ theo ID
    public SemesterResponse getSemesterById(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_EXISTED));
        return semesterMapper.toSemesterResponse(semester);
    }

    // Tạo học kỳ mới
    @Transactional
    public SemesterResponse createSemester(CreateSemesterRequest request) {
        // Validate startDate phải là Thứ Hai
        DayOfWeek dayOfWeek = request.getStartDate().getDayOfWeek();
        if (dayOfWeek != DayOfWeek.MONDAY) {
            throw new AppException(ErrorCode.INVALID_START_DATE);
        }

        // Validate endDate > startDate
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // Validate không được tạo học kỳ ở quá khứ
        if (request.getStartDate().isBefore(java.time.LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_START_DATE_PAST);
        }

        // Validate chống trùng tên
        if (semesterRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.SEMESTER_EXISTED);
        }

        // Validate chống chồng chéo thời gian
        if (semesterRepository.existsByDateRangeOverlap(request.getStartDate(), request.getEndDate())) {
            throw new AppException(ErrorCode.SEMESTER_OVERLAP);
        }

        // Dùng Mapper để convert Request → Entity
        Semester semester = semesterMapper.toSemester(request);
        
        // Cập nhật startWeek
        if (request.getStartWeek() != null) {
            semester.setStartWeek(request.getStartWeek());
        }
        
        // Mặc định isActive = false khi create mới
        semester.setIsActive(false);

        Semester saved = semesterRepository.save(semester);
        return semesterMapper.toSemesterResponse(saved);
    }

    // ✅ Sửa học kỳ (sửa name, isActive)
    @Transactional
    public SemesterResponse updateSemester(Long id, UpdateSemesterRequest request) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_EXISTED));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            if (semesterRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new AppException(ErrorCode.SEMESTER_EXISTED);
            }
            semester.setName(request.getName());
        }

        if (request.getStartWeek() != null) {
            semester.setStartWeek(request.getStartWeek());
        }

        if (request.getIsActive() != null) {
            if (request.getIsActive()) {
                // Bật: kiểm tra có kỳ nào đang active không
                semesterRepository.findByIsActiveTrue().ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new AppException(ErrorCode.ANOTHER_SEMESTER_IS_ACTIVE);
                    }
                });
                semester.setIsActive(true);

            } else {
////                // Tắt: kiểm tra TKB và buổi học
//                boolean hasSchedule = scheduleRepository.existsBySemesterId(id);
//                if (hasSchedule) {
//                    boolean hasUnfinishedSession = classSessionRepository
//                            .existsBySchedule_SemesterIdAndStatusIn(
//                                    id,
//                                    List.of(ClassSession.Status.scheduled, ClassSession.Status.open)
//                            );
//                    if (hasUnfinishedSession) {
//                        throw new AppException(ErrorCode.SEMESTER_HAS_UNFINISHED_SESSIONS);
//                    }
//                }
                semester.setIsActive(false);
            }
        }

        Semester updated = semesterRepository.save(semester);
        return semesterMapper.toSemesterResponse(updated);
    }

    // ✅ Xóa học kỳ
    public void deleteSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_EXISTED));
        
        // Note: Khi ScheduleRepository được implement, thêm check:
         long scheduleCount = scheduleRepository.countBySemesterId(id);
         if (scheduleCount > 0) {
             throw new AppException(ErrorCode.SEMESTER_HAS_SCHEDULES);
         }
        
        semesterRepository.deleteById(id);
    }
}
