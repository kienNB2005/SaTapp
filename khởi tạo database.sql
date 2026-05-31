-- ============================================================
--  HỆ THỐNG ĐIỂM DANH QR — MÔ HÌNH LỚP CỐ ĐỊNH
--  Phiên bản hoàn chỉnh · MySQL 8.0+
--  15 bảng · 1 Stored Procedure · 1 Function · 6 View
-- ============================================================
create database HTDiemDanhSinhVien;
use HTDiemDanhSinhVien;
-- INSERT INTO attendance (class_session_id, student_id, status, is_late, left_early, created_at, scanned_at)
-- SELECT cs.id, s.id, 'present', false, false, NOW(), NOW()
-- FROM class_session cs
-- JOIN schedule sc ON cs.schedule_id = sc.id
-- JOIN student s ON s.admin_class_id = sc.admin_class_id
-- WHERE cs.status = 'scheduled' AND cs.session_date < CURDATE()
--   AND NOT EXISTS (
--       SELECT 1 FROM attendance a2 WHERE a2.class_session_id = cs.id AND a2.student_id = s.id
--   );
--   
-- UPDATE class_session
-- SET status = 'closed', closed_at = NOW()
-- WHERE status = 'scheduled' AND session_date < CURDATE();
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- NHÓM 1 — TỔ CHỨC HÀNH CHÍNH
-- ============================================================

-- ============================================================
-- 1. BẢNG: FACULTY (Khoa)
-- ============================================================
-- Mục đích : Lưu trữ danh mục các khoa trong trường học.
--            Đóng vai trò là đơn vị hành chính cao nhất để phân 
--            cấp quản lý ngành học và giảng viên.
CREATE TABLE faculty (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)     NOT NULL COMMENT 'Mã khoa ngắn gọn, VD: CNTT',
    name        VARCHAR(200)    NOT NULL COMMENT 'Tên đầy đủ của khoa',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_faculty_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Khoa — đơn vị hành chính cao nhất';


