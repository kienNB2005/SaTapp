package ken.example.dekiru.academic.service;
import ken.example.dekiru.attendance.entity.ClassSession;
import ken.example.dekiru.schedule.entity.Schedule;

import ken.example.dekiru.academic.dto.CreateRoomRequest;
import ken.example.dekiru.academic.dto.UpdateRoomRequest;
import ken.example.dekiru.academic.dto.RoomResponse;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.entity.Room;
import ken.example.dekiru.academic.mapper.RoomMapper;
import ken.example.dekiru.attendance.repository.ClassSessionRepository;
import ken.example.dekiru.academic.repository.RoomRepository;
import ken.example.dekiru.schedule.repository.ScheduleRepository;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.DataFormatter;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {

    // ✅ Sức chứa mặc định khi không truyền vào
    private static final short DEFAULT_CAPACITY = 50;

    RoomRepository roomRepository;
    RoomMapper roomMapper;

    // ✅ Dùng DataFormatter thay vì setCellType (deprecated)
    private final DataFormatter dataFormatter = new DataFormatter();
    private final ScheduleRepository scheduleRepository;
    private final ClassSessionRepository classSessionRepository;

    public List<RoomResponse> getAllRooms() {
        return roomMapper.toRoomResponseList(roomRepository.findAll());
    }

    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));
        return roomMapper.toRoomResponse(room);
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
        List<Room> roomsToSave = new ArrayList<>();
        Set<String> codesInCurrentFile = new HashSet<>();
        int totalRows = 0;

        // ✅ Toàn bộ xử lý nằm trong try-with-resources — tránh dùng sheet sau khi workbook đóng
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum();

            // Kiểm tra header
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }

            List<String> expectedHeaders = List.of("Mã phòng", "Tên nhà", "Sức chứa");
            if (!validateHeaders(headerRow, expectedHeaders)) {
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }

            // ✅ Trả về sớm nếu không có dòng dữ liệu
            if (totalRows == 0) {
                return ImportResponse.builder()
                        .totalRows(0)
                        .successCount(0)
                        .updateCount(0)
                        .errorCount(0)
                        .errors(List.of())
                        .build();
            }

            // ✅ Batch load tất cả code tồn tại — tránh N+1 query
            List<String> codesInFile = new ArrayList<>();
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String code = getCellValue(row, 0);
                if (!code.isEmpty()) codesInFile.add(code);
            }

            Map<String, Room> existingRoomsMap = new HashMap<>();
            if (!codesInFile.isEmpty()) {
                roomRepository.findAllByCodeIn(codesInFile)
                        .forEach(r -> existingRoomsMap.put(r.getCode(), r));
            }

            // Xử lý từng dòng
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String code     = getCellValue(row, 0);
                    String building = getCellValue(row, 1);
                    String capStr   = getCellValue(row, 2);

                    // Bỏ qua dòng hoàn toàn rỗng
                    if (code.isEmpty() && building.isEmpty()) continue;

                    if (code.isEmpty()) {
                        throw new RuntimeException("Mã phòng không được để trống");
                    }

                    if (codesInCurrentFile.contains(code)) {
                        throw new RuntimeException("Mã phòng '" + code + "' bị trùng trong file");
                    }
                    codesInCurrentFile.add(code);

                    // ✅ Parse capacity — dùng DEFAULT_CAPACITY nếu ô trống
                    short capacity = DEFAULT_CAPACITY;
                    if (!capStr.isEmpty()) {
                        try {
                            capacity = Short.parseShort(capStr);
                            if (capacity <= 0) throw new RuntimeException("Sức chứa phải lớn hơn 0");
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Sức chứa phải là số nguyên hợp lệ");
                        }
                    }

                    Room existingRoom = existingRoomsMap.get(code);
                    if (existingRoom != null) {
                        boolean changed = !existingRoom.getBuilding().equals(building)
                                || !existingRoom.getCapacity().equals(capacity);
                        if (changed) {
                            existingRoom.setBuilding(building);
                            existingRoom.setCapacity(capacity);
                            roomsToSave.add(existingRoom);
                            updateCount++;
                        }
                    } else {
                        roomsToSave.add(Room.builder()
                                .code(code)
                                .building(building)
                                .capacity(capacity)
                                .build());
                        successCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    errorDetails.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

        } catch (AppException e) {
            throw e; // ✅ Không nuốt AppException
        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_PROCESSING_ERROR);
        }

        if (!roomsToSave.isEmpty()) {
            roomRepository.saveAll(roomsToSave);
        }

        return ImportResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .updateCount(updateCount)
                .errorCount(errorCount)
                .errors(errorDetails)
                .build();
    }

    @Transactional
    public RoomResponse updateRoom(Long id, UpdateRoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        if (request.getBuilding() != null && !request.getBuilding().trim().isEmpty()) {
            room.setBuilding(request.getBuilding());
        }

        if (request.getCapacity() != null && request.getCapacity() > 0) {
            room.setCapacity(request.getCapacity());
        }

        // ✅ GPS: chỉ update khi request có truyền giá trị
        // Nếu muốn XÓA GPS, client cần gửi một flag riêng (ví dụ: clearGps=true)
        // vì null ở đây không phân biệt được "không truyền" vs "muốn xóa"
        if (request.getLatitude() != null) {
            room.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            room.setLongitude(request.getLongitude());
        }
        if (request.getGpsRadiusM() != null && request.getGpsRadiusM() > 0) {
            room.setGpsRadiusM(request.getGpsRadiusM());
        }

        // ✅ Xóa GPS khi client truyền flag clearGps = true
        if (Boolean.TRUE.equals(request.getClearGps())) {
            room.setLatitude(null);
            room.setLongitude(null);
            room.setGpsRadiusM((short) 50); // Bắt buộc phải có giá trị (không được null theo DB)
        }

        return roomMapper.toRoomResponse(roomRepository.save(room));
    }

    public void deleteRoom(Long id) {
        // 1. Kiểm tra phòng có tồn tại không
        roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        // 2. Kiểm tra ràng buộc với Schedule
        if (scheduleRepository.existsByRoom_Id(id)) {
            throw new AppException(ErrorCode.ROOM_IN_USE);
        }

        // 3. Kiểm tra ràng buộc với ClassSession
        if (classSessionRepository.existsByActualRoom_Id(id)) {
            throw new AppException(ErrorCode.ROOM_IN_USE);
        }

        // 4. Thực hiện xóa
        roomRepository.deleteById(id);
    }

    // --- Helper methods ---

    private boolean validateHeaders(Row headerRow, List<String> expectedHeaders) {
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String headerValue = getCellValue(headerRow, i);
            if (!headerValue.equalsIgnoreCase(expectedHeaders.get(i))) return false;
        }
        return true;
    }

    // ✅ Dùng DataFormatter — an toàn với mọi loại cell (số, công thức, text, blank)
    private String getCellValue(Row row, int cellIndex) {
        return dataFormatter.formatCellValue(row.getCell(cellIndex)).trim();
    }

    public RoomResponse createRoom(CreateRoomRequest request) {
        if (roomRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.ROOM_EXISTED);
        }
        Room room = Room.builder()
                .code(request.getCode())
                .building(request.getBuilding())
                .capacity(request.getCapacity() != null ? request.getCapacity() : DEFAULT_CAPACITY)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .gpsRadiusM(request.getGpsRadiusM() != null ? request.getGpsRadiusM() : (short)50)
                .build();
        Room saved = roomRepository.save(room);
        return roomMapper.toRoomResponse(saved);
    }
}