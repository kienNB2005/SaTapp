package ken.example.dekiru.common.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;


@FieldDefaults (level = AccessLevel.PRIVATE)
public class AppException extends RuntimeException {
    final ErrorCode  errorCode;
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    // đoạn này để khi khởi tạo một đối tượng AppException thì đầu tiên nó đẩy message cho thằng cha là RuntimeException
    // sau đó nó sẽ lưu ErrorCode lại để sử dụng

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
