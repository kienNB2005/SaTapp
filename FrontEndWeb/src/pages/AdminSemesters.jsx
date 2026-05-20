import React, { useState, useEffect } from 'react';
import {
  Calendar, CheckCircle2, AlertTriangle, RefreshCw, Search,
  Edit, Trash2, X, Plus
} from 'lucide-react';
import api from '../utils/api';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { vi } from 'date-fns/locale';

export default function AdminSemesters() {
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchText, setSearchText] = useState('');

  const [showCreate, setShowCreate] = useState(false);
  const [createData, setCreateData] = useState({ name: '', startDate: null, endDate: null });
  
  const [editingSemester, setEditingSemester] = useState(null);
  const [editData, setEditData] = useState({ name: '', isActive: false });

  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(null);

  const fetchSemesters = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/admin/semesters');
      setSemesters(res.data.result || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể tải danh sách học kỳ.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSemesters();
  }, []);

  const formatDateForAPI = (dateObj) => {
    if (!dateObj) return '';
    const y = dateObj.getFullYear();
    const m = String(dateObj.getMonth() + 1).padStart(2, '0');
    const d = String(dateObj.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  };

  const handleCreate = async () => {
    if (!createData.name.trim() || !createData.startDate || !createData.endDate) {
      alert('Vui lòng nhập đầy đủ Tên, Ngày bắt đầu và Ngày kết thúc!');
      return;
    }
    if (createData.startDate >= createData.endDate) {
      alert('Ngày bắt đầu phải trước Ngày kết thúc!');
      return;
    }
    
    const payload = {
      name: createData.name,
      startDate: formatDateForAPI(createData.startDate),
      endDate: formatDateForAPI(createData.endDate)
    };

    setIsSaving(true);
    try {
      await api.post('/admin/semesters', payload);
      setShowCreate(false);
      setCreateData({ name: '', startDate: null, endDate: null });
      fetchSemesters();
    } catch (err) {
      alert(err.response?.data?.message || 'Tạo học kỳ thất bại!');
    } finally {
      setIsSaving(false);
    }
  };

  const handleEditClick = (sem) => {
    setEditingSemester(sem);
    setEditData({ name: sem.name, isActive: sem.isActive || false });
  };

  const handleSaveEdit = async () => {
    if (!editData.name.trim()) {
      alert('Tên học kỳ không được để trống!');
      return;
    }
    setIsSaving(true);
    try {
      await api.put(`/admin/semesters/${editingSemester.id}`, editData);
      setEditingSemester(null);
      fetchSemesters();
    } catch (err) {
      alert(err.response?.data?.message || 'Cập nhật thất bại!');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (sem) => {
    if (!window.confirm(`Bạn có chắc muốn xóa học kỳ "${sem.name}" không?`)) return;
    setIsDeleting(sem.id);
    try {
      await api.delete(`/admin/semesters/${sem.id}`);
      fetchSemesters();
    } catch (err) {
      alert(err.response?.data?.message || 'Xóa thất bại!');
    } finally {
      setIsDeleting(null);
    }
  };

  const formatDateStr = (dateStr) => {
    if (!dateStr) return '—';
    const parts = dateStr.split('-');
    if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
    return dateStr;
  };

  const formatDateTime = (isoString) => {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleDateString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
  };

  const filteredSemesters = semesters.filter(s => 
    s.name?.toLowerCase().includes(searchText.toLowerCase())
  );

  return (
    <div className="page active">
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <div className="tb-title" style={{ fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Calendar size={24} color="var(--bl)" />
            Quản lý Học kỳ
          </div>
          <div className="tb-sub">
            Danh sách Học kỳ · {semesters.length} học kỳ trong hệ thống
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn btn-s" onClick={() => fetchSemesters()} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <RefreshCw size={14} /> Làm mới
          </button>
          <button className="btn btn-p" onClick={() => setShowCreate(true)} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Plus size={14} /> Thêm Học kỳ
          </button>
        </div>
      </div>

      {/* LIST VIEW */}
      <div className="card">
        <div className="card-h">
          <div>
            <div className="card-t">Danh sách Học kỳ</div>
            <div className="card-su">Quản lý các học kỳ giảng dạy</div>
          </div>
          <div className="srch">
            <Search className="srch-ic" size={14} />
            <input
              type="text"
              placeholder="Tìm tên học kỳ..."
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </div>
        </div>

        {/* Loading */}
        {loading && (
          <div style={{ padding: '60px', textAlign: 'center' }}>
            <RefreshCw size={32} color="var(--bl)" className="pulse" style={{ marginBottom: '12px' }} />
            <p style={{ color: 'var(--tx3)', fontSize: '14px' }}>Đang tải dữ liệu...</p>
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div style={{ padding: '40px', textAlign: 'center' }}>
            <AlertTriangle size={32} color="var(--rd)" style={{ marginBottom: '12px' }} />
            <p style={{ color: 'var(--rd)', fontSize: '14px', marginBottom: '16px' }}>{error}</p>
            <button className="btn btn-p" onClick={() => fetchSemesters()}>Thử lại</button>
          </div>
        )}

        {/* Empty */}
        {!loading && !error && filteredSemesters.length === 0 && (
          <div style={{ padding: '60px', textAlign: 'center' }}>
            <Calendar size={48} color="var(--tx3)" style={{ marginBottom: '12px', opacity: 0.4 }} />
            <p style={{ color: 'var(--tx3)', fontSize: '14px', marginBottom: '16px' }}>
              {searchText ? `Không tìm thấy học kỳ nào với từ khóa "${searchText}"` : 'Chưa có dữ liệu Học kỳ.'}
            </p>
            {!searchText && (
              <button className="btn btn-p" onClick={() => setShowCreate(true)} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                <Plus size={14} /> Thêm Học kỳ ngay
              </button>
            )}
          </div>
        )}

        {/* Table */}
        {!loading && !error && filteredSemesters.length > 0 && (
          <>
            <div style={{ overflowX: 'auto' }}>
              <table className="tbl">
                <thead>
                  <tr>
                    <th style={{ width: '50px' }}>#</th>
                    <th>Tên Học Kỳ</th>
                    <th style={{ width: '130px' }}>Ngày bắt đầu</th>
                    <th style={{ width: '130px' }}>Ngày kết thúc</th>
                    <th style={{ width: '130px' }}>Trạng thái</th>
                    <th style={{ width: '130px' }}>Ngày tạo</th>
                    <th style={{ width: '100px', textAlign: 'center' }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSemesters.map((sem, index) => (
                    <tr key={sem.id}>
                      <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{index + 1}</td>
                      <td style={{ fontWeight: '500' }}>{sem.name}</td>
                      <td style={{ color: 'var(--tx2)', fontSize: '13px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Calendar size={14} color="var(--bl)" style={{ opacity: 0.6 }} />
                          {formatDateStr(sem.startDate)}
                        </div>
                      </td>
                      <td style={{ color: 'var(--tx2)', fontSize: '13px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Calendar size={14} color="var(--bl)" style={{ opacity: 0.6 }} />
                          {formatDateStr(sem.endDate)}
                        </div>
                      </td>
                      <td>
                        {sem.isActive ? (
                          <span className="bdg b-op" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                            Hoạt động
                          </span>
                        ) : (
                          <span className="bdg b-ca" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                            Vô hiệu
                          </span>
                        )}
                      </td>
                      <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{formatDateTime(sem.createdAt)}</td>
                      <td style={{ textAlign: 'center' }}>
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                          <button
                            className="btn btn-s"
                            style={{ padding: '6px', borderRadius: '6px' }}
                            title="Sửa"
                            onClick={() => handleEditClick(sem)}
                          >
                            <Edit size={14} color="var(--bl)" />
                          </button>
                          <button
                            className="btn btn-s"
                            style={{ padding: '6px', borderRadius: '6px', borderColor: 'rgba(239,68,68,0.3)' }}
                            title="Xóa"
                            onClick={() => handleDelete(sem)}
                            disabled={isDeleting === sem.id}
                          >
                            {isDeleting === sem.id ? (
                              <RefreshCw size={14} color="var(--rd)" className="pulse" />
                            ) : (
                              <Trash2 size={14} color="var(--rd)" />
                            )}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div style={{ padding: '12px 16px', borderTop: '1px solid var(--bd)', fontSize: '12px', color: 'var(--tx3)' }}>
              Hiển thị {filteredSemesters.length}/{semesters.length} học kỳ
            </div>
          </>
        )}
      </div>

      {/* ======================== */}
      {/* CREATE MODAL             */}
      {/* ======================== */}
      {showCreate && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '24px', margin: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm Học kỳ mới</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setShowCreate(false)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Học kỳ</label>
              <input
                type="text"
                value={createData.name}
                onChange={(e) => setCreateData({ ...createData, name: e.target.value })}
                placeholder="Ví dụ: Học kỳ 1 (2024-2025)"
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)' }}
                autoFocus
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px', fontWeight: '500' }}>Ngày bắt đầu</label>
              <div style={{ position: 'relative' }} className="dp-wrapper">
                <DatePicker
                  selected={createData.startDate}
                  onChange={(date) => setCreateData({ ...createData, startDate: date })}
                  locale={vi}
                  dateFormat="dd/MM/yyyy"
                  placeholderText="dd/mm/yyyy"
                  className="fi"
                  autoComplete="off"
                  portalId="root-portal"
                />
              </div>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px', fontWeight: '500' }}>Ngày kết thúc</label>
              <div style={{ position: 'relative' }} className="dp-wrapper">
                <DatePicker
                  selected={createData.endDate}
                  onChange={(date) => setCreateData({ ...createData, endDate: date })}
                  locale={vi}
                  dateFormat="dd/MM/yyyy"
                  placeholderText="dd/mm/yyyy"
                  className="fi"
                  autoComplete="off"
                  minDate={createData.startDate}
                  portalId="root-portal"
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setShowCreate(false)} disabled={isSaving}>Hủy</button>
              <button className="btn btn-p" onClick={handleCreate} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <Plus size={14} />} Tạo mới
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================== */}
      {/* EDIT MODAL               */}
      {/* ======================== */}
      {editingSemester && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '24px', margin: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Cập nhật Học kỳ</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setEditingSemester(null)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Học kỳ</label>
              <input
                type="text"
                value={editData.name}
                onChange={(e) => setEditData({ ...editData, name: e.target.value })}
                placeholder="Tên học kỳ..."
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)' }}
                autoFocus
              />
            </div>

            {/* Read-only dates to provide context */}
            <div style={{ display: 'flex', gap: '14px', marginBottom: '20px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '12px', color: 'var(--tx3)', marginBottom: '6px', fontWeight: '500' }}>Ngày bắt đầu</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 12px', background: 'var(--bg)', border: '1px dashed var(--bd2)', borderRadius: '8px', fontSize: '13px', color: 'var(--tx2)' }}>
                  <Calendar size={16} color="var(--bl)" style={{ opacity: 0.7 }} />
                  {formatDateStr(editingSemester.startDate)}
                </div>
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '12px', color: 'var(--tx3)', marginBottom: '6px', fontWeight: '500' }}>Ngày kết thúc</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 12px', background: 'var(--bg)', border: '1px dashed var(--bd2)', borderRadius: '8px', fontSize: '13px', color: 'var(--tx2)' }}>
                  <Calendar size={16} color="var(--bl)" style={{ opacity: 0.7 }} />
                  {formatDateStr(editingSemester.endDate)}
                </div>
              </div>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={editData.isActive}
                  onChange={(e) => setEditData({ ...editData, isActive: e.target.checked })}
                  style={{ width: '16px', height: '16px', accentColor: 'var(--bl)' }}
                />
                <span style={{ fontSize: '14px', fontWeight: '500' }}>Trạng thái hoạt động</span>
              </label>
              <p style={{ fontSize: '12px', color: 'var(--tx3)', marginTop: '6px', marginLeft: '24px' }}>
                Đánh dấu để học kỳ này có thể được sử dụng trong hệ thống. (Chỉ sửa được tên và trạng thái)
              </p>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setEditingSemester(null)} disabled={isSaving}>Hủy</button>
              <button className="btn btn-p" onClick={handleSaveEdit} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <CheckCircle2 size={14} />} Lưu thay đổi
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
