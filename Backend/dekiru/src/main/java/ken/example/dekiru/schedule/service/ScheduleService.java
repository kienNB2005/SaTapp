package ken.example.dekiru.schedule.service;
import ken.example.dekiru.academic.entity.Subject;
import ken.example.dekiru.academic.repository.LecturerRepository;
import ken.example.dekiru.academic.entity.Semester;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.schedule.entity.Schedule;
import ken.example.dekiru.schedule.repository.ScheduleRepository;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.academic.entity.Room;
import ken.example.dekiru.academic.repository.RoomRepository;
import ken.example.dekiru.academic.entity.AdministrativeClass;
import ken.example.dekiru.academic.repository.SubjectRepository;
import ken.example.dekiru.academic.repository.SemesterRepository;
import ken.example.dekiru.academic.repository.AdministrativeClassRepository;

import ken.example.dekiru.schedule.dto.ScheduleExcelDTO;
import ken.example.dekiru.schedule.dto.SchedulePreviewResponse;


import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import ken.example.dekiru.schedule.dto.ScheduleResponse;
import ken.example.dekiru.common.specification.ScheduleSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduleService {

    ScheduleRepository scheduleRepository;
    SemesterRepository semesterRepository;
    SubjectRepository subjectRepository;
    AdministrativeClassRepository administrativeClassRepository;
    LecturerRepository lecturerRepository;
    RoomRepository roomRepository;
    Validator validator;

    /**
     * Lấy danh sách TKB phân trang + tìm kiếm + lọc.
     * Nếu semesterId == null → tự lấy học kỳ đang active.
     */
    @Transactional(readOnly = true)
    public Page<ScheduleResponse> getSchedules(Long semesterId, String search,
                                                Long adminClassId, Long lecturerId,
                                                Integer dayOfWeek, Pageable pageable) {
        // Nếu không truyền semesterId → lấy học kỳ đang active
        if (semesterId == null) {
            var activeSemesterOpt = semesterRepository.findByIsActiveTrue();

            if (activeSemesterOpt.isPresent()) {
                // Kịch bản 1: Có học kỳ hoạt động -> Lấy làm mặc định
                semesterId = activeSemesterOpt.get().getId();
            } else {
                // Không có học kỳ nào hoạt động, đi kiểm tra tổng số lượng
                long totalSemesters = semesterRepository.count();

                if (totalSemesters == 0) {
                    // Kịch bản 2: Database trống trơn -> Yêu cầu tạo mới
                    throw new AppException(ErrorCode.NO_SEMESTER_EXISTS);
                } else {
                    // Kịch bản 3: Có học kỳ, nhưng không cái nào active -> Ép người dùng tự chọn
                    throw new AppException(ErrorCode.SEMESTER_MUST_BE_SELECTED);
                }
            }
        }

        Specification<Schedule> spec = Specification
                .where(ScheduleSpecifications.hasSemesterId(semesterId))
                .and(ScheduleSpecifications.hasSearchText(search))
                .and(ScheduleSpecifications.hasAdminClassId(adminClassId))
                .and(ScheduleSpecifications.hasLecturerId(lecturerId))
                .and(ScheduleSpecifications.hasDayOfWeek(dayOfWeek));

        Page<Schedule> page = scheduleRepository.findAll(spec, pageable);

        return page.map(s -> ScheduleResponse.builder()
                .id(s.getId())
                .adminClassCode(s.getAdminClass().getCode())
                .adminClassName(s.getAdminClass().getName())
                .subjectCode(s.getSubject().getCode())
                .subjectName(s.getSubject().getName())
                .lecturerCode(s.getLecturer().getLecturerCode())
                .lecturerName(s.getLecturer().getUser().getFullName())
                .roomCode(s.getRoom().getCode())
                .dayOfWeek(s.getDayOfWeek())
                .periodStart(s.getPeriodStart())
                .periodEnd(s.getPeriodEnd())
                .weekStart(s.getWeekStart())
                .weekEnd(s.getWeekEnd())
                .totalSessions(s.getTotalSessions())
                .build()
        );
    }

    /**
     * Bước 1: Preview — Đọc file Excel, validate từng dòng, trả về danh sách preview.
     */
    @Transactional(readOnly = true)
    public List<SchedulePreviewResponse> previewImportSchedule(MultipartFile file) {
        List<ScheduleExcelDTO> rawList = new ArrayList<>();

        // ---- Đọc Excel ----
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("File Excel không có dữ liệu");
            }

            String[] expectedHeaders = {
                "Mã lớp", "Mã môn", "Mã giảng viên", "Mã phòng",
                "Thứ", "Tiết bắt đầu", "Tiết kết thúc", "Tuần bắt đầu", "Tuần kết thúc"
            };
            for (int i = 0; i < expectedHeaders.length; i++) {
                String actual = getCellValue(headerRow.getCell(i));
                if (!expectedHeaders[i].equalsIgnoreCase(actual)) {
                    throw new RuntimeException(
                        "Sai định dạng file mẫu. Cột " + (i + 1) + " phải là '" + expectedHeaders[i] + "' (đọc được: '" + actual + "')"
                    );
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Bỏ qua dòng hoàn toàn trống
                boolean allBlank = true;
                for (int c = 0; c < expectedHeaders.length; c++) {
                    if (!getCellValue(row.getCell(c)).isEmpty()) {
                        allBlank = false;
                        break;
                    }
                }
                if (allBlank) continue;

                ScheduleExcelDTO dto = ScheduleExcelDTO.builder()
                        .rowIndex(i + 1)
                        .adminClassCode(getCellValue(row.getCell(0)))
                        .subjectCode(getCellValue(row.getCell(1)))
                        .lecturerCode(getCellValue(row.getCell(2)))
                        .roomCode(getCellValue(row.getCell(3)))
                        .dayOfWeek(getIntCellValue(row.getCell(4)))
                        .periodStart(getIntCellValue(row.getCell(5)))
                        .periodEnd(getIntCellValue(row.getCell(6)))
                        .weekStart(getIntCellValue(row.getCell(7)))
                        .weekEnd(getIntCellValue(row.getCell(8)))
                        .build();
                rawList.add(dto);
            }
        } catch (RuntimeException e) {
            throw e; // rethrow validation errors
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage());
        }

        if (rawList.isEmpty()) {
            throw new RuntimeException("File Excel không có dữ liệu nào để import");
        }

        // ---- Pre-fetch tất cả entity cần thiết (tránh N+1) ----
        List<String> adminClassCodes = rawList.stream().map(ScheduleExcelDTO::getAdminClassCode).filter(Objects::nonNull).toList();
        List<String> subjectCodes    = rawList.stream().map(ScheduleExcelDTO::getSubjectCode).filter(Objects::nonNull).toList();
        List<String> lecturerCodes   = rawList.stream().map(ScheduleExcelDTO::getLecturerCode).filter(Objects::nonNull).toList();
        List<String> roomCodes       = rawList.stream().map(ScheduleExcelDTO::getRoomCode).filter(Objects::nonNull).toList();

        Map<String, AdministrativeClass> classMap = administrativeClassRepository.findAllByCodeIn(adminClassCodes)
                .stream().collect(Collectors.toMap(AdministrativeClass::getCode, c -> c, (a, b) -> a));
        Map<String, Subject> subjectMap = subjectRepository.findAllByCodeIn(subjectCodes)
                .stream().collect(Collectors.toMap(Subject::getCode, s -> s, (a, b) -> a));
        Map<String, Lecturer> lecturerMap = lecturerRepository.findAllByLecturerCodeIn(lecturerCodes)
                .stream().collect(Collectors.toMap(Lecturer::getLecturerCode, l -> l, (a, b) -> a));
        Map<String, Room> roomMap = roomRepository.findAllByCodeIn(roomCodes)
                .stream().collect(Collectors.toMap(Room::getCode, r -> r, (a, b) -> a));

        // ---- Validate từng dòng ----
        List<SchedulePreviewResponse> result = new ArrayList<>();

        for (ScheduleExcelDTO dto : rawList) {
            SchedulePreviewResponse response = SchedulePreviewResponse.builder()
                    .schedule(dto)
                    .isValid(true)
                    .errors(new ArrayList<>())
                    .build();

            // Jakarta Bean Validation
            Set<ConstraintViolation<ScheduleExcelDTO>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                response.setValid(false);
                violations.forEach(v -> response.getErrors().add(v.getMessage()));
            }

            // Kiểm tra mã lớp có tồn tại không
            if (dto.getAdminClassCode() != null && !dto.getAdminClassCode().isBlank()) {
                AdministrativeClass ac = classMap.get(dto.getAdminClassCode());
                if (ac == null) {
                    response.setValid(false);
                    response.getErrors().add("Mã lớp '" + dto.getAdminClassCode() + "' không tồn tại");
                } else {
                    response.setAdminClassName(ac.getName());
                }
            }

            // Kiểm tra mã môn
            if (dto.getSubjectCode() != null && !dto.getSubjectCode().isBlank()) {
                Subject sub = subjectMap.get(dto.getSubjectCode());
                if (sub == null) {
                    response.setValid(false);
                    response.getErrors().add("Mã môn '" + dto.getSubjectCode() + "' không tồn tại");
                } else {
                    response.setSubjectName(sub.getName());
                }
            }

            // Kiểm tra mã GV
            if (dto.getLecturerCode() != null && !dto.getLecturerCode().isBlank()) {
                Lecturer lec = lecturerMap.get(dto.getLecturerCode());
                if (lec == null) {
                    response.setValid(false);
                    response.getErrors().add("Mã giảng viên '" + dto.getLecturerCode() + "' không tồn tại");
                } else {
                    response.setLecturerName(lec.getUser().getFullName());
                }
            }

            // Kiểm tra mã phòng
            if (dto.getRoomCode() != null && !dto.getRoomCode().isBlank()) {
                Room room = roomMap.get(dto.getRoomCode());
                if (room == null) {
                    response.setValid(false);
                    response.getErrors().add("Mã phòng '" + dto.getRoomCode() + "' không tồn tại");
                }
            }

            // Kiểm tra tiết bắt đầu <= tiết kết thúc
            if (dto.getPeriodStart() != null && dto.getPeriodEnd() != null) {
                if (dto.getPeriodStart() > dto.getPeriodEnd()) {
                    response.setValid(false);
                    response.getErrors().add("Tiết bắt đầu phải <= tiết kết thúc");
                }
            }

            // Kiểm tra tuần bắt đầu <= tuần kết thúc
            if (dto.getWeekStart() != null && dto.getWeekEnd() != null) {
                if (dto.getWeekStart() > dto.getWeekEnd()) {
                    response.setValid(false);
                    response.getErrors().add("Tuần bắt đầu phải <= tuần kết thúc");
                }
                // Tính số buổi dự kiến
                response.setEstimatedSessions(dto.getWeekEnd() - dto.getWeekStart() + 1);
            }

            result.add(response);
        }

        return result;
    }

    /**
     * Bước 2: Confirm — Lưu các dòng hợp lệ vào DB, sinh ClassSession tự động.
     */
    @Transactional
    public Map<String, Object> confirmImportSchedule(Long semesterId, List<ScheduleExcelDTO> schedules) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ với ID: " + semesterId));

        // Pre-fetch maps
        List<String> adminClassCodes = schedules.stream().map(ScheduleExcelDTO::getAdminClassCode).toList();
        List<String> subjectCodes    = schedules.stream().map(ScheduleExcelDTO::getSubjectCode).toList();
        List<String> lecturerCodes   = schedules.stream().map(ScheduleExcelDTO::getLecturerCode).toList();
        List<String> roomCodes       = schedules.stream().map(ScheduleExcelDTO::getRoomCode).toList();

        Map<String, AdministrativeClass> classMap = administrativeClassRepository.findAllByCodeIn(adminClassCodes)
                .stream().collect(Collectors.toMap(AdministrativeClass::getCode, c -> c, (a, b) -> a));
        Map<String, Subject> subjectMap = subjectRepository.findAllByCodeIn(subjectCodes)
                .stream().collect(Collectors.toMap(Subject::getCode, s -> s, (a, b) -> a));
        Map<String, Lecturer> lecturerMap = lecturerRepository.findAllByLecturerCodeIn(lecturerCodes)
                .stream().collect(Collectors.toMap(Lecturer::getLecturerCode, l -> l, (a, b) -> a));
        Map<String, Room> roomMap = roomRepository.findAllByCodeIn(roomCodes)
                .stream().collect(Collectors.toMap(Room::getCode, r -> r, (a, b) -> a));

        List<Schedule> newSchedules = new ArrayList<>();

        for (ScheduleExcelDTO dto : schedules) {
            AdministrativeClass adminClass = classMap.get(dto.getAdminClassCode());
            Subject subject = subjectMap.get(dto.getSubjectCode());
            Lecturer lecturer = lecturerMap.get(dto.getLecturerCode());
            Room room = roomMap.get(dto.getRoomCode());

            if (adminClass == null || subject == null || lecturer == null || room == null) {
                continue; // bỏ qua dòng lỗi (đáng lẽ đã lọc ở preview)
            }

            byte weekStart = dto.getWeekStart().byteValue();
            byte weekEnd   = dto.getWeekEnd().byteValue();

            Schedule schedule = Schedule.builder()
                    .semester(semester)
                    .subject(subject)
                    .adminClass(adminClass)
                    .lecturer(lecturer)
                    .room(room)
                    .dayOfWeek(dto.getDayOfWeek().byteValue())
                    .periodStart(dto.getPeriodStart().byteValue())
                    .periodEnd(dto.getPeriodEnd().byteValue())
                    .weekStart(weekStart)
                    .weekEnd(weekEnd)
                    .build();

            newSchedules.add(schedule);
        }

        scheduleRepository.saveAll(newSchedules);
        // flush để đảm bảo ID đã được gán trước khi gọi stored procedure
        scheduleRepository.flush();

        int totalSessionsGenerated = 0;

        for (Schedule saved : newSchedules) {
            scheduleRepository.generateSessions(saved.getId());
            Schedule reloaded = scheduleRepository.findById(saved.getId()).orElse(saved);
            Byte ts = reloaded.getTotalSessions();
            totalSessionsGenerated += (ts != null ? ts : 0);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSchedules", newSchedules.size());
        stats.put("totalSessions", totalSessionsGenerated);
        return stats;
    }

    // ---- Helpers đọc cell ----
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            double numVal = cell.getNumericCellValue();
            if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                return String.valueOf((long) numVal);
            }
            return String.valueOf(numVal);
        }
        return "";
    }

    private Integer getIntCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) return null;
                return Integer.parseInt(val);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }
}
