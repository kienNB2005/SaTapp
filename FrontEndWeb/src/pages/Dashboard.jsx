import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../utils/api";

// ─── Helpers ────────────────────────────────────────────────────────────────

function dayLabel(dow) {
  // dow: 1=Mon … 7=Sun (ISO) hoặc 2=Mon … 8=Sun tùy DB
  const map = { 1: "CN", 2: "T2", 3: "T3", 4: "T4", 5: "T5", 6: "T6", 7: "T7", 8: "CN" };
  return map[dow] ?? "?";
}

function statusBadge(status) {
  const cfg = {
    open: { label: "Đang mở", cls: "bdg b-op" },
    closed: { label: "Xong", cls: "bdg b-cl" },
    scheduled: { label: "Sắp tới", cls: "bdg b-sc" },
    cancelled: { label: "Huỷ", cls: "bdg b-ca" },
  };
  const s = (status ?? "").toLowerCase();
  const { label, cls } = cfg[s] ?? { label: status, cls: "bdg" };
  return <span className={cls}>{label}</span>;
}

function ProgressBar({ value, max, color = "var(--gr)" }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="pb">
      <div className="pf" style={{ width: `${pct}%`, background: color }} />
    </div>
  );
}

function Skeleton({ h = 14, w = "100%", radius = 6 }) {
  return (
    <div
      style={{
        height: h,
        width: w,
        borderRadius: radius,
        background: "var(--bg3)",
        animation: "pulse 1.4s ease-in-out infinite",
      }}
    />
  );
}

// ─── Sub-components ──────────────────────────────────────────────────────────

function SummaryCards({ summary, loading }) {
  if (loading) {
    return (
      <div className="sg">
        {[..."1234"].map((k) => (
          <div key={k} className="sc" style={{ gap: 8 }}>
            <Skeleton h={20} w={40} />
            <Skeleton h={32} w={60} />
            <Skeleton h={12} w={100} />
          </div>
        ))}
      </div>
    );
  }

  const s = summary ?? {};
  return (
    <div className="sg">
      <div className="sc gr">
        <div className="sc-ic">📅</div>
        <div className="sc-lb">Buổi hôm nay</div>
        <div className="sc-vl gr">{s.sessionsToday ?? 0}</div>
        <div className="sc-su">
          {s.openCount ?? 0} đang mở · {s.upcomingCount ?? 0} sắp tới
        </div>
      </div>
      <div className="sc bl">
        <div className="sc-ic">📚</div>
        <div className="sc-lb">Tuần này</div>
        <div className="sc-vl bl">{s.sessionsThisWeek ?? 0}</div>
        <div className="sc-su">{s.subjectsThisWeek ?? 0} môn khác nhau</div>
      </div>
      <div className="sc am">
        <div className="sc-ic">📊</div>
        <div className="sc-lb">Học kỳ này</div>
        <div className="sc-vl am">{s.totalSessions ?? 0}</div>
        <div className="sc-su">
          {s.closedSessions ?? 0} xong · {s.remainingSessions ?? 0} còn lại
        </div>
      </div>
      <div className="sc pu">
        <div className="sc-ic">🎯</div>
        <div className="sc-lb">Chuyên cần TB</div>
        <div className="sc-vl pu">
          {s.avgAttendanceRate != null
            ? `${Math.round(s.avgAttendanceRate)}%`
            : "—"}
        </div>
        <div className="sc-su">Toàn bộ lớp</div>
      </div>
    </div>
  );
}

// Map error codes/messages từ server sang tiếng Việt thân thiện
function friendlyError(err) {
  const msg = err?.response?.data?.message || err?.message || '';
  const code = err?.response?.data?.code || '';
  const map = {
    CLASS_SESSION_NOT_FOUND: 'Không tìm thấy buổi học.',
    SESSION_ALREADY_OPEN: 'Buổi học đã được mở trước đó.',
    INVALID_SESSION_STATUS: 'Trạng thái buổi học không hợp lệ để thực hiện thao tác này.',
    NO_PERMISSION_ON_SESSION: 'Bạn không có quyền thao tác trên buổi học này.',
    CHECKOUT_ALREADY_ACTIVE: 'Check-out đã được kích hoạt rồi.',
  };
  return map[code] || map[msg] || msg || 'Có lỗi xảy ra, vui lòng thử lại.';
}

