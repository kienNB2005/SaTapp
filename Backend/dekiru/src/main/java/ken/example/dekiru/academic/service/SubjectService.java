package ken.example.dekiru.academic.service;

import ken.example.dekiru.academic.dto.CreateSubjectRequest;
import ken.example.dekiru.academic.dto.UpdateSubjectRequest;
import ken.example.dekiru.academic.dto.SubjectResponse;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.entity.Subject;
import ken.example.dekiru.academic.mapper.SubjectMapper;
import ken.example.dekiru.schedule.repository.ScheduleRepository;
import ken.example.dekiru.academic.repository.SubjectRepository;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectService {

    SubjectRepository subjectRepository;
    SubjectMapper subjectMapper;
    private final ScheduleRepository scheduleRepository;

    // Lấy danh sách tất cả các môn học
    public List<SubjectResponse> getAllSubjects() {
        List<Subject> subjects = subjectRepository.findAll();
        return subjectMapper.toSubjectResponseList(subjects);
    }

    // Lấy thông tin môn học theo ID
    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_EXISTED));
        return subjectMapper.toSubjectResponse(subject);
    }

    @Transactional
    public ImportResponse importExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_IS_EMPTY);
        }

        int successCount = 0;
        int updateCount = 0;
        int errorCount = 0;
        List<String> errorDetails = new ArrayList<>();
        List<Subject> subjectsToSave = new ArrayList<>();
        Set<String> codesInCurrentFile = new HashSet<>();
        int totalRows = 0;

        // PHASE 1: Đọc file Excel
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum();

            // ✅ Validate headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }
            List<String> expectedHeaders = List.of("Mã môn", "Tên môn", "Số tín chỉ");
            if (!validateHeaders(headerRow, expectedHeaders)) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }

            List<String> codesInFile = new ArrayList<>();
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String code = getCellValue(row.getCell(0));
                if (!code.isEmpty()) {
                    codesInFile.add(code);
                }
            }

            Map<String, Subject> existingSubjectsMap = new HashMap<>();
            if (!codesInFile.isEmpty()) {
                List<Subject> existingSubjects = subjectRepository.findAllByCodeIn(codesInFile);
                for (Subject subject : existingSubjects) {
                    existingSubjectsMap.put(subject.getCode(), subject);
                }
            }

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String creditsStr = getCellValue(row.getCell(2));

                    if (code.isEmpty() && name.isEmpty()) {
                        continue;
                    }

                    if (code.isEmpty() || name.isEmpty()) {
                        throw new RuntimeException("Mã môn hoặc Tên môn không được để trống");
                    }

                    if (codesInCurrentFile.contains(code)) {
                        throw new RuntimeException("Mã môn '" + code + "' bị trùng trong file Excel");
                    }
                    codesInCurrentFile.add(code);

                    Byte credits = 3;
                    if (!creditsStr.isEmpty()) {
                        try {
                            credits = Byte.parseByte(creditsStr);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Số tín chỉ phải là số");
                        }
                    }

                    Subject existingSubject = existingSubjectsMap.get(code);

                    if (existingSubject != null) {
                        if (!existingSubject.getName().equals(name) || !existingSubject.getCredits().equals(credits)) {
                            existingSubject.setName(name);
                            existingSubject.setCredits(credits);
                            subjectsToSave.add(existingSubject);
                            updateCount++;
                        }
                    } else {
                        Subject newSubject = Subject.builder()
                                .code(code)
                                .name(name)
                                .credits(credits)
                                .build();
                        subjectsToSave.add(newSubject);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorDetails.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_PROCESSING_ERROR);
        }

        // PHASE 2: Lưu Database (ngoài try-catch file)
        if (!subjectsToSave.isEmpty()) {
            subjectRepository.saveAll(subjectsToSave);
        }

        return ImportResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .updateCount(updateCount)
                .errorCount(errorCount)
                .errors(errorDetails)
                .build();
    }

    // ✅ Helper: Validate headers
    private boolean validateHeaders(Row headerRow, List<String> expectedHeaders) {
        for (int i = 0; i < expectedHeaders.size(); i++) {
            Cell cell = headerRow.getCell(i);
            String headerValue = getCellValue(cell).trim();
            if (!headerValue.equalsIgnoreCase(expectedHeaders.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    // ✅ Sửa môn học (chỉ sửa name, credits)
    public SubjectResponse updateSubject(Long id, UpdateSubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_EXISTED));
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            subject.setName(request.getName());
        }
        
        if (request.getCredits() != null && request.getCredits() > 0) {
            subject.setCredits(request.getCredits());
        }
        
        Subject updated = subjectRepository.save(subject);
        return subjectMapper.toSubjectResponse(updated);
    }

    // ✅ Xóa môn học
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_EXISTED));
        
        // Note: Khi ScheduleRepository được implement, thêm check:
         long scheduleCount = scheduleRepository.countBySubjectId(id);
         if (scheduleCount > 0) {
             throw new AppException(ErrorCode.SUBJECT_HAS_SCHEDULES);
         }
        
        subjectRepository.deleteById(id);
    }

    public SubjectResponse createSubject(CreateSubjectRequest request) {
        if (subjectRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.SUBJECT_EXISTED);
        }
        Subject subject = Subject.builder()
                .code(request.getCode())
                .name(request.getName())
                .credits(request.getCredits() != null ? request.getCredits() : (byte)3)
                .build();
        Subject saved = subjectRepository.save(subject);
        return subjectMapper.toSubjectResponse(saved);
    }
}
