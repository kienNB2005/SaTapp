import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import api from '../utils/api';

// ─── Helpers ──────────────────────────────────────────────────────────────────
const fmtTime = iso =>
  iso ? new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : null;

function StatusBadge({ status, isLate, leftEarly, checkedOutAt }) {
  if (status === 'absent') return <Badge color="#EF4444" bg="#FEE2E2" label="Vắng" />;
  if (status === 'excused') return <Badge color="#8B5CF6" bg="#EDE9FE" label="Có phép" />;
  if (leftEarly) return <Badge color="#F97316" bg="#FFEDD5" label="Về sớm" />;
  if (checkedOutAt) return <Badge color="#10B981" bg="#D1FAE5" label="Check-out ✓" />;
  if (isLate) return <Badge color="#F59E0B" bg="#FEF3C7" label="Muộn" />;
  return <Badge color="#10B981" bg="#D1FAE5" label="Đúng giờ" />;
}
const Badge = ({ color, bg, label }) => (
  <span style={{
    fontSize: 10, fontWeight: 700, padding: '3px 9px',
    borderRadius: 99, color, background: bg, whiteSpace: 'nowrap',
    letterSpacing: '0.02em',
  }}>{label}</span>
);

// Map error code → thông báo tiếng Việt thân thiện
function friendlyError(err) {
  const code = err?.response?.data?.code || '';
  const msg = err?.response?.data?.message || err?.message || '';
  const map = {
    CLASS_SESSION_NOT_FOUND: 'Không tìm thấy buổi học.',
    SESSION_ALREADY_OPEN: 'Buổi học đã được mở trước đó.',
    INVALID_SESSION_STATUS: 'Trạng thái buổi học không hợp lệ.',
    NO_PERMISSION_ON_SESSION: 'Bạn không có quyền thao tác trên buổi học này.',
    CHECKOUT_ALREADY_ACTIVE: 'Check-out đã được kích hoạt rồi.',
    QR_TOKEN_EXPIRED: 'Mã QR đã hết hạn, đang làm mới…',
    SESSION_NOT_OPEN: 'Buổi học chưa được mở.',
  };
  return map[code] || map[msg] || msg || 'Có lỗi xảy ra, vui lòng thử lại.';
}

