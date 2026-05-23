package ken.example.dekiru.student.service;

import ken.example.dekiru.common.config.SecurityUtils;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.security.mapper.UserMapper;
import ken.example.dekiru.student.dto.StudentProfileResponse;
import ken.example.dekiru.student.entity.Student;
import ken.example.dekiru.student.repository.StudentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentProfileService {
    StudentRepository studentRepository;
    UserMapper userMapper;
    SecurityUtils securityUtils;

    public StudentProfileResponse getProfile() {
        Long studentId = securityUtils.getCurrentStudentId();
        Student student = studentRepository.findProfileById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_EXIST));
        return userMapper.toStudentProfileResponse(student);
    }
}
