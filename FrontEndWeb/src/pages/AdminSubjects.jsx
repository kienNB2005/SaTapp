import React, { useState, useEffect, useRef } from 'react';
import {
  UploadCloud, FileSpreadsheet, CheckCircle2, AlertTriangle,
  ArrowLeft, Download, RefreshCw, BookOpen, Search, Calendar,
  Edit, Trash2, X
} from 'lucide-react';
import api from '../utils/api';

export default function AdminSubjects() {
  // --- VIEW STATE ---
  const [view, setView] = useState('list'); // 'list' | 'import'

  // --- LIST STATE ---
  const [subjects, setSubjects] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [searchText, setSearchText] = useState('');

  // --- EDIT & DELETE STATE ---
  const [editingSubject, setEditingSubject] = useState(null);
  const [editName, setEditName] = useState('');
  const [editCredits, setEditCredits] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(null);

  // --- ADD STATE ---
  const [isAdding, setIsAdding] = useState(false);
  const [addCode, setAddCode] = useState('');
  const [addName, setAddName] = useState('');
  const [addCredits, setAddCredits] = useState('');

  // --- IMPORT STATE ---
  const [importStep, setImportStep] = useState('upload'); // upload | importing | success | error
  const [progress, setProgress] = useState(0);
  const [statusText, setStatusText] = useState('');
  const [importResult, setImportResult] = useState(null);
  const [importError, setImportError] = useState('');
  const fileInputRef = useRef(null);

  // =====================
  // FETCH SUBJECT LIST
  // =====================
  const fetchSubjects = async (signal) => {
    setListLoading(true);
    setListError('');
    try {
      const res = await api.get('/api/v1/subjects', { signal });
      setSubjects(res.data.result || []);
    } catch (err) {
      if (err.name === 'AbortError' || err.name === 'CanceledError') return;
      setListError(err.response?.data?.message || 'Không thể tải danh sách môn học.');
    } finally {
      setListLoading(false);
    }
  };

  useEffect(() => {
    if (view !== 'list') return;
    const controller = new AbortController();
    fetchSubjects(controller.signal);
    return () => controller.abort();
  }, [view]);

  // =====================
  // EDIT & DELETE HANDLERS
  // =====================
  const handleEditClick = (subj) => {
    setEditingSubject(subj);
    setEditName(subj.name);
    setEditCredits(subj.credits);
  };

  const handleSaveEdit = async () => {
    if (!editName.trim() || !editCredits) {
      alert('Vui lòng nhập đầy đủ thông tin!');
      return;
    }
    setIsSaving(true);
    try {
      await api.put(`/api/v1/subjects/${editingSubject.id}`, { name: editName, credits: parseInt(editCredits) });
      setEditingSubject(null);
      fetchSubjects();
    } catch (err) {
      alert(err.response?.data?.message || 'Cập nhật thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (subj) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa môn học "${subj.name}" không?`)) return;
    setIsDeleting(subj.id);
    try {
      await api.delete(`/api/v1/subjects/${subj.id}`);
      fetchSubjects();
    } catch (err) {
      alert(err.response?.data?.message || 'Xóa thất bại');
    } finally {
      setIsDeleting(null);
    }
  };

  const handleAddClick = () => {
    setIsAdding(true);
    setAddCode('');
    setAddName('');
    setAddCredits('');
  };

  const handleSaveAdd = async () => {
    if (!addCode.trim() || !addName.trim() || !addCredits) {
      alert('Vui lòng nhập đầy đủ thông tin!');
      return;
    }
    setIsSaving(true);
    try {
      await api.post('/api/v1/subjects', { 
        code: addCode, 
        name: addName, 
        credits: parseInt(addCredits) 
      });
      setIsAdding(false);
      fetchSubjects();
    } catch (err) {
      alert(err.response?.data?.message || 'Thêm mới thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  // =====================
  // IMPORT HANDLERS
  // =====================
  const handleImportClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.name.match(/\.(xlsx|xls|csv)$/)) {
      setImportError('Vui lòng chọn file Excel (.xlsx, .xls) hoặc CSV');
      setImportStep('error');
      e.target.value = null;
      return;
    }

    setImportStep('importing');
    setProgress(10);
    setStatusText('Đang tải file lên...');

    const formData = new FormData();
    formData.append('file', file);

    try {
      setTimeout(() => { setProgress(40); setStatusText('Đang đọc dữ liệu Excel...'); }, 500);

      const res = await api.post('/api/v1/subjects/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setProgress(100);
      setStatusText('Xử lý hoàn tất');
      setImportResult(res.data.result);
      setImportStep('success');

    } catch (err) {
      setProgress(0);
      setImportError(err.response?.data?.message || 'Đã xảy ra lỗi khi import dữ liệu. Vui lòng kiểm tra lại file.');
      setImportStep('error');
    }

    e.target.value = null;
  };

  const downloadTemplate = async () => {
    try {
      const res = await api.get('/api/v1/subjects/template', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Template_Import_MonHoc.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Không thể tải file mẫu. Vui lòng thử lại sau.');
    }
  };

  const closeImport = () => {
    setView('list');
    setImportStep('upload');
    setImportResult(null);
    setImportError('');
  };

  // =====================
  // RENDER HELPERS
  // =====================
  const formatDate = (isoString) => {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleDateString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
  };

  const filteredSubjects = subjects.filter(s =>
    (s.name?.toLowerCase() || '').includes(searchText.toLowerCase()) ||
    (s.code?.toLowerCase() || '').includes(searchText.toLowerCase())
  );

  return (
    <div className="page active">
      {/* HEADER SECTION */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <div className="tb-title" style={{ fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BookOpen size={24} color="var(--bl)" />
            Quản lý Môn học
          </div>
          <div className="tb-sub">
            {view === 'list' ? `Danh sách Môn học · ${subjects.length} môn học trong hệ thống` : 'Import hàng loạt từ file Excel'}
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {view === 'list' ? (
            <>
              <button className="btn btn-s" onClick={() => fetchSubjects()} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <RefreshCw size={14} /> Làm mới
              </button>
              <button className="btn btn-p" onClick={handleAddClick} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                Thêm mới
              </button>
              <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <UploadCloud size={14} /> Import Excel
              </button>
            </>
          ) : (
            <button className="btn btn-s" onClick={closeImport} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <ArrowLeft size={14} /> Quay lại danh sách
            </button>
          )}
        </div>
      </div>

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
                    accept=".xlsx, .xls, .csv"
                    style={{ display: 'none' }}
                  />
                  <div
                    className="upz"
                    onClick={() => fileInputRef.current?.click()}
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
                <h3 style={{ fontSize: '16px', marginBottom: '10px' }}>File mẫu Import Môn học</h3>
                <p style={{ fontSize: '13px', color: 'var(--tx3)', lineHeight: '1.6', maxWidth: '300px' }}>
                  Hệ thống sẽ tự động <strong>thêm mới</strong> những Môn học chưa có và <strong>cập nhật</strong> những Môn học đã tồn tại dựa trên mã Môn học.
                </p>
                <button className="btn btn-p" onClick={downloadTemplate} style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Download size={14} /> Tải file mẫu .xlsx
                </button>
              </div>
            </div>
          )}

          {importStep !== 'upload' && (
            <div className="card" style={{ marginBottom: '20px' }}>
              <div className="card-h" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div className="card-t">Nhập dữ liệu Môn học từ Excel</div>
              <div className="card-su">Hệ thống đang xử lý file tải lên...</div>
            </div>
            <button className="btn btn-s btn-sm" onClick={closeImport}>
              <ArrowLeft size={14} style={{ marginRight: '4px' }} /> Quay lại
            </button>
          </div>

          <div style={{ padding: '30px', maxWidth: '600px', margin: '0 auto' }}>
            {/* Importing State */}
            {importStep === 'importing' && (
              <div style={{ textAlign: 'center' }}>
                <FileSpreadsheet size={48} color="var(--bl)" className="pulse" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '16px', marginBottom: '8px' }}>{statusText}</h3>
                <div className="pb" style={{ marginBottom: '12px' }}>
                  <div className="pf bl" style={{ width: `${progress}%`, transition: 'width 0.3s ease' }}></div>
                </div>
                <p style={{ color: 'var(--tx3)', fontSize: '13px' }}>Vui lòng không đóng trình duyệt trong quá trình này...</p>
              </div>
            )}

            {/* Error State */}
            {importStep === 'error' && (
              <div style={{ textAlign: 'center' }}>
                <AlertTriangle size={48} color="var(--rd)" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '16px', color: 'var(--rd)', marginBottom: '8px' }}>Import thất bại</h3>
                <p style={{ color: 'var(--tx2)', fontSize: '14px', marginBottom: '24px' }}>{importError}</p>
                <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                  <button className="btn btn-s" onClick={closeImport}>Hủy bỏ</button>
                  <button className="btn btn-p" onClick={handleImportClick}>Thử lại file khác</button>
                </div>
              </div>
            )}

            {/* Success State */}
            {importStep === 'success' && importResult && (
              <div>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                  <CheckCircle2 size={48} color="var(--gr)" style={{ marginBottom: '16px' }} />
                  <h3 style={{ fontSize: '16px', color: 'var(--gr)', marginBottom: '8px' }}>Import dữ liệu thành công!</h3>
                  <p style={{ color: 'var(--tx2)', fontSize: '14px' }}>Hệ thống đã đọc và xử lý xong file dữ liệu của bạn.</p>
                </div>

                <div style={{ background: 'var(--bg3)', borderRadius: '12px', padding: '20px', marginBottom: '24px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--bd)' }}>
                    <span style={{ color: 'var(--tx2)' }}>Tổng số dòng hợp lệ:</span>
                    <span style={{ fontWeight: '600' }}>{importResult.totalRowsProcessed || 0} dòng</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--bd)' }}>
                    <span style={{ color: 'var(--tx2)' }}>Thêm mới thành công:</span>
                    <span style={{ fontWeight: '600', color: 'var(--gr)' }}>{importResult.successCount || 0} môn học</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--tx2)' }}>Cập nhật thông tin:</span>
                    <span style={{ fontWeight: '600', color: 'var(--bl)' }}>{importResult.updatedCount || 0} môn học</span>
                  </div>
                </div>

                {importResult.errors && importResult.errors.length > 0 && (
                  <div style={{ background: 'var(--rdL)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: '12px', padding: '20px', marginBottom: '24px' }}>
                    <h4 style={{ color: 'var(--rd)', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '12px' }}>
                      <AlertTriangle size={16} /> Có {importResult.errors.length} cảnh báo/lỗi:
                    </h4>
                    <ul style={{ margin: 0, paddingLeft: '20px', color: 'var(--tx2)', fontSize: '13px' }}>
                      {importResult.errors.map((err, idx) => (
                        <li key={idx} style={{ marginBottom: '4px' }}>{err}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div style={{ textAlign: 'center' }}>
                  <button className="btn btn-p" onClick={closeImport}>Đóng và Xem danh sách</button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
      </>
      )}

      {/* ======================== */}
      {/* VIEW: LIST               */}
      {/* ======================== */}
      {view === 'list' && (
        <>
          <div className="card">
            <div className="card-h">
              <div>
                <div className="card-t">Danh sách Môn học</div>
                <div className="card-su">Tất cả các môn học hiện có trong hệ thống</div>
              </div>
              <div className="srch">
                <Search className="srch-ic" size={14} />
                <input
                  type="text"
                  placeholder="Tìm tên, mã môn..."
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                />
              </div>
            </div>

            {/* Instructions */}
            <div style={{ padding: '16px 20px', background: 'var(--bg3)', borderBottom: '1px solid var(--bd)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'var(--blL)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <FileSpreadsheet size={16} color="var(--bl)" />
                </div>
                <div>
                  <div style={{ fontSize: '13px', fontWeight: '600' }}>Hướng dẫn nhập liệu</div>
                  <div style={{ fontSize: '12px', color: 'var(--tx3)' }}>Tải file mẫu Excel về, điền dữ liệu theo đúng cột và Upload lên hệ thống.</div>
                </div>
              </div>
              <button className="btn btn-s btn-sm" onClick={downloadTemplate} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Download size={14} /> Tải file mẫu
              </button>
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
                <button className="btn btn-p" onClick={() => fetchSubjects()}>Thử lại</button>
              </div>
            )}

            {/* Empty */}
            {!listLoading && !listError && filteredSubjects.length === 0 && (
              <div style={{ padding: '60px', textAlign: 'center' }}>
                <BookOpen size={48} color="var(--tx3)" style={{ marginBottom: '12px', opacity: 0.4 }} />
                <p style={{ color: 'var(--tx3)', fontSize: '14px', marginBottom: '16px' }}>
                  {searchText
                    ? `Không tìm thấy môn học nào với từ khóa "${searchText}"`
                    : 'Chưa có dữ liệu Môn học. Hãy Import file Excel để bắt đầu.'}
                </p>
                {!searchText && (
                  <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                    <UploadCloud size={14} /> Import Excel ngay
                  </button>
                )}
              </div>
            )}

            {/* Table */}
            {!listLoading && !listError && filteredSubjects.length > 0 && (
              <div style={{ overflowX: 'auto' }}>
                <table className="tbl">
                  <thead>
                    <tr>
                      <th style={{ width: '50px' }}>#</th>
                      <th style={{ width: '120px' }}>Mã Môn</th>
                      <th>Tên Môn học</th>
                      <th style={{ width: '100px', textAlign: 'center' }}>Số TC</th>
                      <th style={{ width: '140px' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Calendar size={12} /> Ngày tạo
                        </span>
                      </th>
                      <th style={{ width: '140px' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Calendar size={12} /> Cập nhật lúc
                        </span>
                      </th>
                      <th style={{ width: '100px', textAlign: 'center' }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredSubjects.map((subj, index) => (
                      <tr key={subj.id}>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{index + 1}</td>
                        <td>
                          <span style={{
                            background: 'var(--blL)', color: 'var(--bl)',
                            padding: '3px 10px', borderRadius: '6px',
                            fontSize: '12px', fontWeight: '600', letterSpacing: '0.5px'
                          }}>
                            {subj.code || '—'}
                          </span>
                        </td>
                        <td style={{ fontWeight: '500' }}>{subj.name}</td>
                        <td style={{ textAlign: 'center', fontWeight: '600', color: 'var(--tx2)' }}>
                          {subj.credits}
                        </td>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{formatDate(subj.createdAt)}</td>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{formatDate(subj.updatedAt)}</td>
                        <td style={{ textAlign: 'center' }}>
                          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px' }}
                              title="Sửa"
                              onClick={() => handleEditClick(subj)}
                            >
                              <Edit size={14} color="var(--bl)" />
                            </button>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px', borderColor: 'rgba(239,68,68,0.3)' }}
                              title="Xóa"
                              onClick={() => handleDelete(subj)}
                              disabled={isDeleting === subj.id}
                            >
                              {isDeleting === subj.id ? (
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
            )}
            
            {!listLoading && !listError && filteredSubjects.length > 0 && (
              <div style={{ padding: '12px 16px', borderTop: '1px solid var(--bd)', fontSize: '12px', color: 'var(--tx3)' }}>
                Hiển thị {filteredSubjects.length}/{subjects.length} môn học
              </div>
            )}
          </div>
        </>
      )}

      {/* ======================== */}
      {/* EDIT MODAL               */}
      {/* ======================== */}
      {editingSubject && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '24px', margin: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Cập nhật Môn học</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setEditingSubject(null)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            {/* Read-only Code */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Môn học (Chỉ đọc)</label>
              <div style={{ padding: '10px 14px', background: 'var(--bg3)', borderRadius: '8px', fontSize: '14px', color: 'var(--tx3)', fontWeight: '500' }}>
                {editingSubject.code}
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Môn học</label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                placeholder="Nhập tên môn học..."
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)', background: 'var(--bg2)', color: 'var(--tx)' }}
                autoFocus
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Số Tín chỉ</label>
              <input
                type="number"
                min="1"
                max="10"
                value={editCredits}
                onChange={(e) => setEditCredits(e.target.value)}
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setEditingSubject(null)} disabled={isSaving}>Hủy</button>
              <button className="btn btn-p" onClick={handleSaveEdit} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <CheckCircle2 size={14} />} Lưu thay đổi
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
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm Môn học mới</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setIsAdding(false)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Môn học</label>
              <input
                type="text"
                value={addCode}
                onChange={(e) => setAddCode(e.target.value)}
                placeholder="Ví dụ: IT1110"
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg3)', color: 'var(--tx)' }}
                autoFocus
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Môn học</label>
              <input
                type="text"
                value={addName}
                onChange={(e) => setAddName(e.target.value)}
                placeholder="Nhập tên môn học..."
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)', background: 'var(--bg2)', color: 'var(--tx)' }}
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Số Tín chỉ</label>
              <input
                type="number"
                min="1"
                max="10"
                value={addCredits}
                onChange={(e) => setAddCredits(e.target.value)}
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setIsAdding(false)} disabled={isSaving}>Hủy</button>
              <button className="btn btn-p" onClick={handleSaveAdd} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <CheckCircle2 size={14} />} Thêm mới
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
