package ken.example.dekiru.security.mapper;

import ken.example.dekiru.student.dto.StudentProfileResponse;
import ken.example.dekiru.academic.dto.UpdateLecturerRequest;
import ken.example.dekiru.student.dto.UpdateStudentRequest;
import ken.example.dekiru.academic.dto.LecturerResponse;
import ken.example.dekiru.student.dto.StudentResponse;
import ken.example.dekiru.security.dto.UserResponse;
import ken.example.dekiru.academic.entity.Lecturer;
import ken.example.dekiru.student.entity.Student;
import ken.example.dekiru.security.entity.User;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
    List<UserResponse> toUserResponseList(List<User> users);
    // --- STUDENT MAPPING ---
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.isActive", target = "isActive")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    @Mapping(source = "user.gender", target = "gender")
    @Mapping(source = "user.birthday", target = "birthday")
    @Mapping(source = "user.birthPlace", target = "birthPlace")
    @Mapping(source = "adminClass.code", target = "adminClassCode")
    @Mapping(source = "adminClass.name", target = "adminClassName")
    StudentResponse toStudentResponse(Student student);

    List<StudentResponse> toStudentResponseList(List<Student> students);

    // --- STUDENT PROFILE MAPPING ---
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    @Mapping(source = "user.gender", target = "gender")
    @Mapping(source = "user.birthday", target = "birthday")
    @Mapping(source = "user.birthPlace", target = "birthPlace")
    @Mapping(source = "adminClass.name", target = "adminClassName")
    @Mapping(source = "adminClass.cohortYear", target = "cohortYear")
    @Mapping(source = "adminClass.department.name", target = "departmentName")
    @Mapping(source = "adminClass.department.faculty.name", target = "facultyName")
    @Mapping(source = "adminClass.homeroomTeacher.user.fullName", target = "homeroomTeacherName")
    StudentProfileResponse toStudentProfileResponse(Student student);


    // --- LECTURER MAPPING ---
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.isActive", target = "isActive")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    @Mapping(source = "user.gender", target = "gender")
    @Mapping(source = "user.birthday", target = "birthday")
    @Mapping(source = "user.birthPlace", target = "birthPlace")
    @Mapping(source = "faculty.code", target = "facultyCode")
    @Mapping(source = "faculty.name", target = "facultyName")
    LecturerResponse toLecturerResponse(Lecturer lecturer);

    List<LecturerResponse> toLecturerResponseList(List<Lecturer> lecturers);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromStudentRequest(UpdateStudentRequest request, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromLecturerRequest(UpdateLecturerRequest request, @MappingTarget User user);
}
