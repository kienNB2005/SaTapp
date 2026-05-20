package ken.example.dekiru.common.dto;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportResponse {
    int totalRows;      // Tổng số dòng dữ liệu trong file
    int successCount;    // Số dòng lưu mới thành công
    int updateCount;     // Số dòng đã tồn tại và được cập nhật
    int errorCount;      // Số dòng bị lỗi
    List<String> errors; // Chi tiết lỗi (VD: "Dòng 5: Tên không được để trống")
}
