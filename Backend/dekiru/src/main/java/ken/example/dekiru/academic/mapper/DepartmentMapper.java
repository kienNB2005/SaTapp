package ken.example.dekiru.academic.mapper;

import ken.example.dekiru.academic.dto.DepartmentResponse;
import ken.example.dekiru.academic.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    
    @Mapping(source = "faculty.id", target = "facultyId")
    @Mapping(source = "faculty.code", target = "facultyCode")
    @Mapping(source = "faculty.name", target = "facultyName")
    DepartmentResponse toDepartmentResponse(Department department);

    List<DepartmentResponse> toDepartmentResponseList(List<Department> departments);
    
    Department toDepartment(DepartmentResponse response);
}

