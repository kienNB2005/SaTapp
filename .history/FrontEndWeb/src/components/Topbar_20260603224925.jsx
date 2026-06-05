
import { useLocation } from 'react-router-dom';
import { useRef, useState, useEffect } from 'react';
import { Bell, Settings, LogOut, Lock, User } from "lucide-react";
import AccountInfoModal from './AccountInfoModal';
import ChangePasswordModal from './ChangePasswordModal';
import LogoutConfirmDialog from './LogoutConfirmDialog';

const PAGE_INFO = [
  { path: '/', section: 'Tổng quan', title: 'Dashboard', sub: 'Thứ 3, 02/09/2025 · HK1-2024-2025', exact: true },
  { path: '/tkb', section: 'Quản lý giảng dạy', title: 'Thời khóa biểu', sub: 'Lịch trình giảng dạy của bạn' },
  { path: '/sessions', section: 'Quản lý giảng dạy', title: 'Sổ điểm danh', sub: '15 buổi · HK1-2024-2025' },
  { path: '/sessions/', section: 'Quản lý giảng dạy', title: 'Chi tiết điểm danh', sub: 'Buổi học · CTDL · K22A' },
  { path: '/report', section: 'Báo cáo', title: 'Lớp giảng dạy', sub: 'CTDL · CNTT-K22A · HK1-2024-2025' },
  { path: '/homeroom', section: 'Báo cáo', title: 'Lớp chủ nhiệm', sub: 'CNTT-K22A · HK1-2024-2025' },
  { path: '/requests', section: 'Quản lý giảng dạy', title: 'Yêu cầu của tôi', sub: 'Theo dõi yêu cầu chấm công và điểm danh' },
  { path: '/qr', section: 'Quản lý giảng dạy', title: 'Điểm danh QR', sub: 'Quét mã và quản lý buổi học' },
  { path: '/admin', section: 'Tổng quan', title: 'Dashboard Admin', sub: 'HK1-2024-2025 · Toàn trường', exact: true },
  { path: '/admin/requests', section: 'Tổng quan', title: 'Phê duyệt giảng dạy', sub: 'Quản lý yêu cầu của giảng viên' },
  { path: '/admin/tkb', section: 'Quản lý danh mục', title: 'Thời khóa biểu', sub: 'Upload file TKB từ trường' },
  { path: '/admin/faculties', section: 'Quản lý danh mục', title: 'Quản lý Khoa', sub: 'Import & Quản lý danh sách Khoa' },
  { path: '/admin/departments', section: 'Quản lý danh mục', title: 'Quản lý Ngành', sub: 'Import & Quản lý danh sách Ngành' },
  { path: '/admin/administrative-classes', section: 'Quản lý danh mục', title: 'Lớp hành chính', sub: 'Quản lý danh sách lớp hành chính' },
  { path: '/admin/students', section: 'Quản lý danh mục', title: 'Sinh viên', sub: 'Import & quản lý danh sách Sinh viên' },
  { path: '/admin/lecturers', section: 'Quản lý danh mục', title: 'Giảng viên', sub: 'Import & quản lý danh sách Giảng viên' },
  { path: '/admin/rooms', section: 'Quản lý danh mục', title: 'Phòng học', sub: 'Quản lý thông tin phòng học' },
  { path: '/admin/subjects', section: 'Quản lý danh mục', title: 'Môn học', sub: 'Quản lý danh sách môn học' },
  { path: '/admin/semesters', section: 'Quản lý danh mục', title: 'Học kỳ', sub: 'Quản lý thông tin học kỳ' },
  { path: '/admin/report', section: 'Báo cáo', title: 'Báo cáo Toàn trường', sub: 'HK1-2024-2025' },
];

function getPageInfo(pathname) {
  const exactMatch = PAGE_INFO.find(item => item.exact && item.path === pathname);
  if (exactMatch) return exactMatch;
  const prefixMatch = PAGE_INFO.find(item => !item.exact && pathname.startsWith(item.path));
  return exactMatch || prefixMatch || { section: 'Tổng quan', title: 'Dashboard', sub: '' };
}

export default function Topbar() {
  const location = useLocation();
  const [showMenu, setShowMenu] = useState(false);
  const menuRef = useRef(null);

  const { section, title, sub } = getPageInfo(location.pathname);
  const displayTitle = section || 'Dashboard';
  const displaySub = title ? `${title} · ${sub}` : sub || '';

  // Close menu when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowMenu(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    // TODO: Implement logout logic
    console.log('Logging out...');
    // localStorage.removeItem('token');
    // navigate('/login');
  };

  return (
    <div className="topbar">
      <div>
        <div className="tb-title" id="pgTitle">{displayTitle}</div>
        <div className="tb-sub" id="pgSub">{displaySub}</div>
      </div>
      <div className="tb-acts">
        <button className="btn btn-bell" style={{ padding: '7px 10px' }}>
          <Bell size={16} className="sb-icon" />
        </button>
        
        <div className="settings-menu" ref={menuRef} style={{ position: 'relative' }}>
          <button 
            className="btn btn-settings"
            onClick={() => setShowMenu(!showMenu)}
            style={{ padding: '7px 10px' }}
          >
            <Settings size={16} className="sb-icon" />
          </button>

          {showMenu && (
            <div className="dropdown-menu" style={{
              position: 'absolute',
              right: 0,
              top: '100%',
              marginTop: '8px',
              backgroundColor: 'white',
              border: '1px solid #ddd',
              borderRadius: '8px',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
              minWidth: '200px',
              zIndex: 1000
            }}>
              <button
                className="menu-item"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  width: '100%',
                  padding: '12px 16px',
                  border: 'none',
                  backgroundColor: 'transparent',
                  cursor: 'pointer',
                  fontSize: '14px',
                  color: '#333',
                  borderBottom: '1px solid #eee',
                  textAlign: 'left'
                }}
                onMouseEnter={(e) => e.target.style.backgroundColor = '#f5f5f5'}
                onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
              >
                <User size={16} />
                Thông tin tài khoản
              </button>

              <button
                className="menu-item"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  width: '100%',
                  padding: '12px 16px',
                  border: 'none',
                  backgroundColor: 'transparent',
                  cursor: 'pointer',
                  fontSize: '14px',
                  color: '#333',
                  borderBottom: '1px solid #eee',
                  textAlign: 'left'
                }}
                onMouseEnter={(e) => e.target.style.backgroundColor = '#f5f5f5'}
                onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
              >
                <Lock size={16} />
                Đổi mật khẩu
              </button>

              <button
                className="menu-item"
                onClick={handleLogout}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  width: '100%',
                  padding: '12px 16px',
                  border: 'none',
                  backgroundColor: 'transparent',
                  cursor: 'pointer',
                  fontSize: '14px',
                  color: '#e74c3c',
                  textAlign: 'left'
                }}
                onMouseEnter={(e) => e.target.style.backgroundColor = '#fff5f5'}
                onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
              >
                <LogOut size={16} />
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
