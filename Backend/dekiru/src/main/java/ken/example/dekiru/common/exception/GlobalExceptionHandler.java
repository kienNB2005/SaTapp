package ken.example.dekiru.common.exception;

import ken.example.dekiru.common.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Bắt lỗi do chúng ta chủ động ném ra
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handlingAppException(AppException appException, WebRequest request){
        ErrorCode errorCode = appException.getErrorCode();

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        // Trả về HTTP Status động dựa theo cấu hình trong Enum
        // Force Content-Type là application/json cho các kết nối SSE bị lỗi lúc Subscribe
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .contentType(isSseRequest(request) ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON)
                .body(apiResponse);
    }

    // Bắt tất cả các lỗi RuntimeException khác chưa được lường trước
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlingRuntimeException(Exception exception, WebRequest request){
        // Kiểm tra nếu là lỗi ngắt kết nối Client của SSE (chặn cả tin nhắn và loại trừ loại exception cụ thể của Tomcat/Spring)
        String message = exception.getMessage();
        if (message != null && (message.contains("disconnected client") || message.contains("Broken pipe") || message.contains("Connection reset by peer"))) {
            return null; 
        }

        // Nếu là HttpMessageNotWritableException phát sinh sau khi đã ngắt kết nối
        if (exception instanceof org.springframework.http.converter.HttpMessageNotWritableException && isSseRequest(request)) {
            return null;
        }

        // IN LỖI RA CONSOLE ĐỂ DEBUGGING
        exception.printStackTrace();

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();

        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatus())
                .contentType(isSseRequest(request) ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON)
                .body(apiResponse);
    }

    private boolean isSseRequest(WebRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeoutException() {
        // Để trống thế này.
        // Trình duyệt sẽ tự động kết nối lại (reconnect) một ống nước mới 30 phút nữa.
    }
}
