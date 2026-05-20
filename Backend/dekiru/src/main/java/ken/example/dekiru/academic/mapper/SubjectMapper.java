package ken.example.dekiru.academic.mapper;

import ken.example.dekiru.academic.dto.SubjectResponse;
import ken.example.dekiru.academic.entity.Subject;
import ken.example.dekiru.attendance.dto.DropdownOption;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubjectMapper {
    
    SubjectResponse toSubjectResponse(Subject subject);
    
    List<SubjectResponse> toSubjectResponseList(List<Subject> subjects);
    
    Subject toSubject(SubjectResponse response);

    List<DropdownOption> toDropdownOptionList(List<Subject> subjects);
}

