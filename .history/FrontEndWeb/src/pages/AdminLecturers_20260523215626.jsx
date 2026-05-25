import React, { useState, useEffect, useRef } from 'react';
import {
  UploadCloud, FileSpreadsheet, CheckCircle2, AlertTriangle,
  ArrowLeft, Download, RefreshCw, Search, Users, UserPlus, Filter, ChevronLeft, ChevronRight, X
} from 'lucide-react';
import api from '../utils/api';

export default function AdminLecturers() {
  // --- VIEW STATE ---
  const [view, setView] = useState('list'); // 'list' | 'import'

  // --- LIST STATE ---
  const [lecturers, setLecturers] = useState([]);
  const [faculties, setFaculties] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');

  // Pagination & Filters
  const [searchText, setSearchText] = useState('');
  const [selectedDepartmentId, setSelectedDepartmentId] = useState('');
  const [selectedStatus, setSelectedStatus] = useState(''); // '' = all, 'true' = active, 'false' = inactive
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const size = 10;

  // --- ADD STATE ---
  const [isAddingLecturer, setIsAddingLecturer] = useState(false);
  const [addForm, setAddForm] = useState({ fullName: '', email: '', lecturerCode: '', facultyId: '', phoneNumber: '', gender: '', birthday: '', birthPlace: '' });
  const [addLoading, setAddLoading] = useState(false);
  const [addError, setAddError] = useState('');

  // --- EDIT STATE ---
  const [editingLecturer, setEditingLecturer] = useState(null);
  const [editForm, setEditForm] = useState({ fullName: '', email: '', isActive: true, facultyId: '', phoneNumber: '', gender: '', birthday: '', birthPlace: '' });
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState('');

  // --- IMPORT STATE ---
  const [importStep, setImportStep] = useState('upload');
  const [progress, setProgress] = useState(0);
  const [statusText, setStatusText] = useState('');
  const [previewList, setPreviewList] = useState([]);
  const [importError, setImportError] = useState('');
  const fileInputRef = useRef(null);

  const [debouncedSearchText, setDebouncedSearchText] = useState('');
  const isFirstRender = useRef(true);

  useEffect(() => {
    fetchFaculties();
  }, []);

  // Handle debounced search
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    const timer = setTimeout(() => {
      setDebouncedSearchText(searchText);
      setPage(0); // Reset về trang đầu khi tìm kiếm
    }, 500);
    return () => clearTimeout(timer);
  }, [searchText]);

  // Main fetch effect
  useEffect(() => {
    if (view === 'list') {
      fetchLecturers();
    }
  }, [page, selectedDepartmentId, selectedStatus, debouncedSearchText, view]);

  const fetchFaculties = async () => {
    try {
      const res = await api.get('/api/v1/faculties');
      const data = res.data.result || res.data.data || res.data;
      const facultyList = Array.isArray(data) ? data : (data?.content ? data.content : []);
      setFaculties(facultyList);
    } catch (err) {
      console.error("Lỗi lấy danh sách khoa", err);
    }
  };

  const fetchLecturers = async () => {
    setListLoading(true);
    setListError('');
    try {
      const params = {
        page,
        size,
        sort: 'id,desc' // Override default sort to avoid missing createdAt field error
      };
      if (debouncedSearchText.trim()) params.search = debouncedSearchText.trim();
      if (selectedDepartmentId) params.departmentId = selectedDepartmentId;
      if (selectedStatus !== '') params.isActive = selectedStatus === 'true';

      const res = await api.get('/api/v1/users/lecturers', { params });
      if (res.data.result) {
        setLecturers(res.data.result.content || []);
        setTotalPages(res.data.result.page?.totalPages || res.data.result.totalPages || 1);
      }
    } catch (err) {
      console.error(err);
      setListError(err.response?.data?.message || err.message || JSON.stringify(err) || 'Không thể tải danh sách giảng viên.');
    } finally {
      setListLoading(false);
    }
  };

  const handleEditClick = (lecturer) => {
    setEditingLecturer(lecturer);
    // Try to find the faculty ID by code if facultyId is not directly in the lecturer object
    let fId = lecturer.facultyId;
    if (!fId) {
      const f = faculties.find(fac => fac.code === lecturer.facultyCode);
      if (f) fId = f.id;
    }
    setEditForm({
      fullName: lecturer.fullName || '',
      email: lecturer.email || '',
      isActive: lecturer.isActive !== false,
      facultyId: fId || '',
      phoneNumber: lecturer.phoneNumber || '',
      gender: lecturer.gender || '',
      birthday: lecturer.birthday || '',
      birthPlace: lecturer.birthPlace || ''
    });
    setEditError('');
  };

  const handleAddLecturerClick = () => {
    setIsAddingLecturer(true);
    setAddForm({ fullName: '', email: '', lecturerCode: '', facultyId: '', phoneNumber: '', gender: '', birthday: '', birthPlace: '' });
    setAddError('');
  };

  const handleCreateLecturer = async () => {
    if (!addForm.fullName || !addForm.email || !addForm.lecturerCode || !addForm.facultyId) {
      setAddError('Vui lòng điền đầy đủ Tên, Email, Mã giảng viên và chọn Khoa');
      return;
    }
    setAddLoading(true);
    setAddError('');
    try {
      const payload = {
        ...addForm,
        facultyId: Number(addForm.facultyId)
      };
      await api.post('/api/v1/users/lecturers', payload);
      setIsAddingLecturer(false);
      fetchLecturers(); // Refresh list
    } catch (err) {
      setAddError(err.response?.data?.message || 'Lỗi khi thêm giảng viên');
    } finally {
      setAddLoading(false);
    }
  };

  const handleUpdateLecturer = async () => {
    if (!editForm.fullName || !editForm.email || !editForm.facultyId) {
      setEditError('Vui lòng điền đầy đủ Tên, Email và chọn Khoa');
      return;
    }
    setEditLoading(true);
    setEditError('');
    try {
      const payload = {
        ...editForm,
        facultyId: Number(editForm.facultyId)
      };
      await api.put(`/api/v1/users/lecturers/${editingLecturer.id}`, payload);
      setEditingLecturer(null);
      fetchLecturers(); // Refresh list
    } catch (err) {
      setEditError(err.response?.data?.message || 'Lỗi khi cập nhật giảng viên');
    } finally {
      setEditLoading(false);
    }
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

    setImportStep('importing_preview');
    setProgress(20);
    setStatusText('Đang tải file lên để kiểm tra...');

    const formData = new FormData();
    formData.append('file', file);

    try {
      setTimeout(() => { setProgress(60); setStatusText('Đang phân tích dữ liệu Excel...'); }, 500);

      const res = await api.post('/api/v1/users/lecturers/import/preview', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setProgress(100);
      setPreviewList(res.data.result || []);
      setImportStep('preview');
    } catch (err) {
      setProgress(0);
      setImportError(err.response?.data?.message || 'Đã xảy ra lỗi khi đọc file dữ liệu. Vui lòng kiểm tra lại cấu trúc file.');
      setImportStep('error');
    }

    e.target.value = null;
  };

  const handleConfirmImport = async () => {
    const validLecturers = previewList.filter(item => item.valid).map(item => item.lecturer);
    if (validLecturers.length === 0) {
      alert("Không có dòng dữ liệu hợp lệ nào để import!");
      return;
    }

    setImportStep('importing_confirm');
    setProgress(30);
    setStatusText(`Đang lưu ${validLecturers.length} giảng viên vào hệ thống...`);

    try {
      await api.post('/api/v1/users/lecturers/import/confirm', validLecturers);
      setProgress(100);
      setStatusText('Lưu dữ liệu thành công!');
      setImportStep('success');
    } catch (err) {
      setProgress(0);
      setImportError(err.response?.data?.message || 'Lỗi khi lưu dữ liệu. Vui lòng thử lại sau.');
      setImportStep('error');
    }
  };

  const downloadTemplate = async () => {
    try {
      const res = await api.get('/api/v1/users/lecturers/template', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Template_Import_GiangVien.xlsx');
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
    setPreviewList([]);
    setImportError('');
  };

  const validCount = previewList.filter(x => x.valid).length;
  const invalidCount = previewList.length - validCount;

  return (
    <div className="page active">
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <div className="tb-title" style={{ fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Users size={24} color="var(--bl)" />
            Quản lý Giảng viên
          </div>
          <div className="tb-sub">
            {view === 'list'
              ? 'Danh sách giảng viên, trạng thái tài khoản và nhập liệu hàng loạt'
              : 'Import hàng loạt từ file Excel'}
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {view === 'list' ? (
            <>
              <button className="btn btn-s" onClick={() => { setPage(0); fetchLecturers(); }} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <RefreshCw size={14} /> Làm mới
              </button>
              <button className="btn btn-p" onClick={handleAddLecturerClick} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <UserPlus size={14} /> Thêm Giảng Viên
              </button>
              <button className="btn btn-s" onClick={() => setView('import')} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <UploadCloud size={14} /> Import Giảng Viên
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
                <h3 style={{ fontSize: '16px', marginBottom: '10px' }}>File mẫu Import Giảng Viên</h3>
                <p style={{ fontSize: '13px', color: 'var(--tx3)', lineHeight: '1.6', maxWidth: '300px' }}>
                  Hệ thống sẽ tự động <strong>thêm mới</strong> những Giảng viên chưa có và <strong>cập nhật</strong> những Giảng viên đã tồn tại dựa trên mã Giảng viên. Giảng viên phải thuộc một <strong>Khoa hợp lệ</strong>.
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
                  <div className="card-t">Import Giảng viên hàng loạt</div>
                  <div className="card-su">
                    {importStep === 'preview' ? 'Kiểm tra dữ liệu trước khi lưu' : 'Hệ thống đang xử lý file...'}
                  </div>
                </div>
                <button className="btn btn-s btn-sm" onClick={closeImport}>
                  <ArrowLeft size={14} style={{ marginRight: '4px' }} /> Quay lại
                </button>
              </div>

              <div style={{ padding: '30px' }}>
                {(importStep === 'importing_preview' || importStep === 'importing_confirm') && (
                  <div style={{ textAlign: 'center', maxWidth: '600px', margin: '0 auto' }}>
                    <FileSpreadsheet size={48} color="var(--bl)" className="pulse" style={{ marginBottom: '16px' }} />
                    <h3 style={{ fontSize: '16px', marginBottom: '8px' }}>{statusText}</h3>
                    <div className="pb" style={{ marginBottom: '12px' }}>
                      <div className="pf bl" style={{ width: `${progress}%`, transition: 'width 0.3s ease' }}></div>
                    </div>
                    <p style={{ color: 'var(--tx3)', fontSize: '13px' }}>Vui lòng không đóng trình duyệt trong quá trình này...</p>
                  </div>
                )}

                {importStep === 'error' && (
                  <div style={{ textAlign: 'center', maxWidth: '600px', margin: '0 auto' }}>
                    <AlertTriangle size={48} color="var(--rd)" style={{ marginBottom: '16px' }} />
                    <h3 style={{ fontSize: '16px', color: 'var(--rd)', marginBottom: '8px' }}>Import thất bại</h3>
                    <p style={{ color: 'var(--tx2)', fontSize: '14px', marginBottom: '24px' }}>{importError}</p>
                    <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                      <button className="btn btn-s" onClick={() => { setImportStep('upload'); setImportError(''); }}>Hủy bỏ</button>
                      <button className="btn btn-p" onClick={() => fileInputRef.current?.click()}>Thử lại file khác</button>
                    </div>
                  </div>
                )}

                {importStep === 'success' && (
                  <div style={{ maxWidth: '600px', margin: '0 auto' }}>
                    <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                      <CheckCircle2 size={48} color="var(--gr)" style={{ marginBottom: '16px' }} />
                      <h3 style={{ fontSize: '16px', color: 'var(--gr)', marginBottom: '8px' }}>Import dữ liệu thành công!</h3>
                      <p style={{ color: 'var(--tx2)', fontSize: '14px' }}>
                        Đã lưu thành công {validCount} giảng viên vào hệ thống.
                      </p>
                    </div>
                    <div style={{ textAlign: 'center' }}>
                      <button className="btn btn-p" onClick={closeImport}>Đóng và Xem danh sách</button>
                    </div>
                  </div>
                )}

                {importStep === 'preview' && (
                  <div>
                    <div style={{ display: 'flex', gap: '20px', marginBottom: '20px', flexWrap: 'wrap' }}>
                      <div style={{ flex: 1, background: 'var(--bg3)', borderRadius: '12px', padding: '16px', borderLeft: '4px solid var(--bl)' }}>
                        <div style={{ fontSize: '12px', color: 'var(--tx3)', marginBottom: '4px' }}>Tổng số dòng quét được</div>
                        <div style={{ fontSize: '24px', fontWeight: '700' }}>{previewList.length}</div>
                      </div>
                      <div style={{ flex: 1, background: 'var(--grL)', borderRadius: '12px', padding: '16px', borderLeft: '4px solid var(--gr)' }}>
                        <div style={{ fontSize: '12px', color: 'var(--tx3)', marginBottom: '4px' }}>Dòng hợp lệ (Sẵn sàng)</div>
                        <div style={{ fontSize: '24px', fontWeight: '700', color: 'var(--gr)' }}>{validCount}</div>
                      </div>
                      <div style={{ flex: 1, background: 'var(--rdL)', borderRadius: '12px', padding: '16px', borderLeft: '4px solid var(--rd)' }}>
                        <div style={{ fontSize: '12px', color: 'var(--tx3)', marginBottom: '4px' }}>Dòng lỗi (Sẽ bị bỏ qua)</div>
                        <div style={{ fontSize: '24px', fontWeight: '700', color: 'var(--rd)' }}>{invalidCount}</div>
                      </div>
                    </div>

                    <div style={{ border: '1px solid var(--bd)', borderRadius: '12px', overflow: 'hidden', marginBottom: '24px' }}>
                      <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                        <table className="tbl">
                          <thead style={{ position: 'sticky', top: 0, background: 'var(--bg)', zIndex: 1, boxShadow: '0 1px 0 var(--bd)' }}>
                            <tr>
                              <th style={{ width: '60px' }}>Dòng</th>
                              <th style={{ width: '120px' }}>Mã GV</th>
                              <th>Họ và Tên</th>
                              <th>Email</th>
                              <th style={{ width: '120px' }}>Mã Khoa</th>
                              <th style={{ width: '250px' }}>Trạng thái kiểm tra</th>
                            </tr>
                          </thead>
                          <tbody>
                            {previewList.map((item, idx) => {
                              const s = item.lecturer || {};
                              return (
                                <tr key={idx} style={{ background: item.valid ? 'transparent' : 'var(--rdL)' }}>
                                  <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{s.rowIndex || (idx + 1)}</td>
                                  <td style={{ fontWeight: '600' }}>{s.lecturerCode || '—'}</td>
                                  <td>{s.fullName || '—'}</td>
                                  <td style={{ color: 'var(--tx2)', fontSize: '13px' }}>{s.email || '—'}</td>
                                  <td>
                                    <span style={{ background: 'var(--bg3)', padding: '2px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: '500' }}>
                                      {s.facultyCode || '—'}
                                    </span>
                                  </td>
                                  <td>
                                    {item.valid ? (
                                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--gr)', fontSize: '12px', fontWeight: '500' }}>
                                        <CheckCircle2 size={14} /> Hợp lệ
                                      </span>
                                    ) : (
                                      <div style={{ color: 'var(--rd)', fontSize: '12px' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: '600', marginBottom: '4px' }}>
                                          <AlertTriangle size={14} /> Lỗi dữ liệu:
                                        </div>
                                        <ul style={{ margin: 0, paddingLeft: '16px' }}>
                                          {(item.errors || []).map((e, i) => <li key={i}>{e}</li>)}
                                        </ul>
                                      </div>
                                    )}
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div style={{ color: 'var(--tx3)', fontSize: '13px' }}>
                        * Hệ thống chỉ lưu lại những dòng báo <span style={{ color: 'var(--gr)', fontWeight: '600' }}>Hợp lệ</span>.
                      </div>
                      <div style={{ display: 'flex', gap: '10px' }}>
                        <button className="btn btn-s" onClick={closeImport}>Hủy bỏ</button>
                        <button
                          className="btn btn-p"
                          onClick={handleConfirmImport}
                          disabled={validCount === 0}
                          style={{ opacity: validCount === 0 ? 0.5 : 1 }}
                        >
                          Xác nhận lưu {validCount} giảng viên hợp lệ
                        </button>
                      </div>
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
        <div className="card">
          <div className="card-h">
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
              <div className="srch">
                <Search className="srch-ic" size={14} />
                <input
                  type="text"
                  placeholder="Tìm Tên, email, mã GV..."
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'var(--bg2)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--bd)' }}>
                <Filter size={14} color="var(--tx3)" />
                <select
                  className="fi"
                  style={{ width: '160px', border: 'none', background: 'transparent', outline: 'none' }}
                  value={selectedDepartmentId}
                  onChange={e => { setPage(0); setSelectedDepartmentId(e.target.value); }}
                >
                  <option value="">Tất cả Khoa</option>
                  {faculties.map(f => (
                    <option key={f.id} value={f.id}>{f.code} - {f.name}</option>
                  ))}
                </select>
              </div>
              <select
                className="fi"
                style={{ width: '150px' }}
                value={selectedStatus}
                onChange={e => { setPage(0); setSelectedStatus(e.target.value); }}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="true">Đang hoạt động</option>
                <option value="false">Đã khóa</option>
              </select>
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

          {listLoading ? (
            <div style={{ padding: '60px', textAlign: 'center' }}>
              <RefreshCw size={32} color="var(--bl)" className="pulse" style={{ marginBottom: '12px' }} />
              <p style={{ color: 'var(--tx3)', fontSize: '14px' }}>Đang tải dữ liệu...</p>
            </div>
          ) : listError ? (
            <div style={{ padding: '40px', textAlign: 'center' }}>
              <AlertTriangle size={32} color="var(--rd)" style={{ marginBottom: '12px' }} />
              <p style={{ color: 'var(--rd)', fontSize: '14px', marginBottom: '16px' }}>{listError}</p>
              <button className="btn btn-p" onClick={() => fetchLecturers()}>Thử lại</button>
            </div>
          ) : lecturers.length === 0 ? (
            <div style={{ padding: '60px', textAlign: 'center' }}>
              <Users size={48} color="var(--tx3)" style={{ marginBottom: '12px', opacity: 0.4 }} />
              <p style={{ color: 'var(--tx3)', fontSize: '14px', marginBottom: '16px' }}>
                {debouncedSearchText
                  ? `Không tìm thấy giảng viên nào với từ khóa "${debouncedSearchText}"`
                  : 'Chưa có dữ liệu Giảng viên. Hãy Import file Excel để bắt đầu.'}
              </p>
              {!debouncedSearchText && (
                <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <UploadCloud size={14} /> Import Excel ngay
                </button>
              )}
            </div>
          ) : (
            <>
              <div style={{ overflowX: 'auto' }}>
                <table className="tbl">
                  <thead>
                    <tr>
                      <th style={{ width: '50px' }}>#</th>
                      <th>Mã GV</th>
                      <th>Họ tên</th>
                      <th>Email</th>
                      <th>SĐT</th>
                      <th>Giới tính</th>
                      <th>Khoa</th>
                      <th>Trạng thái</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {lecturers.map((lecturer, idx) => (
                      <tr key={lecturer.id}>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{page * size + idx + 1}</td>
                        <td style={{ fontWeight: '600' }}>{lecturer.lecturerCode}</td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <div style={{
                              width: '26px', height: '26px', borderRadius: '50%',
                              background: 'linear-gradient(135deg,var(--bl),var(--pu))',
                              display: 'flex', alignItems: 'center', justifyContent: 'center',
                              fontSize: '10px', fontWeight: '600', color: '#fff'
                            }}>
                              {lecturer.fullName ? lecturer.fullName.substring(0, 2).toUpperCase() : 'GV'}
                            </div>
                            <span style={{ fontWeight: '500' }}>{lecturer.fullName}</span>
                          </div>
                        </td>
                        <td style={{ fontSize: '12px', color: 'var(--tx3)' }}>{lecturer.email}</td>
                        <td style={{ fontSize: '12px', color: 'var(--tx3)' }}>{lecturer.phoneNumber || '—'}</td>
                        <td style={{ fontSize: '12px' }}>
                          {lecturer.gender === 'male' ? 'Nam' : lecturer.gender === 'female' ? 'Nữ' : lecturer.gender === 'other' ? 'Khác' : '—'}
                        </td>
                        <td>
                          <div style={{ fontSize: '12px', fontWeight: '500' }}>{lecturer.facultyCode || '—'}</div>
                          <div style={{ fontSize: '11px', color: 'var(--tx3)' }}>{lecturer.facultyName}</div>
                        </td>
                        <td>
                          {lecturer.isActive ? (
                            <span className="bdg b-op">Hoạt động</span>
                          ) : (
                            <span className="bdg b-ca">Đã khóa</span>
                          )}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <button className="btn btn-s btn-sm" onClick={() => handleEditClick(lecturer)}>Sửa</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination control */}
              {totalPages > 1 && (
                <div style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  padding: '12px 16px', borderTop: '1px solid var(--bd)'
                }}>
                  <div style={{ fontSize: '12px', color: 'var(--tx3)' }}>
                    Trang {page + 1} / {totalPages}
                  </div>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      className="btn btn-s" style={{ padding: '6px 10px' }}
                      disabled={page === 0}
                      onClick={() => setPage(p => Math.max(0, p - 1))}
                    >
                      <ChevronLeft size={16} />
                    </button>
                    <button
                      className="btn btn-s" style={{ padding: '6px 10px' }}
                      disabled={page >= totalPages - 1}
                      onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    >
                      <ChevronRight size={16} />
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* EDIT MODAL */}
      {editingLecturer && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 999,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '400px', padding: '24px', position: 'relative', boxShadow: '0 10px 25px rgba(0,0,0,0.1)' }}>
            <button
              onClick={() => setEditingLecturer(null)}
              style={{ position: 'absolute', top: '20px', right: '20px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--tx3)' }}
            >
              <X size={20} />
            </button>
            <h3 style={{ marginBottom: '20px', fontSize: '18px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Users size={20} color="var(--bl)" /> Chỉnh sửa Giảng viên
            </h3>

            {editError && (
              <div style={{ background: 'var(--rdL)', color: 'var(--rd)', padding: '10px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <AlertTriangle size={16} /> {editError}
              </div>
            )}

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Họ và tên</label>
              <input
                type="text"
                value={editForm.fullName}
                onChange={e => setEditForm({ ...editForm, fullName: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                placeholder="Nhập họ và tên"
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Email</label>
              <input
                type="email"
                value={editForm.email}
                onChange={e => setEditForm({ ...editForm, email: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                placeholder="Nhập email"
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Số điện thoại</label>
                <input
                  type="text"
                  value={editForm.phoneNumber}
                  onChange={e => setEditForm({ ...editForm, phoneNumber: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                  placeholder="SĐT"
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Giới tính</label>
                <select
                  value={editForm.gender}
                  onChange={e => setEditForm({ ...editForm, gender: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                >
                  <option value="">-- Chọn --</option>
                  <option value="male">Nam</option>
                  <option value="female">Nữ</option>
                  <option value="other">Khác</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Ngày sinh</label>
                <input
                  type="date"
                  value={editForm.birthday}
                  onChange={e => setEditForm({ ...editForm, birthday: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Nơi sinh</label>
                <input
                  type="text"
                  value={editForm.birthPlace}
                  onChange={e => setEditForm({ ...editForm, birthPlace: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                  placeholder="Nơi sinh"
                />
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Khoa</label>
              <select
                value={editForm.facultyId}
                onChange={e => setEditForm({ ...editForm, facultyId: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
              >
                <option value="">-- Chọn Khoa --</option>
                {faculties.map(f => (
                  <option key={f.id} value={f.id}>{f.code} - {f.name}</option>
                ))}
              </select>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Trạng thái tài khoản</label>
              <select
                value={editForm.isActive ? "true" : "false"}
                onChange={e => setEditForm({ ...editForm, isActive: e.target.value === "true" })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
              >
                <option value="true">Hoạt động</option>
                <option value="false">Đã khóa</option>
              </select>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setEditingLecturer(null)}>Hủy</button>
              <button className="btn btn-p" onClick={handleUpdateLecturer} disabled={editLoading}>
                {editLoading ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ADD MODAL */}
      {isAddingLecturer && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 999,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '400px', padding: '24px', position: 'relative', boxShadow: '0 10px 25px rgba(0,0,0,0.1)' }}>
            <button
              onClick={() => setIsAddingLecturer(false)}
              style={{ position: 'absolute', top: '20px', right: '20px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--tx3)' }}
            >
              <X size={20} />
            </button>
            <h3 style={{ marginBottom: '20px', fontSize: '18px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <UserPlus size={20} color="var(--bl)" /> Thêm mới Giảng viên
            </h3>

            {addError && (
              <div style={{ background: 'var(--rdL)', color: 'var(--rd)', padding: '10px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <AlertTriangle size={16} /> {addError}
              </div>
            )}

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Mã giảng viên</label>
              <input
                type="text"
                value={addForm.lecturerCode}
                onChange={e => setAddForm({ ...addForm, lecturerCode: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                placeholder="Nhập mã giảng viên"
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Họ và tên</label>
              <input
                type="text"
                value={addForm.fullName}
                onChange={e => setAddForm({ ...addForm, fullName: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                placeholder="Nhập họ và tên"
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Email</label>
              <input
                type="email"
                value={addForm.email}
                onChange={e => setAddForm({ ...addForm, email: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                placeholder="Nhập email"
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Số điện thoại</label>
                <input
                  type="text"
                  value={addForm.phoneNumber}
                  onChange={e => setAddForm({ ...addForm, phoneNumber: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                  placeholder="SĐT"
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Giới tính</label>
                <select
                  value={addForm.gender}
                  onChange={e => setAddForm({ ...addForm, gender: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                >
                  <option value="">-- Chọn --</option>
                  <option value="male">Nam</option>
                  <option value="female">Nữ</option>
                  <option value="other">Khác</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Ngày sinh</label>
                <input
                  type="date"
                  value={addForm.birthday}
                  onChange={e => setAddForm({ ...addForm, birthday: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Nơi sinh</label>
                <input
                  type="text"
                  value={addForm.birthPlace}
                  onChange={e => setAddForm({ ...addForm, birthPlace: e.target.value })}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
                  placeholder="Nơi sinh"
                />
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600' }}>Khoa</label>
              <select
                value={addForm.facultyId}
                onChange={e => setAddForm({ ...addForm, facultyId: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--bd)', outline: 'none' }}
              >
                <option value="">-- Chọn Khoa --</option>
                {faculties.map(f => (
                  <option key={f.id} value={f.id}>{f.code} - {f.name}</option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setIsAddingLecturer(false)}>Hủy</button>
              <button className="btn btn-p" onClick={handleCreateLecturer} disabled={addLoading}>
                {addLoading ? 'Đang thêm...' : 'Thêm mới'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
