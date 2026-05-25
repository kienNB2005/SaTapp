import { useState, useEffect, useRef } from 'react';
import {
  UploadCloud, FileSpreadsheet, CheckCircle2, AlertTriangle,
  ArrowLeft, Download, RefreshCw, Building, Search, Calendar,
  Edit, Trash2, X
} from 'lucide-react';
import api from '../utils/api';

export default function AdminFaculties() {
  // --- VIEW STATE ---
  const [view, setView] = useState('list'); // 'list' | 'import'

  // --- LIST STATE ---
  const [faculties, setFaculties] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [searchText, setSearchText] = useState('');

  // --- EDIT & DELETE STATE ---
  const [editingFaculty, setEditingFaculty] = useState(null);
  const [editName, setEditName] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(null);

  // --- ADD STATE ---
  const [isAdding, setIsAdding] = useState(false);
  const [addCode, setAddCode] = useState('');
  const [addName, setAddName] = useState('');

  // --- IMPORT STATE ---
  const [importStep, setImportStep] = useState('upload'); // upload | importing | success | error
  const [progress, setProgress] = useState(0);
  const [statusText, setStatusText] = useState('');
  const [importResult, setImportResult] = useState(null);
  const [importError, setImportError] = useState('');
  const fileInputRef = useRef(null);

  // =====================
  // FETCH FACULTY LIST
  // =====================
  const fetchFaculties = async (signal) => {
    setListLoading(true);
    setListError('');
    try {
      const res = await api.get('/api/v1/faculties', { signal });
      setFaculties(res.data.result || []);
    } catch (err) {
      if (err.name === 'AbortError' || err.name === 'CanceledError') return; // bị hủy, bỏ qua
      setListError(err.response?.data?.message || 'Không thể tải danh sách khoa. Kiểm tra kết nối máy chủ.');
    } finally {
      setListLoading(false);
    }
  };

  useEffect(() => {
    if (view !== 'list') return;
    const controller = new AbortController();
    fetchFaculties(controller.signal);
    return () => controller.abort(); // cleanup: hủy request nếu component unmount
  }, [view]);

  // =====================
  // IMPORT HANDLERS
  // =====================
  const downloadTemplate = async () => {
    try {
      const res = await api.get('/api/v1/faculties/template', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Template_Import_Khoa.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Không thể tải file mẫu. Vui lòng thử lại sau.');
    }
  };

  const handleFileChange = async (event) => {
    const file = event.target.files[0];
    if (!file) return;
    // Reset input để cho phép chọn lại cùng 1 file
    event.target.value = '';

    if (!file.name.endsWith('.xlsx')) {
      setImportError('Định dạng file không hợp lệ. Vui lòng chọn file .xlsx!');
      setImportStep('error');
      return;
    }

    setImportStep('importing');
    setProgress(20);
    setStatusText('Đang tải file lên máy chủ...');

    const formData = new FormData();
    formData.append('file', file);

    const progressInterval = setInterval(() => {
      setProgress(prev => (prev >= 85 ? prev : prev + 10));
    }, 350);

    try {
      const response = await api.post('/api/v1/faculties/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      clearInterval(progressInterval);
      setProgress(100);
      setStatusText('Hoàn thành!');

      setTimeout(() => {
        if (response.data?.result) {
          setImportResult(response.data.result);
          setImportStep('success');
        } else {
          setImportError('Phản hồi từ máy chủ không hợp lệ.');
          setImportStep('error');
        }
      }, 500);
    } catch (err) {
      clearInterval(progressInterval);
      setProgress(0);
      setTimeout(() => {
        setImportError(err.response?.data?.message || 'Có lỗi xảy ra khi kết nối đến máy chủ.');
        setImportStep('error');
      }, 300);
    }
  };

  const resetImport = () => {
    setImportStep('upload');
    setImportResult(null);
    setImportError('');
    setProgress(0);
  };

  const goBackToList = () => {
    resetImport();
    setView('list');
  };

  // =====================
  // EDIT & DELETE HANDLERS
  // =====================
  const handleEditClick = (faculty) => {
    setEditingFaculty(faculty);
    setEditName(faculty.name);
  };

  const handleSaveEdit = async () => {
    if (!editName.trim()) {
      alert('Tên khoa không được để trống!');
      return;
    }
    setIsSaving(true);
    try {
      await api.put(`/api/v1/faculties/${editingFaculty.id}`, { name: editName });
      setEditingFaculty(null);
      fetchFaculties(); // Refresh list
    } catch (err) {
      alert(err.response?.data?.message || 'Cập nhật thất bại. Vui lòng thử lại.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteClick = async (faculty) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa khoa "${faculty.name}" không?`)) {
      return;
    }
    setIsDeleting(faculty.id);
    try {
      await api.delete(`/api/v1/faculties/${faculty.id}`);
      fetchFaculties(); // Refresh list
    } catch (err) {
      alert(err.response?.data?.message || 'Xóa thất bại. Vui lòng thử lại.');
    } finally {
      setIsDeleting(null);
    }
  };

  const handleAdd = async () => {
    if (!addCode.trim() || !addName.trim()) {
      alert('Mã khoa và tên khoa không được để trống!');
      return;
    }
    setIsSaving(true);
    try {
      await api.post('/api/v1/faculties', { code: addCode, name: addName });
      setIsAdding(false);
      setAddCode('');
      setAddName('');
      fetchFaculties(); // Refresh list
    } catch (err) {
      alert(err.response?.data?.message || 'Thêm mới thất bại. Vui lòng thử lại.');
    } finally {
      setIsSaving(false);
    }
  };

  // =====================
  // HELPERS
  // =====================
  const formatDate = (isoString) => {
    if (!isoString) return '—';
    const d = new Date(isoString);
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const filteredFaculties = faculties.filter(f =>
    f.name?.toLowerCase().includes(searchText.toLowerCase()) ||
    f.code?.toLowerCase().includes(searchText.toLowerCase())
  );

  // =====================
  // RENDER
  // =====================
  return (
    <div className="page active">
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <div className="tb-title" style={{ fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Building size={24} color="var(--bl)" />
            Quản lý Khoa
          </div>
          <div className="tb-sub">
            {view === 'list' ? `Danh sách Khoa · ${faculties.length} khoa trong hệ thống` : 'Import hàng loạt từ file Excel'}
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {view === 'list' ? (
            <>
              <button className="btn btn-s" onClick={() => fetchFaculties()} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <RefreshCw size={14} /> Làm mới
              </button>
              <button className="btn btn-p" onClick={() => setIsAdding(true)} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                Thêm mới
              </button>
              <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <UploadCloud size={14} /> Import Excel
              </button>
            </>
          ) : (
            <button className="btn btn-s" onClick={goBackToList} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <ArrowLeft size={14} /> Quay lại danh sách
            </button>
          )}
        </div>
      </div>

      {/* ======================== */}
      {/* VIEW: LIST               */}
      {/* ======================== */}
      {view === 'list' && (
        <div className="card">
          <div className="card-h">
            <div>
              <div className="card-t">Danh sách Khoa</div>
              <div className="card-su">Tất cả các khoa hiện có trong hệ thống</div>
            </div>
            <div className="srch">
              <Search className="srch-ic" size={14} />
              <input
                type="text"
                placeholder="Tìm tên hoặc mã khoa..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
              />
            </div>
          </div>

          {/* Loading */}
          {listLoading && (
            <div style={{ padding: '60px', textAlign: 'center' }}>
              <RefreshCw size={32} color="var(--bl)" className="pulse" style={{ marginBottom: '12px' }} />
              <p style={{ color: 'var(--tx3)', fontSize: '14px' }}>Đang tải dữ liệu...</p>
            </div>
          )}

          {/* Error */}
          {!listLoading && listError && (
            <div style={{ padding: '40px', textAlign: 'center' }}>
              <AlertTriangle size={32} color="var(--rd)" style={{ marginBottom: '12px' }} />
              <p style={{ color: 'var(--rd)', fontSize: '14px', marginBottom: '16px' }}>{listError}</p>
              <button className="btn btn-p" onClick={() => fetchFaculties()}>Thử lại</button>
            </div>
          )}

          {/* Empty */}
          {!listLoading && !listError && filteredFaculties.length === 0 && (
            <div style={{ padding: '60px', textAlign: 'center' }}>
              <Building size={48} color="var(--tx3)" style={{ marginBottom: '12px', opacity: 0.4 }} />
              <p style={{ color: 'var(--tx3)', fontSize: '14px', marginBottom: '16px' }}>
                {searchText ? `Không tìm thấy khoa nào với từ khóa "${searchText}"` : 'Chưa có dữ liệu Khoa. Hãy Import file Excel để bắt đầu.'}
              </p>
              {!searchText && (
                <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <UploadCloud size={14} /> Import ngay
                </button>
              )}
            </div>
          )}

          {/* Table */}
          {!listLoading && !listError && filteredFaculties.length > 0 && (
            <>
              <div style={{ overflowX: 'auto' }}>
                <table className="tbl">
                  <thead>
                    <tr>
                      <th style={{ width: '50px' }}>#</th>
                      <th style={{ width: '120px' }}>Mã Khoa</th>
                      <th>Tên Khoa</th>
                      <th style={{ width: '150px' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Calendar size={12} /> Ngày tạo
                        </span>
                      </th>
                      <th style={{ width: '150px' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Calendar size={12} /> Cập nhật lúc
                        </span>
                      </th>
                      <th style={{ width: '100px', textAlign: 'center' }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredFaculties.map((faculty, index) => (
                      <tr key={faculty.id}>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{index + 1}</td>
                        <td>
                          <span style={{
                            background: 'var(--blL)', color: 'var(--bl)',
                            padding: '3px 10px', borderRadius: '6px',
                            fontSize: '12px', fontWeight: '600', letterSpacing: '0.5px'
                          }}>
                            {faculty.code || '—'}
                          </span>
                        </td>
                        <td style={{ fontWeight: '500' }}>{faculty.name}</td>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{formatDate(faculty.createdAt)}</td>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{formatDate(faculty.updatedAt)}</td>
                        <td style={{ textAlign: 'center' }}>
                          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px' }}
                              title="Sửa"
                              onClick={() => handleEditClick(faculty)}
                            >
                              <Edit size={14} color="var(--bl)" />
                            </button>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px', borderColor: 'rgba(239,68,68,0.3)' }}
                              title="Xóa"
                              onClick={() => handleDeleteClick(faculty)}
                              disabled={isDeleting === faculty.id}
                            >
                              {isDeleting === faculty.id ? (
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
                Hiển thị {filteredFaculties.length}/{faculties.length} khoa
              </div>
            </>
          )}
        </div>
      )}

      {/* ======================== */}
      {/* VIEW: IMPORT             */}
      {/* ======================== */}
      {view === 'import' && (
        <>
          {/* UPLOAD */}
          {importStep === 'upload' && (
            <div className="g2">
              <div className="card">
                <div className="card-h">
                  <div className="card-t">Tải lên file Excel</div>
                </div>
                <div style={{ padding: '30px' }}>
                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleFileChange}
                    accept=".xlsx"
                    style={{ display: 'none' }}
                  />
                  <div
                    className="upz"
                    onClick={() => fileInputRef.current.click()}
                    style={{ padding: '50px 30px', cursor: 'pointer' }}
                  >
                    <FileSpreadsheet size={48} color="var(--bl)" style={{ marginBottom: '16px' }} />
                    <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '8px' }}>Nhấn để chọn file Excel</div>
                    <div style={{ fontSize: '13px', color: 'var(--tx3)', marginBottom: '24px' }}>Vui lòng sử dụng đúng file mẫu từ hệ thống</div>
                    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '11px', background: 'var(--bg3)', padding: '6px 12px', borderRadius: '20px', color: 'var(--tx2)' }}>
                      <CheckCircle2 size={12} color="var(--gr)" /> Chỉ hỗ trợ file .xlsx
                    </div>
                  </div>
                </div>
              </div>

              <div className="card" style={{ background: 'linear-gradient(135deg, var(--bg3), var(--bg2))', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '30px', textAlign: 'center' }}>
                <div style={{ width: '64px', height: '64px', borderRadius: '50%', background: 'linear-gradient(135deg, var(--blL), var(--puL))', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px', border: '1px solid var(--bd)' }}>
                  <Download size={32} color="var(--bl)" />
                </div>
                <h3 style={{ fontSize: '16px', marginBottom: '10px' }}>File mẫu Import Khoa</h3>
                <p style={{ fontSize: '13px', color: 'var(--tx3)', lineHeight: '1.6', maxWidth: '300px' }}>
                  Hệ thống sẽ tự động <strong>thêm mới</strong> những Khoa chưa có và <strong>cập nhật</strong> những Khoa đã tồn tại dựa trên mã Khoa.
                </p>
                <button className="btn btn-p" onClick={downloadTemplate} style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Download size={14} /> Tải file mẫu .xlsx
                </button>
              </div>
            </div>
          )}

          {/* IMPORTING PROGRESS */}
          {importStep === 'importing' && (
            <div className="card" style={{ maxWidth: '500px', margin: '40px auto', padding: '40px 30px', textAlign: 'center' }}>
              <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'var(--blL)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', border: '1px solid var(--bl)' }}>
                <RefreshCw size={36} color="var(--bl)" className="pulse" />
              </div>
              <h3 style={{ fontSize: '18px', marginBottom: '8px' }}>Đang xử lý & Import dữ liệu</h3>
              <p style={{ fontSize: '13px', color: 'var(--tx3)', marginBottom: '30px', minHeight: '20px' }}>{statusText}</p>
              <div style={{ background: 'var(--bg3)', height: '8px', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{ height: '100%', background: 'linear-gradient(90deg, var(--bl2), var(--bl))', width: `${progress}%`, transition: 'width 0.3s ease-out' }} />
              </div>
              <div style={{ fontSize: '12px', color: 'var(--tx2)', marginTop: '12px', fontWeight: '600' }}>{Math.round(progress)}%</div>
            </div>
          )}

          {/* SUCCESS */}
          {importStep === 'success' && importResult && (
            <div className="card" style={{ margin: '0 auto', padding: '40px 30px', textAlign: 'center' }}>
              <div style={{ width: '90px', height: '90px', borderRadius: '50%', background: importResult.errorCount === 0 ? 'var(--grL)' : 'var(--amL)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', border: `1px solid ${importResult.errorCount === 0 ? 'var(--gr)' : 'var(--am)'}` }}>
                {importResult.errorCount === 0
                  ? <CheckCircle2 size={48} color="var(--gr)" />
                  : <AlertTriangle size={48} color="var(--am)" />}
              </div>
              <h2 style={{ fontSize: '22px', marginBottom: '8px' }}>Kết quả Import Khoa</h2>
              <p style={{ fontSize: '14px', color: 'var(--tx2)', marginBottom: '30px' }}>
                Đã đọc tổng cộng <strong>{importResult.totalRows}</strong> dòng từ file.
              </p>

              <div className="sg" style={{ marginBottom: '30px' }}>
                <div className="sc gr">
                  <div className="sc-lb">Thêm mới</div>
                  <div className="sc-vl gr">{importResult.successCount}</div>
                  <div className="sc-su">Khoa được tạo mới hoàn toàn</div>
                </div>
                <div className="sc bl">
                  <div className="sc-lb">Cập nhật</div>
                  <div className="sc-vl bl">{importResult.updateCount}</div>
                  <div className="sc-su">Khoa đã có và được làm mới</div>
                </div>
                <div className="sc rd">
                  <div className="sc-lb">Lỗi</div>
                  <div className="sc-vl" style={{ color: 'var(--rd)' }}>{importResult.errorCount}</div>
                  <div className="sc-su">Dòng bị bỏ qua</div>
                </div>
              </div>

              {importResult.errors && importResult.errors.length > 0 && (
                <div style={{ background: 'rgba(239,68,68,0.05)', padding: '20px', borderRadius: '12px', border: '1px solid rgba(239,68,68,0.2)', textAlign: 'left', marginBottom: '24px' }}>
                  <div style={{ fontSize: '14px', fontWeight: '600', color: 'var(--rd)', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <AlertTriangle size={16} /> Chi tiết các dòng lỗi
                  </div>
                  <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '13px', color: 'var(--tx)', lineHeight: '1.8' }}>
                    {importResult.errors.map((err, idx) => (
                      <li key={idx}>{err}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
                <button className="btn btn-s" onClick={resetImport} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <UploadCloud size={14} /> Import thêm file
                </button>
                <button className="btn btn-p" onClick={goBackToList} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Building size={14} /> Xem danh sách Khoa
                </button>
              </div>
            </div>
          )}

          {/* ERROR */}
          {importStep === 'error' && (
            <div className="card" style={{ maxWidth: '500px', margin: '40px auto', padding: '40px 30px', textAlign: 'center' }}>
              <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'rgba(239,68,68,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', border: '1px solid var(--rd)' }}>
                <AlertTriangle size={36} color="var(--rd)" />
              </div>
              <h3 style={{ fontSize: '18px', marginBottom: '8px', color: 'var(--rd)' }}>Import Thất Bại</h3>
              <p style={{ fontSize: '14px', color: 'var(--tx)', marginBottom: '30px' }}>{importError}</p>
              <button className="btn btn-p" onClick={resetImport}>Thử lại</button>
            </div>
          )}
        </>
      )}

      {/* ======================== */}
      {/* EDIT MODAL               */}
      {/* ======================== */}
      {editingFaculty && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '24px', margin: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Cập nhật Khoa</h3>
              <button
                className="btn btn-s"
                style={{ padding: '4px', border: 'none' }}
                onClick={() => setEditingFaculty(null)}
              >
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Khoa</label>
              <input
                type="text"
                value={editingFaculty.code || ''}
                readOnly
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg3)', color: 'var(--tx3)' }}
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Khoa</label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                placeholder="Nhập tên khoa..."
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)' }}
                autoFocus
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setEditingFaculty(null)} disabled={isSaving}>
                Hủy
              </button>
              <button className="btn btn-p" onClick={handleSaveEdit} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <CheckCircle2 size={14} />}
                Lưu thay đổi
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================== */}
      {/* ADD MODAL               */}
      {/* ======================== */}
      {isAdding && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '24px', margin: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm Khoa mới</h3>
              <button
                className="btn btn-s"
                style={{ padding: '4px', border: 'none' }}
                onClick={() => setIsAdding(false)}
              >
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Khoa</label>
              <input
                type="text"
                value={addCode}
                onChange={(e) => setAddCode(e.target.value)}
                placeholder="Nhập mã khoa..."
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)' }}
                autoFocus
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Khoa</label>
              <input
                type="text"
                value={addName}
                onChange={(e) => setAddName(e.target.value)}
                placeholder="Nhập tên khoa..."
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)' }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setIsAdding(false)} disabled={isSaving}>
                Hủy
              </button>
              <button className="btn btn-p" onClick={handleAdd} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <CheckCircle2 size={14} />}
                Thêm mới
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
