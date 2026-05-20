package ken.example.dekiru.academic.mapper;

import ken.example.dekiru.academic.dto.FacultyResponse;
import ken.example.dekiru.academic.entity.Faculty;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FacultyMapper {
    
    // Chuyển đổi từ Faculty Entity sang FacultyResponse DTO
    FacultyResponse toFacultyResponse(Faculty faculty);
    
    // Chuyển đổi danh sách Faculty sang danh sách FacultyResponse
    List<FacultyResponse> toFacultyResponseList(List<Faculty> faculties);
    
    // Chuyển đổi từ FacultyResponse DTO sang Faculty Entity (nếu cần)
    Faculty toFaculty(FacultyResponse response);
}

