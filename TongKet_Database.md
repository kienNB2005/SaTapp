# Tổng Kết Cơ Sở Dữ Liệu - Hệ Thống Điểm Danh QR

Hệ thống cơ sở dữ liệu được thiết kế hoàn chỉnh cho mô hình lớp học cố định, bao gồm **14 Bảng**, **1 Stored Procedure**, **1 Function** và **9 Khung nhìn (View)**.

## 1. Danh sách các bảng chính (14 Bảng)
Được chia thành 6 nhóm nghiệp vụ cốt lõi:

*   **Nhóm 1 - Tổ chức hành chính:** `faculty`, `department`, `administrative_class`
*   **Nhóm 2 - Người dùng:** `user`, `lecturer`, `student`
*   **Nhóm 3 - Danh mục học thuật:** `subject`, `room`, `semester`, `period_time`
*   **Nhóm 4 - Thời khóa biểu & Buổi học:** `schedule`, `class_session`
*   **Nhóm 5 - Check-out tức thời:** `checkout_event`
*   **Nhóm 6 - Vận hành điểm danh:** `attendance`

## 2. Logic xử lý nội bộ (Database Engine)
*   **Stored Procedure:** `generate_sessions_for_schedule` (Tự động sinh ra hàng loạt các buổi học cụ thể từ một mẫu thời khóa biểu).
*   **Function:** `haversine_distance` (Tính toán khoảng cách theo công thức Haversine để kiểm tra sinh viên có đứng đúng trong phòng học khi quét QR hay không).

## 3. Khung nhìn (Views) - 9 Views
Hỗ trợ truy xuất dữ liệu phức tạp cho phía Backend một cách nhanh chóng và tối ưu:
1.  `v_lecturer_today`: Dashboard buổi học của giảng viên hôm nay.
2.  `v_schedule_progress`: Tiến độ hoàn thành các buổi học.
3.  `v_attendance_summary`: Thống kê tỉ lệ chuyên cần tổng quan.
4.  `v_lecturer_week`: Lịch học trong tuần của giảng viên.
5.  `v_lecturer_semester_summary`: Tổng kết học kỳ cho giảng viên.
6.  `v_student_today`: Dashboard các lớp hôm nay cho sinh viên.
7.  `v_student_schedule`: Toàn bộ thời khóa biểu của sinh viên.
8.  `v_student_attendance_overview`: Tổng quan điểm danh của sinh viên.
9.  `v_student_attendance_by_subject`: Chi tiết chuyên cần theo từng môn.

*(Ghi chú: 3 Views cũ không còn sử dụng đã được comment và đẩy xuống cuối file `.sql` nhằm lưu trữ)*.
