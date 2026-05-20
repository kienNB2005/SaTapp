import os
import shutil
import re
from pathlib import Path
# Setup mapping
src_dir = Path("D:/Backend/dekiru/src/main/java/ken/example/dekiru")
moves = {
    # Controllers
    "Controller/AuthenticationController.java": "security/controller",
    "Controller/UserController.java": "security/controller",
    "Controller/DepartmentController.java": "academic/controller",
    "Controller/FacultyController.java": "academic/controller",
    "Controller/RoomController.java": "academic/controller",
    "Controller/AdministrativeClassController.java": "academic/controller",
    "Controller/SemesterController.java": "academic/controller",
    "Controller/SubjectController.java": "academic/controller",
    "Controller/ScheduleController.java": "schedule/controller",
    # DTO Response
    "DTO/Response/AdministrativeClassResponse.java": "academic/dto",
    "DTO/Response/DepartmentResponse.java": "academic/dto",
    "DTO/Response/FacultyResponse.java": "academic/dto",
    "DTO/Response/RoomResponse.java": "academic/dto",
    "DTO/Response/SemesterResponse.java": "academic/dto",
    "DTO/Response/SubjectResponse.java": "academic/dto",
    "DTO/Response/LecturerPreviewResponse.java": "academic/dto",
    "DTO/Response/LecturerResponse.java": "academic/dto",
    "DTO/Response/SchedulePreviewResponse.java": "schedule/dto",
    "DTO/Response/ScheduleResponse.java": "schedule/dto",
    "DTO/Response/LoginResponse.java": "security/dto",
    "DTO/Response/UserResponse.java": "security/dto",
    "DTO/Response/StudentPreviewResponse.java": "student/dto",
    "DTO/Response/StudentResponse.java": "student/dto",
    "DTO/Response/ImportResponse.java": "common/dto",
    # DTO Request
    "DTO/Request/CreateSemesterRequest.java": "academic/dto",
    "DTO/Request/UpdateSemesterRequest.java": "academic/dto",
    "DTO/Request/UpdateDepartmentRequest.java": "academic/dto",
    "DTO/Request/UpdateFacultyRequest.java": "academic/dto",
    "DTO/Request/UpdateRoomRequest.java": "academic/dto",
    "DTO/Request/UpdateAdministrativeClassRequest.java": "academic/dto",
    "DTO/Request/UpdateSubjectRequest.java": "academic/dto",
    "DTO/Request/UpdateLecturerRequest.java": "academic/dto",
    "DTO/Request/LecturerExcelDTO.java": "academic/dto",
    "DTO/Request/ScheduleExcelDTO.java": "schedule/dto",
    "DTO/Request/LoginRequest.java": "security/dto",
    "DTO/Request/RefreshTokeRequest.java": "security/dto",
    "DTO/Request/StudentExcelDTO.java": "student/dto",
    "DTO/Request/UpdateStudentRequest.java": "student/dto",
    # DTO JWT
    "DTO/JWT/JwtInfo.java": "security/jwt",
    "DTO/JWT/TokenPayload.java": "security/jwt",
    # Entity
    "Entity/Faculty.java": "academic/entity",
    "Entity/Department.java": "academic/entity",
    "Entity/Room.java": "academic/entity",
    "Entity/AdministrativeClass.java": "academic/entity",
    "Entity/Semester.java": "academic/entity",
    "Entity/Subject.java": "academic/entity",
    "Entity/Lecturer.java": "academic/entity",
    "Entity/Schedule.java": "schedule/entity",
    "Entity/PeriodTime.java": "schedule/entity",
    "Entity/User.java": "security/entity",
    "Entity/RedisToken.java": "security/entity",
    "Entity/RefreshToken.java": "security/entity",
    "Entity/Student.java": "student/entity",
    "Entity/CheckoutEvent.java": "attendance/entity",
    "Entity/ClassSession.java": "attendance/entity",
    "Entity/Attendance.java": "attendance/entity",
    # Mapper
    "Mapper/AdministrativeClassMapper.java": "academic/mapper",
    "Mapper/DepartmentMapper.java": "academic/mapper",
    "Mapper/FacultyMapper.java": "academic/mapper",
    "Mapper/RoomMapper.java": "academic/mapper",
    "Mapper/SemesterMapper.java": "academic/mapper",
    "Mapper/SubjectMapper.java": "academic/mapper",
    "Mapper/UserMapper.java": "security/mapper",
    # Repository
    "Repository/AdministrativeClassRepository.java": "academic/repository",
    "Repository/DepartmentRepository.java": "academic/repository",
    "Repository/FacultyRepository.java": "academic/repository",
    "Repository/LecturerRepository.java": "academic/repository",
    "Repository/RoomRepository.java": "academic/repository",
    "Repository/SemesterRepository.java": "academic/repository",
    "Repository/SubjectRepository.java": "academic/repository",
    "Repository/ScheduleRepository.java": "schedule/repository",
    "Repository/PeriodTimeRepository.java": "schedule/repository",
    "Repository/UserRepository.java": "security/repository",
    "Repository/RedisTokenRepository.java": "security/repository",
    "Repository/RefreshTokenRepository.java": "security/repository",
    "Repository/StudentRepository.java": "student/repository",
    "Repository/ClassSessionRepository.java": "attendance/repository",
    # Service
    "Service/AdministrativeClassService.java": "academic/service",
    "Service/DepartmentService.java": "academic/service",
    "Service/FacultyService.java": "academic/service",
    "Service/RoomService.java": "academic/service",
    "Service/SemesterService.java": "academic/service",
    "Service/SubjectService.java": "academic/service",
    "Service/ScheduleService.java": "schedule/service",
    "Service/AuthenticationService.java": "security/service",
    "Service/JWTService.java": "security/service",
    "Service/UserService.java": "security/service"
}
# Add package rename mappings (Old Package -> New Package)
package_renames = {
    # Packages for common
    "ken.example.dekiru.common.ApiReponse": "ken.example.dekiru.common.response",
    "ken.example.dekiru.common.Configuration": "ken.example.dekiru.common.config",
    "ken.example.dekiru.common.Exception": "ken.example.dekiru.common.exception",
    "ken.example.dekiru.specification": "ken.example.dekiru.common.specification",
    # And we'll compute old -> new based on moves
}
moved_files = []
for rel_path, dest_dir in moves.items():
    src_file = src_dir / rel_path
    if src_file.exists():
        dest = src_dir / dest_dir
        dest.mkdir(parents=True, exist_ok=True)
        new_file = dest / src_file.name
        print(f"Moving {src_file.name} to {dest_dir}")
        shutil.move(str(src_file), str(new_file))
        # Calculate old and new package names
        old_pkg = "ken.example.dekiru." + str(Path(rel_path).parent).replace("\\", ".").replace("/", ".")
        new_pkg = "ken.example.dekiru." + dest_dir.replace("/", ".")
        # Make case-sensitive package replacements? Yes.
        # But wait, multiple classes can share the same old_pkg. So we map ClassName to OldPkg and NewPkg.
        moved_files.append((src_file.stem, old_pkg, new_pkg, new_file))
