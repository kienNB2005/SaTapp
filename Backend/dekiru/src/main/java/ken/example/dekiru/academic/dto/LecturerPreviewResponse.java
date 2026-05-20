package ken.example.dekiru.academic.dto;

import ken.example.dekiru.academic.dto.LecturerExcelDTO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LecturerPreviewResponse {
    LecturerExcelDTO lecturer;
    boolean isValid;
    @Builder.Default
    List<String> errors = new ArrayList<>();
}