// ─── Main ─────────────────────────────────────────────────────────────────────
export default function QR() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get('sessionId');

  const [session, setSession] = useState(null);
  const [sessionLoading, setSessionLoading] = useState(true);
  const [qrData, setQrData] = useState(null);
  const [qrType, setQrType] = useState('CHECK_IN');
  const [timerVal, setTimerVal] = useState(60);
  const [qrLoading, setQrLoading] = useState(false);
  const [attendances, setAttendances] = useState([]);
  const [sessionClosed, setSessionClosed] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [checkoutMins, setCheckoutMins] = useState(5);
  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const [checkoutActive, setCheckoutActive] = useState(false);
  const [isLocked, setIsLocked] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [openError, setOpenError] = useState(null); // lỗi khi mở session

  const sseRef = useRef(null);
  const timerRef = useRef(null);

  const toast = useCallback((msg, type = 'info') => {
    const id = Date.now();
    setToasts(p => [...p, { id, msg, type }]);
    setTimeout(() => setToasts(p => p.filter(t => t.id !== id)), 3500);
  }, []);

  const startTimer = useCallback((expiresAt) => {
    clearInterval(timerRef.current);
    if (!expiresAt) return;
    const tick = () => setTimerVal(Math.max(0, Math.round((new Date(expiresAt) - Date.now()) / 1000)));
    tick();
    timerRef.current = setInterval(tick, 1000);
  }, []);

  const applyQrResponse = useCallback((res) => {
    setQrData(res.qrCodeData);
    setQrType(res.type);
    if (res.type === 'CHECK_IN') setCheckoutActive(false);
    else if (res.type === 'CHECK_OUT') setCheckoutActive(true);
    startTimer(res.qrExpiresAt);
  }, [startTimer]);

  useEffect(() => {
    if (timerVal === 0 && !sessionClosed && qrData && !isLocked) handleRefreshQr();
    // eslint-disable-next-line
  }, [timerVal, isLocked]);

  const fetchDetail = useCallback(async () => {
    try {
      setSessionLoading(true);
      const res = await api.get(`/api/v1/sessions/${sessionId}`);
      const detail = res.data.result;
      setSession(detail);
      if (detail.status === 'closed') setSessionClosed(true);
      if (detail.status === 'open' && detail.qrCodeData) {
        setQrData(detail.qrCodeData);
        setQrType(detail.qrType ?? 'CHECK_IN');
        startTimer(detail.qrExpiresAt);
        if (detail.qrType === 'CHECK_OUT') setCheckoutActive(true);
      }
    } catch (e) {
      toast(e.response?.data?.message || e.message, 'error');
    } finally {
      setSessionLoading(false);
    }
  }, [sessionId, startTimer, toast]);

  useEffect(() => {
    if (!sessionId) return;
    fetchDetail();

    // Gọi open ngay — server tự xử lý idempotent nếu đã open rồi
    setQrLoading(true);
    api.patch(`/api/v1/sessions/${sessionId}/status`, { status: "OPEN" })
      .then(res => { applyQrResponse(res.data.result); fetchDetail(); })
      .catch(err => {
        const msg = friendlyError(err);
        // Nếu session đã open rồi thì fetchDetail lấy QR hiện tại, không cần báo lỗi
        if (err?.response?.data?.code === 'SESSION_ALREADY_OPEN') {
          fetchDetail();
        } else {
          setOpenError(msg);
        }
      })
      .finally(() => {
        setQrLoading(false);

        // CHỈ kêt nối SSE SAU KHI api.post('/open') đã hoàn tất
        // để đảm bảo backend đã tạo xong danh sách sinh viên mặc định
        const token = localStorage.getItem('accessToken');
        const baseUrl = api.defaults.baseURL || '';
        const sseUrl = `${baseUrl}/api/v1/sessions/${sessionId}/attendances/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`;
        const sse = new EventSource(sseUrl);
        sseRef.current = sse;

        sse.addEventListener('snapshot', e => setAttendances(JSON.parse(e.data)));
        sse.addEventListener('attendance-update', e => {
          const updated = JSON.parse(e.data);
          setAttendances(prev => {
            const idx = prev.findIndex(a => a.attendanceId === updated.attendanceId);
            if (idx >= 0) { const n = [...prev]; n[idx] = updated; return n; }
            return [updated, ...prev];
          });
        });
        sse.addEventListener('left-early-update', e => {
          setAttendances(JSON.parse(e.data));
          toast('Đã đóng Check-out. Hệ thống tự động khóa Check-in!', 'warning');
          setCheckoutActive(false);
          setIsLocked(true);
          clearInterval(timerRef.current);
        });
        sse.addEventListener('session-closed', () => { setSessionClosed(true); sse.close(); });
      });

    return () => {
      if (sseRef.current) sseRef.current.close();
      clearInterval(timerRef.current);
    };
    // eslint-disable-next-line
  }, [sessionId]);

  const handleRefreshQr = async () => {
    try {
      setQrLoading(true);
      const res = await api.post(`/api/v1/sessions/${sessionId}/qr/refresh`);
      applyQrResponse(res.data.result);
    } catch (e) {
      toast(friendlyError(e), 'error');
    } finally {
      setQrLoading(false);
    }
  };

  const handleStartCheckout = async () => {
    try {
      setCheckoutLoading(true);
      const res = await api.patch(`/api/v1/sessions/${sessionId}/status`, { status: "CHECKING_OUT", checkoutMinutes: checkoutMins });
      applyQrResponse(res.data.result);
      setCheckoutActive(true);
      setModalOpen(false);
      setIsLocked(false);
      toast('Đã kích hoạt Check-out!', 'success');
    } catch (e) {
      toast(friendlyError(e), 'error');
    } finally {
      setCheckoutLoading(false);
    }
  };

  const handleCloseSession = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn kết thúc buổi học này không?')) return;
    try {
      await api.patch(`/api/v1/sessions/${sessionId}/status`, { status: "CLOSED" });
      toast('Đã kết thúc buổi học!', 'success');
      setTimeout(() => navigate(`/sessions/${sessionId}/attendances`), 1200);
    } catch (e) {
      toast(friendlyError(e), 'error');
    }
  };

  const handleToggleLock = () => {
    const willLock = !isLocked;
    setIsLocked(willLock);
    if (willLock) {
      clearInterval(timerRef.current);
      toast('Đã khóa QR điểm danh.', 'warning');
    } else {
      handleRefreshQr();
      toast('Đã mở lại điểm danh!', 'success');
    }
  };

  // ── Stats ──────────────────────────────────────────────────────────────────
  const present = attendances.filter(a => a.status === 'present');
  const onTime = present.filter(a => !a.isLate).length;
  const lateCount = present.filter(a => a.isLate).length;
  const absent = attendances.filter(a => a.status === 'absent').length;
  const total = attendances.length;

  const pct = Math.min(timerVal / 60, 1);
  const circ = 2 * Math.PI * 26;
  const offset = circ * (1 - pct);
  const timerColor = pct > 0.4 ? '#10B981' : pct > 0.2 ? '#F59E0B' : '#EF4444';
  const isCheckout = qrType === 'CHECK_OUT';

  // ── Toast colors ───────────────────────────────────────────────────────────
  const toastBg = { error: '#EF4444', success: '#10B981', warning: '#F59E0B', info: '#3B82F6' };

  return (
    <>
      <style>{`
        * { box-sizing: border-box; }
        .qr-root {
          display: flex; flex-direction: column;
          height: 100%; padding: 24px 28px 20px;
          font-family: 'Inter', system-ui, sans-serif;
          background: #F8FAFC;
          color: #0F172A;
        }
        /* ── Header ── */
        .qr-header { margin-bottom: 20px; }
        .qr-header-title {
          font-size: 20px; font-weight: 800; color: #0F172A;
          display: flex; align-items: center; gap: 10px; margin-bottom: 5px;
        }
        .qr-header-sub { font-size: 13px; color: #64748B; line-height: 1.5; }
        .badge-closed {
          font-size: 10px; font-weight: 700; padding: 3px 10px;
          background: #FEE2E2; color: #EF4444;
          border-radius: 99px; letter-spacing: 0.03em;
        }
        /* ── Body ── */
        .qr-body {
          flex: 1; min-height: 0;
          display: grid;
          grid-template-columns: 380px 1fr;
          gap: 20px;
        }
        /* ── Left panel ── */
        .qr-left {
          background: white;
          border-radius: 16px;
          border: 1px solid #E2E8F0;
          padding: 28px 24px 20px;
          display: flex; flex-direction: column;
          align-items: center; gap: 0;
          box-shadow: 0 1px 4px rgba(0,0,0,0.04);
          overflow-y: auto;
        }
        /* Mode badge */
        .mode-badge {
          font-size: 11px; font-weight: 700;
          padding: 5px 16px; border-radius: 99px;
          letter-spacing: 0.04em;
          margin-bottom: 22px;
        }
        .mode-checkin  { background: #ECFDF5; color: #059669; border: 1px solid #A7F3D0; }
        .mode-checkout { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
        /* QR area */
        .qr-area {
          width: 240px; height: 240px;
          display: flex; align-items: center; justify-content: center;
          margin-bottom: 20px;
          position: relative;
        }
        .qr-inner {
          background: white; padding: 14px; border-radius: 14px;
          box-shadow: 0 0 0 1px #E2E8F0, 0 4px 20px rgba(0,0,0,0.07);
        }
        .qr-locked {
          width: 240px; height: 240px; border-radius: 14px;
          background: #F1F5F9; border: 2px dashed #CBD5E1;
          display: flex; flex-direction: column;
          align-items: center; justify-content: center;
          gap: 10px; color: #64748B;
        }
        .qr-locked-icon { font-size: 44px; }
        .qr-locked-text { font-size: 13px; font-weight: 600; text-align: center; }
        .qr-overlay {
          position: absolute; inset: 0; border-radius: 14px;
          background: rgba(255,255,255,0.9);
          display: flex; align-items: center; justify-content: center;
          font-size: 12px; font-weight: 600; color: #64748B; z-index: 2;
        }
        /* Timer */
        .timer-row {
          display: flex; align-items: center; gap: 14px;
          width: 100%; padding: 14px 16px;
          background: #F8FAFC; border-radius: 12px;
          margin-bottom: 16px;
          border: 1px solid #E2E8F0;
        }
        .timer-ring { position: relative; flex-shrink: 0; cursor: pointer; }
        .timer-ring:hover { opacity: 0.8; }
        .timer-num {
          position: absolute; inset: 0;
          display: flex; align-items: center; justify-content: center;
          font-size: 13px; font-weight: 800; font-family: 'Inter', monospace;
        }
        .timer-info-label { font-size: 13px; font-weight: 600; color: #0F172A; }
        .timer-info-sub   { font-size: 11px; color: #94A3B8; margin-top: 2px; }
        /* Status pill */
        .status-pill {
          display: flex; align-items: center; gap: 8px;
          padding: 9px 16px; border-radius: 10px;
          font-size: 12px; font-weight: 600;
          width: 100%; margin-bottom: 20px;
        }
        .status-dot {
          width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0;
        }
        .status-active  { background: #ECFDF5; color: #059669; }
        .status-checkout{ background: #FFFBEB; color: #D97706; }
        .status-locked  { background: #F1F5F9; color: #64748B; }
        .status-closed  { background: #FEF2F2; color: #DC2626; }
        /* Action buttons */
        .qr-actions { display: flex; flex-direction: column; gap: 8px; width: 100%; margin-top: auto; }
        .btn-action {
          padding: 10px 16px; border-radius: 10px;
          font-size: 12px; font-weight: 700;
          border: none; cursor: pointer;
          width: 100%; text-align: center;
          letter-spacing: 0.02em;
          transition: opacity 0.15s, transform 0.1s;
        }
        .btn-action:hover:not(:disabled) { opacity: 0.88; transform: translateY(-1px); }
        .btn-action:disabled { opacity: 0.38; cursor: not-allowed; }
        .btn-lock     { background: #F59E0B; color: #fff; }
        .btn-unlock   { background: #10B981; color: #fff; }
        .btn-checkout { background: #0EA5E9; color: #fff; }
        .btn-close    { background: white; color: #EF4444; border: 1.5px solid #FECACA; }
        .btn-row      { display: flex; gap: 8px; }
        .btn-row .btn-action { flex: 1; }
        /* ── Right panel ── */
        .qr-right {
          background: white; border-radius: 16px;
          border: 1px solid #E2E8F0;
          display: flex; flex-direction: column;
          overflow: hidden;
          box-shadow: 0 1px 4px rgba(0,0,0,0.04);
        }
        /* Stats bar */
        .att-stats {
          display: grid; grid-template-columns: repeat(4, 1fr);
          border-bottom: 1px solid #F1F5F9;
          flex-shrink: 0;
        }
        .stat-cell {
          padding: 16px 20px; border-right: 1px solid #F1F5F9;
          display: flex; flex-direction: column; gap: 3px;
        }
        .stat-cell:last-child { border-right: none; }
        .stat-label { font-size: 11px; color: #94A3B8; font-weight: 500; }
        .stat-value { font-size: 22px; font-weight: 800; line-height: 1; }
        .stat-sub   { font-size: 11px; color: #94A3B8; }
        /* Student list */
        .att-list-header {
          padding: 14px 20px 10px;
          font-size: 12px; font-weight: 700; color: #64748B;
          letter-spacing: 0.05em; text-transform: uppercase;
          border-bottom: 1px solid #F1F5F9;
          flex-shrink: 0;
        }
        .att-list { flex: 1; overflow-y: auto; }
        .att-empty {
          display: flex; flex-direction: column;
          align-items: center; justify-content: center;
          height: 200px; gap: 10px; color: #94A3B8;
        }
        .att-empty-icon { font-size: 28px; opacity: 0.5; }
        .att-empty-text { font-size: 13px; text-align: center; line-height: 1.6; }
        .att-row {
          display: flex; align-items: center; gap: 12px;
          padding: 10px 20px;
          border-bottom: 1px solid #F8FAFC;
          transition: background 0.1s;
        }
        .att-row:hover { background: #FAFAFA; }
        .att-av {
          width: 36px; height: 36px; border-radius: 10px;
          display: flex; align-items: center; justify-content: center;
          font-size: 14px; font-weight: 700; flex-shrink: 0;
        }
        .att-name { font-size: 13px; font-weight: 600; color: #0F172A; }
        .att-code { font-size: 11px; color: #94A3B8; margin-top: 1px; font-family: 'Roboto Mono', monospace; }
        .att-late-note { color: #F59E0B; }
        .att-time { font-size: 11px; color: #64748B; font-family: 'Roboto Mono', monospace; }
        .att-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; margin-left: auto; }
        /* Toast */
        .toast-wrap {
          position: fixed; top: 20px; right: 20px;
          z-index: 9999; display: flex; flex-direction: column; gap: 8px;
          pointer-events: none;
        }
        .toast {
          padding: 10px 16px; border-radius: 10px;
          font-size: 13px; font-weight: 600; color: white;
          box-shadow: 0 4px 20px rgba(0,0,0,0.15);
          animation: toastIn 0.2s ease; max-width: 320px;
          pointer-events: all;
        }
        /* Modal */
        .mo-overlay {
          position: fixed; inset: 0; z-index: 1000;
          background: rgba(15,23,42,0.4);
          display: flex; align-items: center; justify-content: center;
          opacity: 0; pointer-events: none; transition: opacity 0.2s;
        }
        .mo-overlay.open { opacity: 1; pointer-events: all; }
        .mo {
          background: white; border-radius: 16px;
          width: 400px; padding: 28px;
          box-shadow: 0 20px 60px rgba(0,0,0,0.15);
          transform: translateY(8px); transition: transform 0.2s;
        }
        .mo-overlay.open .mo { transform: none; }
        .mo-title { font-size: 17px; font-weight: 800; margin-bottom: 8px; }
        .mo-sub { font-size: 13px; color: #64748B; margin-bottom: 16px; line-height: 1.6; }
        .mo-warn {
          background: #FFFBEB; border: 1px solid #FDE68A;
          border-radius: 8px; padding: 10px 12px;
          font-size: 12px; color: #92400E; margin-bottom: 16px;
        }
        .mo-field label { font-size: 12px; font-weight: 600; color: #374151; display: block; margin-bottom: 6px; }
        .mo-field input {
          width: 100%; padding: 9px 12px; border-radius: 8px;
          border: 1.5px solid #E2E8F0; font-size: 14px; outline: none;
          transition: border-color 0.15s;
        }
        .mo-field input:focus { border-color: #0EA5E9; }
        .mo-info {
          background: #F8FAFC; border: 1px solid #E2E8F0;
          border-radius: 8px; padding: 10px 12px;
          font-size: 12px; color: #64748B; margin: 12px 0 20px;
        }
        .mo-actions { display: flex; gap: 8px; }
        .btn-mo {
          flex: 1; padding: 10px; border-radius: 10px;
          font-size: 13px; font-weight: 700; border: none; cursor: pointer;
          transition: opacity 0.15s;
        }
        .btn-mo:hover { opacity: 0.85; }
        .btn-mo-cancel { background: #F1F5F9; color: #475569; }
        .btn-mo-confirm { background: #0EA5E9; color: white; }
        @keyframes toastIn {
          from { opacity: 0; transform: translateY(-6px); }
          to   { opacity: 1; transform: none; }
        }
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.4; }
        }
        .pulse-dot { animation: pulse 2s infinite; }
        @media (max-width: 900px) {
          .qr-body { grid-template-columns: 1fr; }
          .att-stats { grid-template-columns: repeat(2,1fr); }
        }
      `}</style>

      {/* Toasts */}
      <div className="toast-wrap">
        {toasts.map(t => (
          <div key={t.id} className="toast" style={{ background: toastBg[t.type] || toastBg.info }}>
            {t.msg}
          </div>
        ))}
      </div>

      <div className="qr-root">
        {/* Header */}
        <div className="qr-header">
          {sessionLoading ? (
            <div style={{ fontSize: 14, color: '#94A3B8' }}>Đang tải thông tin buổi học…</div>
          ) : session ? (
            <>
              <div className="qr-header-title">
                QR Điểm danh · Buổi {session.sessionNumber}/{session.totalSessions}
                {sessionClosed && <span className="badge-closed">Đã kết thúc</span>}
              </div>
              <div className="qr-header-sub">
                {session.subjectName} ({session.subjectCode})
                &nbsp;·&nbsp;Lớp {session.className}
                &nbsp;·&nbsp;Tiết {session.periodStart}–{session.periodEnd}
                {session.periodStartTime && ` (${String(session.periodStartTime).slice(0, 5)}–${String(session.periodEndTime || '').slice(0, 5)})`}
                &nbsp;·&nbsp;Phòng {session.roomCode}{session.building ? ` - ${session.building}` : ''}
              </div>
            </>
          ) : (
            <div style={{ color: '#EF4444', fontSize: 14 }}>Không tìm thấy thông tin buổi học</div>
          )}
        </div>

        {/* Body */}
        <div className="qr-body">
          {/* ── Left: QR panel ── */}
          <div className="qr-left">
            {/* Mode badge */}
            <div className={`mode-badge ${isCheckout ? 'mode-checkout' : 'mode-checkin'}`}>
              {isCheckout ? '⚡ Chế độ CHECK-OUT' : '✅ Chế độ CHECK-IN'}
            </div>

            {/* QR / Locked */}
            <div className="qr-area">
              {isLocked ? (
                <div className="qr-locked">
                  <div className="qr-locked-icon">🔒</div>
                  <div className="qr-locked-text">Điểm danh<br />đang tạm khóa</div>
                </div>
              ) : (
                <>
                  {qrLoading && <div className="qr-overlay">Đang làm mới QR…</div>}
                  <div className="qr-inner">
                    {qrData
                      ? <QRCodeSVG value={qrData} size={212} level="M" />
                      : <div style={{ width: 212, height: 212, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94A3B8', fontSize: 13 }}>Chưa có mã QR</div>
                    }
                  </div>
                </>
              )}
            </div>

            {/* Timer */}
            <div className="timer-row">
              <div className="timer-ring"
                style={{ opacity: isLocked ? 0.3 : 1, cursor: isLocked ? 'not-allowed' : 'pointer' }}
                onClick={() => { if (!isLocked) handleRefreshQr(); }}
                title={isLocked ? 'Đang khóa' : 'Nhấn để làm mới ngay'}
              >
                <svg width="60" height="60" viewBox="0 0 60 60">
                  <circle cx="30" cy="30" r="26" fill="none" stroke="#F1F5F9" strokeWidth="4" />
                  <circle cx="30" cy="30" r="26" fill="none" stroke={timerColor} strokeWidth="4"
                    strokeDasharray={circ} strokeDashoffset={offset} strokeLinecap="round"
                    style={{ transform: 'rotate(-90deg)', transformOrigin: '30px 30px', transition: 'stroke-dashoffset 1s linear' }} />
                </svg>
                <div className="timer-num" style={{ color: timerColor }}>{isLocked ? '—' : timerVal}</div>
              </div>
              <div>
                <div className="timer-info-label">
                  {isLocked ? 'Tạm dừng' : <>Làm mới sau <span style={{ color: timerColor }}>{timerVal}s</span></>}
                </div>
                <div className="timer-info-sub">
                  {isLocked ? 'Mở khóa để tiếp tục' : 'Click vòng tròn để refresh ngay'}
                </div>
              </div>
            </div>

            {/* Status */}
            <div className={`status-pill ${sessionClosed ? 'status-closed' :
              isLocked ? 'status-locked' :
                isCheckout ? 'status-checkout' : 'status-active'
              }`}>
              <div className="status-dot pulse-dot" style={{
                background: sessionClosed ? '#EF4444' : isLocked ? '#94A3B8' : isCheckout ? '#F59E0B' : '#10B981',
                animationPlayState: (sessionClosed || isLocked) ? 'paused' : 'running',
              }} />
              {sessionClosed ? 'Buổi học đã kết thúc'
                : isLocked ? 'Điểm danh đang tạm khóa'
                  : isCheckout ? 'Check-out đang mở'
                    : 'Buổi học đang diễn ra'}
            </div>

            {/* Buttons */}
            <div className="qr-actions">
              <button
                className={`btn-action ${isLocked ? 'btn-unlock' : 'btn-lock'}`}
                disabled={checkoutActive || sessionClosed}
                onClick={handleToggleLock}
              >
                {isLocked ? '🔓 Mở lại Check-in' : '🔒 Khóa Check-in'}
              </button>
              <div className="btn-row">
                <button
                  className="btn-action btn-checkout"
                  disabled={checkoutActive || sessionClosed}
                  onClick={() => setModalOpen(true)}
                >
                  ⚡ Mở Check-out
                </button>
                <button
                  className="btn-action btn-close"
                  disabled={sessionClosed}
                  onClick={handleCloseSession}
                >
                  ⏹ Kết thúc buổi
                </button>
              </div>
            </div>
          </div>

          {/* ── Right: Attendance panel ── */}
          <div className="qr-right">
            {/* Stats */}
            <div className="att-stats">
              <div className="stat-cell">
                <div className="stat-label">Có mặt</div>
                <div className="stat-value" style={{ color: '#10B981' }}>{present.length}</div>
                <div className="stat-sub">/ {total} sinh viên</div>
              </div>
              <div className="stat-cell">
                <div className="stat-label">Đúng giờ</div>
                <div className="stat-value" style={{ color: '#0EA5E9' }}>{onTime}</div>
                <div className="stat-sub">sinh viên</div>
              </div>
              <div className="stat-cell">
                <div className="stat-label">Muộn</div>
                <div className="stat-value" style={{ color: '#F59E0B' }}>{lateCount}</div>
                <div className="stat-sub">sinh viên</div>
              </div>
              <div className="stat-cell">
                <div className="stat-label">Vắng</div>
                <div className="stat-value" style={{ color: '#EF4444' }}>{absent}</div>
                <div className="stat-sub">sinh viên</div>
              </div>
            </div>

            <div className="att-list-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>Danh sách điểm danh</span>
              <button
                onClick={() => navigate(`/sessions/${sessionId}/attendances`)}
                style={{
                  background: '#F1F5F9', border: 'none', padding: '4px 10px',
                  borderRadius: '6px', fontSize: '11px', fontWeight: 600,
                  color: '#475569', cursor: 'pointer', transition: 'background 0.1s'
                }}
                onMouseOver={e => e.currentTarget.style.background = '#E2E8F0'}
                onMouseOut={e => e.currentTarget.style.background = '#F1F5F9'}
                title="Sửa điểm danh thủ công"
              >
                🖊️ Sửa thủ công
              </button>
            </div>

            <div className="att-list">
              {attendances.length === 0 ? (
                <div className="att-empty">
                  <div className="att-empty-icon">📋</div>
                  <div className="att-empty-text">
                    Chưa có sinh viên nào điểm danh.<br />
                    Danh sách cập nhật real-time khi SV quét mã.
                  </div>
                </div>
              ) : attendances.map(a => (
                <div className="att-row" key={a.attendanceId}>
                  <div className="att-av" style={{
                    background: a.status === 'absent' ? '#FEE2E2' : a.isLate ? '#FEF3C7' : '#D1FAE5',
                    color: a.status === 'absent' ? '#EF4444' : a.isLate ? '#D97706' : '#059669',
                  }}>
                    {a.fullName?.split(' ').pop()?.[0] ?? '?'}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="att-name" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {a.fullName}
                    </div>
                    <div className="att-code">
                      {a.studentCode}
                      {a.lateMinutes > 0 && <span className="att-late-note"> · Muộn {a.lateMinutes}p</span>}
                    </div>
                  </div>
                  <div className="att-right">
                    {a.scannedAt && <span className="att-time">{fmtTime(a.scannedAt)}</span>}
                    {a.checkedOutAt && (
                      <span className="att-time" style={{ color: '#10B981' }}>→ {fmtTime(a.checkedOutAt)}</span>
                    )}
                    {a.gpsVerified === false && <span title="GPS ngoài khu vực" style={{ fontSize: 13 }}>📍❌</span>}
                    <StatusBadge
                      status={a.status}
                      isLate={a.isLate}
                      leftEarly={a.leftEarly}
                      checkedOutAt={a.checkedOutAt}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* ── Lỗi mở buổi học ── */}
      {openError && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 2000,
          background: 'rgba(15,23,42,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{
            background: 'white', borderRadius: 16, width: 380, padding: 28,
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
          }}>
            <div style={{ fontSize: 32, marginBottom: 12, textAlign: 'center' }}>⚠️</div>
            <div style={{ fontSize: 16, fontWeight: 800, marginBottom: 8, textAlign: 'center' }}>
              Không thể mở buổi học
            </div>
            <div style={{
              fontSize: 13, color: '#64748B', textAlign: 'center',
              marginBottom: 20, lineHeight: 1.6,
            }}>
              {openError}
            </div>
            <button
              style={{
                width: '100%', padding: '10px', borderRadius: 10,
                background: '#EF4444', color: 'white', border: 'none',
                fontSize: 13, fontWeight: 700, cursor: 'pointer',
              }}
              onClick={() => navigate(-1)}
            >
              ← Quay lại
            </button>
          </div>
        </div>
      )}

      {/* ── Checkout Modal ── */}
      <div className={`mo-overlay ${modalOpen ? 'open' : ''}`}
        onClick={e => { if (e.target.classList.contains('mo-overlay')) setModalOpen(false); }}>
        <div className="mo" onClick={e => e.stopPropagation()}>
          <div className="mo-title">⚡ Kích hoạt Check-out</div>
          <div className="mo-sub">
            Sinh viên không quét trong thời gian cho phép sẽ bị đánh dấu <strong>về sớm</strong>.
            Buổi học vẫn tiếp tục bình thường sau khi cửa sổ check-out đóng.
          </div>
          <div className="mo-warn">
            ⚠️ SV không quét trong {checkoutMins} phút → <strong>left_early = true</strong>.
            Trạng thái vẫn là "Có mặt".
          </div>
          <div className="mo-field">
            <label>Thời gian cho SV quét (phút)</label>
            <input type="number" value={checkoutMins}
              onChange={e => setCheckoutMins(+e.target.value)} min="2" max="15" />
          </div>
          <div className="mo-info">
            {present.length} SV đã check-in · Deadline: <strong>{checkoutMins} phút</strong> kể từ lúc kích hoạt
          </div>
          <div className="mo-actions">
            <button className="btn-mo btn-mo-cancel" onClick={() => setModalOpen(false)}>Huỷ</button>
            <button className="btn-mo btn-mo-confirm"
              disabled={checkoutLoading} onClick={handleStartCheckout}
              style={{ opacity: checkoutLoading ? 0.6 : 1 }}>
              {checkoutLoading ? 'Đang kích hoạt…' : '⚡ Kích hoạt ngay'}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}