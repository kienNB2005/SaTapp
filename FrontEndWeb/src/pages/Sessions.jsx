import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../utils/api';

// Map error codes từ server sang tiếng Việt
function friendlyError(err) {
  const code = err?.response?.data?.code ?? '';
  const msg = err?.response?.data?.message ?? err?.message ?? '';
  const map = {
    CLASS_SESSION_NOT_FOUND: 'Không tìm thấy buổi học.',
    SESSION_ALREADY_OPEN: 'Buổi học đã được mở trước đó.',
    INVALID_SESSION_STATUS: 'Trạng thái buổi học không hợp lệ.',
    NO_PERMISSION_ON_SESSION: 'Bạn không có quyền thao tác trên buổi học này.',
  };
  return map[code] || map[msg] || msg || 'Có lỗi xảy ra, vui lòng thử lại.';
}

// Component hiển thị Badge trạng thái chuẩn hóa
function StatusBadge({ status }) {
  const s = (status ?? "").toLowerCase();
  const map = {
    closed: { label: 'Đã xong', cls: 'bdg b-cl' },
    open: { label: 'Đang mở', cls: 'bdg b-op' },
    scheduled: { label: 'Sắp tới', cls: 'bdg b-sc' },
    cancelled: { label: 'Đã hủy', cls: 'bdg b-ca' },
  };
  const { label, cls } = map[s] ?? { label: status, cls: 'bdg' };
  return <span className={cls}>{label}</span>;
}

// Component tính toán tỉ lệ chuyên cần: Present / Total (Present đã bao gồm Late)
function AttendanceCell({ present, late, total }) {
  if (total == null || total === 0) return <span style={{ color: 'var(--tx3)' }}>0/0</span>;

  const attended = present || 0;
  const pct = Math.round((attended / total) * 100);
  const color = pct >= 80 ? 'var(--gr)' : pct >= 60 ? 'var(--am)' : '#ef4444';

  return (
    <>
      <span style={{ color: 'var(--gr)', fontWeight: 600 }}>{attended}/{total}</span>{' '}
      <span style={{ color, fontSize: '11px' }}>({pct}%)</span>
    </>
  );
}

