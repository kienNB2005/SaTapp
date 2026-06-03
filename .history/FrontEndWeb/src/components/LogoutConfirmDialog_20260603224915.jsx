import { useState } from 'react';
import { AlertCircle, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../utils/api';

export default function LogoutConfirmDialog({ isOpen, onClose }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      setLoading(true);
      setError('');

      const refreshToken = localStorage.getItem('refreshToken');

      // TODO: Replace with actual logout API endpoint
      // await api.post('/auth/logout', {
      //   refreshToken: refreshToken
      // });

      // Clear tokens from localStorage
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');

      // Redirect to login
      navigate('/login');
      onClose();
    } catch (err) {
      setError('Lỗi khi đăng xuất. Vui lòng thử lại.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setError('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="dialog-overlay" onClick={handleCancel} style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1001
    }}>
      <div className="dialog-content" onClick={(e) => e.stopPropagation()} style={{
        backgroundColor: 'white',
        borderRadius: '12px',
        padding: '32px',
        maxWidth: '400px',
        width: '90%',
        boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)'
      }}>
        {/* Icon */}
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          marginBottom: '24px'
        }}>
          <div style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            backgroundColor: '#ffe5e5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <AlertCircle size={36} style={{ color: '#e74c3c' }} />
          </div>
        </div>

        {/* Title */}
        <h2 style={{
          textAlign: 'center',
          margin: '0 0 12px 0',
          fontSize: '20px',
          fontWeight: '600',
          color: '#333'
        }}>
          Xác nhận đăng xuất
        </h2>

        {/* Description */}
        <p style={{
          textAlign: 'center',
          margin: '0 0 24px 0',
          fontSize: '14px',
          color: '#666'
        }}>
          Bạn có chắc chắn muốn đăng xuất? Bạn sẽ cần đăng nhập lại để tiếp tục.
        </p>

        {/* Error message */}
        {error && (
          <div style={{
            backgroundColor: '#fee',
            color: '#c00',
            padding: '12px',
            borderRadius: '8px',
            marginBottom: '16px',
            fontSize: '14px',
            textAlign: 'center'
          }}>
            {error}
          </div>
        )}

        {/* Buttons */}
        <div style={{
          display: 'flex',
          gap: '12px'
        }}>
          <button
            onClick={handleCancel}
            disabled={loading}
            style={{
              flex: 1,
              padding: '12px 24px',
              backgroundColor: '#f0f0f0',
              color: '#333',
              border: 'none',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: '500',
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.6 : 1,
              transition: 'background-color 0.2s'
            }}
            onMouseEnter={(e) => !loading && (e.target.style.backgroundColor = '#e0e0e0')}
            onMouseLeave={(e) => !loading && (e.target.style.backgroundColor = '#f0f0f0')}
          >
            Hủy
          </button>
          <button
            onClick={handleLogout}
            disabled={loading}
            style={{
              flex: 1,
              padding: '12px 24px',
              backgroundColor: '#e74c3c',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: '500',
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.6 : 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              transition: 'background-color 0.2s'
            }}
            onMouseEnter={(e) => !loading && (e.target.style.backgroundColor = '#c0392b')}
            onMouseLeave={(e) => !loading && (e.target.style.backgroundColor = '#e74c3c')}
          >
            <LogOut size={16} />
            {loading ? 'Đang xử lý...' : 'Đăng xuất'}
          </button>
        </div>
      </div>
    </div>
  );
}
