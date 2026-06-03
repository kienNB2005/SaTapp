import { useState, useEffect } from 'react';
import { X, Mail, Phone, Calendar, MapPin, User } from 'lucide-react';
import api from '../utils/api';

export default function AccountInfoModal({ isOpen, onClose }) {
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchUserInfo();
    }
  }, [isOpen]);

  const fetchUserInfo = async () => {
    try {
      setLoading(true);
      setError('');
      // TODO: Replace with actual API endpoint
      // const response = await api.get('/api/v1/users/me');
      // setUserInfo(response.data.result);

      // Mock data for now
      setUserInfo({
        id: 1,
        fullName: 'Nguyễn Văn A',
        email: 'example@student.hcmus.edu.vn',
        phoneNumber: '0123456789',
        gender: 'male',
        birthday: '2000-01-15',
        birthPlace: 'Hồ Chí Minh',
        role: 'student'
      });
    } catch (err) {
      setError('Lỗi khi tải thông tin tài khoản');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
  };

  const getRoleLabel = (role) => {
    const roleMap = {
      student: 'Sinh viên',
      lecturer: 'Giảng viên',
      admin: 'Quản trị viên'
    };
    return roleMap[role] || role;
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose} style={{
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
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{
        backgroundColor: 'white',
        borderRadius: '12px',
        padding: '32px',
        maxWidth: '500px',
        width: '90%',
        maxHeight: '80vh',
        overflowY: 'auto',
        boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)'
      }}>
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px'
        }}>
          <h2 style={{ margin: 0, fontSize: '24px', fontWeight: '600' }}>
            Thông tin tài khoản
          </h2>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '24px',
              cursor: 'pointer',
              color: '#999'
            }}
          >
            <X size={24} />
          </button>
        </div>

        {loading && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <p>Đang tải...</p>
          </div>
        )}

        {error && (
          <div style={{
            backgroundColor: '#fee',
            color: '#c00',
            padding: '12px',
            borderRadius: '8px',
            marginBottom: '16px'
          }}>
            {error}
          </div>
        )}

        {userInfo && !loading && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Họ và tên */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '16px',
              padding: '12px',
              backgroundColor: '#f9f9f9',
              borderRadius: '8px'
            }}>
              <User size={24} style={{ color: '#3498db', flexShrink: 0 }} />
              <div>
                <p style={{ margin: 0, fontSize: '12px', color: '#999' }}>Họ và tên</p>
                <p style={{ margin: '4px 0 0 0', fontSize: '16px', fontWeight: '500' }}>
                  {userInfo.fullName}
                </p>
              </div>
            </div>

            {/* Email */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '16px',
              padding: '12px',
              backgroundColor: '#f9f9f9',
              borderRadius: '8px'
            }}>
              <Mail size={24} style={{ color: '#e74c3c', flexShrink: 0 }} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{ margin: 0, fontSize: '12px', color: '#999' }}>Email</p>
                <p style={{
                  margin: '4px 0 0 0',
                  fontSize: '14px',
                  fontWeight: '500',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap'
                }}>
                  {userInfo.email}
                </p>
              </div>
            </div>

            {/* Điện thoại */}
            {userInfo.phoneNumber && (
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                padding: '12px',
                backgroundColor: '#f9f9f9',
                borderRadius: '8px'
              }}>
                <Phone size={24} style={{ color: '#27ae60', flexShrink: 0 }} />
                <div>
                  <p style={{ margin: 0, fontSize: '12px', color: '#999' }}>Số điện thoại</p>
                  <p style={{ margin: '4px 0 0 0', fontSize: '16px', fontWeight: '500' }}>
                    {userInfo.phoneNumber}
                  </p>
                </div>
              </div>
            )}

            {/* Ngày sinh */}
            {userInfo.birthday && (
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                padding: '12px',
                backgroundColor: '#f9f9f9',
                borderRadius: '8px'
              }}>
                <Calendar size={24} style={{ color: '#f39c12', flexShrink: 0 }} />
                <div>
                  <p style={{ margin: 0, fontSize: '12px', color: '#999' }}>Ngày sinh</p>
                  <p style={{ margin: '4px 0 0 0', fontSize: '16px', fontWeight: '500' }}>
                    {formatDate(userInfo.birthday)}
                  </p>
                </div>
              </div>
            )}

            {/* Nơi sinh */}
            {userInfo.birthPlace && (
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                padding: '12px',
                backgroundColor: '#f9f9f9',
                borderRadius: '8px'
              }}>
                <MapPin size={24} style={{ color: '#9b59b6', flexShrink: 0 }} />
                <div>
                  <p style={{ margin: 0, fontSize: '12px', color: '#999' }}>Nơi sinh</p>
                  <p style={{ margin: '4px 0 0 0', fontSize: '16px', fontWeight: '500' }}>
                    {userInfo.birthPlace}
                  </p>
                </div>
              </div>
            )}

            {/* Role */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '16px',
              padding: '12px',
              backgroundColor: '#f9f9f9',
              borderRadius: '8px'
            }}>
              <User size={24} style={{ color: '#34495e', flexShrink: 0 }} />
              <div>
                <p style={{ margin: 0, fontSize: '12px', color: '#999' }}>Vai trò</p>
                <p style={{ margin: '4px 0 0 0', fontSize: '16px', fontWeight: '500' }}>
                  {getRoleLabel(userInfo.role)}
                </p>
              </div>
            </div>

            {/* Close Button */}
            <button
              onClick={onClose}
              style={{
                marginTop: '24px',
                padding: '12px 24px',
                backgroundColor: '#3498db',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: '500',
                cursor: 'pointer'
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = '#2980b9'}
              onMouseLeave={(e) => e.target.style.backgroundColor = '#3498db'}
            >
              Đóng
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
