import os
import re
from pathlib import Path
src_dir = Path("D:/Backend/dekiru/src/main/java/ken/example/dekiru")
# List of all moved classes and their new fully qualified names
class_mappings = {
    # Entities
    "Faculty": "ken.example.dekiru.academic.entity.Faculty",
    "Department": "ken.example.dekiru.academic.entity.Department",
    "Room": "ken.example.dekiru.academic.entity.Room",
    "AdministrativeClass": "ken.example.dekiru.academic.entity.AdministrativeClass",
    "Semester": "ken.example.dekiru.academic.entity.Semester",
    "Subject": "ken.example.dekiru.academic.entity.Subject",
    "Lecturer": "ken.example.dekiru.academic.entity.Lecturer",
    "Schedule": "ken.example.dekiru.schedule.entity.Schedule",
    "PeriodTime": "ken.example.dekiru.schedule.entity.PeriodTime",
    "User": "ken.example.dekiru.security.entity.User",
    "RedisToken": "ken.example.dekiru.security.entity.RedisToken",
    "RefreshToken": "ken.example.dekiru.security.entity.RefreshToken",
    "Student": "ken.example.dekiru.student.entity.Student",
    "CheckoutEvent": "ken.example.dekiru.attendance.entity.CheckoutEvent",
    "ClassSession": "ken.example.dekiru.attendance.entity.ClassSession",
    "Attendance": "ken.example.dekiru.attendance.entity.Attendance",
    # Repositories
    "AdministrativeClassRepository": "ken.example.dekiru.academic.repository.AdministrativeClassRepository",
    "DepartmentRepository": "ken.example.dekiru.academic.repository.DepartmentRepository",
    "FacultyRepository": "ken.example.dekiru.academic.repository.FacultyRepository",
    "LecturerRepository": "ken.example.dekiru.academic.repository.LecturerRepository",
    "RoomRepository": "ken.example.dekiru.academic.repository.RoomRepository",
    "SemesterRepository": "ken.example.dekiru.academic.repository.SemesterRepository",
    "SubjectRepository": "ken.example.dekiru.academic.repository.SubjectRepository",
    "ScheduleRepository": "ken.example.dekiru.schedule.repository.ScheduleRepository",
    "PeriodTimeRepository": "ken.example.dekiru.schedule.repository.PeriodTimeRepository",
    "UserRepository": "ken.example.dekiru.security.repository.UserRepository",
    "RedisTokenRepository": "ken.example.dekiru.security.repository.RedisTokenRepository",
    "RefreshTokenRepository": "ken.example.dekiru.security.repository.RefreshTokenRepository",
    "StudentRepository": "ken.example.dekiru.student.repository.StudentRepository",
    "ClassSessionRepository": "ken.example.dekiru.attendance.repository.ClassSessionRepository",
    # We will also clean up old wildcard imports
}
all_java_files = list(src_dir.rglob("*.java"))
for jf in all_java_files:
    try:
        content = jf.read_text(encoding="utf-8")
        original_content = content
        # Remove old wildcard imports
        content = re.sub(r"^import\s+ken\.example\.dekiru\.Entity\.\*;\s*$", "", content, flags=re.MULTILINE)
        content = re.sub(r"^import\s+ken\.example\.dekiru\.Repository\.\*;\s*$", "", content, flags=re.MULTILINE)
        content = re.sub(r"^import\s+ken\.example\.dekiru\.DTO\..*;\s*$", "", content, flags=re.MULTILINE)
        content = content.replace("package ken.example.dekiru.Entity;", "") # just in case
        # Remove remaining bad package imports
        content = re.sub(r"^import\s+ken\.example\.dekiru\.Entity.*?;\s*$", "", content, flags=re.MULTILINE)
        content = re.sub(r"^import\s+ken\.example\.dekiru\.Repository.*?;\s*$", "", content, flags=re.MULTILINE)
        # Prepend missing imports if word exists in text
        imports_to_add = set()
        for cls_name, fq_name in class_mappings.items():
            # If the class name appears as a whole word, and it's not the same package (handled by checking if fq_name already imported or in current package)
            if re.search(r'\b' + cls_name + r'\b', content):
                # Don't add if it's already imported
                if f"import {fq_name};" not in content:
                    # Also check if it's not in the same package (by checking package statement)
                    pkg_name = fq_name.rsplit('.', 1)[0]
                    if not re.search(r"^package\s+" + re.escape(pkg_name) + r"\s*;", content, flags=re.MULTILINE):
                        imports_to_add.add(f"import {fq_name};")
        if imports_to_add:
            # Insert after package statement
            insert_pos = content.find(";") + 1
            if insert_pos > 0:
                imports_str = "\n" + "\n".join(imports_to_add)
                content = content[:insert_pos] + imports_str + content[insert_pos:]
        if content != original_content:
            jf.write_text(content, encoding="utf-8")
    except Exception as e:
        print(f"Error processing {jf}: {e}")
print("Fix applied.")
