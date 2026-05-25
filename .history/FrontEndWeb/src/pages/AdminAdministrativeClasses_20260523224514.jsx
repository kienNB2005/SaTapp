import { useState, useEffect, useRef } from 'react';
import {
  UploadCloud, FileSpreadsheet, CheckCircle2, AlertTriangle,
  ArrowLeft, Download, RefreshCw, Search,
  Edit, Trash2, X, Users
} from 'lucide-react';
import api from '../utils/api';

export default function AdminAdministrativeClasses() {
  // --- VIEW STATE ---
  const [view, setView] = useState('list'); // 'list' | 'import'

  // --- LIST STATE ---
  const [classes, setClasses] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [searchText, setSearchText] = useState('');

  // --- EDIT & DELETE STATE ---
  const [editingClass, setEditingClass] = useState(null);
  const [editName, setEditName] = useState('');
  const [editCohortYear, setEditCohortYear] = useState('');
  const [editLecturerCode, setEditLecturerCode] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(null);

  // --- ADD STATE ---
  const [isAdding, setIsAdding] = useState(false);
  const [addCode, setAddCode] = useState('');
  const [addName, setAddName] = useState('');
  const [addCohortYear, setAddCohortYear] = useState('');
  const [addDepartmentId, setAddDepartmentId] = useState('');
  const [addLecturerCode, setAddLecturerCode] = useState('');
  
  const [departments, setDepartments] = useState([]);

  // --- IMPORT STATE ---
  const [importStep, setImportStep] = useState('upload');
  const [progress, setProgress] = useState(0);
  const [statusText, setStatusText] = useState('');
  const [importResult, setImportResult] = useState(null);
  const [importError, setImportError] = useState('');
  const fileInputRef = useRef(null);

  // =====================
  // FETCH LIST
  // =====================
  const fetchClasses = async (signal) => {
    setListLoading(true);
    setListError('');
    try {
      const res = await api.get('/api/v1/administrative-classes', { signal });
      setClasses(res.data.result || []);
    } catch (err) {
      if (err.name === 'AbortError' || err.name === 'CanceledError') return;
      setListError(err.response?.data?.message || 'Không thể tải danh sách lớp hành chính.');
    } finally {
      setListLoading(false);
    }
  };

  useEffect(() => {
    if (view !== 'list') return;
    const controller = new AbortController();
    fetchClasses(controller.signal);
    return () => controller.abort();
  }, [view]);

  // =====================
  // EDIT & DELETE
  // =====================
  const handleEditClick = (cls) => {
    setEditingClass(cls);
    setEditName(cls.name || '');
    setEditCohortYear(cls.cohortYear || '');
    setEditLecturerCode(cls.homeroomTeacherCode || '');
  };

  const handleSaveEdit = async () => {
    if (!editName.trim() || !editCohortYear.trim()) {
      alert('Vui lòng nhập Tên lớp và Khóa học!');
      return;
    }
    setIsSaving(true);
    try {
      const payload = {
        name: editName,
        cohortYear: editCohortYear,
        lecturerCode: editLecturerCode || ''
      };
      await api.put(`/api/v1/administrative-classes/${editingClass.id}`, payload);
      setEditingClass(null);
      fetchClasses();
    } catch (err) {
      alert(err.response?.data?.message || 'Cập nhật thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (cls) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa lớp "${cls.code}" không?`)) return;
    setIsDeleting(cls.id);
    try {
      await api.delete(`/api/v1/administrative-classes/${cls.id}`);
      fetchClasses();
    } catch (err) {
      alert(err.response?.data?.message || 'Xóa thất bại');
    } finally {
      setIsDeleting(null);
    }
  };

  const handleAddClick = async () => {
    setIsAdding(true);
    setAddCode('');
    setAddName('');
    setAddCohortYear('');
    setAddDepartmentId('');
    setAddLecturerCode('');
    try {
      const res = await api.get('/api/v1/departments');
      setDepartments(res.data.result || []);
    } catch (err) {
      console.error("Lỗi lấy danh sách ngành", err);
    }
  };

  const handleSaveAdd = async () => {
    if (!addCode.trim() || !addName.trim() || !addCohortYear.trim()) {
      alert('Vui lòng nhập Mã lớp, Tên lớp và Khóa học!');
      return;
    }
    if (!addDepartmentId) {
      alert('Vui lòng chọn Ngành!');
      return;
    }
    setIsSaving(true);
    try {
      await api.post('/api/v1/administrative-classes', {
        code: addCode,
        name: addName,
        cohortYear: addCohortYear,
        departmentId: Number(addDepartmentId),
        homeroomTeacherCode: addLecturerCode || ''
      });
      setIsAdding(false);
      fetchClasses();
    } catch (err) {
      alert(err.response?.data?.message || 'Thêm mới thất bại. Vui lòng thử lại.');
    } finally {
      setIsSaving(false);
    }
  };

  // =====================
  // IMPORT
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

      const res = await api.post('/api/v1/administrative-classes/import', formData, {
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
      const res = await api.get('/api/v1/administrative-classes/template', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Template_Import_LopHanhChinh.xlsx');
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

  const filteredClasses = classes.filter(c =>
    (c.code?.toLowerCase() || '').includes(searchText.toLowerCase()) ||
    (c.name?.toLowerCase() || '').includes(searchText.toLowerCase()) ||
    (c.departmentName?.toLowerCase() || '').includes(searchText.toLowerCase()) ||
    (c.homeroomTeacherName?.toLowerCase() || '').includes(searchText.toLowerCase())
  );

  return (
    <div className="page active">
      {/* HEADER SECTION */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <div className="tb-title" style={{ fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Users size={24} color="var(--bl)" />
            Quản lý Lớp hành chính
          </div>
          <div className="tb-sub">
            {view === 'list' ? `Danh sách Lớp sinh viên · ${classes.length} lớp trong hệ thống` : 'Import hàng loạt từ file Excel'}
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {view === 'list' ? (
            <>
              <button className="btn btn-s" onClick={() => fetchClasses()} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
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
                <h3 style={{ fontSize: '16px', marginBottom: '10px' }}>File mẫu Import Lớp</h3>
                <p style={{ fontSize: '13px', color: 'var(--tx3)', lineHeight: '1.6', maxWidth: '300px' }}>
                  Hệ thống sẽ tự động <strong>thêm mới</strong> những Lớp chưa có và <strong>cập nhật</strong> những Lớp đã tồn tại.
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
                  <div className="card-t">Nhập dữ liệu Lớp từ Excel</div>
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
                    <span style={{ fontWeight: '600', color: 'var(--gr)' }}>{importResult.successCount || 0} lớp</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--tx2)' }}>Cập nhật thông tin:</span>
                    <span style={{ fontWeight: '600', color: 'var(--bl)' }}>{importResult.updatedCount || 0} lớp</span>
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
                <div className="card-t">Danh sách Lớp</div>
                <div className="card-su">Tất cả các lớp sinh viên hiện có trong hệ thống</div>
              </div>
              <div className="srch">
                <Search className="srch-ic" size={14} />
                <input
                  type="text"
                  placeholder="Tìm theo mã, tên, khoa..."
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
                <button className="btn btn-p" onClick={() => fetchClasses()}>Thử lại</button>
              </div>
            )}

            {/* Empty */}
            {!listLoading && !listError && filteredClasses.length === 0 && (
              <div style={{ padding: '60px', textAlign: 'center' }}>
                <Users size={48} color="var(--tx3)" style={{ marginBottom: '12px', opacity: 0.4 }} />
                <p style={{ color: 'var(--tx3)', fontSize: '14px', marginBottom: '16px' }}>
                  {searchText
                    ? `Không tìm thấy lớp nào với từ khóa "${searchText}"`
                    : 'Chưa có dữ liệu Lớp. Hãy Import file Excel để bắt đầu.'}
                </p>
                {!searchText && (
                  <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                    <UploadCloud size={14} /> Import Excel ngay
                  </button>
                )}
              </div>
            )}

            {/* Table */}
            {!listLoading && !listError && filteredClasses.length > 0 && (
              <div style={{ overflowX: 'auto' }}>
                <table className="tbl">
                  <thead>
                    <tr>
                      <th style={{ width: '50px' }}>#</th>
                      <th style={{ width: '120px' }}>Mã Lớp</th>
                      <th>Tên lớp & Khoa</th>
                      <th>Khóa học</th>
                      <th>GV Chủ nhiệm</th>
                      <th style={{ width: '100px', textAlign: 'center' }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredClasses.map((cls, index) => (
                      <tr key={cls.id}>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{index + 1}</td>
                        <td>
                          <span style={{
                            background: 'var(--blL)', color: 'var(--bl)',
                            padding: '3px 10px', borderRadius: '6px',
                            fontSize: '12px', fontWeight: '600', letterSpacing: '0.5px'
                          }}>
                            {cls.code || '—'}
                          </span>
                        </td>
                        <td>
                          <div style={{ fontWeight: '500' }}>{cls.name}</div>
                          <div style={{ fontSize: '12px', color: 'var(--tx3)', marginTop: '2px' }}>
                            {cls.departmentName || 'Chưa thuộc khoa/ngành'}
                          </div>
                        </td>
                        <td>
                          <span style={{ fontWeight: '500', color: 'var(--tx2)' }}>
                            {cls.cohortYear || '—'}
                          </span>
                        </td>
                        <td>
                          {cls.homeroomTeacherName ? (
                            <span style={{ color: 'var(--tx)' }}>{cls.homeroomTeacherName}</span>
                          ) : (
                            <span style={{ fontSize: '12px', color: 'var(--tx3)', fontStyle: 'italic' }}>Chưa phân công</span>
                          )}
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px' }}
                              title="Sửa"
                              onClick={() => handleEditClick(cls)}
                            >
                              <Edit size={14} color="var(--bl)" />
                            </button>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px', borderColor: 'rgba(239,68,68,0.3)' }}
                              title="Xóa"
                              onClick={() => handleDelete(cls)}
                              disabled={isDeleting === cls.id}
                            >
                              {isDeleting === cls.id ? (
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
            
            {!listLoading && !listError && filteredClasses.length > 0 && (
              <div style={{ padding: '12px 16px', borderTop: '1px solid var(--bd)', fontSize: '12px', color: 'var(--tx3)' }}>
                Hiển thị {filteredClasses.length}/{classes.length} lớp
              </div>
            )}
          </div>
        </>
      )}

      {/* ======================== */}
      {/* EDIT MODAL               */}
      {/* ======================== */}
      {editingClass && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '450px', padding: '24px', margin: '20px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Cập nhật Lớp hành chính</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setEditingClass(null)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            {/* Read-only Code & Dept */}
            <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Lớp (Chỉ đọc)</label>
                <div style={{ padding: '10px 14px', background: 'var(--bg3)', borderRadius: '8px', fontSize: '14px', color: 'var(--tx3)', fontWeight: '500' }}>
                  {editingClass.code}
                </div>
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Ngành (Chỉ đọc)</label>
                <div style={{ padding: '10px 14px', background: 'var(--bg3)', borderRadius: '8px', fontSize: '14px', color: 'var(--tx3)', fontWeight: '500', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={editingClass.departmentName}>
                  {editingClass.departmentCode}
                </div>
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Lớp</label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                placeholder="Ví dụ: Lớp CNTT K22A"
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)', background: 'var(--bg2)', color: 'var(--tx)' }}
                autoFocus
              />
            </div>
            
            <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Khóa học</label>
                <input
                  type="text"
                  value={editCohortYear}
                  onChange={(e) => setEditCohortYear(e.target.value)}
                  placeholder="Ví dụ: K22"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Giảng viên CN</label>
                <input
                  type="text"
                  value={editLecturerCode}
                  onChange={(e) => setEditLecturerCode(e.target.value)}
                  placeholder="Mã Giảng viên"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
            </div>
            <div style={{ fontSize: '12px', color: 'var(--tx3)', marginBottom: '24px', fontStyle: 'italic' }}>
              * Nhập mã giảng viên chủ nhiệm. Để trống nếu chưa phân công.
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button className="btn btn-s" onClick={() => setEditingClass(null)} disabled={isSaving}>Hủy</button>
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
          <div className="card" style={{ width: '100%', maxWidth: '450px', padding: '24px', margin: '20px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm Lớp hành chính mới</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setIsAdding(false)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Lớp</label>
                <input
                  type="text"
                  value={addCode}
                  onChange={(e) => setAddCode(e.target.value)}
                  placeholder="Ví dụ: CNTTK22A"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg3)', color: 'var(--tx)' }}
                  autoFocus
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tên Lớp</label>
                <input
                  type="text"
                  value={addName}
                  onChange={(e) => setAddName(e.target.value)}
                  placeholder="Ví dụ: Lớp CNTT K22A"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Ngành</label>
              <select
                className="fi"
                value={addDepartmentId}
                onChange={(e) => setAddDepartmentId(e.target.value)}
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg3)', color: 'var(--tx)' }}
              >
                <option value="" disabled>-- Chọn Ngành --</option>
                {departments.map(d => (
                  <option key={d.id} value={d.id}>{d.name} ({d.code})</option>
                ))}
              </select>
            </div>
            
            <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Khóa học</label>
                <input
                  type="text"
                  value={addCohortYear}
                  onChange={(e) => setAddCohortYear(e.target.value)}
                  placeholder="Ví dụ: K22"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Giảng viên CN</label>
                <input
                  type="text"
                  value={addLecturerCode}
                  onChange={(e) => setAddLecturerCode(e.target.value)}
                  placeholder="Mã Giảng viên"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
            </div>
            <div style={{ fontSize: '12px', color: 'var(--tx3)', marginBottom: '24px', fontStyle: 'italic' }}>
              * Nhập mã giảng viên chủ nhiệm. Để trống nếu chưa phân công.
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
