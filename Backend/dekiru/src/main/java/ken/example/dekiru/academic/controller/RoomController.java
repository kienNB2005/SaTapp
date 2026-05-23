package ken.example.dekiru.academic.controller;

import ken.example.dekiru.academic.dto.CreateRoomRequest;
import ken.example.dekiru.academic.dto.UpdateRoomRequest;
import ken.example.dekiru.common.dto.ImportResponse;
import ken.example.dekiru.academic.dto.RoomResponse;
import ken.example.dekiru.academic.service.RoomService;
import ken.example.dekiru.attendance.service.ClassSessionService;
import ken.example.dekiru.attendance.dto.DropdownOption;
import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomService roomService;
    ClassSessionService classSessionService;

    @GetMapping("/available")
    public ApiResponse<List<DropdownOption>> getAvailableRooms(
            @RequestParam java.time.LocalDate sessionDate,
            @RequestParam Byte periodStart,
            @RequestParam Byte periodEnd) {
        List<ken.example.dekiru.academic.entity.Room> rooms = classSessionService.findAvailableRooms(sessionDate, periodStart, periodEnd);
        List<DropdownOption> options = rooms.stream()
                .map(r -> new DropdownOption(r.getId(), r.getCode(), r.getCode() + (r.getBuilding() != null ? " - " + r.getBuilding() : "")))
                .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(options, "Lấy danh sách phòng trống thành công");
    }

    @GetMapping
    public ApiResponse<List<RoomResponse>> getAllRooms() {
        List<RoomResponse> rooms = roomService.getAllRooms();
        return ApiResponse.success(rooms, "Lấy danh sách phòng thành công");
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> getRoomById(@PathVariable Long id) {
        RoomResponse room = roomService.getRoomById(id);
        return ApiResponse.success(room, "Lấy thông tin phòng thành công");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        RoomResponse room = roomService.createRoom(request);
        return ApiResponse.success(room, "Thêm mới phòng thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoomResponse> updateRoom(@PathVariable Long id, @RequestBody UpdateRoomRequest request) {
        RoomResponse room = roomService.updateRoom(id, request);
        return ApiResponse.success(room, "Cập nhật phòng thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ApiResponse.success(null, "Xóa phòng thành công");
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResponse> importRooms(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }
        ImportResponse report = roomService.importExcel(file);
        return ApiResponse.success(report, "Đã xử lý xong file dữ liệu.");
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        // Trỏ đường dẫn tới file Excel nằm trong thư mục resources
        Resource resource = new ClassPathResource("templates/Template_Import_Phong.xlsx");

        // Kiểm tra xem có lỡ tay xóa mất file không
        if (!resource.exists()) {
            // Bạn có thể throw AppException ở đây nếu muốn
            throw new RuntimeException("Không tìm thấy file mẫu trên server!");
        }

        // Trả file về cho trình duyệt tải xuống
        return ResponseEntity.ok()
                // Header này báo cho trình duyệt biết đây là file đính kèm để tải về
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Import_Phong.xlsx")
                // Khai báo định dạng file Excel
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