-- ============================================================
-- 2. BẢNG: DEPARTMENT (Ngành học)
-- ============================================================
-- Mục đích : Quản lý các ngành học thuộc từng khoa.
--            Giúp phân loại sinh viên và lớp hành chính theo 
--            chuyên môn đào tạo cụ thể.
CREATE TABLE department (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    faculty_id  INT UNSIGNED    NOT NULL COMMENT 'Thuộc khoa nào',
    code        VARCHAR(20)     NOT NULL COMMENT 'Mã ngành, VD: KTPM',
    name        VARCHAR(200)    NOT NULL COMMENT 'Tên ngành đầy đủ',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dept_code (code),
    CONSTRAINT fk_dept_faculty
        FOREIGN KEY (faculty_id) REFERENCES faculty(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Ngành học — thuộc một khoa';


-- ============================================================
-- 3. BẢNG: ADMINISTRATIVE_CLASS (Lớp hành chính)
-- ============================================================
-- Mục đích : Quản lý danh sách các lớp hành chính cố định.
--            Tập hợp các sinh viên cùng ngành học và cùng khóa, 
--            làm cơ sở để xếp thời khóa biểu và điểm danh.
CREATE TABLE administrative_class (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    department_id   INT UNSIGNED    NOT NULL COMMENT 'Ngành của lớp',
    code            VARCHAR(30)     NOT NULL COMMENT 'Mã lớp, VD: CNTT-K22A — phải khớp file TKB',
    name            VARCHAR(200)    NOT NULL COMMENT 'Tên lớp đầy đủ',
    cohort_year     VARCHAR (20)        NOT NULL COMMENT 'khóa học (vd: k15)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_class_code (code),
    CONSTRAINT fk_class_dept
        FOREIGN KEY (department_id) REFERENCES department(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Lớp hành chính — tập hợp SV cùng ngành cùng khoá';


-- ============================================================
-- NHÓM 2 — NGƯỜI DÙNG
-- ============================================================

-- ============================================================
-- 4. BẢNG: USER (Tài khoản người dùng)
-- ============================================================
-- Mục đích : Bảng trung tâm quản lý tài khoản xác thực của mọi
--            thành viên trong hệ thống (Admin, Giảng viên, Sinh viên).
--            Lưu trữ thông tin cá nhân và quyền truy cập (Role).
CREATE TABLE user (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    email       VARCHAR(255)    NOT NULL COMMENT 'Email Google tổ chức — định danh OAuth2',
    full_name   VARCHAR(200)    NOT NULL COMMENT 'Tên hiển thị trên UI và báo cáo',
    role        ENUM('admin','lecturer','student')
                                NOT NULL COMMENT 'Vai trò: quyết định quyền truy cập',
    is_active   Boolean     NOT NULL DEFAULT true
                                COMMENT 'Khoá tài khoản = 0, mở = 1. Dữ liệu lịch sử giữ nguyên khi khoá',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_email (email),
    KEY idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Tài khoản xác thực — bảng trung tâm cho mọi vai trò';
ALTER TABLE user
ADD COLUMN phone_number VARCHAR(20)  NULL COMMENT 'Số điện thoại liên hệ',
ADD COLUMN gender       ENUM('male', 'female', 'other') NULL COMMENT 'Giới tính',
ADD COLUMN birthday     DATE         NULL COMMENT 'Ngày sinh (YYYY-MM-DD)',
ADD COLUMN birth_place  VARCHAR(150) NULL COMMENT 'Nơi sinh';

-- Insert tài khoản Admin mặc định
INSERT INTO user (email, full_name, role, is_active) 
VALUES ('admin@gmail.com', 'System Admin', 'admin', 1);


-- ============================================================
-- 5. BẢNG: LECTURER (Hồ sơ giảng viên)
-- ============================================================
-- Mục đích : Lưu trữ thông tin đặc thù của giảng viên.
--            Liên kết 1-1 với bảng USER, quản lý mã giảng viên và 
--            đơn vị khoa trực thuộc để phân công giảng dạy.
CREATE TABLE lecturer (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    user_id         INT UNSIGNED    NOT NULL COMMENT 'Liên kết 1-1 với user',
    faculty_id      INT UNSIGNED    NOT NULL COMMENT 'GV thuộc khoa nào',
    lecturer_code   VARCHAR(20)     NOT NULL COMMENT 'Mã GV của trường — dùng mapping file TKB',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_lecturer_user (user_id),
    UNIQUE KEY uq_lecturer_code (lecturer_code),
    CONSTRAINT fk_lecturer_user
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_lecturer_faculty
        FOREIGN KEY (faculty_id) REFERENCES faculty(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Hồ sơ giảng viên — UNIQUE user_id đảm bảo 1 tài khoản = 1 GV';
  
-- Cập nhật bảng ADMINISTRATIVE_CLASS
ALTER TABLE administrative_class
ADD COLUMN homeroom_teacher_id INT UNSIGNED NULL COMMENT 'Khóa ngoại trỏ tới giảng viên chủ nhiệm',
ADD CONSTRAINT fk_adminclass_lecturer 
    FOREIGN KEY (homeroom_teacher_id) 
    REFERENCES lecturer(id) 
    ON DELETE SET NULL;
-- ============================================================
-- 6. BẢNG: STUDENT (Hồ sơ sinh viên)
-- ============================================================
-- Mục đích : Lưu trữ thông tin đặc thù của sinh viên.
--            Liên kết 1-1 với bảng USER, xác định sinh viên thuộc 
--            lớp hành chính nào làm cơ sở đối chiếu khi điểm danh.
CREATE TABLE student (
    id                  INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    user_id             INT UNSIGNED    NOT NULL COMMENT 'Liên kết 1-1 với user',
    admin_class_id      INT UNSIGNED    NOT NULL
                        COMMENT 'Lớp HC của SV — khi quét QR server so sánh với schedule.admin_class_id',
    student_code        VARCHAR(20)     NOT NULL COMMENT 'Mã số sinh viên',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_student_user (user_id),
    UNIQUE KEY uq_student_code (student_code),
    CONSTRAINT fk_student_user
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_student_class
        FOREIGN KEY (admin_class_id) REFERENCES administrative_class(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Hồ sơ sinh viên — admin_class_id là cơ sở lọc buổi học hợp lệ';


-- ============================================================
-- NHÓM 3 — DANH MỤC HỌC THUẬT
-- ============================================================

-- ============================================================
-- 7. BẢNG: SUBJECT (Môn học)
-- ============================================================
-- Mục đích : Danh mục các môn học được đào tạo trong trường.
--            Cung cấp thông tin số tín chỉ và mã môn để tái sử dụng 
--            qua nhiều học kỳ khác nhau.
CREATE TABLE subject (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)     NOT NULL COMMENT 'Mã môn — phải khớp chính xác với file TKB trường',
    name        VARCHAR(200)    NOT NULL COMMENT 'Tên môn đầy đủ',
    credits     TINYINT         NOT NULL DEFAULT 3 COMMENT 'Số tín chỉ (1–10)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Môn học — danh mục tái sử dụng qua nhiều học kỳ';


-- ============================================================
-- 8. BẢNG: ROOM (Phòng học)
-- ============================================================
-- Mục đích : Danh mục các phòng học vật lý của trường.
--            Quan trọng nhất: Lưu trữ tọa độ GPS (latitude, longitude)
--            và bán kính hợp lệ để xác thực vị trí quét QR của sinh viên.
CREATE TABLE room (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)     NOT NULL COMMENT 'Mã phòng — phải khớp file TKB, VD: B201',
    building        VARCHAR(100)    NULL     COMMENT 'Toà nhà, VD: Nhà B',
    capacity        SMALLINT        NOT NULL DEFAULT 50 COMMENT 'Sức chứa tối đa',
    latitude        DECIMAL(10,7)   NULL     COMMENT 'Vĩ độ GPS trung tâm phòng. NULL = chưa nhập GPS',
    longitude       DECIMAL(10,7)   NULL     COMMENT 'Kinh độ GPS trung tâm phòng. NULL = chưa nhập GPS',
    gps_radius_m    SMALLINT        NOT NULL DEFAULT 50
                    COMMENT 'Bán kính hợp lệ (mét). SV phải đứng trong bán kính này mới điểm danh được',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_room_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Phòng học — lưu GPS để xác thực vị trí SV khi quét QR';


-- ============================================================
-- 9. BẢNG: SEMESTER (Học kỳ)
-- ============================================================
-- Mục đích : Quản lý khung thời gian các học kỳ trong năm.
--            Dùng để nhóm thời khóa biểu và buổi học theo từng đợt, 
--            chỉ định học kỳ nào đang diễn ra (is_active).
CREATE TABLE semester (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)     NOT NULL COMMENT 'Tên học kỳ, VD: HK1-2024-2025',
    start_date  DATE            NOT NULL
                COMMENT 'Ngày đầu tuần 1 (thứ Hai). Stored procedure dùng để tính ngày thực tế ClassSession',
    end_date    DATE            NOT NULL COMMENT 'Ngày cuối học kỳ',
    start_week  TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tuần bắt đầu của học kỳ so với năm học',
    is_active   Boolean     NOT NULL DEFAULT false
                COMMENT 'Học kỳ đang diễn ra. Chỉ một học kỳ active = 1 tại một thời điểm',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_semester_name (name),
    CONSTRAINT chk_semester_dates CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Học kỳ — khung thời gian bao trùm toàn bộ TKB';


-- ============================================================
-- 10. BẢNG: PERIOD_TIME (Thời gian tiết học)
-- ============================================================
-- Mục đích : Bảng tra cứu thời gian bắt đầu và kết thúc của từng 
--            tiết học (1-15) trong ngày. Là cơ sở để hệ thống tính 
--            toán số phút đi muộn của sinh viên.
CREATE TABLE period_time (
    period_number   TINYINT     NOT NULL COMMENT 'Số thứ tự tiết: 1–15',
    start_time      TIME        NOT NULL COMMENT 'Giờ bắt đầu tiết. AttendanceService dùng tính phút muộn',
    end_time        TIME        NOT NULL COMMENT 'Giờ kết thúc tiết. Dùng tự động đóng buổi học',
    PRIMARY KEY (period_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Bảng tra giờ học theo số tiết — không có bảng này không tính được phút muộn';

INSERT INTO period_time VALUES
(1,  '07:00', '07:50'),
(2,  '07:50', '08:40'),
(3,  '08:40', '09:30'),
(4,  '09:45', '10:35'),
(5,  '10:35', '11:25'),
(6,  '11:25', '12:15'),
(7,  '12:30', '13:20'),
(8,  '13:20', '14:10'),
(9,  '14:10', '15:00'),
(10, '15:15', '16:05'),
(11, '16:05', '16:55'),
(12, '16:55', '17:45'),
(13, '18:00', '18:50'),
(14, '18:50', '19:40'),
(15, '19:40', '20:30');


-- ============================================================
-- NHÓM 4 — THỜI KHÓA BIỂU & BUỔI HỌC
-- ============================================================

-- ============================================================
-- 11. BẢNG: SCHEDULE (Thời khóa biểu)
-- ============================================================
-- Mục đích : Lưu trữ cấu trúc thời khóa biểu lặp lại theo tuần.
--            Đóng vai trò là khuôn mẫu (1 dòng) để Stored Procedure
--            tự động sinh ra hàng loạt buổi học (N dòng) thực tế.
--     1 dòng = môn × lớp × GV × phòng × thứ/tiết/tuần
--     Stored procedure đọc bảng này để sinh hàng loạt ClassSession
CREATE TABLE schedule (
    id                  INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    semester_id         INT UNSIGNED    NOT NULL COMMENT 'Thuộc học kỳ nào',
    subject_id          INT UNSIGNED    NOT NULL COMMENT 'Môn học được dạy',
    admin_class_id      INT UNSIGNED    NOT NULL
                        COMMENT 'Lớp HC tham gia. Khi SV quét QR, so sánh với student.admin_class_id',
    lecturer_id         INT UNSIGNED    NOT NULL COMMENT 'GV phụ trách — chỉ GV này mở được buổi học',
    room_id             INT UNSIGNED    NOT NULL COMMENT 'Phòng học — dùng lấy GPS xác thực vị trí',
    day_of_week         TINYINT         NOT NULL COMMENT 'Thứ học: 2=T2 ... 8=CN',
    period_start        TINYINT         NOT NULL COMMENT 'Tiết bắt đầu (1–15)',
    period_end          TINYINT         NOT NULL COMMENT 'Tiết kết thúc (1–15)',
    week_start          TINYINT         NOT NULL DEFAULT 1  COMMENT 'Tuần bắt đầu trong học kỳ',
    week_end            TINYINT         NOT NULL            COMMENT 'Tuần kết thúc, VD: 15',
    total_sessions      TINYINT         NOT NULL DEFAULT 0
                        COMMENT 'Tổng buổi học — stored procedure UPDATE sau khi sinh xong. Dùng hiển thị "Buổi 3/15"',
    late_threshold_min  TINYINT         NOT NULL DEFAULT 15
                        COMMENT 'Muộn ≤ N phút vẫn tính present nhưng đánh is_late=true',
    max_late_min        TINYINT         NOT NULL DEFAULT 30
                        COMMENT 'Muộn > N phút → server từ chối điểm danh, trả lỗi TOO_LATE',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_schedule_semester   (semester_id),
    KEY idx_schedule_lecturer   (lecturer_id),
    KEY idx_schedule_class      (admin_class_id),
    CONSTRAINT chk_period_order CHECK (period_end >= period_start),
    CONSTRAINT chk_week_order   CHECK (week_end  >= week_start),
    CONSTRAINT chk_day_of_week  CHECK (day_of_week BETWEEN 2 AND 8),
    CONSTRAINT chk_period_start CHECK (period_start BETWEEN 1 AND 15),
    CONSTRAINT chk_period_end   CHECK (period_end   BETWEEN 1 AND 15),
    CONSTRAINT fk_schedule_semester
        FOREIGN KEY (semester_id)    REFERENCES semester(id)             ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_schedule_subject
        FOREIGN KEY (subject_id)     REFERENCES subject(id)              ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_schedule_class
        FOREIGN KEY (admin_class_id) REFERENCES administrative_class(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_schedule_lecturer
        FOREIGN KEY (lecturer_id)    REFERENCES lecturer(id)             ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_schedule_room
        FOREIGN KEY (room_id)        REFERENCES room(id)                 ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Công thức TKB — 1 dòng sinh ra N buổi học khi Admin xác nhận nhập TKB';


-- ============================================================
-- 12. BẢNG: CLASS_SESSION (Buổi học thực tế)
-- ============================================================
-- Mục đích : Quản lý chi tiết từng buổi học cụ thể diễn ra theo ngày.
--            Lưu trữ mã QR token theo thời gian thực và trạng thái
--            của buổi học (Đang mở, Đã đóng, Bị hủy).
--     Sinh hàng loạt tự động khi Admin nhập TKB (UC-08)
--     GV không tạo buổi — chỉ kích hoạt buổi đã có sẵn
--     QR token ghi đè mỗi 60s trên CÙNG MỘT HÀNG, không INSERT thêm
CREATE TABLE class_session (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    schedule_id     INT UNSIGNED    NOT NULL
                    COMMENT 'Thuộc dòng TKB nào — JOIN lấy môn/lớp/GV/phòng',
	actual_room_id INT UNSIGNED NOT NULL COMMENT 'Phòng học thực tế của buổi này (Copy từ schedule lúc sinh)',
    actual_lecturer_id INT UNSIGNED NOT NULL COMMENT 'Giảng viên dạy thực tế (Copy từ schedule lúc sinh)',
    actual_period_start TINYINT      NOT NULL COMMENT 'Tiết bắt đầu thực tế (1-15)',
    actual_period_end   TINYINT      NOT NULL COMMENT 'Tiết kết thúc thực tế (1-15)',
    session_date    DATE            NOT NULL COMMENT 'Ngày học thực tế, VD: 2025-09-02',
    session_number  TINYINT         NOT NULL
                    COMMENT 'Thứ tự buổi trong môn (1..N). Hiển thị "Buổi 3/15". Buổi bù giữ nguyên số buổi gốc',
    status          ENUM('scheduled','open','closed','cancelled')
                                    NOT NULL DEFAULT 'scheduled'
                    COMMENT 'scheduled=chưa đến · open=đang dạy · closed=đã xong · cancelled=đã hủy',
    -- QR token (chỉ có giá trị khi status=open)
    qr_token        VARCHAR(255)    NULL
                    COMMENT 'Token QR hiện tại (HMAC-SHA256). Server ghi đè mỗi 60s — UPDATE 1 cột, không INSERT',
    qr_expires_at   DATETIME        NULL
                    COMMENT 'Thời điểm token hết hạn. Server so sánh NOW() khi SV quét',
    -- GPS toggle per buổi
    gps_enabled     Boolean     NOT NULL DEFAULT true
                    COMMENT 'Bật/tắt xác thực GPS cho buổi này. GV tắt khi tín hiệu yếu',
    -- Thời gian mở/đóng
    opened_at       DATETIME        NULL COMMENT 'Lúc GV nhấn Bắt đầu',
    closed_at       DATETIME        NULL COMMENT 'Lúc GV nhấn Kết thúc hoặc hết giờ tự động',
    -- Hủy buổi học
    cancel_reason   VARCHAR(500)    NULL COMMENT 'Lý do hủy (bắt buộc khi hủy). NULL nếu không bị hủy',
    cancelled_by    INT UNSIGNED    NULL COMMENT 'user.id của GV/Admin đã hủy. NULL nếu không bị hủy',
    cancelled_at    DATETIME        NULL COMMENT 'Thời điểm hủy buổi',
    -- Buổi dạy bù (self-referencing FK)
    makeup_for_id   INT UNSIGNED    NULL
                    COMMENT 'Nếu đây là buổi DẠY BÙ, trỏ về class_session.id bị hủy. NULL = buổi học thường',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_session_schedule_date_period (schedule_id, session_date, actual_period_start)
                    COMMENT 'Không thể có 2 buổi cùng dòng TKB cùng ngày và cùng tiết bắt đầu',
    KEY idx_cs_date     (session_date),
    KEY idx_cs_status   (status),
    KEY idx_cs_makeup   (makeup_for_id),
    CONSTRAINT fk_cs_schedule
        FOREIGN KEY (schedule_id)   REFERENCES schedule(id)      ON DELETE RESTRICT ON UPDATE CASCADE,
	CONSTRAINT fk_cs_actual_room
        FOREIGN KEY (actual_room_id) REFERENCES room(id) ON DELETE RESTRICT ON UPDATE CASCADE,
	CONSTRAINT fk_cs_actual_lecturer
        FOREIGN KEY (actual_lecturer_id) REFERENCES lecturer(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cs_cancelled_by
        FOREIGN KEY (cancelled_by)  REFERENCES user(id)          ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_cs_makeup
        FOREIGN KEY (makeup_for_id) REFERENCES class_session(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_cs_period_start CHECK (actual_period_start BETWEEN 1 AND 15),
    CONSTRAINT chk_cs_period_end   CHECK (actual_period_end   BETWEEN 1 AND 15),
    CONSTRAINT chk_cs_period_order CHECK (actual_period_end >= actual_period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Buổi học cụ thể — sinh sẵn toàn bộ khi nhập TKB, GV chỉ kích hoạt';

-- ============================================================
-- 12.5 BẢNG: CLASS_SESSION_REQUEST (Yêu cầu hủy/bù buổi học)
-- ============================================================
CREATE TABLE class_session_request (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    lecturer_id INT UNSIGNED NOT NULL COMMENT 'Giảng viên gửi yêu cầu',
    class_session_id INT UNSIGNED NOT NULL COMMENT 'Buổi học gốc cần hủy hoặc cần bù',
    
    -- Trạng thái phê duyệt độc lập cho từng bước
    cancel_status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending' COMMENT 'Trạng thái duyệt hủy',
    makeup_status ENUM('pending', 'approved', 'rejected') NULL COMMENT 'Trạng thái duyệt bù',
    
    -- Thông tin hủy buổi
    cancel_reason VARCHAR(500) NULL COMMENT 'Lý do hủy',
    
    -- Thông tin dạy bù
    makeup_date DATE NULL COMMENT 'Ngày dạy bù đề xuất',
    makeup_period_start TINYINT NULL COMMENT 'Tiết bắt đầu dạy bù',
    makeup_period_end TINYINT NULL COMMENT 'Tiết kết thúc dạy bù',
    makeup_room_id INT UNSIGNED NULL COMMENT 'Phòng dạy bù đề xuất',
    
    -- Thông tin kiểm duyệt (Audit)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by INT UNSIGNED NULL COMMENT 'Admin xử lý duyệt',
    approved_at DATETIME NULL,
    reject_reason VARCHAR(500) NULL COMMENT 'Lý do từ chối nếu bị bác bỏ',
    
    CONSTRAINT fk_csreq_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturer(id) ON DELETE CASCADE,
    CONSTRAINT fk_csreq_session FOREIGN KEY (class_session_id) REFERENCES class_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_csreq_room FOREIGN KEY (makeup_room_id) REFERENCES room(id) ON DELETE SET NULL,
    CONSTRAINT fk_csreq_approver FOREIGN KEY (approved_by) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- NHÓM 5 — CHECK-OUT TỨC THỜI
-- ============================================================

-- ============================================================
-- 13. BẢNG: CHECKOUT_EVENT (Sự kiện Check-out)
-- ============================================================
-- Mục đích : Quản lý các lần giảng viên kích hoạt yêu cầu check-out 
--            đột xuất giữa buổi học. Giúp chống tình trạng sinh viên
--            điểm danh xong rồi bỏ về sớm.
--     GV có thể kích hoạt bất kỳ thời điểm nào trong buổi học
--     khi nghi ngờ có SV về sớm. Có thể kích hoạt nhiều lần.
--     Sau deadline: Scheduler đánh dấu left_early=1 cho SV chưa quét
--     Sau khi đóng: QR trở về chế độ check-in, buổi học tiếp tục bình thường
CREATE TABLE checkout_event (
    id                  INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    class_session_id    INT UNSIGNED    NOT NULL COMMENT 'Thuộc buổi học nào',
    triggered_by        INT UNSIGNED    NOT NULL COMMENT 'user.id của GV đã kích hoạt',
    triggered_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                        COMMENT 'Thời điểm GV nhấn "Yêu cầu check-out"',
    deadline_at         DATETIME        NOT NULL
                        COMMENT 'Hết giờ SV phải quét (thường triggered_at + 3–10 phút).
                                 Scheduler so sánh NOW() để đóng event và đánh dấu left_early',
    note                VARCHAR(300)    NULL
                        COMMENT 'Ghi chú lý do nghi ngờ, VD: "Nghi ngờ SV về sớm tiết 3"',
    closed_at           DATETIME        NULL
                        COMMENT 'Thời điểm đóng check-out. NULL = đang mở.
                                 Ghi bởi Scheduler khi deadline_at < NOW() hoặc GV đóng thủ công',
    PRIMARY KEY (id),
    KEY idx_co_session  (class_session_id),
    KEY idx_co_open     (class_session_id, closed_at)
                        COMMENT 'Index tìm nhanh event đang mở: WHERE closed_at IS NULL',
    CONSTRAINT fk_co_session
        FOREIGN KEY (class_session_id) REFERENCES class_session(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_co_triggered_by
        FOREIGN KEY (triggered_by)     REFERENCES user(id)          ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Check-out tức thời — mỗi lần GV kích hoạt giữa buổi học = 1 bản ghi';
-- ============================================================
-- NHÓM 6 — VẬN HÀNH ĐIỂM DANH
-- ============================================================

-- ============================================================
-- 14. BẢNG: ATTENDANCE (Bản ghi điểm danh)
-- ============================================================
-- Mục đích : Lưu trữ kết quả điểm danh của sinh viên theo từng buổi học.
--            Đảm bảo mỗi sinh viên chỉ có 1 bản ghi mỗi buổi, lưu thông
--            tin tọa độ quét, số phút đi muộn, về sớm.
--     1 dòng = 1 SV × 1 buổi học
--     UNIQUE(session_id, student_id) chống quét trùng
--     is_late (đến muộn) và left_early (về sớm) hoàn toàn độc lập nhau
CREATE TABLE attendance (
    id                  INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    class_session_id    INT UNSIGNED    NOT NULL COMMENT 'Buổi học nào',
    student_id          INT UNSIGNED    NOT NULL COMMENT 'Sinh viên nào',
    device_id VARCHAR(255) NULL,
    -- Trạng thái điểm danh
    status              ENUM('present','absent','excused')
                                        NOT NULL DEFAULT 'absent'
                        COMMENT 'present=có mặt · absent=vắng · excused=vắng có phép (đơn được duyệt)',
    -- CHECK-IN (lần quét đầu tiên)
    scanned_at          DATETIME        NULL COMMENT 'Thời điểm CHECK-IN. NULL nếu vắng',
    is_late             TINYINT(1)      NOT NULL DEFAULT 0
                        COMMENT 'TRUE nếu scanned_at - period_start > late_threshold_min. CHỈ từ CHECK-IN',
    late_minutes        TINYINT         NULL
                        COMMENT 'Số phút muộn lúc CHECK-IN. 0=đúng giờ, NULL=vắng. KHÔNG bị thay đổi bởi check-out',
    -- GPS check-in
    scan_lat            DECIMAL(10,7)   NULL COMMENT 'Vĩ độ GPS lúc CHECK-IN',
    scan_lng            DECIMAL(10,7)   NULL COMMENT 'Kinh độ GPS lúc CHECK-IN',
    distance_m          SMALLINT        NULL COMMENT 'Khoảng cách SV → tâm phòng (mét), tính Haversine server-side',
    gps_verified        boolean      NULL
                        COMMENT '1=trong phạm vi · 0=ngoài phạm vi nhưng GV override · NULL=không kiểm tra GPS',
    -- CHECK-OUT (lần quét thứ hai — chỉ khi GV kích hoạt checkout_event)
    checkout_event_id   INT UNSIGNED    NULL
                        COMMENT 'FK → checkout_event.id. Lần check-out nào SV đã quét. NULL = chưa check-out',
    checked_out_at      DATETIME        NULL
                        COMMENT 'Thời điểm CHECK-OUT. NULL = chưa quét lần 2. KHÔNG liên quan đến is_late',
    checkout_lat        DECIMAL(10,7)   NULL COMMENT 'Vĩ độ GPS lúc CHECK-OUT (audit)',
    checkout_lng        DECIMAL(10,7)   NULL COMMENT 'Kinh độ GPS lúc CHECK-OUT (audit)',
    left_early          boolean      NOT NULL DEFAULT false
                        COMMENT '1 = có checkout_event nhưng SV không quét trong deadline = về sớm.
                                 KHÔNG thay đổi status (vẫn present). KHÔNG liên quan đến is_late',
    -- Chỉnh sửa thủ công (audit log)
    note                TEXT            NULL COMMENT 'Ghi chú khi GV sửa tay, VD: SV bị tai nạn',
    edited_by           INT UNSIGNED    NULL COMMENT 'user.id người sửa. NULL = chưa bị sửa tay',
    edited_at           DATETIME        NULL COMMENT 'Khi nào bị sửa — cặp với edited_by tạo audit log đầy đủ',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_att_session_student (class_session_id, student_id)
                        COMMENT 'Mỗi SV chỉ có 1 bản ghi mỗi buổi — quét lần 2 bị DB từ chối ngay',
    KEY idx_att_student         (student_id),
    KEY idx_att_status          (status),
    KEY idx_att_gps             (gps_verified, distance_m),
    KEY idx_att_checkout_event  (checkout_event_id),
    CONSTRAINT fk_att_session
        FOREIGN KEY (class_session_id) REFERENCES class_session(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_att_student
        FOREIGN KEY (student_id)       REFERENCES student(id)       ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_att_editor
        FOREIGN KEY (edited_by)        REFERENCES user(id)          ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_att_checkout_event
        FOREIGN KEY (checkout_event_id) REFERENCES checkout_event(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Bản ghi điểm danh — 1 dòng = 1 SV × 1 buổi. is_late và left_early hoàn toàn độc lập';

-- Query chống gian lận thiết bị (chạy mỗi lần SV quét)
ALTER TABLE attendance ADD INDEX idx_device_status (class_session_id, device_id, status);

-- Query scheduler mỗi 30s
ALTER TABLE checkout_event ADD INDEX idx_deadline (closed_at, deadline_at);

-- ============================================================
-- STORED PROCEDURE — Sinh ClassSession từ Schedule
-- Gọi sau khi Admin INSERT từng dòng schedule khi nhập TKB
-- ============================================================
DELIMITER $$

CREATE PROCEDURE generate_sessions_for_schedule(IN p_schedule_id INT UNSIGNED)
BEGIN
    DECLARE v_semester_start    DATE;
    DECLARE v_semester_start_week TINYINT;
    DECLARE v_day_of_week       TINYINT;
    DECLARE v_week_start        TINYINT;
    DECLARE v_week_end          TINYINT;
    DECLARE v_cur_date          DATE;
    DECLARE v_session_num       TINYINT DEFAULT 0;
    DECLARE v_days_to_add       INT;
    DECLARE v_first_day_dow     INT;
    DECLARE v_target_dow        INT;
    DECLARE v_room_id INT UNSIGNED;
    DECLARE v_lecturer_id INT UNSIGNED;
    DECLARE v_period_start      TINYINT;
    DECLARE v_period_end        TINYINT;
    DECLARE v_anchor_monday DATE;

    -- Lấy thông tin Schedule + Semester
    SELECT
        sem.start_date,
        sem.start_week,
        sch.day_of_week,
        sch.week_start,
        sch.week_end,
        sch.room_id,         -- Lấy thêm phòng gốc
        sch.lecturer_id,     -- Lấy thêm giảng viên gốc
        sch.period_start,
        sch.period_end
    INTO
        v_semester_start,
        v_semester_start_week,
        v_day_of_week,
        v_week_start,
        v_week_end,
        v_room_id,           -- Gán vào biến
        v_lecturer_id,       -- Gán vào biến
        v_period_start,
        v_period_end
    FROM schedule sch
    JOIN semester sem ON sem.id = sch.semester_id
    WHERE sch.id = p_schedule_id;

-- Nếu start_date là Thứ Tư (WEEKDAY = 2), nó sẽ trừ đi 2 ngày để về đúng Thứ Hai.
SET v_anchor_monday = DATE_SUB(v_semester_start, INTERVAL WEEKDAY(v_semester_start) DAY);
    -- Chuyển day_of_week hệ thống (2=T2...8=CN)
    -- sang MySQL DAYOFWEEK (2=Mon...7=Sat, 1=Sun)
    SET v_target_dow = CASE v_day_of_week
        WHEN 2 THEN 2   -- T2 = Monday
        WHEN 3 THEN 3   -- T3 = Tuesday
        WHEN 4 THEN 4   -- T4 = Wednesday
        WHEN 5 THEN 5   -- T5 = Thursday
        WHEN 6 THEN 6   -- T6 = Friday
        WHEN 7 THEN 7   -- T7 = Saturday
        WHEN 8 THEN 1   -- CN = Sunday
    END;

    -- Tìm ra ngày đầu tiên của cái tuần mà môn học này bắt đầu.
    -- lấy thời gian khai giảng làm mốc rồi cộng với công thức (tuần bắt đầu học môn - tuần bắt đầu của học kỳ) * 7) day) 
    -- để dịch chuyển biến đến ngày đầu tiên của tuần bắt đầu môn học nhưng lúc này vẫn chưa biết chính xác hôm bắt đầu buổi học là thứ mấy
    SET v_cur_date = DATE_ADD(v_anchor_monday,
                              INTERVAL ((v_week_start - v_semester_start_week) * 7) DAY);

    -- Dịch đến đúng thứ trong tuần đó
    SET v_first_day_dow = DAYOFWEEK(v_cur_date);
    -- Này MySQL, cái ngày v_cur_date hiện tại nó đang là Thứ mấy trong tuần chuẩn của mày?"
	-- Nó sẽ trả về một con số từ 1 (Chủ Nhật) đến 7 (Thứ Bảy) và nhét vào biến v_first_day_dow (Ngày đầu tiên của tuần học đang là thứ mấy).
    SET v_days_to_add   = (v_target_dow - v_first_day_dow + 7) % 7; -- Tính xem cần phải CỘNG THÊM bao nhiêu ngày nữa vào cái ngày mốc để ra đúng cái Thứ mà sinh viên đi học.
    -- (lấy thứ đích - thứ hiện tại + 7) % 7 (cộng với 7 để phòng hờ số âm để không bị lui về tuần học trước còn % 7 để gọt đi số ngày thừa khi cộng với 7 để không bị tiến tới tuần sau)
 
    SET v_cur_date      = DATE_ADD(v_cur_date, INTERVAL v_days_to_add DAY); -- Thực thi việc dịch chuyển dịch ngày đầu tiên của tuần học đến chính xác bắt đầu buổi học
    -- công thức:  ngày đầu tiên của tuần học (tính ở công thức đầu tiên)  + ngày phải cộng thêm (vừa tính ở trên) 

    -- Lặp: mỗi tuần từ week_start đến week_end
    WHILE v_cur_date <= DATE_ADD(v_anchor_monday,
                                 INTERVAL ((v_week_end - v_semester_start_week) * 7 + 6) DAY) DO
        SET v_session_num = v_session_num + 1;

        INSERT INTO class_session
            (schedule_id, session_date, session_number, status, actual_room_id, actual_lecturer_id, actual_period_start, actual_period_end)
        VALUES
            (p_schedule_id, v_cur_date, v_session_num, 'scheduled', v_room_id, v_lecturer_id, v_period_start, v_period_end)
        ON DUPLICATE KEY UPDATE
            session_number = v_session_num,
            actual_room_id = v_room_id,             -- Cập nhật nếu duplicate
            actual_lecturer_id = v_lecturer_id,     -- Cập nhật nếu duplicate
            actual_period_start = v_period_start,
            actual_period_end = v_period_end;

        -- Tuần tiếp theo
        SET v_cur_date = DATE_ADD(v_cur_date, INTERVAL 7 DAY);
    END WHILE;

    -- Cập nhật total_sessions vào Schedule
    UPDATE schedule
    SET    total_sessions = v_session_num
    WHERE  id = p_schedule_id;
END$$

DELIMITER ;


-- ============================================================
-- FUNCTION — Tính khoảng cách Haversine (mét)
-- Gọi trong AttendanceService khi SV quét QR
-- để so sánh với room.gps_radius_m
-- ============================================================
DELIMITER $$

CREATE FUNCTION haversine_distance(
    lat1 DECIMAL(10,7),
    lng1 DECIMAL(10,7),
    lat2 DECIMAL(10,7),
    lng2 DECIMAL(10,7)
)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE R       INT    DEFAULT 6371000; -- bán kính Trái Đất (mét)
    DECLARE dLat    DOUBLE;
    DECLARE dLng    DOUBLE;
    DECLARE a       DOUBLE;

    SET dLat = RADIANS(lat2 - lat1);
    SET dLng = RADIANS(lng2 - lng1);
    SET a    = SIN(dLat / 2) * SIN(dLat / 2)
             + COS(RADIANS(lat1)) * COS(RADIANS(lat2))
             * SIN(dLng / 2) * SIN(dLng / 2);

    RETURN ROUND(6371000 * 2 * ATAN2(SQRT(a), SQRT(1 - a)));
END$$

DELIMITER ;


-- ============================================================
--  HỆ THỐNG ĐIỂM DANH QR — VIEWS (đã sửa lỗi)
--  Cập nhật:
--    · V1  : JOIN actual_room_id / actual_lecturer_id thay vì schedule
--    · V2  : Bỏ cancelled_sessions (chưa dùng tính năng hủy)
--    · V3  : Lọc buổi cancelled; giữ excused_count (GV chỉnh tay)
--    · V4  : JOIN room qua cs.actual_room_id
--    · V5  : Sửa r_buu / r_goc dùng actual_room_id; bỏ cột hủy buổi
--    · V6  : Không đổi logic, làm rõ comment
-- ============================================================
 
 
-- ============================================================
-- V1: v_lecturer_today — Dashboard buổi học của GV hôm nay
-- ============================================================
--
--  Mục đích : Cung cấp toàn bộ dữ liệu cần thiết để hiển thị
--             màn hình chính của GV trong ngày — danh sách buổi
--             học, trạng thái, thống kê điểm danh real-time.
--
--  Ai dùng  : Giảng viên (lọc thêm theo lecturer_id ở tầng app)
--
--  Lọc sẵn : Chỉ trả về buổi có session_date = ngày hôm nay
--
--  Các cột trả về:
--  ┌─────────────────────┬──────────────────────────────────────────────────┐
--  │ class_session_id    │ ID buổi học — dùng để mở/đóng buổi, tạo QR      │
--  │ session_number      │ Thứ tự buổi trong môn, VD: 3 → hiển thị "Buổi 3"│
--  │ session_date        │ Ngày học (= hôm nay)                             │
--  │ status              │ Trạng thái: scheduled / open / closed            │
--  │ period_start_time   │ Giờ bắt đầu tiết đầu, VD: 07:00                 │
--  │ period_end_time     │ Giờ kết thúc tiết cuối, VD: 09:30               │
--  │ period_start        │ Số tiết bắt đầu (1–15) — dùng tính phút muộn    │
--  │ period_end          │ Số tiết kết thúc (1–15)                          │
--  │ late_threshold_min  │ Muộn ≤ N phút → present nhưng đánh is_late      │
--  │ max_late_min        │ Muộn > N phút → từ chối điểm danh (TOO_LATE)    │
--  │ subject_name        │ Tên môn học đầy đủ                               │
--  │ subject_code        │ Mã môn học                                       │
--  │ class_name          │ Tên lớp hành chính                               │
--  │ room_code           │ Mã phòng thực tế (actual_room_id)                │
--  │ building            │ Toà nhà của phòng thực tế                        │
--  │ gps_radius_m        │ Bán kính GPS hợp lệ của phòng thực tế (mét)      │
--  │ lecturer_id         │ ID GV thực tế dạy buổi này                       │
--  │ total_students      │ Tổng SV đã có bản ghi điểm danh trong buổi       │
--  │ present_count       │ Số SV có mặt (status = 'present')                │
--  │ late_count          │ Số SV đến muộn (is_late = 1)                     │
--  │ left_early_count    │ Số SV về sớm (left_early = 1)                    │
--  └─────────────────────┴──────────────────────────────────────────────────┘
--
--  Lưu ý   : room_code / gps_radius_m lấy từ actual_room_id (không phải
--             schedule.room_id) → đúng với buổi dạy bù / đổi phòng.
-- ============================================================
CREATE OR REPLACE VIEW v_lecturer_today AS
SELECT
    cs.id                                                           AS class_session_id,
    cs.session_number,
    cs.session_date,
    cs.status,
    pt_s.start_time                                                 AS period_start_time,
    pt_e.end_time                                                   AS period_end_time,
    cs.actual_period_start                                          AS period_start,
    cs.actual_period_end                                            AS period_end,
    sc.late_threshold_min,
    sc.max_late_min,
    sc.total_sessions,
    sub.name                                                        AS subject_name,
    sub.code                                                        AS subject_code,
    ac.name                                                         AS class_name,
    r.code                                                          AS room_code,
    r.building,
    r.gps_radius_m,
    l.id                                                            AS lecturer_id,
    cs.makeup_for_id                                                AS makeup_for_id,
    cs_org.session_date                                             AS original_session_date,
    COUNT(a.id)                                                     AS total_students,
    COUNT(CASE WHEN a.status   = 'present' THEN 1 END)             AS present_count,
    COUNT(CASE WHEN a.is_late  = 1         THEN 1 END)             AS late_count,
    COUNT(CASE WHEN a.left_early = 1       THEN 1 END)             AS left_early_count
FROM class_session cs
JOIN schedule               sc   ON sc.id  = cs.schedule_id
JOIN subject                sub  ON sub.id = sc.subject_id
JOIN administrative_class   ac   ON ac.id  = sc.admin_class_id
JOIN room                   r    ON r.id   = cs.actual_room_id
JOIN lecturer               l    ON l.id   = cs.actual_lecturer_id
JOIN period_time            pt_s ON pt_s.period_number = cs.actual_period_start
JOIN period_time            pt_e ON pt_e.period_number = cs.actual_period_end
LEFT JOIN class_session     cs_org ON cs_org.id = cs.makeup_for_id
LEFT JOIN attendance        a    ON a.class_session_id = cs.id
WHERE cs.session_date = CURDATE()
GROUP BY
    cs.id, cs.session_number, cs.session_date, cs.status,
    pt_s.start_time, pt_e.end_time,
    cs.actual_period_start, cs.actual_period_end, sc.late_threshold_min, sc.max_late_min,
    sc.total_sessions,
    sub.name, sub.code, ac.name,
    r.code, r.building, r.gps_radius_m,
    l.id, cs.makeup_for_id, cs_org.session_date;
 
 
-- ============================================================
-- V2: v_schedule_progress — Tiến độ buổi học theo môn / học kỳ
-- ============================================================
--
--  Mục đích : Cho GV và Admin thấy tổng quan tiến độ dạy học
--             của từng dòng TKB trong học kỳ: đã dạy bao nhiêu
--             buổi, còn lại bao nhiêu, đang mở buổi nào.
--
--  Ai dùng  : Giảng viên (xem môn mình dạy), Admin (xem toàn bộ)
--
--  Các cột trả về:
--  ┌────────────────────┬───────────────────────────────────────────────────┐
--  │ schedule_id        │ ID dòng TKB — khoá chính để truy vấn chi tiết    │
--  │ semester_id        │ ID học kỳ — dùng lọc theo học kỳ hiện tại        │
--  │ subject_name       │ Tên môn học                                       │
--  │ class_name         │ Tên lớp hành chính                               │
--  │ lecturer_id        │ ID giảng viên phụ trách dòng TKB                 │
--  │ total_sessions     │ Tổng số buổi theo kế hoạch (từ schedule)         │
--  │ generated_sessions │ Số buổi đã được sinh thực tế trong class_session │
--  │ closed_sessions    │ Số buổi đã kết thúc (status = 'closed')          │
--  │ open_sessions      │ Số buổi đang mở (status = 'open') — thường 0     │
--  │                    │ hoặc 1 tại một thời điểm                         │
--  │ upcoming_sessions  │ Số buổi chưa đến (status = 'scheduled')          │
--  └────────────────────┴───────────────────────────────────────────────────┘
--
--  Công thức tiến độ gợi ý ở tầng app:
--    "Buổi X / Y" = closed_sessions / total_sessions
-- ============================================================
CREATE OR REPLACE VIEW v_schedule_progress AS
SELECT
    sc.id                                                           AS schedule_id,
    sc.semester_id,
    sub.name                                                        AS subject_name,
    ac.name                                                         AS class_name,
    l.id                                                            AS lecturer_id,
    sc.total_sessions,
    COUNT(CASE WHEN cs.makeup_for_id IS NULL THEN 1 END)            AS generated_sessions,
    COUNT(CASE WHEN cs.status = 'closed'    THEN 1 END)            AS closed_sessions,
    COUNT(CASE WHEN cs.status = 'open'      THEN 1 END)            AS open_sessions,
    -- [ĐÃ FIX LỖI]
    COUNT(CASE WHEN cs.makeup_for_id IS NULL THEN 1 END) - 
    COUNT(CASE WHEN cs.status = 'closed' THEN 1 END) - 
    COUNT(CASE WHEN cs.status = 'open' THEN 1 END)                 AS upcoming_sessions
FROM schedule sc
JOIN subject              sub ON sub.id = sc.subject_id
JOIN administrative_class ac  ON ac.id  = sc.admin_class_id
JOIN lecturer             l   ON l.id   = sc.lecturer_id
LEFT JOIN class_session   cs  ON cs.schedule_id = sc.id
GROUP BY sc.id, sc.semester_id, sub.name, ac.name, l.id, sc.total_sessions;
 
 
-- ============================================================
-- V3: v_attendance_summary — Tỉ lệ chuyên cần SV × Môn × Học kỳ
-- ============================================================
--
--  Mục đích : Báo cáo chuyên cần tổng hợp theo từng sinh viên,
--             từng môn học, từng học kỳ. Dùng để xuất bảng điểm
--             danh cuối kỳ, cảnh báo SV gần vượt ngưỡng nghỉ.
--
--  Ai dùng  : Giảng viên, Admin, và SV (xem riêng bản thân)
--
--  Lọc sẵn : Loại trừ buổi bị huỷ (status = 'cancelled') khỏi
--             mọi phép đếm để tránh pha loãng tỉ lệ chuyên cần.
--
--  Các cột trả về:
--  ┌──────────────────┬─────────────────────────────────────────────────────┐
--  │ student_id       │ ID sinh viên                                        │
--  │ student_name     │ Họ tên sinh viên                                    │
--  │ student_code     │ Mã số sinh viên                                     │
--  │ semester_id      │ ID học kỳ                                           │
--  │ semester_name    │ Tên học kỳ, VD: HK1-2024-2025                       │
--  │ subject_id       │ ID môn học                                          │
--  │ subject_name     │ Tên môn học                                         │
--  │ total_sessions   │ Tổng số buổi học có hiệu lực (không tính bị huỷ)   │
--  │ present_count    │ Số buổi có mặt                                      │
--  │ absent_count     │ Số buổi vắng không phép                             │
--  │ excused_count    │ Số buổi vắng có phép (GV duyệt thủ công)           │
--  │ late_count       │ Số buổi đến muộn (is_late = 1, vẫn tính present)   │
--  │ left_early_count │ Số buổi về sớm (left_early = 1, vẫn tính present)  │
--  │ attendance_rate  │ Tỉ lệ có mặt (%), làm tròn 1 chữ số thập phân     │
--  │                  │ = present_count / total_sessions × 100             │
--  └──────────────────┴─────────────────────────────────────────────────────┘
--
--  Lưu ý   : excused_count dùng được ngay — GV sửa tay
--             attendance.status = 'excused' kèm ghi chú vào cột note,
--             audit trail qua edited_by / edited_at.
-- ============================================================
CREATE OR REPLACE VIEW v_attendance_summary AS
SELECT
    s.id                                                            AS student_id,
    u.full_name                                                     AS student_name,
    s.student_code,
    sc.semester_id,
    sem.name                                                        AS semester_name,
    sc.subject_id,
    sub.name                                                        AS subject_name,
    sc.total_sessions                                               AS total_sessions,
    COUNT(CASE WHEN a.status   = 'present' THEN 1 END)             AS present_count,
    COUNT(CASE WHEN a.status   = 'absent'  THEN 1 END)             AS absent_count,
    COUNT(CASE WHEN a.status   = 'excused' THEN 1 END)             AS excused_count,
    COUNT(CASE WHEN a.is_late  = 1         THEN 1 END)             AS late_count,
    COUNT(CASE WHEN a.left_early = 1       THEN 1 END)             AS left_early_count,
    COUNT(DISTINCT CASE WHEN cs2.status IN ('closed', 'open') THEN cs2.id END) AS finished_sessions,
    ROUND(
        COALESCE(
            SUM(CASE WHEN cs2.status IN ('closed', 'open') AND a.status IN ('present', 'excused') THEN 1.0 ELSE 0.0 END)
            / NULLIF(COUNT(DISTINCT CASE WHEN cs2.status IN ('closed', 'open') THEN cs2.id END), 0)
            * 100
        , 100.0)
    , 1)                                                            AS attendance_rate
FROM student s
JOIN user               u   ON u.id   = s.user_id
JOIN attendance         a   ON a.student_id = s.id
JOIN class_session      cs2 ON cs2.id = a.class_session_id
JOIN schedule           sc  ON sc.id  = cs2.schedule_id
JOIN semester           sem ON sem.id = sc.semester_id
JOIN subject            sub ON sub.id = sc.subject_id
-- Loại trừ buổi bị hủy khỏi thống kê chuyên cần
WHERE cs2.status != 'cancelled'
GROUP BY
    s.id, u.full_name, s.student_code,
    sc.semester_id, sem.name, sc.subject_id, sub.name, sc.total_sessions;
 
 
 

CREATE OR REPLACE VIEW v_lecturer_week AS
SELECT
    cs.id                   AS class_session_id,
    cs.session_date,
    cs.session_number,
    cs.status,
    CASE DAYOFWEEK(cs.session_date)
        WHEN 1 THEN 8
        ELSE DAYOFWEEK(cs.session_date)
    END AS day_of_week,
    cs.actual_period_start  AS period_start,
    cs.actual_period_end    AS period_end,
    pt_s.start_time         AS period_start_time,
    pt_e.end_time           AS period_end_time,
    sub.name                AS subject_name,
    sub.code                AS subject_code,
    ac.code                 AS class_code,
    ac.name                 AS class_name,
    r.code                  AS room_code,
    l.id                    AS lecturer_id,
    cs.makeup_for_id        AS makeup_for_id,
    cs_org.session_date     AS original_session_date,
    sc.total_sessions
FROM class_session cs
JOIN schedule               sc   ON sc.id  = cs.schedule_id
JOIN subject                sub  ON sub.id = sc.subject_id
JOIN administrative_class   ac   ON ac.id  = sc.admin_class_id
JOIN room                   r    ON r.id   = cs.actual_room_id
JOIN lecturer               l    ON l.id   = cs.actual_lecturer_id
JOIN period_time            pt_s ON pt_s.period_number = cs.actual_period_start
JOIN period_time            pt_e ON pt_e.period_number = cs.actual_period_end
LEFT JOIN class_session     cs_org ON cs_org.id = cs.makeup_for_id
-- ĐÃ BỎ: LEFT JOIN attendance
WHERE cs.session_date BETWEEN
    DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)  -- Thứ Hai tuần này
    AND DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)
-- ĐÃ BỎ: GROUP BY (Vì không còn hàm COUNT nào nữa, query sẽ cực nhẹ)
;
    
    

CREATE OR REPLACE VIEW v_lecturer_semester_summary AS
SELECT
    l.id                                        AS lecturer_id,
    sc.semester_id,
    sem.name                                    AS semester_name,
    
    -- "Tuần này": đếm số buổi trong tuần hiện tại
    COUNT(DISTINCT CASE
        WHEN cs.session_date BETWEEN
            DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
            AND DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)
            AND cs.status != 'cancelled'
        THEN cs.id END)                         AS sessions_this_week,
        
    -- Số môn khác nhau trong tuần
    COUNT(DISTINCT CASE
        WHEN cs.session_date BETWEEN
            DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
            AND DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)
            AND cs.status != 'cancelled'
        THEN sc.subject_id END)                 AS subjects_this_week,
        
    -- "Học kỳ này": tổng buổi + đã xong / còn lại (Cố định tổng buổi: 1 buổi gốc chỉ đếm 1 lần, bỏ qua buổi bù)
    COUNT(DISTINCT CASE WHEN cs.makeup_for_id IS NULL THEN cs.id END) AS total_sessions_semester,
    COUNT(DISTINCT CASE WHEN cs.status = 'closed'    THEN cs.id END) AS closed_sessions,
    
    -- [ĐÃ FIX LỖI] Còn lại = Tổng gốc - Đã chốt (bao hàm cả buổi đang mở, sắp tới, và buổi đã hủy chưa dạy bù)
    COUNT(DISTINCT CASE WHEN cs.makeup_for_id IS NULL THEN cs.id END) - 
    COUNT(DISTINCT CASE WHEN cs.status = 'closed' THEN cs.id END) AS remaining_sessions,
    
    -- "Chuyên cần TB": trung bình tỉ lệ có mặt toàn lớp trong học kỳ
    ROUND(
        COALESCE(
            SUM(CASE WHEN a.status IN ('present', 'excused') THEN 1.0 ELSE 0.0 END)
            / NULLIF(COUNT(a.id), 0) * 100
        , 100.0)
    , 1)                                        AS avg_attendance_rate
FROM lecturer l
JOIN schedule           sc  ON sc.lecturer_id   = l.id
JOIN semester           sem ON sem.id           = sc.semester_id
LEFT JOIN class_session cs  ON cs.schedule_id   = sc.id
LEFT JOIN attendance    a   ON a.class_session_id = cs.id
    AND cs.status != 'cancelled'
GROUP BY l.id, sc.semester_id, sem.name;


-- ============================================================
-- V7: v_student_today — Trang chủ: Danh sách lớp hôm nay của SV
-- ============================================================
--  Mục đích : Cung cấp API trực tiếp cho màn hình Trang Chủ của app Sinh viên.
--             Trả về danh sách môn học trong ngày hiện tại kèm trạng thái chuẩn.
-- ============================================================
CREATE OR REPLACE VIEW v_student_today AS
SELECT
    s.id AS student_id,
    cs.id AS class_session_id,
    sub.name AS subject_name,
    u_gv.full_name AS lecturer_name,      -- Lấy tên GV dạy thực tế của buổi đó
    r.code AS room_code,                  -- Lấy phòng học thực tế
    pt_s.start_time AS period_start_time,
    pt_e.end_time AS period_end_time,
    cs.session_number,
    sc.total_sessions,
    cs.status AS session_status,
    -- Tính toán 4 trạng thái linh hoạt cho App React Native
    CASE 
        -- 1. Đã điểm danh thành công
        WHEN a.id IS NOT NULL AND a.status = 'present' THEN 'present'         
        -- 2. Vắng học nhưng có phép (được duyệt chỉnh tay)
        WHEN a.id IS NOT NULL AND a.status = 'excused' THEN 'excused'         
        -- 3. Buổi học ĐÃ ĐÓNG mà sinh viên chưa quét hoặc bị đánh dấu vắng
        WHEN cs.status = 'closed' AND (a.id IS NULL OR a.status = 'absent') THEN 'absent'   
        -- 4. Buổi học ĐANG MỞ nhưng sinh viên chưa quét (Cần báo màu Đỏ/Cam để nhắc quét)
        WHEN cs.status = 'open' AND a.id IS NULL THEN 'ongoing_absent'        
        -- 5. Buổi học ở tương lai (Chưa mở)
        WHEN cs.status = 'scheduled' THEN 'upcoming'                          
        ELSE 'pending'
    END AS attendance_status,
    a.scanned_at,
    cs_org.session_date AS original_session_date
FROM student s
JOIN schedule sc ON sc.admin_class_id = s.admin_class_id
-- Chỉ lấy lịch của ngày hôm nay
JOIN class_session cs ON cs.schedule_id = sc.id AND cs.session_date = CURDATE()
LEFT JOIN class_session cs_org ON cs_org.id = cs.makeup_for_id
JOIN subject sub ON sub.id = sc.subject_id
JOIN room r ON r.id = cs.actual_room_id
JOIN lecturer l ON l.id = cs.actual_lecturer_id 
JOIN user u_gv ON u_gv.id = l.user_id
JOIN period_time pt_s ON pt_s.period_number = cs.actual_period_start
JOIN period_time pt_e ON pt_e.period_number = cs.actual_period_end
LEFT JOIN attendance a ON a.class_session_id = cs.id AND a.student_id = s.id;


-- ============================================================
-- V8: v_student_schedule — Lịch học: Toàn bộ TKB của SV
-- ============================================================
--  Mục đích : Cung cấp API cho màn hình Lịch Học (theo tuần).
--             Backend sẽ gọi View này kết hợp WHERE session_date BETWEEN ...
-- ============================================================
CREATE OR REPLACE VIEW v_student_schedule AS
SELECT
    s.id AS student_id,
    cs.id AS class_session_id,
    cs.session_date,
    CASE DAYOFWEEK(cs.session_date)
        WHEN 1 THEN 8
        ELSE DAYOFWEEK(cs.session_date)
    END AS day_of_week,
    sub.name AS subject_name,
    u_gv.full_name AS lecturer_name,
    r.code AS room_code,
    pt_s.start_time AS period_start_time,
    pt_e.end_time AS period_end_time,
    cs.session_number,
    sc.total_sessions,
    cs.status AS session_status,
    -- Logic trạng thái giống hệt v_student_today để đồng bộ UI
    CASE 
        WHEN a.id IS NOT NULL AND a.status = 'present' THEN 'present'
        WHEN a.id IS NOT NULL AND a.status = 'excused' THEN 'excused'
        WHEN cs.status = 'closed' AND (a.id IS NULL OR a.status = 'absent') THEN 'absent'
        WHEN cs.status = 'open' AND a.id IS NULL THEN 'ongoing_absent'
        WHEN cs.status = 'scheduled' THEN 'upcoming'
        ELSE 'pending'
    END AS attendance_status,
    a.scanned_at,
    cs_org.session_date AS original_session_date
FROM student s
JOIN schedule sc ON sc.admin_class_id = s.admin_class_id
JOIN class_session cs ON cs.schedule_id = sc.id
LEFT JOIN class_session cs_org ON cs_org.id = cs.makeup_for_id
JOIN subject sub ON sub.id = sc.subject_id
JOIN room r ON r.id = cs.actual_room_id
JOIN lecturer l ON l.id = cs.actual_lecturer_id 
JOIN user u_gv ON u_gv.id = l.user_id
JOIN period_time pt_s ON pt_s.period_number = cs.actual_period_start
JOIN period_time pt_e ON pt_e.period_number = cs.actual_period_end
LEFT JOIN attendance a ON a.class_session_id = cs.id AND a.student_id = s.id;


-- ============================================================
--  VIEW CHO MÀN HÌNH CHUYÊN CẦN SINH VIÊN
--  Gồm 2 view:
--    1. v_student_attendance_overview  — Thẻ tổng quan (header)
--    2. v_student_attendance_by_subject — Chi tiết từng môn
--
--  Cách dùng (Backend truyền student_id vào WHERE):
--    SELECT * FROM v_student_attendance_overview   WHERE student_id = ?;
--    SELECT * FROM v_student_attendance_by_subject WHERE student_id = ? ORDER BY subject_name;
-- ============================================================


-- ============================================================
-- VIEW 1: v_student_attendance_overview
-- Mục đích : Cung cấp dữ liệu cho thẻ "Tổng quan" đầu màn hình.
--            1 dòng = 1 sinh viên × 1 học kỳ đang active.
--
-- Cột trả về:
--   student_id          — ID sinh viên (dùng WHERE)
--   student_code        — Mã số sinh viên
--   full_name           — Họ tên sinh viên
--   admin_class_code    — Mã lớp hành chính (VD: CNTT-K22A)
--   semester_id         — ID học kỳ hiện tại
--   semester_name       — Tên học kỳ (VD: HK1-2024-2025)
--   total_passed        — Tổng buổi học đã diễn ra (closed), không tính cancelled
--   total_present       — Số buổi có mặt (status='present')
--   total_absent        — Số buổi vắng (status='absent'), không kể excused
--   total_excused       — Số buổi vắng có phép (status='excused')
--   total_late          — Số buổi đi muộn (is_late=1, status='present')
--   total_leave_early   — Số buổi về sớm (left_early=1, status='present')
--   attendance_rate_pct — Tỉ lệ chuyên cần % = (present+excused) / total_passed * 100
--                         Excused tính là "hợp lệ" — không phạt chuyên cần
-- ============================================================
CREATE OR REPLACE VIEW v_student_attendance_overview AS
SELECT
    s.id                                        AS student_id,
    s.student_code,
    u.full_name,
    ac.code                                     AS admin_class_code,
    sem.id                                      AS semester_id,
    sem.name                                    AS semester_name,

    COUNT(DISTINCT CASE WHEN cs.status IN ('closed', 'open') THEN cs.id END) AS total_passed,
    
    COUNT(CASE WHEN a.status = 'present' THEN 1 END)              AS total_present,
    COUNT(CASE WHEN a.status = 'absent' THEN 1 END)               AS total_absent,
    COUNT(CASE WHEN a.status = 'excused' THEN 1 END)              AS total_excused,
    COUNT(CASE WHEN a.status = 'present' AND a.is_late = 1 THEN 1 END) AS total_late,
    COUNT(CASE WHEN a.status = 'present' AND a.left_early = 1 THEN 1 END) AS total_leave_early,

    ROUND(
        COALESCE(
            SUM(CASE WHEN cs.status IN ('closed', 'open') AND a.status IN ('present','excused') THEN 1.0 ELSE 0.0 END)
            / NULLIF(COUNT(DISTINCT CASE WHEN cs.status IN ('closed', 'open') THEN cs.id END), 0)
            * 100
        , 100.0)
    , 1)                                        AS attendance_rate_pct

FROM student s
JOIN user                u   ON u.id  = s.user_id
JOIN administrative_class ac ON ac.id = s.admin_class_id
JOIN schedule            sc  ON sc.admin_class_id = s.admin_class_id
JOIN semester            sem ON sem.id = sc.semester_id -- [ĐÃ SỬA: JOIN TỰ NHIÊN THEO LỊCH]
JOIN class_session       cs  ON cs.schedule_id = sc.id
                             AND cs.status != 'cancelled'
LEFT JOIN attendance     a   ON a.class_session_id = cs.id
                             AND a.student_id       = s.id
GROUP BY
    s.id, s.student_code, u.full_name,
    ac.code, sem.id, sem.name;


-- ============================================================
-- VIEW 2: v_student_attendance_by_subject
-- Mục đích : Cung cấp dữ liệu cho danh sách "Chi tiết các môn"
--            (các thẻ môn học bên dưới).
--            1 dòng = 1 sinh viên × 1 môn học × 1 học kỳ active.
--
-- Cột trả về:
--   student_id          — ID sinh viên (dùng WHERE)
--   semester_id         — ID học kỳ hiện tại
--   subject_id          — ID môn học
--   subject_code        — Mã môn (VD: LTC101)
--   subject_name        — Tên môn đầy đủ
--   credits             — Số tín chỉ
--   lecturer_name       — Họ tên giảng viên phụ trách
--   total_sessions      — Tổng số buổi của môn (kể cả chưa học)
--   passed_sessions     — Số buổi đã kết thúc (closed)
--   remaining_sessions  — Số buổi chưa đến (scheduled + open)
--   present_count       — Số buổi có mặt
--   absent_count        — Số buổi vắng không phép
--   excused_count       — Số buổi vắng có phép
--   late_count          — Số buổi đi muộn (present + is_late=1)
--   leave_early_count   — Số buổi về sớm (present + left_early=1)
--   attendance_rate_pct — Tỉ lệ chuyên cần % theo môn
--   max_absent_allowed  — Ngưỡng vắng tối đa = FLOOR(total_sessions * 0.2)
--   is_danger           — 1 nếu absent_count > max_absent_allowed (cảnh báo đỏ)
--   attendance_status   — Nhãn tổng kết: 'safe' | 'warning' | 'danger'
--                         safe    : rate >= 80%
--                         warning : 60% <= rate < 80%
--                         danger  : rate < 60% hoặc vượt ngưỡng vắng
-- ============================================================
CREATE OR REPLACE VIEW v_student_attendance_by_subject AS
SELECT
    s.id                                        AS student_id,
    sem.id                                      AS semester_id,
    sub.id                                      AS subject_id,
    sub.code                                    AS subject_code,
    sub.name                                    AS subject_name,
    sub.credits,
    u_lec.full_name                             AS lecturer_name,

    -- [ĐÃ FIX LỖI] Cố định tổng buổi học (Bỏ qua buổi dạy bù)
    COUNT(DISTINCT CASE WHEN cs.makeup_for_id IS NULL THEN cs.id END) AS total_sessions,
    
    COUNT(DISTINCT CASE WHEN cs.status IN ('closed', 'open') THEN cs.id END) AS passed_sessions,
    
    -- [ĐÃ FIX LỖI] Còn lại = Tổng gốc - Đã diễn ra
    COUNT(DISTINCT CASE WHEN cs.makeup_for_id IS NULL THEN cs.id END) - 
    COUNT(DISTINCT CASE WHEN cs.status IN ('closed', 'open') THEN cs.id END) AS remaining_sessions,

    COUNT(CASE WHEN a.status = 'present' THEN 1 END) AS present_count,
    COUNT(CASE WHEN a.status = 'absent' THEN 1 END)  AS absent_count,
    COUNT(CASE WHEN a.status = 'excused' THEN 1 END) AS excused_count,
    COUNT(CASE WHEN a.status = 'present' AND a.is_late = 1 THEN 1 END) AS late_count,
    COUNT(CASE WHEN a.status = 'present' AND a.left_early = 1 THEN 1 END) AS leave_early_count,

    ROUND(
        COALESCE(
            SUM(CASE WHEN cs.status IN ('closed', 'open') AND a.status IN ('present','excused') THEN 1.0 ELSE 0.0 END)
            / NULLIF(COUNT(DISTINCT CASE WHEN cs.status IN ('closed', 'open') THEN cs.id END), 0)
            * 100
        , 100.0)
    , 1)                                        AS attendance_rate_pct,

    FLOOR(COUNT(DISTINCT CASE WHEN cs.makeup_for_id IS NULL THEN cs.id END) * 0.2) AS max_absent_allowed,

    IF(COUNT(CASE WHEN a.status = 'absent' THEN 1 END) > FLOOR(COUNT(DISTINCT CASE WHEN cs.makeup_for_id IS NULL THEN cs.id END) * 0.2), 1, 0) AS is_danger,

    CASE
        WHEN COUNT(CASE WHEN a.status = 'absent' THEN 1 END) > FLOOR(COUNT(DISTINCT CASE WHEN cs.makeup_for_id IS NULL THEN cs.id END) * 0.2)
            THEN 'danger'
        WHEN ROUND(
                COALESCE(
                    SUM(CASE WHEN cs.status IN ('closed', 'open') AND a.status IN ('present','excused') THEN 1.0 ELSE 0.0 END)
                    / NULLIF(COUNT(DISTINCT CASE WHEN cs.status IN ('closed', 'open') THEN cs.id END), 0)
                    * 100
                , 100.0)
             , 1) < 60
            THEN 'danger'
        WHEN ROUND(
                COALESCE(
                    SUM(CASE WHEN cs.status IN ('closed', 'open') AND a.status IN ('present','excused') THEN 1.0 ELSE 0.0 END)
                    / NULLIF(COUNT(DISTINCT CASE WHEN cs.status IN ('closed', 'open') THEN cs.id END), 0)
                    * 100
                , 100.0)
             , 1) < 80
            THEN 'warning'
        ELSE 'safe'
    END                                         AS attendance_status

FROM student s
JOIN user                u_stu ON u_stu.id = s.user_id
JOIN schedule            sc    ON sc.admin_class_id = s.admin_class_id
JOIN semester            sem   ON sem.id = sc.semester_id -- [ĐÃ SỬA: JOIN TỰ NHIÊN THEO LỊCH]
JOIN subject             sub   ON sub.id = sc.subject_id
JOIN lecturer            lec   ON lec.id = sc.lecturer_id
JOIN user                u_lec ON u_lec.id = lec.user_id
-- [ĐÃ FIX LỖI] Không loại trừ cancelled ở đây nữa để giữ nguyên Tổng số buổi
JOIN class_session       cs    ON cs.schedule_id = sc.id
LEFT JOIN attendance     a     ON a.class_session_id = cs.id
                               AND a.student_id       = s.id
GROUP BY
    s.id, sem.id, sub.id, sub.code, sub.name, sub.credits, u_lec.full_name;


-- ============================================================
-- USAGE EXAMPLES
-- ============================================================

-- [1] Lấy tổng quan cho sinh viên có student_id = 42
--     → Dùng cho thẻ header "Tổng quan" + 4 badge số liệu
-- SELECT * FROM v_student_attendance_overview WHERE student_id = 42;

-- [2] Lấy chi tiết từng môn cho sinh viên đó
--     → Dùng cho danh sách thẻ môn học bên dưới
-- SELECT * FROM v_student_attendance_by_subject
-- WHERE student_id = 42
-- ORDER BY attendance_status DESC, subject_name;
-- (ORDER: danger lên đầu → warning → safe)

-- [3] Lọc chỉ môn đang nguy hiểm (cảnh báo cấm thi)
-- SELECT subject_name, absent_count, max_absent_allowed, attendance_rate_pct
-- FROM v_student_attendance_by_subject
-- WHERE student_id = 42 AND is_danger = 1;

-- ============================================================
-- ============================================================
-- KẾT THÚC
-- ============================================================

-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================
-- TỔNG KẾT HỆ THỐNG CƠ SỞ DỮ LIỆU
-- ============================================================
-- Tổng số bảng chính  : 14 Bảng
-- 
-- Phân loại theo nhóm:
--   Nhóm 1 (Tổ chức)      : faculty, department, administrative_class
--   Nhóm 2 (Người dùng)   : user, lecturer, student
--   Nhóm 3 (Học thuật)    : subject, room, semester, period_time
--   Nhóm 4 (Lịch học)     : schedule, class_session
--   Nhóm 5 (Check-out)    : checkout_event
--   Nhóm 6 (Điểm danh)    : attendance
--
-- Logic & Khung nhìn:
--   Stored Procedure      : 1 (generate_sessions_for_schedule)
--   Function              : 1 (haversine_distance)
--   View đang hoạt động   : 9 View
--       1. v_lecturer_today
--       2. v_schedule_progress
--       3. v_attendance_summary
--       4. v_lecturer_week
--       5. v_lecturer_semester_summary
--       6. v_student_today
--       7. v_student_schedule
--       8. v_student_attendance_overview
--       9. v_student_attendance_by_subject
--
-- Ghi chú: 3 View cũ (suspicious_attendance, makeup_sessions, checkout_summary) 
--          đã được chuyển xuống cuối file và comment lại.
-- ============================================================
-- ============================================================
-- CÁC VIEW KHÔNG SỬ DỤNG (ĐƯỢC COMMENT VÀ ĐẨY XUỐNG CUỐI)
-- ============================================================
-- ============================================================
-- V4: v_suspicious_attendance — Điểm danh đáng ngờ (audit GPS)
-- ============================================================
--
--  Mục đích : Liệt kê các bản ghi điểm danh có dấu hiệu gian lận
--             vị trí — SV quét QR từ xa hoặc bị GV override GPS.
--             Dùng cho Admin / GV kiểm tra và xử lý thủ công.
--
--  Ai dùng  : Admin, Giảng viên (kiểm tra sau buổi học)
--
--  Lọc sẵn : Chỉ trả về bản ghi thoả một trong hai điều kiện:
--             (1) gps_verified = 0 → ngoài phạm vi nhưng GV override
--             (2) distance_m > gps_radius_m × 2 → xa gấp đôi bán kính
--
--  Các cột trả về:
--  ┌──────────────────┬─────────────────────────────────────────────────────┐
--  │ attendance_id    │ ID bản ghi điểm danh — dùng để tra cứu / sửa tay   │
--  │ student_name     │ Họ tên sinh viên                                    │
--  │ student_code     │ Mã số sinh viên                                     │
--  │ subject_name     │ Môn học của buổi điểm danh                         │
--  │ session_date     │ Ngày diễn ra buổi học                               │
--  │ scanned_at       │ Thời điểm SV quét QR check-in                       │
--  │ scan_lat         │ Vĩ độ GPS lúc SV quét                               │
--  │ scan_lng         │ Kinh độ GPS lúc SV quét                             │
--  │ distance_m       │ Khoảng cách SV đến tâm phòng (mét, tính Haversine) │
--  │ gps_radius_m     │ Bán kính hợp lệ của phòng thực tế (mét)            │
--  │ over_radius_m    │ Số mét vượt bán kính (distance_m - gps_radius_m)   │
--  │                  │ Âm = trong phòng; dương = ngoài phạm vi            │
--  │ gps_verified     │ 0 = ngoài phạm vi nhưng GV đã bấm override         │
--  │                  │ NULL = GPS bị tắt cho buổi đó (không nghi ngờ)     │
--  │ late_minutes     │ Số phút muộn lúc check-in (0 = đúng giờ)           │
--  └──────────────────┴─────────────────────────────────────────────────────┘
--
--  Lưu ý   : gps_radius_m lấy từ actual_room_id (phòng thực tế của buổi)
--             để đảm bảo so sánh chính xác khi buổi học ở phòng bù.
-- ============================================================
-- CREATE OR REPLACE VIEW v_suspicious_attendance AS
-- SELECT
--     a.id                                        AS attendance_id,
--     u.full_name                                 AS student_name,
--     s.student_code,
--     sub.name                                    AS subject_name,
--     cs.session_date,
--     a.scanned_at,
--     a.scan_lat,
--     a.scan_lng,
--     a.distance_m,
--     r.gps_radius_m,
--     (a.distance_m - r.gps_radius_m)            AS over_radius_m,
--     a.gps_verified,
--     a.late_minutes
-- FROM attendance a
-- JOIN student            s   ON s.id   = a.student_id
-- JOIN user               u   ON u.id   = s.user_id
-- JOIN class_session      cs  ON cs.id  = a.class_session_id
-- JOIN schedule           sc  ON sc.id  = cs.schedule_id
-- JOIN subject            sub ON sub.id = sc.subject_id
-- ★ Dùng actual_room_id — GPS radius phải khớp phòng thực tế của buổi học
-- JOIN room               r   ON r.id   = cs.actual_room_id
-- WHERE a.gps_verified = 0
--    OR (a.distance_m IS NOT NULL AND a.distance_m > r.gps_radius_m * 2);
 
 
-- ============================================================
-- V5: v_makeup_sessions — Buổi dạy bù và buổi gốc tương ứng
-- ============================================================
--
--  Mục đích : Liệt kê tất cả buổi dạy bù trong hệ thống kèm
--             thông tin buổi gốc mà buổi bù thay thế. Dùng để
--             Admin / GV theo dõi và đối chiếu lịch bù.
--
--  Ai dùng  : Admin, Giảng viên
--
--  Lọc sẵn : Chỉ trả về class_session có makeup_for_id IS NOT NULL
--             (tức là các buổi được tạo ra để bù một buổi khác)
--
--  Các cột trả về:
--  ┌──────────────────────┬───────────────────────────────────────────────────┐
--  │ makeup_session_id    │ ID buổi dạy bù                                    │
--  │ schedule_id          │ ID dòng TKB gốc mà buổi bù thuộc về              │
--  │ makeup_date          │ Ngày dạy bù thực tế                               │
--  │ makeup_status        │ Trạng thái buổi bù: scheduled / open / closed     │
--  │ session_number       │ Số thứ tự buổi (giữ nguyên số của buổi gốc)       │
--  │ original_session_id  │ ID buổi gốc bị thay thế                           │
--  │ original_date        │ Ngày của buổi gốc                                 │
--  │ subject_name         │ Tên môn học                                       │
--  │ class_name           │ Tên lớp hành chính                               │
--  │ makeup_room          │ Mã phòng học buổi bù (actual_room_id của buổi bù) │
--  │ makeup_building      │ Toà nhà phòng học buổi bù                        │
--  │ original_room        │ Mã phòng buổi gốc (actual_room_id của buổi gốc)   │
--  │ original_building    │ Toà nhà phòng buổi gốc                           │
--  │ makeup_lecturer_id   │ ID GV dạy buổi bù (có thể khác GV gốc)           │
--  │ makeup_lecturer_name │ Họ tên GV dạy buổi bù                            │
--  └──────────────────────┴───────────────────────────────────────────────────┘
--
--  Lưu ý   : makeup_room và original_room lấy từ actual_room_id của
--             từng buổi (không phải schedule.room_id), đảm bảo phản
--             ánh đúng phòng thực tế kể cả khi đổi phòng nhiều lần.
-- ============================================================
-- CREATE OR REPLACE VIEW v_makeup_sessions AS
-- SELECT
--     cs_buu.id                                   AS makeup_session_id,
--     cs_buu.schedule_id,
--     cs_buu.session_date                         AS makeup_date,
--     cs_buu.status                               AS makeup_status,
--     cs_buu.session_number,
--     cs_buu.actual_period_start                  AS makeup_period_start,
--     cs_buu.actual_period_end                    AS makeup_period_end,
--     pt_buu_s.start_time                         AS makeup_start_time,
--     pt_buu_e.end_time                           AS makeup_end_time,
--     cs_goc.id                                   AS original_session_id,
--     cs_goc.session_date                         AS original_date,
--     cs_goc.actual_period_start                  AS original_period_start,
--     cs_goc.actual_period_end                    AS original_period_end,
--     pt_goc_s.start_time                         AS original_start_time,
--     pt_goc_e.end_time                           AS original_end_time,
--     sub.name                                    AS subject_name,
--     ac.name                                     AS class_name,
--     r_buu.code                                  AS makeup_room,
--     r_buu.building                              AS makeup_building,
--     r_goc.code                                  AS original_room,
--     r_goc.building                              AS original_building,
--     l_buu.id                                    AS makeup_lecturer_id,
--     u_buu.full_name                             AS makeup_lecturer_name
-- FROM class_session cs_buu
-- JOIN class_session        cs_goc  ON cs_goc.id   = cs_buu.makeup_for_id
-- JOIN schedule             sc      ON sc.id        = cs_buu.schedule_id
-- JOIN subject              sub     ON sub.id       = sc.subject_id
-- JOIN administrative_class ac      ON ac.id        = sc.admin_class_id
-- ★ Phòng dạy bù = actual_room_id của buổi bù
-- JOIN room                 r_buu   ON r_buu.id     = cs_buu.actual_room_id
-- ★ Phòng buổi gốc = actual_room_id của buổi gốc
-- JOIN room                 r_goc   ON r_goc.id     = cs_goc.actual_room_id
-- GV dạy buổi bù (có thể khác GV dạy buổi gốc)
-- JOIN lecturer             l_buu   ON l_buu.id     = cs_buu.actual_lecturer_id
-- JOIN user                 u_buu   ON u_buu.id     = l_buu.user_id
-- JOIN period_time          pt_buu_s ON pt_buu_s.period_number = cs_buu.actual_period_start
-- JOIN period_time          pt_buu_e ON pt_buu_e.period_number = cs_buu.actual_period_end
-- JOIN period_time          pt_goc_s ON pt_goc_s.period_number = cs_goc.actual_period_start
-- JOIN period_time          pt_goc_e ON pt_goc_e.period_number = cs_goc.actual_period_end
-- WHERE cs_buu.makeup_for_id IS NOT NULL;
 
 
-- ============================================================
-- V6: v_checkout_summary — Kết quả từng lần kích hoạt check-out
-- ============================================================
--
--  Mục đích : Tổng hợp kết quả sau mỗi lần GV kích hoạt check-out
--             giữa buổi học để kiểm tra SV về sớm. Dùng để GV xem
--             lại lịch sử các lần check-out và tỉ lệ SV phản hồi.
--
--  Ai dùng  : Giảng viên (xem lại sau buổi), Admin (audit)
--
--  Các cột trả về:
--  ┌────────────────────────┬─────────────────────────────────────────────────┐
--  │ checkout_event_id      │ ID lần check-out — khoá tra cứu chi tiết        │
--  │ class_session_id       │ ID buổi học mà check-out được kích hoạt         │
--  │ triggered_at           │ Thời điểm GV nhấn "Yêu cầu check-out"           │
--  │ deadline_at            │ Deadline SV phải quét (thường triggered_at + 5')│
--  │ closed_at              │ Thời điểm đóng check-out. NULL = đang mở         │
--  │ note                   │ Ghi chú lý do GV kích hoạt                      │
--  │ subject_name           │ Tên môn học của buổi                             │
--  │ class_name             │ Tên lớp hành chính                              │
--  │ triggered_by_name      │ Họ tên GV đã kích hoạt check-out                │
--  │ total_present_students │ Tổng SV đang có mặt (present) tại thời điểm đó  │
--  │ checked_out_count      │ Số SV đã quét QR check-out trước deadline        │
--  │ left_early_count       │ Số SV bị đánh left_early = 1 (không quét kịp)   │
--  └────────────────────────┴─────────────────────────────────────────────────┘
--
--  Lưu ý   : total_present_students chỉ đếm SV có status = 'present'
--             vì SV absent/excused không cần quét check-out.
--             Tỉ lệ phản hồi = checked_out_count / total_present_students.
-- ============================================================
-- CREATE OR REPLACE VIEW v_checkout_summary AS
-- SELECT
--     co.id                                                           AS checkout_event_id,
--     co.class_session_id,
--     co.triggered_at,
--     co.deadline_at,
--     co.closed_at,
--     co.note,
--     sub.name                                                        AS subject_name,
--     ac.name                                                         AS class_name,
--     u_gv.full_name                                                  AS triggered_by_name,
--     COUNT(a.id)                                                     AS total_present_students,
--     COUNT(CASE WHEN a.checked_out_at IS NOT NULL THEN 1 END)       AS checked_out_count,
--     COUNT(CASE WHEN a.left_early = 1             THEN 1 END)       AS left_early_count
-- FROM checkout_event co
-- JOIN class_session        cs   ON cs.id    = co.class_session_id
-- JOIN schedule             sc   ON sc.id    = cs.schedule_id
-- JOIN subject              sub  ON sub.id   = sc.subject_id
-- JOIN administrative_class ac   ON ac.id    = sc.admin_class_id
-- JOIN user                 u_gv ON u_gv.id  = co.triggered_by
-- Chỉ đếm SV đang có mặt (present) — SV absent/excused không cần check-out
-- LEFT JOIN attendance      a    ON a.class_session_id = co.class_session_id
--                                AND a.status = 'present'
-- GROUP BY
--     co.id, co.class_session_id, co.triggered_at, co.deadline_at,
--     co.closed_at, co.note, sub.name, ac.name, u_gv.full_name;

-- ============================================================
-- V10: V_ADMIN_SCHOOL_REPORT — Báo cáo toàn trường cho Admin
-- ============================================================
CREATE OR REPLACE VIEW v_admin_school_report AS
SELECT
    sc.id AS schedule_id,
    sc.semester_id,
    sem.name AS semester_name,
    f.id AS faculty_id,
    f.name AS faculty_name,
    d.id AS department_id,
    d.name AS department_name,
    ac.name AS class_name,
    sub.name AS subject_name,
    u_lec.full_name AS lecturer_name,
    (SELECT COUNT(*) FROM student s WHERE s.admin_class_id = ac.id) AS total_students,
    COUNT(DISTINCT CASE WHEN cs.status = 'closed' THEN cs.id END) AS completed_sessions,
    sc.total_sessions AS total_sessions,
    ROUND(
        COALESCE(
            SUM(CASE WHEN cs.status = 'closed' AND a.status IN ('present', 'excused') THEN 1.0 ELSE 0.0 END)
            / NULLIF(
                COUNT(DISTINCT CASE WHEN cs.status = 'closed' THEN cs.id END) * 
                (SELECT COUNT(*) FROM student s WHERE s.admin_class_id = ac.id)
            , 0) * 100
        , 100.0)
    , 1) AS attendance_rate
FROM schedule sc
JOIN semester sem ON sem.id = sc.semester_id
JOIN subject sub ON sub.id = sc.subject_id
JOIN administrative_class ac ON ac.id = sc.admin_class_id
JOIN department d ON d.id = ac.department_id
JOIN faculty f ON f.id = d.faculty_id
JOIN lecturer l ON l.id = sc.lecturer_id
JOIN user u_lec ON u_lec.id = l.user_id
LEFT JOIN class_session cs ON cs.schedule_id = sc.id AND cs.status != 'cancelled'
LEFT JOIN attendance a ON a.class_session_id = cs.id
GROUP BY
    sc.id, sc.semester_id, sem.name, f.id, f.name, d.id, d.name, ac.id, ac.name, sub.name, u_lec.full_name, sc.total_sessions;
