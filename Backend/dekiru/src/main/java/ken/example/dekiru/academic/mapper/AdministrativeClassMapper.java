package ken.example.dekiru.academic.mapper;

import ken.example.dekiru.academic.dto.AdministrativeClassResponse;
import ken.example.dekiru.academic.entity.AdministrativeClass;
import ken.example.dekiru.attendance.dto.DropdownOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdministrativeClassMapper {
    
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.code", target = "departmentCode")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "homeroomTeacher.id", target = "homeroomTeacherId")
    @Mapping(source = "homeroomTeacher.user.fullName", target = "homeroomTeacherName")
    @Mapping(source = "homeroomTeacher.lecturerCode", target = "homeroomTeacherCode")
    AdministrativeClassResponse toAdministrativeClassResponse(AdministrativeClass administrativeClass);

    List<AdministrativeClassResponse> toAdministrativeClassResponseList(List<AdministrativeClass> administrativeClasses);
    
    AdministrativeClass toAdministrativeClass(AdministrativeClassResponse response);

    List<DropdownOption> toDropdownOptionList (List<AdministrativeClass> administrativeClass);
}

