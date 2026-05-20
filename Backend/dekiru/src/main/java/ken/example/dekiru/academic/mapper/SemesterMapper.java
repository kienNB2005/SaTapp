package ken.example.dekiru.academic.mapper;

import ken.example.dekiru.academic.dto.CreateSemesterRequest;
import ken.example.dekiru.academic.dto.SemesterResponse;
import ken.example.dekiru.academic.entity.Semester;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SemesterMapper {
    
    SemesterResponse toSemesterResponse(Semester semester);
    
    List<SemesterResponse> toSemesterResponseList(List<Semester> semesters);
    
    Semester toSemester(SemesterResponse response);
    
    Semester toSemester(CreateSemesterRequest request);
}

