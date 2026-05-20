import React, { useState, useEffect, useRef } from 'react';
import {
  UploadCloud, FileSpreadsheet, CheckCircle2, AlertTriangle, ChevronRight,
  Download, ArrowLeft, Info, CalendarClock, RefreshCw, Search, Calendar,
  Users, BookOpen, Clock
} from 'lucide-react';
import api from '../utils/api';

export default function AdminTkb() {
  const [view, setView] = useState('list'); // 'list' | 'import'
  const [step, setStep] = useState('upload'); // 'upload' | 'analyzing' | 'preview' | 'importing' | 'success'

  // Global selections
  const [semesters, setSemesters] = useState([]);
  const [selectedSemester, setSelectedSemester] = useState('');

  // --- LIST STATE ---
  const [schedules, setSchedules] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState('');
  
  // Filters
  const [searchText, setSearchText] = useState('');
  const [filterDay, setFilterDay] = useState('');

  // --- IMPORT STATE ---
  const [previewData, setPreviewData] = useState([]);
  const [importStats, setImportStats] = useState(null);
  const [importProgress, setImportProgress] = useState(0);
  const [statusText, setStatusText] = useState('');
  const fileInputRef = useRef(null);

  const [isSemestersLoading, setIsSemestersLoading] = useState(true);

  // =====================
  // INITIALIZATION
  // =====================
  useEffect(() => {
    fetchSemesters();
  }, []);

  const fetchSemesters = async () => {
    setIsSemestersLoading(true);
    try {
      const res = await api.get('/admin/semesters');
      const sems = res.data.result || [];
      setSemesters(sems);
      
      if (sems.length > 0) {
        const active = sems.find(s => s.isActive);
        if (active) {
          setSelectedSemester(active.id.toString());
        } else {
          setSelectedSemester(''); // Let backend enforce selection or user select manually
        }
      } else {
        setSelectedSemester('');
      }
    } catch (err) {
      console.error('Failed to fetch semesters:', err);
    } finally {
      setIsSemestersLoading(false);
    }
  };

  // Fetch schedules whenever semester or filters change (if in list view)
  useEffect(() => {
    if (view === 'list' && selectedSemester) {
      fetchSchedules(0); // Reset to page 0 when filters change
    }
  }, [selectedSemester, view, filterDay]); // Deliberately omit searchText here to require explicit search trigger or rely on separate hook

  // Debounced search
  useEffect(() => {
    if (view === 'list' && selectedSemester) {
      const timeoutId = setTimeout(() => {
        fetchSchedules(0);
      }, 500);
      return () => clearTimeout(timeoutId);
    }
  }, [searchText]);

  // =====================
  // LIST FETCHING
  // =====================
  const fetchSchedules = async (page = currentPage) => {
    setListLoading(true);
    setListError('');
    try {
      let url = `/admin/schedules?page=${page}&size=15`;
      if (selectedSemester) url += `&semesterId=${selectedSemester}`;
      if (searchText) url += `&search=${encodeURIComponent(searchText)}`;
      if (filterDay) url += `&dayOfWeek=${filterDay}`;
      
      const res = await api.get(url);
      const pageData = res.data.result;
      
      setSchedules(pageData.content || []);
      setTotalElements(pageData.totalElements ?? pageData.page?.totalElements ?? 0);
      setTotalPages(pageData.totalPages ?? pageData.page?.totalPages ?? 0);
      setCurrentPage(pageData.number ?? pageData.page?.number ?? 0);
      
    } catch (err) {
      setListError(err.response?.data?.message || 'Không thể tải danh sách Thời khóa biểu.');
      setSchedules([]);
      setTotalElements(0);
      setTotalPages(0);
    } finally {
      setListLoading(false);
    }
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      fetchSchedules(newPage);
    }
  };

  // =====================
  // IMPORT WORKFLOW
  // =====================
  const handleImportClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.name.match(/\.(xlsx|xls|csv)$/)) {
      alert('Vui lòng chọn file Excel (.xlsx, .xls) hoặc CSV');
      e.target.value = null;
      return;
    }

    setStep('analyzing');
    setImportProgress(20);
    setStatusText('Đang tải file lên và đọc dữ liệu Excel...');

    const formData = new FormData();
    formData.append('file', file);

    try {
      // Simulate progress for UI UX
      const interval = setInterval(() => {
        setImportProgress(prev => {
          if (prev >= 85) { clearInterval(interval); return 85; }
          return prev + 15;
        });
        setStatusText('Đang ánh xạ mã lớp, môn, giảng viên...');
      }, 500);

      const res = await api.post('/admin/schedules/import/preview', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      clearInterval(interval);
      setImportProgress(100);
      
      setTimeout(() => {
        setPreviewData(res.data.result || []);
        setStep('preview');
      }, 600);

    } catch (err) {
      alert(err.response?.data?.message || 'Đã xảy ra lỗi khi đọc file Excel. Vui lòng kiểm tra lại định dạng file.');
      setStep('upload');
    }

    e.target.value = null;
  };

  const startImport = async () => {
    if (!selectedSemester) {
      alert('Vui lòng chọn học kỳ trước khi xác nhận nhập dữ liệu!');
      return;
    }

    // Filter valid rows to send back to server for confirm
    const validSchedules = previewData.filter(item => item.valid).map(item => item.schedule);
    
    if (validSchedules.length === 0) {
      alert('Không có dòng dữ liệu hợp lệ nào để import!');
      return;
    }

    setStep('importing');
    setImportProgress(10);
    setStatusText('Đang lưu dữ liệu TKB gốc...');

    try {
      // Simulate progress
      const interval = setInterval(() => {
        setImportProgress(prev => {
          if (prev >= 90) { clearInterval(interval); return 90; }
          return prev + 10;
        });
        if (importProgress > 30) setStatusText('Đang sinh lịch học chi tiết cho từng lớp...');
        if (importProgress > 60) setStatusText('Đang thiết lập trạng thái "Đã lên lịch"...');
      }, 500);

      const res = await api.post(`/admin/schedules/import/confirm?semesterId=${selectedSemester}`, validSchedules);

      clearInterval(interval);
      setImportProgress(100);
      
      setTimeout(() => {
        setImportStats(res.data.result);
        setStep('success');
      }, 600);

    } catch (err) {
      alert(err.response?.data?.message || 'Có lỗi xảy ra khi xác nhận Import. Vui lòng thử lại!');
      setStep('preview'); // fallback
    }
  };

  const downloadTemplate = async () => {
    try {
      const res = await api.get('/admin/schedules/template', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Template_Import_TKB.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Không thể tải file mẫu. Vui lòng thử lại sau.');
    }
  };

  const closeImport = () => {
    setView('list');
    setStep('upload');
    setPreviewData([]);
    setImportStats(null);
    if (selectedSemester) {
        fetchSchedules(0); // refresh list
    }
  };

  // =====================
  // RENDER HELPERS
  // =====================
  const formatDayOfWeek = (day) => {
    if (day === 8) return 'Chủ nhật';
    return `Thứ ${day}`;
  };

  return (
    <div className="page active">
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <div className="tb-title" style={{ fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CalendarClock size={24} color="var(--bl)" />
            Thời Khóa Biểu
          </div>
          <div className="tb-sub">Khởi tạo và tự động sinh lịch học toàn học kỳ</div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {/* Semester Selector (always visible to know context) */}
          <select 
            className="fi" 
            value={selectedSemester} 
            onChange={(e) => setSelectedSemester(e.target.value)} 
            style={{ width: '200px', background: 'var(--bg2)', padding: '8px 12px' }}
          >
            {semesters.length === 0 && <option value="">Đang tải...</option>}
            {semesters.length > 0 && <option value="">-- Chọn học kỳ --</option>}
            {semesters.map(s => (
              <option key={s.id} value={s.id}>{s.name} {s.isActive ? '(Hiện tại)' : ''}</option>
            ))}
          </select>

          {view === 'list' ? (
            <>
              <button className="btn btn-s" onClick={() => fetchSchedules()} disabled={!selectedSemester} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <RefreshCw size={14} /> Làm mới
              </button>
              {schedules.length === 0 && !searchText && !filterDay && !listLoading && (
                <button className="btn btn-p" onClick={() => setView('import')} disabled={semesters.length === 0} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <UploadCloud size={14} /> Import TKB
                </button>
              )}
            </>
          ) : (
            <button className="btn btn-s" onClick={closeImport} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <ArrowLeft size={14} /> Trở lại danh sách
            </button>
          )}
        </div>
      </div>

      {/* ======================== */}
      {/* VIEW: LIST               */}
      {/* ======================== */}
      {view === 'list' && (
        <>
          {isSemestersLoading ? (
            <div style={{ padding: '60px', textAlign: 'center' }}>
              <RefreshCw size={32} color="var(--bl)" className="pulse" style={{ marginBottom: '12px' }} />
              <p style={{ color: 'var(--tx3)', fontSize: '14px' }}>Đang tải thông tin học kỳ...</p>
            </div>
          ) : semesters.length === 0 ? (
            <div className="card" style={{ padding: '60px', textAlign: 'center', marginTop: '20px' }}>
              <AlertTriangle size={48} color="var(--am)" style={{ marginBottom: '16px' }} />
              <h3 style={{ fontSize: '18px', marginBottom: '8px' }}>Hệ thống chưa có học kỳ nào</h3>
              <p style={{ color: 'var(--tx3)' }}>Vui lòng thêm học kỳ mới tại trang "Quản lý Học kỳ" để có thể quản lý và import thời khóa biểu.</p>
            </div>
          ) : !selectedSemester ? (
            <div className="card" style={{ padding: '60px', textAlign: 'center', marginTop: '20px' }}>
              <Calendar size={48} color="var(--bl)" style={{ marginBottom: '16px' }} />
              <h3 style={{ fontSize: '18px', marginBottom: '8px' }}>Vui lòng chọn học kỳ</h3>
              <p style={{ color: 'var(--tx3)' }}>Không có học kỳ nào đang hoạt động. Vui lòng chọn một học kỳ ở phía trên để xem dữ liệu TKB.</p>
            </div>
          ) : listError ? (
            <div className="card" style={{ padding: '60px', textAlign: 'center', marginTop: '20px' }}>
              <AlertTriangle size={48} color="var(--rd)" style={{ marginBottom: '16px' }} />
              <h3 style={{ fontSize: '18px', marginBottom: '8px' }}>Lỗi khi tải dữ liệu</h3>
              <p style={{ color: 'var(--rd)' }}>{listError}</p>
              <button className="btn btn-p" style={{ marginTop: '20px' }} onClick={() => fetchSchedules(0)}>Thử lại</button>
            </div>
          ) : schedules.length === 0 && !searchText && !filterDay && !listLoading ? (
            <div className="g2">
              <div className="card">
                <div className="card-h">
                  <div className="card-t">Trạng thái học kỳ</div>
                </div>
                <div style={{ padding: '30px', textAlign: 'center' }}>
                  <div style={{ background: 'var(--bg3)', padding: '20px', borderRadius: '12px', border: '1px solid var(--bd)', marginBottom: '24px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', justifyContent: 'center' }}>
                      <AlertTriangle size={24} color="var(--am)" />
                      <span style={{ fontSize: '16px', fontWeight: '600', color: 'var(--am)' }}>
                        Học kỳ này chưa có dữ liệu Thời khóa biểu
                      </span>
                    </div>
                  </div>
                  <button className="btn btn-p" onClick={() => setView('import')} style={{ padding: '12px 24px', fontSize: '15px', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                    <UploadCloud size={18} />
                    Khởi tạo TKB từ Excel
                  </button>
                </div>
              </div>
              
              <div className="card" style={{ background: 'linear-gradient(135deg, var(--bg3), var(--bg2))', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '30px', textAlign: 'center' }}>
                <div style={{ width: '64px', height: '64px', borderRadius: '50%', background: 'linear-gradient(135deg, var(--blL), var(--puL))', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px', border: '1px solid var(--bd)' }}>
                  <Info size={32} color="var(--bl)" />
                </div>
                <h3 style={{ fontSize: '16px', marginBottom: '10px' }}>Hướng dẫn khởi tạo TKB</h3>
                <p style={{ fontSize: '13px', color: 'var(--tx3)', lineHeight: '1.6', maxWidth: '300px' }}>
                  Bạn chỉ có thể khởi tạo TKB cho các học kỳ chưa có dữ liệu. Hệ thống hỗ trợ file Excel chuẩn từ nhà trường. Toàn bộ lịch học sẽ được sinh tự động ngay lập tức.
                </p>
                <button className="btn btn-s" onClick={downloadTemplate} style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Download size={14} /> Tải file mẫu .xlsx
                </button>
              </div>
            </div>
          ) : (
            <div className="card">
              <div className="card-h">
                <div>
                  <div className="card-t">Dữ liệu Thời khóa biểu</div>
                  <div className="card-su">Tổng số: {totalElements} dòng TKB gốc</div>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <div className="srch">
                    <select value={filterDay} onChange={(e) => setFilterDay(e.target.value)} style={{ border: 'none', background: 'transparent', outline: 'none', fontSize: '13px', color: 'var(--tx)', width: '100px' }}>
                      <option value="">Tất cả ngày</option>
                      <option value="2">Thứ 2</option>
                      <option value="3">Thứ 3</option>
                      <option value="4">Thứ 4</option>
                      <option value="5">Thứ 5</option>
                      <option value="6">Thứ 6</option>
                      <option value="7">Thứ 7</option>
                      <option value="8">Chủ nhật</option>
                    </select>
                  </div>
                  <div className="srch">
                    <Search className="srch-ic" size={14} />
                    <input
                      type="text"
                      placeholder="Tìm mã lớp, giảng viên, môn..."
                      value={searchText}
                      onChange={(e) => setSearchText(e.target.value)}
                    />
                  </div>
                </div>
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
                  <button className="btn btn-p" onClick={() => fetchSchedules()}>Thử lại</button>
                </div>
              ) : schedules.length === 0 ? (
                <div style={{ padding: '60px', textAlign: 'center' }}>
                  <CalendarClock size={48} color="var(--tx3)" style={{ marginBottom: '12px', opacity: 0.4 }} />
                  <p style={{ color: 'var(--tx3)', fontSize: '14px', marginBottom: '16px' }}>
                    Không tìm thấy TKB nào phù hợp với bộ lọc.
                  </p>
                </div>
              ) : (
                <>
                  <div style={{ overflowX: 'auto' }}>
                    <table className="tbl">
                      <thead>
                        <tr>
                          <th>Lớp</th>
                          <th>Môn học</th>
                          <th>Giảng viên</th>
                          <th>Phòng</th>
                          <th>Thứ / Tiết</th>
                          <th>Tuần học</th>
                          <th style={{ textAlign: 'center' }}>Số buổi</th>
                        </tr>
                      </thead>
                      <tbody>
                        {schedules.map((item) => (
                          <tr key={item.id}>
                            <td>
                              <div style={{ fontWeight: '600' }}>{item.adminClassCode}</div>
                              {/* <div style={{ fontSize: '12px', color: 'var(--tx3)' }}>{item.adminClassName}</div> */}
                            </td>
                            <td>
                              <div style={{ fontWeight: '500' }}>{item.subjectName}</div>
                              <div style={{ fontSize: '12px', color: 'var(--tx3)' }}>{item.subjectCode}</div>
                            </td>
                            <td>
                              <div style={{ fontWeight: '500' }}>{item.lecturerName}</div>
                              <div style={{ fontSize: '12px', color: 'var(--tx3)' }}>{item.lecturerCode}</div>
                            </td>
                            <td>
                              <span style={{ background: 'var(--bg3)', padding: '4px 8px', borderRadius: '6px', fontSize: '13px', fontWeight: '600' }}>
                                {item.roomCode}
                              </span>
                            </td>
                            <td>
                              <div style={{ fontWeight: '500', color: 'var(--bl)' }}>{formatDayOfWeek(item.dayOfWeek)}</div>
                              <div style={{ fontSize: '12px', color: 'var(--tx2)' }}>Tiết {item.periodStart} - {item.periodEnd}</div>
                            </td>
                            <td>
                              {item.weekStart} - {item.weekEnd}
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <span style={{ fontWeight: '600', color: 'var(--gr)' }}>{item.totalSessions}</span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  
                  {/* Pagination */}
                  <div style={{ padding: '14px', borderTop: '1px solid var(--bd)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12px' }}>
                    <span style={{ color: 'var(--tx3)' }}>
                      Hiển thị trang {currentPage + 1} / {totalPages} (Tổng số {totalElements} dòng)
                    </span>
                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button 
                        className="btn btn-s btn-sm" 
                        disabled={currentPage === 0}
                        onClick={() => handlePageChange(currentPage - 1)}
                      >
                        Trước
                      </button>
                      
                      {/* Simple pagination logic for demo */}
                      {Array.from({ length: Math.min(5, totalPages) }).map((_, idx) => {
                        let pageNum = currentPage - 2 + idx;
                        if (currentPage < 2) pageNum = idx;
                        if (currentPage > totalPages - 3) pageNum = totalPages - 5 + idx;
                        if (pageNum < 0 || pageNum >= totalPages) return null;
                        
                        return (
                          <button 
                            key={pageNum}
                            className={`btn btn-sm ${currentPage === pageNum ? 'btn-p' : 'btn-s'}`}
                            style={currentPage === pageNum ? { background: 'var(--bg3)', color: 'var(--tx)', border: '1px solid var(--bl)' } : {}}
                            onClick={() => handlePageChange(pageNum)}
                          >
                            {pageNum + 1}
                          </button>
                        );
                      })}
                      
                      <button 
                        className="btn btn-s btn-sm" 
                        disabled={currentPage === totalPages - 1 || totalPages === 0}
                        onClick={() => handlePageChange(currentPage + 1)}
                      >
                        Sau
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}
        </>
      )}

      {/* ======================== */}
      {/* VIEW: IMPORT             */}
      {/* ======================== */}
      {view === 'import' && (
        <>
          {/* STEP 1: UPLOAD */}
          {step === 'upload' && (
            <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
              <div className="card-h">
                <div className="card-t">Tải lên file TKB</div>
                <button className="btn btn-s btn-sm" onClick={downloadTemplate}>
                  <Download size={14} style={{ marginRight: '4px' }} /> Tải file mẫu
                </button>
              </div>
              <div style={{ padding: '30px' }}>
                <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleFileChange}
                    accept=".xlsx, .xls, .csv"
                    style={{ display: 'none' }}
                />
                <div className="upz" onClick={handleImportClick} style={{ padding: '50px 30px' }}>
                  <FileSpreadsheet size={48} color="var(--bl)" style={{ marginBottom: '16px' }} />
                  <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '8px' }}>Kéo thả file Excel / CSV vào đây</div>
                  <div style={{ fontSize: '13px', color: 'var(--tx3)', marginBottom: '24px' }}>hoặc nhấn để chọn file từ máy tính của bạn</div>
                  <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '11px', background: 'var(--bg3)', padding: '6px 12px', borderRadius: '20px', color: 'var(--tx2)' }}>
                    <CheckCircle2 size={12} color="var(--gr)" /> Hỗ trợ .xlsx, .csv (Tối đa 10MB)
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* STEP 2 & 4: ANALYZING / IMPORTING (PROGRESS) */}
          {(step === 'analyzing' || step === 'importing') && (
            <div className="card" style={{ maxWidth: '500px', margin: '40px auto', padding: '40px 30px', textAlign: 'center' }}>
              <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: step === 'analyzing' ? 'var(--blL)' : 'var(--puL)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', border: `1px solid ${step === 'analyzing' ? 'var(--bl)' : 'var(--pu)'}` }}>
                {step === 'analyzing' ? (
                  <RefreshCw size={36} color="var(--bl)" className="pulse" />
                ) : (
                  <CalendarClock size={36} color="var(--pu)" className="pulse" />
                )}
              </div>
              <h3 style={{ fontSize: '18px', marginBottom: '8px' }}>
                {step === 'analyzing' ? 'Đang phân tích dữ liệu' : 'Đang xử lý & Sinh lịch học'}
              </h3>
              <p style={{ fontSize: '13px', color: 'var(--tx3)', marginBottom: '30px', height: '20px' }}>{statusText}</p>
              
              <div style={{ background: 'var(--bg3)', height: '8px', borderRadius: '4px', overflow: 'hidden', width: '100%' }}>
                <div style={{ 
                  height: '100%', 
                  background: step === 'analyzing' ? 'linear-gradient(90deg, var(--bl2), var(--bl))' : 'linear-gradient(90deg, var(--pu), #d946ef)', 
                  width: `${importProgress}%`,
                  transition: 'width 0.3s ease-out'
                }}></div>
              </div>
              <div style={{ fontSize: '12px', color: 'var(--tx2)', marginTop: '12px', fontWeight: '600' }}>{Math.round(importProgress)}%</div>
            </div>
          )}

          {/* STEP 3: PREVIEW */}
          {step === 'preview' && (
            <>
              <div className="sg">
                <div className="sc bl">
                  <div className="sc-lb">Tổng dòng dữ liệu</div>
                  <div className="sc-vl bl">{previewData.length}</div>
                  <div className="sc-su">Đọc từ file Excel</div>
                  <FileSpreadsheet className="sc-ic" color="var(--bl)" />
                </div>
                <div className="sc gr">
                  <div className="sc-lb">Dòng hợp lệ</div>
                  <div className="sc-vl gr">{previewData.filter(d => d.valid).length}</div>
                  <div className="sc-su">Đủ thông tin & ánh xạ thành công</div>
                  <CheckCircle2 className="sc-ic" color="var(--gr)" />
                </div>
                <div className="sc rd">
                  <div className="sc-lb">Dòng có lỗi</div>
                  <div className="sc-vl" style={{ color: 'var(--rd)' }}>{previewData.filter(d => !d.valid).length}</div>
                  <div className="sc-su">Bị thiếu/sai dữ liệu tham chiếu</div>
                  <AlertTriangle className="sc-ic" color="var(--rd)" />
                </div>
                <div className="sc pu">
                  <div className="sc-lb">Ước tính buổi học</div>
                  <div className="sc-vl pu">
                    ~{previewData.filter(d => d.valid).reduce((acc, curr) => acc + (curr.estimatedSessions || 0), 0).toLocaleString()}
                  </div>
                  <div className="sc-su">Sẽ được tự động sinh</div>
                  <CalendarClock className="sc-ic" color="var(--pu)" />
                </div>
              </div>

              <div className="card">
                <div className="card-h">
                  <div>
                    <div className="card-t">Bảng xem trước dữ liệu (Preview)</div>
                    <div className="card-su">Kiểm tra các dòng lỗi (màu đỏ). Chúng sẽ bị bỏ qua khi Import.</div>
                  </div>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button className="btn btn-s" onClick={() => setStep('upload')}>Hủy & Upload lại</button>
                    <button 
                      className="btn btn-p" 
                      onClick={startImport} 
                      disabled={previewData.filter(d => d.valid).length === 0}
                      style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                    >
                      Xác nhận nhập <ChevronRight size={14} />
                    </button>
                  </div>
                </div>
                <div style={{ overflowX: 'auto', maxHeight: '500px' }}>
                  <table className="tbl">
                    <thead style={{ position: 'sticky', top: 0, background: 'var(--bg)', zIndex: 1 }}>
                      <tr>
                        <th style={{ width: '40px' }}>Dòng</th>
                        <th>Lớp hành chính</th>
                        <th>Môn học</th>
                        <th>Giảng viên</th>
                        <th>Phòng</th>
                        <th>Thứ</th>
                        <th>Tiết</th>
                        <th>Tuần</th>
                        <th>Dự kiến</th>
                        <th style={{ width: '250px' }}>Trạng thái</th>
                      </tr>
                    </thead>
                    <tbody>
                      {previewData.map((row, idx) => (
                        <tr key={idx} style={{ background: !row.valid ? 'rgba(239,68,68,.04)' : '' }}>
                          <td style={{ color: 'var(--tx3)' }}>{row.schedule.rowIndex}</td>
                          <td>
                            <div style={{ fontWeight: '500', color: !row.valid && row.errors?.some(e => e.includes('Lớp')) ? 'var(--rd)' : 'var(--tx)' }}>
                              {row.adminClassName || row.schedule.adminClassCode}
                            </div>
                            {!row.adminClassName && <div style={{ fontSize: '11px', color: 'var(--rd)' }}>Không tìm thấy mã</div>}
                          </td>
                          <td>
                            <div style={{ fontWeight: '500', color: !row.valid && row.errors?.some(e => e.includes('Môn')) ? 'var(--rd)' : 'var(--tx)' }}>
                              {row.subjectName || row.schedule.subjectCode}
                            </div>
                          </td>
                          <td>
                            <div style={{ fontWeight: '500', color: !row.valid && row.errors?.some(e => e.includes('Giảng viên')) ? 'var(--rd)' : 'var(--tx)' }}>
                              {row.lecturerName || row.schedule.lecturerCode}
                            </div>
                          </td>
                          <td>
                            <div style={{ fontWeight: '500', color: !row.valid && row.errors?.some(e => e.includes('Phòng')) ? 'var(--rd)' : 'var(--tx)' }}>
                              {row.schedule.roomCode}
                            </div>
                          </td>
                          <td>{row.schedule.dayOfWeek}</td>
                          <td>{row.schedule.periodStart}-{row.schedule.periodEnd}</td>
                          <td>{row.schedule.weekStart}-{row.schedule.weekEnd}</td>
                          <td style={{ fontWeight: '600', color: row.valid ? 'var(--gr)' : 'var(--rd)' }}>
                            {row.estimatedSessions || '-'}
                          </td>
                          <td>
                            {row.valid ? (
                              <span className="bdg b-op">Hợp lệ</span>
                            ) : (
                              <div style={{ color: 'var(--rd)', fontSize: '12px' }}>
                                {row.errors?.map((err, i) => (
                                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '4px', marginBottom: '4px' }}>
                                    <AlertTriangle size={12} /> {err}
                                  </div>
                                ))}
                              </div>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div style={{ padding: '12px 16px', background: 'var(--bg3)', borderTop: '1px solid var(--bd)', fontSize: '12px', color: 'var(--tx3)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Info size={14} /> Các dòng dữ liệu báo lỗi sẽ tự động bị bỏ qua trong quá trình import.
                </div>
              </div>
            </>
          )}

          {/* STEP 5: SUCCESS */}
          {step === 'success' && importStats && (
            <div className="card" style={{ maxWidth: '600px', margin: '40px auto', padding: '50px 30px', textAlign: 'center' }}>
              <div style={{ width: '90px', height: '90px', borderRadius: '50%', background: 'linear-gradient(135deg, rgba(34,197,94,0.1), rgba(34,197,94,0.05))', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', border: '1px solid rgba(34,197,94,0.3)', boxShadow: '0 0 40px rgba(34,197,94,0.1)' }}>
                <CheckCircle2 size={48} color="var(--gr)" />
              </div>
              <h2 style={{ fontSize: '24px', marginBottom: '12px', color: 'var(--tx)' }}>Nhập Thời Khóa Biểu Thành Công!</h2>
              <p style={{ fontSize: '14px', color: 'var(--tx2)', marginBottom: '30px', lineHeight: '1.6' }}>
                Hệ thống đã lưu <strong>{importStats.totalSaved}</strong> dòng TKB hợp lệ và tự động sinh <strong>{importStats.totalSessionsCreated?.toLocaleString() || 0}</strong> buổi học chi tiết.
              </p>
              
              <div style={{ background: 'var(--bg3)', padding: '20px', borderRadius: '12px', border: '1px solid var(--bd)', display: 'inline-block', textAlign: 'left', marginBottom: '40px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                  <CalendarClock size={16} color="var(--pu)" /> 
                  <span style={{ fontSize: '13px' }}>Trạng thái: <strong>Đã lên lịch</strong> cho tất cả sinh viên & giảng viên.</span>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '14px', justifyContent: 'center' }}>
                <button className="btn btn-p" onClick={closeImport} style={{ padding: '10px 20px', fontSize: '13px' }}>Xem danh sách Thời khóa biểu</button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
