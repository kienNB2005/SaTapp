package ken.example.dekiru.common.config;

import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(User.Role.admin)) {
            User admin = User.builder()
                    .email("kienvanbau2k5@gmail.com") // Email Google của mày
                    .role(User.Role.admin)
                    .fullName("Quản Trị Viên") // Thêm tên đầy đủ cho admin
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Đã khởi tạo tài khoản Admin đầu tiên!");
        }
        else {
            System.out.println(">>> Admin đã tồn tại rồi, đéo tạo nữa!"); // THÊM DÒNG NÀY NỮA
        }
    }
}