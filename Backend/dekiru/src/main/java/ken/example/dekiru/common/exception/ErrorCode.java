package ken.example.dekiru.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter // Dùng Lombok để tự động tạo Getter cho code, message, httpStatus
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi chưa được phân loại", HttpStatus.INTERNAL_SERVER_ERROR),
    ROLE_EXISTED(1000, "Vai trò đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1001, "Tên người dùng phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1002, "Email đã tồn tại", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_EXISTED(1003, "Email không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATION(1004, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1005, "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED), // Sửa lỗi chính tả tên biến
    ROLE_NOT_EXISTED(1111, "Vai trò không tồn tại", HttpStatus.NOT_FOUND),
    FACULTY_NOT_EXISTED(1112, "Khoa không tồn tại", HttpStatus.NOT_FOUND),
    SUBJECT_NOT_EXISTED(1113, "Môn học không tồn tại", HttpStatus.NOT_FOUND),
    ROOM_NOT_EXISTED(1114, "Phòng không tồn tại", HttpStatus.NOT_FOUND),
    SEMESTER_NOT_EXISTED(1115, "Học kỳ không tồn tại", HttpStatus.NOT_FOUND),
    ANOTHER_SEMESTER_IS_ACTIVE(1116, "Một học kỳ khác đang hoạt động", HttpStatus.CONFLICT),
    DEPARTMENT_NOT_EXISTED(1116, "Bộ môn không tồn tại", HttpStatus.NOT_FOUND),
    ADMINISTRATIVE_CLASS_NOT_EXISTED(1117, "Lớp hành chính không tồn tại", HttpStatus.NOT_FOUND),
    LECTURER_NOT_EXISTED(1118, "Giảng viên không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_START_DATE(1119, "Ngày bắt đầu phải là thứ Hai", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(1120, "Ngày kết thúc phải sau ngày bắt đầu", HttpStatus.BAD_REQUEST),
    FACULTY_HAS_DEPARTMENTS(1121, "Không thể xóa khoa khi đang có bộ môn", HttpStatus.CONFLICT),
    DEPARTMENT_HAS_ADMINISTRATIVE_CLASSES(1122, "Không thể xóa bộ môn khi đang có lớp hành chính", HttpStatus.CONFLICT),
    SUBJECT_HAS_SCHEDULES(1123, "Không thể xóa môn học khi đang có lịch biểu", HttpStatus.CONFLICT),
    ROOM_HAS_SCHEDULES(1124, "Không thể xóa phòng khi đang có lịch biểu hoặc buổi học", HttpStatus.CONFLICT),
    ADMINISTRATIVE_CLASS_HAS_SCHEDULES(1125, "Không thể xóa lớp hành chính khi đang có lịch biểu", HttpStatus.CONFLICT),
    FILE_IS_EMPTY(1006, "File tải lên đang trống", HttpStatus.BAD_REQUEST),
    FILE_PROCESSING_ERROR(1007, "Lỗi khi đọc hoặc xử lý file Excel", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_FORMAT(1008, "Định dạng file không hợp lệ, vui lòng dùng file .xlsx", HttpStatus.BAD_REQUEST),

    CANCEL_REASON_REQUIRED(1227, "Lý do hủy buổi học là bắt buộc", HttpStatus.BAD_REQUEST),
    MAKEUP_ALREADY_EXISTS(1228, "Buổi học này đã có lịch dạy bù đang hoạt động", HttpStatus.CONFLICT),
    INVALID_PERIOD(1229, "Tiết học thực tế không hợp lệ", HttpStatus.BAD_REQUEST),
    CLASS_CONFLICT(1230, "Lớp hành chính đã có lịch học khác trong khoảng thời gian này", HttpStatus.CONFLICT),
    ROOM_CONFLICT(1231, "Phòng học đã được sử dụng cho lớp khác trong khoảng thời gian này", HttpStatus.CONFLICT),
    LECTURER_CONFLICT(1232, "Bạn đã có lịch giảng dạy lớp khác trong khoảng thời gian này", HttpStatus.CONFLICT),
    SESSION_NOT_CANCELLED(1233, "Chỉ có thể lên lịch dạy bù cho buổi học đã bị hủy", HttpStatus.BAD_REQUEST),
    FORBIDDEN_ACTION(1234, "Bạn không có quyền thực hiện thao tác này trên buổi học", HttpStatus.FORBIDDEN),

    SEMESTER_HAS_UNFINISHED_SESSIONS(1126, "Không thể xóa học kỳ vì có buổi học chưa hoàn thành", HttpStatus.CONFLICT),
    SEMESTER_HAS_SCHEDULES(1127, "Không thể xóa học kỳ vì có lịch biểu", HttpStatus.CONFLICT),
    ROOM_IN_USE(1128, "Không thể xóa phòng đang được sử dụng trong lịch biểu hoặc buổi học", HttpStatus.CONFLICT),
    NO_SEMESTER_EXISTS (1129, "Không có học kỳ nào. Vui lòng tạo học kỳ trước khi tạo lịch biểu.", HttpStatus.BAD_REQUEST),
    SEMESTER_MUST_BE_SELECTED(1130, "Bắt buộc phải chọn học kỳ khi tạo lịch biểu.", HttpStatus.BAD_REQUEST),
    // ATTENDANCE & CLASS SESSION
    CLASS_SESSION_NOT_FOUND(1201, "Không tìm thấy buổi học", HttpStatus.NOT_FOUND),
    CLASS_SESSION_ALREADY_OPEN(1202, "Giảng viên đang có một buổi học khác đang mở. Vui lòng đóng buổi học đó trước khi mở buổi mới.", HttpStatus.BAD_REQUEST),
    NO_PERMISSION_ON_SESSION(1203, "Bạn không có quyền thao tác trên buổi học này", HttpStatus.FORBIDDEN),
    INVALID_SESSION_STATUS(1204, "Trạng thái buổi học không hợp lệ để thực hiện thao tác", HttpStatus.BAD_REQUEST),
    CHECKOUT_ALREADY_ACTIVE(1205, "Buổi học này đã đang trong trạng thái Check-out", HttpStatus.BAD_REQUEST),
    QR_INVALID(1206, "Mã QR không hợp lệ hoặc đã bị thay đổi", HttpStatus.BAD_REQUEST),
    QR_EXPIRED(1207, "Mã QR đã hết hạn, vui lòng nhắc Giảng viên làm mới", HttpStatus.BAD_REQUEST),
    CHECKOUT_NOT_ALLOWED(1208, "Mã QR là loại Check-out nhưng chưa được phép điểm danh về (không có sự kiện Check-out)", HttpStatus.BAD_REQUEST),
    CHECKIN_NOT_ALLOWED(1209, "Giảng viên đã chuyển sang chế độ quét mã ra về, mã Check-in không còn hiệu lực", HttpStatus.BAD_REQUEST),
    DEVICE_ALREADY_USED(1210, "Thiết bị này đã được sử dụng để điểm danh cho người khác", HttpStatus.CONFLICT),
    STUDENT_NOT_IN_CLASS(1211, "Bạn không có trong danh sách lớp học này", HttpStatus.FORBIDDEN),
    ALREADY_CHECKED_IN(1212, "Bạn đã điểm danh đến (Check-in) từ trước rồi", HttpStatus.BAD_REQUEST),
    NOT_CHECKED_IN(1213, "Bạn chưa điểm danh đến (Check-in), không thể Check-out", HttpStatus.BAD_REQUEST),
    ALREADY_CHECKED_OUT(1214, "Bạn đã điểm danh về (Check-out) từ trước rồi", HttpStatus.BAD_REQUEST),
    OUT_OF_LOCATION(1215, "Bạn nằm ngoài phạm vi cho phép của phòng học", HttpStatus.BAD_REQUEST),
    PERIOD_TIME_NOT_FOUND(1216, "Không tìm thấy thông tin tiết học", HttpStatus.NOT_FOUND),
    ATTENDANCE_LIMIT_EXCEEDED(1217, "Đã quá thời gian điểm danh cho phép", HttpStatus.BAD_REQUEST),
    INVALID_ATTENDANCE_TYPE(1218, "Loại điểm danh không hợp lệ", HttpStatus.BAD_REQUEST),
    CHECKOUT_EVENT_NOT_FOUND(1219, "Không tìm thấy sự kiện Check-out hợp lệ", HttpStatus.NOT_FOUND),
     ATTENDANCE_EXCUSED(1220, "Bạn đã có đơn xin phép được duyệt nên không thể điểm danh", HttpStatus.BAD_REQUEST),
     SESSION_NOT_YET_STARTED(1221, "Buổi học chưa đến giờ mở, vui lòng chờ (sớm hơn 15 phút giờ bắt đầu)", HttpStatus.BAD_REQUEST),
     SESSION_ALREADY_ENDED(1222, "Buổi học đã kết thúc rồi, không thể mở", HttpStatus.BAD_REQUEST),
     ATTENDANCE_NOT_FOUND(1223, "Không tìm thấy bản ghi điểm danh", HttpStatus.NOT_FOUND),
     INVALID_ATTENDANCE_STATUS(1224, "Trạng thái điểm danh không hợp lệ", HttpStatus.BAD_REQUEST),
     USER_NOT_FOUND(1225, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
     CAN_NOT_GENERATE_TOKEN (1226, "không thể tạo token thử lại", HttpStatus.BAD_REQUEST),
    ADMINISTRATIVE_CLASS_EXISTED(1129, "Lớp hành chính đã tồn tại", HttpStatus.BAD_REQUEST),
    DEPARTMENT_EXISTED (1130, "Ngành đã tồn tại", HttpStatus.BAD_REQUEST),
    FACULTY_EXISTED (1131, "Khoa đã tồn tại", HttpStatus.BAD_REQUEST),
    SUBJECT_EXISTED (1132, "Môn học đã tồn tại", HttpStatus.BAD_REQUEST),
    ROOM_EXISTED (1133, "Phòng đã tồn tại", HttpStatus.BAD_REQUEST),
    STUDENT_NOT_EXIST (1134, "Sinh viên không tồn tại", HttpStatus.NOT_FOUND),
    LECTURER_EXISTED (1135, "Giảng viên đã tồn tại", HttpStatus.BAD_REQUEST),
    STUDENT_EXISTED (1136, "Sinh viên đã tồn tại", HttpStatus.BAD_REQUEST),
    MAKEUP_DATE_BEFORE_ORIGINAL(1235, "Ngày dạy bù phải diễn ra từ ngày có buổi học gốc trở đi", HttpStatus.BAD_REQUEST),
    MAKEUP_DATE_AFTER_SEMESTER(1236, "Ngày dạy bù phải diễn ra trước ngày kết thúc học kỳ", HttpStatus.BAD_REQUEST),
    MAKEUP_DATE_MUST_BE_FUTURE(1241, "Ngày dạy bù phải diễn ra từ ngày mai trở đi", HttpStatus.BAD_REQUEST),
    DUPLICATE_SESSION_DATE(1237, "Môn học này đã có buổi học diễn ra vào cùng ngày và tiết học được chọn", HttpStatus.CONFLICT),
    CANCEL_TOO_LATE(1238, "Chỉ được gửi yêu cầu hủy buổi học trước giờ học tối thiểu 15 phút", HttpStatus.BAD_REQUEST),
    SESSION_REQUEST_NOT_FOUND(1239, "Không tìm thấy yêu cầu phê duyệt", HttpStatus.NOT_FOUND),
    INVALID_REQUEST_STATUS(1240, "Trạng thái yêu cầu không hợp lệ để xử lý", HttpStatus.BAD_REQUEST),
    SEMESTER_NOT_FOUND (1137, "Không tìm thấy học kỳ", HttpStatus.NOT_FOUND),
    SEMESTER_EXISTED (1138, "Tên học kỳ đã tồn tại", HttpStatus.CONFLICT),
    SEMESTER_OVERLAP (1139, "Khoảng thời gian của học kỳ bị trùng lặp với học kỳ khác", HttpStatus.CONFLICT),
    INVALID_START_DATE_PAST (1140, "Ngày bắt đầu học kỳ không được nằm trong quá khứ", HttpStatus.BAD_REQUEST);
    private final int code;
    private final String message;
    private final HttpStatus httpStatus; // Thêm trường này

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}