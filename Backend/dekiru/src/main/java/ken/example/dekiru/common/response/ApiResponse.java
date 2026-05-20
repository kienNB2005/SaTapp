package ken.example.dekiru.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse <T> {
    @Builder.Default
    int code = 200;
    String message;
    T result;
    public static <T> ApiResponse<T> success(T result) {
        return ApiResponse.<T>builder()
                .result(result)
                .build();
    }

    /**
     * Trả về khi API thành công, có kèm message
     */
    public static <T> ApiResponse<T> success(T result, String message) {
        return ApiResponse.<T>builder()
                .result(result)
                .message(message)
                .build();
    }

    /**
     * Trả về khi API thất bại (Dùng nhiều trong Global Exception Handler)
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