function TodaySessions({ sessions, loading, navigate }) {
  const today = new Date().toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
  const weekday = new Date().toLocaleDateString("vi-VN", { weekday: "long" });

  return (
    <div className="card">
      <div className="card-h">
        <div>
          <div className="card-t">Buổi học hôm nay</div>
          <div className="card-su">
            {today} · {weekday}
          </div>
        </div>
      </div>
      <div style={{ padding: "8px", display: "flex", flexDirection: "column", gap: "8px" }}>
        {loading ? (
          [0, 1].map((i) => (
            <div
              key={i}
              style={{
                background: "var(--bg3)",
                borderRadius: 10,
                padding: 12,
                display: "flex",
                flexDirection: "column",
                gap: 8,
              }}
            >
              <Skeleton h={14} w="60%" />
              <Skeleton h={11} w="80%" />
              <Skeleton h={8} />
            </div>
          ))
        ) : sessions.length === 0 ? (
          <div style={{ padding: "24px 12px", textAlign: "center", color: "var(--tx3)", fontSize: 13 }}>
            Không có buổi học hôm nay 🎉
          </div>
        ) : (
          sessions.map((s) => {
            const isOpen = s.status?.toLowerCase() === "open";
            const isClosed = s.status?.toLowerCase() === "closed";
            const isCancelled = s.status?.toLowerCase() === "cancelled";
            const borderColor = isOpen ? "rgba(34,197,94,.2)" : "var(--bd)";
            const present = s.presentCount ?? 0;
            const late = s.lateCount ?? 0;
            const total = s.totalStudents ?? 0;
            const attended = present;

            return (
              <div
                key={s.classSessionId}
                style={{
                  background: "var(--bg3)",
                  border: `1px solid ${borderColor}`,
                  borderRadius: 10,
                  padding: 12,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    marginBottom: 10,
                  }}
                >
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 2 }}>
                      {s.subjectName}
                    </div>
                    <div style={{ fontSize: 11, color: "var(--tx3)" }}>
                      {s.className} · Tiết {s.periodStart}–{s.periodEnd} · {s.roomCode}
                      {s.building ? ` · ${s.building}` : ""} · Buổi{" "}
                      {s.sessionNumber ?? "?"}/{s.totalSessions ?? "?"}
                    </div>
                  </div>
                  {statusBadge(s.status)}
                </div>

                {isCancelled ? (
                  <div style={{ padding: "6px", textAlign: "center", background: "var(--bg2)", borderRadius: 6, color: "var(--tx3)", fontSize: 12, border: "1px dashed var(--bd)" }}>
                    Đã hủy buổi học
                  </div>
                ) : isOpen ? (
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <ProgressBar value={attended} max={total} />
                    <span
                      style={{
                        fontSize: 12,
                        fontWeight: 600,
                        color: "var(--gr)",
                        fontFamily: "var(--mo)",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {attended}/{total}
                    </span>
                    <button
                      className="btn btn-s btn-sm"
                      onClick={() => navigate(`/qr?sessionId=${s.classSessionId}`)}
                    >
                      Xem QR
                    </button>
                    <button
                      className="btn btn-s btn-sm"
                      style={{ background: "white", color: "var(--tx2)", border: "1px solid var(--bd)" }}
                      onClick={() => navigate(`/sessions/${s.classSessionId}/attendances`)}
                      title="Sổ điểm danh (Sửa thủ công)"
                    >
                      📋 Sửa
                    </button>
                  </div>
                ) : isClosed ? (
                  <button
                    className="btn btn-p"
                    style={{ width: "100%", fontSize: 12, background: "var(--bg3)", color: "var(--tx3)", border: "1px solid var(--bd)" }}
                    onClick={() => navigate(`/sessions/${s.classSessionId}/attendances`)}
                  >
                    📋 Xem điểm danh
                  </button>
                ) : (
                  <button
                    className="btn btn-p"
                    style={{ width: "100%", fontSize: 12 }}
                    onClick={() => navigate(`/qr?sessionId=${s.classSessionId}`)}
                  >
                    ▶ Tạo mã QR
                  </button>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

function SemesterProgress({ progress, loading, navigate }) {
  return (
    <div className="card">
      <div className="card-h">
        <div className="card-t">Tiến độ học kỳ</div>
        <div className="card-su">Nhấn vào môn để xem sổ điểm danh</div>
      </div>
      <div style={{ padding: "12px 14px", display: "flex", flexDirection: "column", gap: 14 }}>
        {loading ? (
          [0, 1, 2].map((i) => (
            <div key={i} style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              <Skeleton h={12} w="70%" />
              <Skeleton h={8} />
            </div>
          ))
        ) : progress.length === 0 ? (
          <div style={{ color: "var(--tx3)", fontSize: 13, padding: "12px 0" }}>
            Chưa có dữ liệu tiến độ.
          </div>
        ) : (
          progress.map((p) => {
            const closed = p.closedSessions ?? 0;
            const total = p.totalSessions ?? 0;

            return (
              <div
                key={p.scheduleId}
                onClick={() => navigate(`/sessions?className=${encodeURIComponent(p.className)}&subjectName=${encodeURIComponent(p.subjectName)}`)}
                style={{ cursor: "pointer" }}
                title="Xem chi tiết sổ điểm danh"
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    fontSize: 12,
                    marginBottom: 6,
                  }}
                >
                  <span style={{ fontWeight: 600, color: "var(--bl)" }}>
                    {p.subjectName}{" "}
                    <span
                      style={{
                        color: "var(--tx3)",
                        fontWeight: 500,
                        fontSize: 11,
                      }}
                    >
                      · {p.className}
                    </span>
                  </span>
                  <span style={{ color: "var(--tx3)", fontFamily: "var(--mo)" }}>
                    {closed}/{total}
                  </span>
                </div>
                <ProgressBar value={closed} max={total} />
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

function SessionDetailPopover({ session: s, onClose }) {
  const fmt = (t) => t ? String(t).slice(0, 5) : "—";
  const dateStr = s.sessionDate
    ? new Date(s.sessionDate).toLocaleDateString("vi-VN", { weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" })
    : "—";

  return (
    <div
      style={{
        position: "fixed", inset: 0, zIndex: 1000,
        display: "flex", alignItems: "center", justifyContent: "center",
        background: "rgba(0,0,0,0.35)",
      }}
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: "var(--bg2)",
          border: "1px solid var(--bd)",
          borderRadius: 14,
          padding: "20px 22px",
          width: 320,
          boxShadow: "0 8px 32px rgba(0,0,0,0.18)",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
          <div>
            <div style={{ fontSize: 15, fontWeight: 700, color: "var(--tx)", marginBottom: 2 }}>
              {s.subjectName}
            </div>
            <div style={{ fontSize: 12, color: "var(--tx3)" }}>{dateStr}</div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            {statusBadge(s.status)}
            <button
              onClick={onClose}
              style={{ background: "none", border: "none", cursor: "pointer", padding: 2, color: "var(--tx3)", fontSize: 18, lineHeight: 1 }}
            >×</button>
          </div>
        </div>

        {[
          ["🏫", "Lớp", s.className],
          ["📍", "Phòng", s.roomCode ? `${s.roomCode}${s.building ? " · " + s.building : ""}` : "—"],
          ["⏰", "Tiết", `${s.periodStart}–${s.periodEnd} (${fmt(s.periodStartTime)} – ${fmt(s.periodEndTime)})`],
          ["📋", "Buổi thứ", s.sessionNumber != null && s.totalSessions != null ? `${s.sessionNumber} / ${s.totalSessions}` : "—"],
        ].map(([icon, label, value]) => (
          <div key={label} style={{ display: "flex", gap: 10, padding: "7px 0", borderBottom: "1px solid var(--bd)" }}>
            <span style={{ fontSize: 14, width: 20, flexShrink: 0 }}>{icon}</span>
            <span style={{ fontSize: 12, color: "var(--tx3)", width: 64, flexShrink: 0 }}>{label}</span>
            <span style={{ fontSize: 12, fontWeight: 500, color: "var(--tx)" }}>{value || "—"}</span>
          </div>
        ))}

        <div style={{ marginTop: 14, fontSize: 11, color: "var(--tx3)", textAlign: "center" }}>
          Nhấn ra ngoài để đóng
        </div>
      </div>
    </div>
  );
}

function WeekSchedule({ sessions, loading }) {
  const [selected, setSelected] = useState(null);

  return (
    <div className="card">
      <div className="card-h">
        <div className="card-t">Lịch tuần này</div>
        <div className="card-su">Nhấn vào buổi để xem chi tiết</div>
      </div>
      <table className="tbl">
        <tbody>
          {loading ? (
            [0, 1, 2, 3].map((i) => (
              <tr key={i}>
                <td><Skeleton h={11} w={20} /></td>
                <td><Skeleton h={11} /></td>
                <td><Skeleton h={20} w={50} /></td>
              </tr>
            ))
          ) : sessions.length === 0 ? (
            <tr>
              <td colSpan={3} style={{ color: "var(--tx3)", fontSize: 13, padding: 12 }}>
                Không có buổi học tuần này.
              </td>
            </tr>
          ) : (
            sessions.map((s) => {
              const isToday = (() => {
                if (!s.sessionDate) return false;
                const d = new Date(s.sessionDate);
                const n = new Date();
                return d.getFullYear() === n.getFullYear() &&
                  d.getMonth() === n.getMonth() &&
                  d.getDate() === n.getDate();
              })();
              const dow = s.dayOfWeek ?? (s.sessionDate ? new Date(s.sessionDate).getDay() + 1 : 0);

              return (
                <tr
                  key={s.classSessionId}
                  onClick={() => setSelected(s)}
                  style={{ cursor: "pointer" }}
                >
                  <td style={{ color: isToday ? "var(--gr)" : "var(--tx3)", fontSize: 11, fontWeight: isToday ? 600 : 400 }}>
                    {dayLabel(dow)}
                  </td>
                  <td style={{ fontWeight: isToday ? 500 : 400 }}>
                    {s.subjectName} · {s.className} · {s.roomCode} · T.{s.periodStart}–{s.periodEnd}
                  </td>
                  <td>
                    {isToday && s.status?.toLowerCase() === "open"
                      ? <span className="bdg b-op">Hôm nay</span>
                      : statusBadge(s.status)}
                  </td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>

      {selected && (
        <SessionDetailPopover session={selected} onClose={() => setSelected(null)} />
      )}
    </div>
  );
}

// ─── Main Component ──────────────────────────────────────────────────────────

export default function Dashboard() {
  const navigate = useNavigate();

  const [semesters, setSemesters] = useState([]);   // [{id, name}] — derived from API response
  const [selectedSemesterId, setSelectedSemesterId] = useState(null);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

  // ── Fetch dashboard data ──────────────────────────────────────
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    const params = selectedSemesterId ? { semesterId: selectedSemesterId } : {};

    api.get("/lecturer/dashboard", { params, signal: controller.signal })
      .then((res) => {
        const json = res.data;
        setData(json);

        // Populate semester dropdown from the response's semesterSummary (first load only)
        if (semesters.length === 0 && json.semesterSummary?.semesterId) {
          setSemesters([
            {
              id: json.semesterSummary.semesterId,
              name: json.semesterSummary.semesterName ?? "Học kỳ hiện tại",
            },
          ]);
        }
      })
      .catch((err) => {
        if (err.name === "AbortError" || err.name === "CanceledError") return;
        setError(err.response?.data?.message || err.message || "Không thể tải dữ liệu.");
      })
      .finally(() => {
        setLoading(false);
      });

    return () => controller.abort();
  }, [selectedSemesterId, retryCount]);

  // ── Derived state ─────────────────────────────────────────────
  const summary = data?.semesterSummary ?? null;
  const todaySessions = data?.todaySessions ?? [];
  const weekSessions = data?.weekSessions ?? [];
  const progress = data?.progress ?? [];

  // ── Render ────────────────────────────────────────────────────
  return (
    <div className="page active">
      {/* Header */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 20,
        }}
      >
        <h2 style={{ fontSize: 18, fontWeight: 700, margin: 0 }}>
          Tổng quan Giảng dạy
        </h2>

        <select
          className="fi"
          style={{ width: 220 }}
          value={selectedSemesterId ?? ""}
          onChange={(e) =>
            setSelectedSemesterId(e.target.value ? Number(e.target.value) : null)
          }
        >
          <option value="">Học kỳ hiện tại</option>
          {semesters.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </select>
      </div>

      {/* Error banner */}
      {error && (
        <div
          style={{
            background: "rgba(239,68,68,.1)",
            border: "1px solid rgba(239,68,68,.3)",
            borderRadius: 8,
            padding: "10px 14px",
            marginBottom: 16,
            fontSize: 13,
            color: "var(--rd, #ef4444)",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <span>⚠️ Không thể tải dữ liệu: {error}</span>
          <button
            style={{ fontSize: 12, cursor: "pointer", background: "none", border: "none", color: "inherit", textDecoration: "underline" }}
            onClick={() => setRetryCount((c) => c + 1)}
          >
            Thử lại
          </button>
        </div>
      )}

      {/* 4 summary cards */}
      <SummaryCards summary={summary} loading={loading} />

      {/* Main 2-col grid */}
      <div className="g2">
        {/* Left column */}
        <div>
          <TodaySessions
            sessions={todaySessions}
            loading={loading}
            navigate={navigate}
          />
          <SemesterProgress
            progress={progress}
            loading={loading}
            navigate={navigate}
          />
        </div>

        {/* Right column */}
        <div>
          <WeekSchedule sessions={weekSessions} loading={loading} />
        </div>
      </div>

      {/* Pulse keyframe (inline so it works without global CSS) */}
      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: .4; }
        }
      `}</style>
    </div>
  );
}