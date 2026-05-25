import { NavLink, useNavigate } from 'react-router-dom';
import api from '../utils/api';

export default function Sidebar({ role }) {
  const isAdmin = role === 'admin';
  const navigate = useNavigate();

  const userName = localStorage.getItem('userName') || (isAdmin ? 'Quản trị viên' : 'Giảng viên');
  const userAvatar = localStorage.getItem('userAvatar');

  const handleLogout = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        // Gọi API backend để xóa refresh token khỏi database
        await api.post('/auth/logout', { refreshToken });
      }
    } catch (error) {
      console.error('Lỗi khi logout:', error);
    } finally {
      // Luôn dọn dẹp bộ nhớ nội bộ cho dù gọi API có lỗi hay không
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userName');
      localStorage.removeItem('userAvatar');
      navigate('/login');
    }
  };

  return (
    <aside className="sb">
      {/* Logo */}
      <div className="sb-logo">
        <div className="sb-logo-ic">📡</div>
        <div>
          <div className="sb-logo-tx">QRAttend</div>
          <div className="sb-logo-su">Hệ thống điểm danh QR</div>
        </div>
      </div>

      {/* User info */}
      <div className="sb-user">
        {userAvatar ? (
          <img src={userAvatar} alt="Avatar" className="sb-av" style={{ border: 'none', objectFit: 'cover' }} referrerPolicy="no-referrer" />
        ) : (
          <div 
            className="sb-av" 
            style={{ 
              background: isAdmin ? 'linear-gradient(135deg,#3B82F6,#A855F7)' : 'linear-gradient(135deg,#22C55E,#14B8A6)' 
            }}
          >
            {userName.substring(0, 2).toUpperCase()}
          </div>
        )}
        <div>
          <div className="sb-un">{userName}</div>
          <div className="sb-ur">{isAdmin ? 'Admin · Hệ thống' : 'Giảng viên · Khoa CNTT'}</div>
        </div>
      </div>


      {/* Navigation */}
      {!isAdmin ? (
        <div id="nav-gv">
          <div className="sb-sec">Quản lý giảng dạy</div>
          <NavLink to="/" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`} end>
            <span className="dot"></span>Tổng quan
          </NavLink>
          <NavLink to="/tkb" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Thời khóa biểu
          </NavLink>
          <NavLink to="/sessions" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Sổ điểm danh
          </NavLink>
          <NavLink to="/requests" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Yêu cầu của tôi
          </NavLink>

          <div className="sb-sec">Báo cáo</div>
          <NavLink to="/report" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Lớp giảng dạy
          </NavLink>
          <NavLink to="/homeroom" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Lớp chủ nhiệm
          </NavLink>
        </div>
      ) : (
        <div id="nav-admin">
          <div className="sb-sec">Tổng quan</div>
          <NavLink to="/admin" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`} end>
            <span className="dot"></span>Dashboard Admin
          </NavLink>
          <NavLink to="/admin/requests" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Phê duyệt giảng dạy
          </NavLink>

          <div className="sb-sec">Quản lý danh mục</div>
          <NavLink to="/admin/tkb" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Thời khóa biểu
          </NavLink>
          <NavLink to="/admin/faculties" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Quản lý Khoa
          </NavLink>
          <NavLink to="/admin/departments" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Quản lý Ngành
          </NavLink>
          <NavLink to="/admin/administrative-classes" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Lớp hành chính
          </NavLink>
          <NavLink to="/admin/students" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Sinh viên
          </NavLink>
          <NavLink to="/admin/lecturers" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Giảng viên
          </NavLink>
          <NavLink to="/admin/rooms" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Phòng học
          </NavLink>
          <NavLink to="/admin/subjects" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Môn học
          </NavLink>
          <NavLink to="/admin/semesters" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Học kỳ
          </NavLink>

          <div className="sb-sec">Báo cáo</div>
          <NavLink to="/admin/report" className={({ isActive }) => `sb-it ${isActive ? 'on' : ''}`}>
            <span className="dot"></span>Báo cáo toàn trường
          </NavLink>
        </div>
      )}

      <div className="sb-foot">
        <button className="sb-out" onClick={handleLogout}>⎋ &nbsp;Đăng xuất</button>
      </div>
    </aside>
  );
}