export default function Sessions() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initClassName = searchParams.get('className');
  const initSubjectName = searchParams.get('subjectName');

  const [classes, setClasses] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [selectedClass, setSelectedClass] = useState(null);
  const [selectedSubject, setSelectedSubject] = useState(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(null);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

  // Load danh sách lớp hành chính
  useEffect(() => {
    api.get('/sessions/filter/admin-classes')
      .then(({ data }) => {
        const list = data.result ?? data.data ?? data;
        setClasses(list);
        if (list.length > 0) {
          let found = null;
          if (initClassName) {
            found = list.find(c => c.code === initClassName || c.name === initClassName);
          }
          setSelectedClass(found || list[0]);
        }
      })
      .catch(() => setError('Không thể tải danh sách lớp.'));
  }, [initClassName]);

  // Load môn học khi đổi lớp
  useEffect(() => {
    if (!selectedClass) return;
    setSubjects([]);
    setSelectedSubject(null);
    api.get('/sessions/filter/subjects', { params: { adminClassId: selectedClass.id } })
      .then(({ data }) => {
        const list = data.result ?? data.data ?? data;
        setSubjects(list);
        if (list.length > 0) {
          let found = null;
          if (initSubjectName && (selectedClass.code === initClassName || selectedClass.name === initClassName)) {
            found = list.find(s => s.name === initSubjectName || s.code === initSubjectName);
          }
          setSelectedSubject(found || list[0]);
        }
      })
      .catch(() => setError('Không thể tải danh sách môn học.'));
  }, [selectedClass, initClassName, initSubjectName]);

  // Load danh sách buổi học
  const loadSessions = useCallback(() => {
    if (!selectedClass || !selectedSubject) return;
    setLoading(true);
    setError(null);
    api.get('/sessions/list', {
      params: { adminClassId: selectedClass.id, subjectId: selectedSubject.id }
    })
      .then(({ data }) => {
        const list = data.result ?? data.data ?? data;
        setSessions(list);
      })
      .catch((err) => setError(friendlyError(err)))
      .finally(() => setLoading(false));
  }, [selectedClass, selectedSubject, retryCount]);

  useEffect(() => { loadSessions(); }, [loadSessions]);

  // Thống kê tiến độ
  const totalSessions = sessions[0]?.totalSessions ?? sessions.length;
  const doneSessions = sessions.filter((s) => {
    const st = s.status?.toLowerCase();
    return st === 'closed' || st === 'cancelled';
  }).length;

  // Xử lý Mở buổi học
  async function handleOpen(sessionId) {
    setActionLoading(sessionId);
    try {
      await api.post(`/sessions/${sessionId}/open`);
      await loadSessions();
      navigate(`/qr?sessionId=${sessionId}`);
    } catch (err) {
      alert(friendlyError(err));
    } finally {
      setActionLoading(null);
    }
  }

  // Xử lý Đóng buổi học
  async function handleClose(sessionId) {
    if (!confirm('Bạn có chắc muốn kết thúc buổi học này?')) return;
    setActionLoading(sessionId);
    try {
      await api.post(`/sessions/${sessionId}/close`);
      loadSessions();
    } catch (err) {
      alert(friendlyError(err));
    } finally {
      setActionLoading(null);
    }
  }

  function renderActions(session) {
    const sId = session.id || session.classSessionId;
    const status = session.status?.toLowerCase();
    const busy = actionLoading === sId;
    const hasOpen = sessions.some((s) => s.status?.toLowerCase() === 'open');

    switch (status) {
      case 'closed':
      case 'cancelled':
        return (
          <button className="btn btn-s btn-sm" onClick={() => navigate(`/sessions/${sId}/attendances`)}>
            Danh sách điểm danh
          </button>
        );
      case 'open':
        return (
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-p btn-sm" onClick={() => navigate(`/qr?sessionId=${sId}`)}>
              Xem QR
            </button>
            <button
              className="btn btn-s btn-sm"
              disabled={busy}
              onClick={() => handleClose(sId)}
              style={{ color: 'var(--am)' }}
            >
              {busy ? '...' : 'Kết thúc'}
            </button>
          </div>
        );
      case 'scheduled':
      default:
        return (
          <button
            className="btn btn-p btn-sm"
            disabled={busy || hasOpen}
            style={hasOpen ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
            onClick={() => {
              if (hasOpen) {
                alert('Vui lòng kết thúc buổi học đang mở trước khi tạo mã QR mới!');
              } else {
                handleOpen(sId);
              }
            }}
          >
            {busy ? '...' : '▶ Tạo mã QR'}
          </button>
        );
    }
  }

  return (
    <div className="page active">
      {/* Filters & Stats */}
      <div style={{ display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <label style={{ fontSize: '11px', color: 'var(--tx3)', fontWeight: '600' }}>Lớp hành chính</label>
          <select
            className="fi" style={{ width: '160px' }}
            value={selectedClass?.id ?? ''}
            onChange={(e) => setSelectedClass(classes.find(c => String(c.id) === e.target.value))}
          >
            {classes.map((c) => (
              <option key={c.id} value={c.id}>{c.name} ({c.code})</option>
            ))}
          </select>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <label style={{ fontSize: '11px', color: 'var(--tx3)', fontWeight: '600' }}>Môn học</label>
          <select
            className="fi" style={{ width: '200px' }}
            value={selectedSubject?.id ?? ''}
            onChange={(e) => setSelectedSubject(subjects.find(s => String(s.id) === e.target.value))}
          >
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
          <div style={{ color: 'var(--tx3)', fontSize: '12px' }}>Tiến độ học phần</div>
          <div style={{ fontWeight: '600', fontSize: '13px' }}>
            {totalSessions} buổi · <span style={{ color: 'var(--gr)' }}>{doneSessions} đã xong</span>
          </div>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="err-banner" style={{ color: '#ef4444', marginBottom: 16 }}>
          <span>⚠️ {error}</span>
          <button onClick={() => setRetryCount(c => c + 1)}>Thử lại</button>
        </div>
      )}

      {/* Main Table */}
      <div className="card">
        {loading ? (
          <div className="empty-state">Đang tải...</div>
        ) : sessions.length === 0 ? (
          <div className="empty-state">Không có buổi học nào được tìm thấy.</div>
        ) : (
          <table className="tbl">
            <thead>
              <tr>
                <th>Buổi</th>
                <th>Ngày</th>
                <th>Tiết</th>
                <th>Phòng</th>
                <th>Sĩ số</th>
                <th>Muộn</th>
                <th>Trạng thái</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((s) => {
                const status = s.status?.toLowerCase();
                const isUpcoming = status === 'scheduled';
                const isOpen = status === 'open';

                return (
                  <tr key={s.id || s.classSessionId} style={isOpen ? { background: 'rgba(34,197,94,.03)' } : {}}>
                    <td style={{ fontWeight: 600, color: isOpen ? 'var(--gr)' : undefined }}>
                      {s.sessionNumber}/{totalSessions}
                    </td>
                    <td>
                      {s.sessionDate ? new Date(s.sessionDate).toLocaleDateString('vi-VN') : '—'}
                    </td>
                    <td>{s.periodStart}–{s.periodEnd}</td>
                    <td>{s.roomCode}</td>
                    <td>
                      {isUpcoming ? '—' : (
                        <AttendanceCell
                          present={s.presentCount}
                          late={s.lateCount}
                          total={s.totalCount || s.totalStudents}
                        />
                      )}
                    </td>
                    <td style={{ color: s.lateCount > 0 ? 'var(--am)' : 'var(--tx3)' }}>
                      {isUpcoming ? '—' : (s.lateCount ?? 0)}
                    </td>
                    <td><StatusBadge status={s.status} /></td>
                    <td style={{ textAlign: 'right' }}>{renderActions(s)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}