print("Applying package and import updates...")
all_java_files = list(src_dir.rglob("*.java"))
# Find all java files to update imports
for jf in all_java_files:
    try:
        content = jf.read_text(encoding="utf-8")
        original_content = content
        # 1. Update common package text matches
        content = content.replace("ken.example.dekiru.common.ApiReponse", "ken.example.dekiru.common.response")
        content = content.replace("ken.example.dekiru.common.Configuration", "ken.example.dekiru.common.config")
        content = content.replace("ken.example.dekiru.common.Exception", "ken.example.dekiru.common.exception")
        content = content.replace("ken.example.dekiru.specification", "ken.example.dekiru.common.specification")
        # 2. Update imports for specifically moved classes
        for class_name, old_pkg, new_pkg, new_file_path in moved_files:
            # If the current file is the moved file, update its package declaration
            if jf == new_file_path:
                content = re.sub(r"^package\s+" + re.escape(old_pkg) + r"\s*;", f"package {new_pkg};", content, flags=re.MULTILINE)
            # Update imports of this class in ALL files
            old_import = f"import {old_pkg}.{class_name};"
            new_import = f"import {new_pkg}.{class_name};"
            content = content.replace(old_import, new_import)
        if content != original_content:
            jf.write_text(content, encoding="utf-8")
    except Exception as e:
        print(f"Error processing {jf}: {e}")
# Clean up empty directories
def remove_empty_dirs(path):
    deleted = False
    for root, dirs, files in os.walk(path, topdown=False):
        for name in dirs:
            dir_path = os.path.join(root, name)
            if not os.listdir(dir_path):
                print(f"Removing empty directory: {dir_path}")
                os.rmdir(dir_path)
                deleted = True
    return deleted
remove_empty_dirs(str(src_dir))
print("Refactoring complete.")
