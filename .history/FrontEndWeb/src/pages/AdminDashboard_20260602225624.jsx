// AdminDashboard.jsx
import { useState, useEffect } from 'react';
import api from '../utils/api';
import '../css/AdminDashboard.css';

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboard = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/v1/admin/dashboard');
      setData(res.data?.result || res.data);
      setError(null);
    } catch (err) {
      console.error("Lỗi khi tải dữ liệu dashboard:", err);
      setError("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const handleRequestAction = async (id, type, action) => {
    try {
      const typeParam = type.toLowerCase();
      if (action === 'approve') {
        await api.post(`/api/v1/session-requests/admin/${id}/approve?type=${typeParam}`);
      } else {
        await api.post(`/api/v1/session-requests/admin/${id}/reject?type=${typeParam}`, {
          rejectReason: "Từ chối phê duyệt từ Dashboard"
        });
      }
      // Tải lại dữ liệu sau khi duyệt thành công
      fetchDashboard();
    } catch (err) {
      console.error("Lỗi xử lý yêu cầu:", err);
      alert(err.response?.data?.message || "Không thể thực hiện phê duyệt.");
    }
  };

  if (loading && !data) {
    return (
      <div className="page active" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh', color: 'var(--tx3)' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
          <div className="pulse" style={{ width: '16px', height: '16px', background: 'var(--primary)' }}></div>
          Đang tải dữ liệu dashboard...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page active" style={{ padding: '24px', textAlign: 'center' }}>
        <div className="card" style={{ padding: '32px', border: '1px solid rgba(220,38,38,.2)', background: 'var(--rdL)' }}>
          <span style={{ fontSize: '32px' }}>⚠️</span>
          <div style={{ color: 'var(--rd)', fontWeight: '700', fontSize: '15px', marginTop: '12px' }}>{error}</div>
          <button className="btn btn-p" onClick={fetchDashboard} style={{ marginTop: '16px' }}>Thử lại</button>
        </div>
      </div>
    );
  }

  const dashboardData = data || {
    stats: { activeSemesterName: "---", totalClasses: 0, totalStudents: 0, totalLecturers: 0 },
    todayOverview: { totalSessions: 0, activeSessions: 0, completedSessions: 0 },
    activeSessions: [],
    pendingRequests: []
  };

  return (
    <div className="page active">
      <AdminHeader stats={dashboardData.stats} />

      <StatisticCards stats={dashboardData.stats} />

      <AttendanceCharts />

      <div className="g2">
        <ActiveSessions 
          sessions={dashboardData.activeSessions} 
          overview={dashboardData.todayOverview}
        />
        <PendingRequests 
          requests={dashboardData.pendingRequests} 
          onAction={handleRequestAction}
        />
      </div>
    </div>
  );
}

function AdminHeader({ stats }) {
  return (
    <div className="adm-bar">
      <div className="adm-ic">🛡️</div>

      <div>
        <div className="adm-title">Quản trị hệ thống</div>
        <div className="adm-subtitle">
          {stats.activeSemesterName} · {stats.totalClasses} lớp · {stats.totalStudents.toLocaleString()} sinh viên · {stats.totalLecturers} giảng viên
        </div>
      </div>
    </div>
  );
}

function StatisticCards({ stats }) {
  return (
    <div className="sg admin-stats-grid">
      <div className="sc-premium">
        <div className="sc-info">
          <span className="sc-label">Sinh viên</span>
          <span className="sc-value">{stats.totalStudents.toLocaleString()}</span>
        </div>
        <div className="sc-icon-wrapper">👥</div>
        <span className="sc-pct">+12%</span>
      </div>

      <div className="sc-premium">
        <div className="sc-info">
          <span className="sc-label">Lượt điểm danh QR</span>
          <span className="sc-value">{(stats.qrCheckins || stats.qrCheckinsCount || stats.qrCount || stats.qr || 0).toLocaleString()}</span>
        </div>
        <div className="sc-icon-wrapper">🔍</div>
        <span className="sc-pct">+24%</span>
      </div>

      <div className="sc-premium">
        <div className="sc-info">
          <span className="sc-label">Giảng viên</span>
          <span className="sc-value">{stats.totalLecturers}</span>
        </div>
        <div className="sc-icon-wrapper">👨‍🏫</div>
        <span className="sc-pct">+3%</span>
      </div>

      <div className="sc-premium">
        <div className="sc-info">
          <span className="sc-label">Lớp HC</span>
          <span className="sc-value">{stats.totalClasses}</span>
        </div>
        <div className="sc-icon-wrapper">🏫</div>
        <span className="sc-pct">+8%</span>
      </div>
    </div>
  );
}

function ActiveSessions({ sessions, overview }) {
  return (
    <div className="card">
      <div className="card-h">
        <div>
          <div className="card-t">Lớp học đang diễn ra</div>
          <div className="card-su" style={{ marginTop: '4px' }}>
            Tổng số ca hôm nay: <strong>{overview.totalSessions}</strong> ca · Đã hoàn thành: <strong>{overview.completedSessions}</strong> ca
          </div>
        </div>
        <span className="bdg b-op" style={{ padding: '4px 10px' }}>
          Live: {overview.activeSessions} ca
        </span>
      </div>

      <table className="tbl">
        <thead>
          <tr>
            <th>Giảng viên</th>
            <th>Môn học & Lớp</th>
            <th>Phòng</th>
            <th>Thời gian</th>
            <th>Bắt đầu</th>
          </tr>
        </thead>

        <tbody>
          {sessions.length === 0 ? (
            <tr>
              <td colSpan="5" style={{ textAlign: 'center', padding: '32px', color: 'var(--tx3)' }}>
                Hiện tại không có ca học nào đang điểm danh.
              </td>
            </tr>
          ) : (
            sessions.map((cls) => (
              <tr key={cls.id} className="class-list-row">
                <td>
                  <div className="lecturer-profile">
                    <div className="lecturer-avatar">{cls.lecturerInitial}</div>
                    <span>{cls.lecturerName}</span>
                  </div>
                </td>
                <td>
                  <div className="class-subject-info">
                    <span className="subject-name">{cls.subjectName}</span>
                    <span className="class-name-sub">Lớp hành chính: {cls.className}</span>
                  </div>
                </td>
                <td className="room-text" style={{ fontWeight: 600 }}>{cls.roomCode}</td>
                <td>
                  <span className="period-pill">{cls.periodText}</span>
                </td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontFamily: 'var(--mo)', fontSize: '11px', color: 'var(--gr)' }}>
                    <span className="pulse"></span>
                    {cls.openedAt}
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function PendingRequests({ requests, onAction }) {
  return (
    <div className="card timeline-card">
      <div className="card-h" style={{ padding: '12px 16px', borderBottom: '1px solid var(--bd)' }}>
        <div className="card-t">Yêu cầu cần phê duyệt</div>
        <span className="bdg b-sc">
          Chờ duyệt: {requests.filter(r => r.status === 'pending').length}
        </span>
      </div>

      <div className="tl-list" style={{ padding: 0 }}>
        {requests.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '32px', color: 'var(--tx3)', fontSize: '12px' }}>
            🎉 Đã xử lý hết các yêu cầu phê duyệt!
          </div>
        ) : (
          requests.map((req) => (
            <div className="activity-item" key={req.requestId}>
              <div className={`activity-icon-box ${req.type === 'MAKEUP' ? 'bg-blue' : 'bg-orange'}`}>
                {req.type === 'MAKEUP' ? '📅' : '⚠️'}
              </div>

              <div className="activity-content">
                <div className="activity-header">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="activity-title" style={{ fontWeight: 700 }}>{req.lecturerName}</span>
                    <span className={`bdg ${req.type === 'MAKEUP' ? 'b-sc' : 'b-ex'}`} style={{ fontSize: '9px', padding: '1px 6px' }}>
                      {req.typeName}
                    </span>
                  </div>
                  <span className="activity-time">{req.time}</span>
                </div>
                
                <div className="activity-desc" style={{ marginTop: '4px' }}>
                  <strong>{req.subjectName}</strong> ({req.className})
                  <div style={{ color: 'var(--tx3)', fontSize: '11px', marginTop: '2px' }}>{req.details}</div>
                </div>

                {req.status === 'pending' && (
                  <div className="activity-actions" style={{ marginTop: '8px' }}>
                    <button 
                      className="action-btn-sm action-btn-approve"
                      onClick={() => onAction(req.requestId, req.type, 'approve')}
                    >
                      Phê duyệt
                    </button>
                    <button 
                      className="action-btn-sm action-btn-reject"
                      onClick={() => onAction(req.requestId, req.type, 'reject')}
                    >
                      Từ chối
                    </button>
                  </div>
                )}

                {req.status === 'approved' && (
                  <div className="status-badge-inline" style={{ marginTop: '6px' }}>
                    Đã phê duyệt yêu cầu
                  </div>
                )}

                {req.status === 'rejected' && (
                  <div className="status-badge-inline rejected" style={{ marginTop: '6px', color: 'var(--rd)' }}>
                    Đã từ chối yêu cầu
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function AttendanceCharts() {
  const [hoveredDay, setHoveredDay] = useState(null);
  const [hoveredMonth, setHoveredMonth] = useState(null);

  // Mock data for weekly attendance
  const weeklyData = [
    { day: 'T2', name: 'Thứ 2', present: 45, absent: 12, total: 57 },
    { day: 'T3', name: 'Thứ 3', present: 48, absent: 10, total: 58 },
    { day: 'T4', name: 'Thứ 4', present: 52, absent: 8, total: 60 },
    { day: 'T5', name: 'Thứ 5', present: 50, absent: 11, total: 61 },
    { day: 'T6', name: 'Thứ 6', present: 43, absent: 15, total: 58 },
    { day: 'T7', name: 'Thứ 7', present: 38, absent: 18, total: 56 },
  ];

  // Mock data for monthly trend
  const monthlyData = [
    { month: 'T1', name: 'Tháng 1', rate: 72 },
    { month: 'T2', name: 'Tháng 2', rate: 76 },
    { month: 'T3', name: 'Tháng 3', rate: 78 },
    { month: 'T4', name: 'Tháng 4', rate: 80 },
    { month: 'T5', name: 'Tháng 5', rate: 82 },
    { month: 'T6', name: 'Tháng 6', rate: 84 },
    { month: 'T7', name: 'Tháng 7', rate: 83 },
    { month: 'T8', name: 'Tháng 8', rate: 86 },
    { month: 'T9', name: 'Tháng 9', rate: 88 },
    { month: 'T10', name: 'Tháng 10', rate: 85 },
    { month: 'T11', name: 'Tháng 11', rate: 87 },
    { month: 'T12', name: 'Tháng 12', rate: 90 },
  ];
  const avgMonthlyRate = Math.round(monthlyData.reduce((sum, m) => sum + m.rate, 0) / monthlyData.length);

  return (
    <div className="g2" style={{ marginTop: '24px' }}>
      <WeeklyAttendanceChart data={weeklyData} hoveredDay={hoveredDay} setHoveredDay={setHoveredDay} />
      <MonthlyAttendanceTrend data={monthlyData} hoveredMonth={hoveredMonth} setHoveredMonth={setHoveredMonth} avgRate={avgMonthlyRate} />
    </div>
  );
}

function WeeklyAttendanceChart({ data, hoveredDay, setHoveredDay }) {
  const maxTotal = Math.max(...data.map(d => d.total));
  const chartHeight = 280;
  const barWidth = 35;
  const gapBetweenBars = 15;
  const startX = 50;
  const topPadding = 20;
  const bottomPadding = 50;
  const chartRightPadding = 20;
  const chartWidth = startX + data.length * (barWidth * 2 + gapBetweenBars) - gapBetweenBars;
  const viewBoxWidth = chartWidth + chartRightPadding + 10;
  const chartAreaLeft = startX - 10;
  const chartAreaRight = chartWidth;

  return (
    <div className="card">
      <div className="card-h">
        <div>
          <div className="card-t">Tỷ lệ điểm danh tuần này</div>
          <div className="card-su" style={{ marginTop: '4px', fontSize: '11px', color: 'var(--tx3)' }}>
            Số lượng có mặt / vắng mặt theo ngày
          </div>
        </div>
      </div>

      <div style={{ padding: '20px', position: 'relative' }}>
        <svg width="100%" height={chartHeight + topPadding + bottomPadding} viewBox={`0 0 ${viewBoxWidth} ${chartHeight + topPadding + bottomPadding}`} style={{ minWidth: '100%', overflow: 'visible' }}>
          <rect
            x={chartAreaLeft}
            y={topPadding}
            width={chartAreaRight - chartAreaLeft}
            height={chartHeight}
            fill="transparent"
            stroke="#cbd5e1"
            strokeWidth="0.8"
            opacity="0.18"
            rx="10"
          />

          {/* Y-axis labels and horizontal grid lines */}
          {[0, 25, 50, 75, 100].map((val) => {
            const y = topPadding + chartHeight - (val / 100) * chartHeight;
            return (
              <g key={`y-${val}`}>
                <text x="18" y={y + 5} fontSize="11" fill="var(--tx3)" textAnchor="end" fontWeight="500">
                  {val}
                </text>
                <line
                  x1={chartAreaLeft}
                  y1={y}
                  x2={chartAreaRight}
                  y2={y}
                  stroke="#cbd5e1"
                  strokeWidth="0.75"
                  strokeDasharray="4,4"
                  opacity="0.45"
                />
              </g>
            );
          })}

          <line
            x1={chartAreaLeft}
            y1={topPadding + chartHeight}
            x2={chartAreaRight}
            y2={topPadding + chartHeight}
            stroke="#9ca3af"
            strokeWidth="1"
            opacity="0.45"
          />

          {/* Vertical grid lines */}
          {data.map((item, idx) => {
            const x = startX + idx * (barWidth * 2 + gapBetweenBars) + barWidth + 3;
            return (
              <line
                key={`grid-v-${idx}`}
                x1={x}
                y1={topPadding}
                x2={x}
                y2={topPadding + chartHeight}
                stroke="#cbd5e1"
                strokeWidth="0.75"
                strokeDasharray="4,4"
                opacity="0.25"
              />
            );
          })}

          {/* Bars */}
          {data.map((item, idx) => {
            const x = startX + idx * (barWidth * 2 + gapBetweenBars);
            const presentHeight = (item.present / maxTotal) * chartHeight;
            const absentHeight = (item.absent / maxTotal) * chartHeight;
            const isHovered = hoveredDay === idx;

            return (
              <g key={`bar-${idx}`}>
                {/* Present bar (blue) */}
                <rect
                  x={x}
                  y={topPadding + chartHeight - presentHeight}
                  width={barWidth}
                  height={presentHeight}
                  fill={isHovered ? '#2563eb' : '#3b82f6'}
                  rx="4"
                  style={{ cursor: 'pointer', opacity: isHovered ? 1 : 0.95, transition: 'all 0.2s' }}
                  onMouseEnter={() => setHoveredDay(idx)}
                  onMouseLeave={() => setHoveredDay(null)}
                />

                {/* Absent bar (red) */}
                <rect
                  x={x + barWidth + 6}
                  y={topPadding + chartHeight - absentHeight}
                  width={barWidth}
                  height={absentHeight}
                  fill={isHovered ? '#dc2626' : '#ef4444'}
                  rx="4"
                  style={{ cursor: 'pointer', opacity: isHovered ? 1 : 0.95, transition: 'all 0.2s' }}
                  onMouseEnter={() => setHoveredDay(idx)}
                  onMouseLeave={() => setHoveredDay(null)}
                />

                {/* Day label */}
                <text x={x + barWidth + 3} y={topPadding + chartHeight + 24} fontSize="12" fill="var(--tx2)" textAnchor="middle" fontWeight="600" dy="0.1em">
                  {item.day}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Tooltip */}
        {hoveredDay !== null && (
          <div className="chart-tooltip" style={{
            position: 'absolute',
            bottom: chartHeight + 20,
            left: startX + hoveredDay * (barWidth * 2 + gapBetweenBars) + barWidth + 3,
            transform: 'translateX(-50%)',
            background: '#fff',
            border: '1px solid var(--bd)',
            borderRadius: '8px',
            padding: '8px 12px',
            fontSize: '11px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
            zIndex: 10,
            whiteSpace: 'nowrap'
          }}>
            <div style={{ fontWeight: 700, color: 'var(--tx)' }}>{data[hoveredDay].name}</div>
            <div style={{ color: 'var(--tx2)', marginTop: '2px' }}>
              Có mặt: {((data[hoveredDay].present / data[hoveredDay].total) * 100).toFixed(0)}%
            </div>
            <div style={{ color: 'var(--tx2)' }}>
              Vắng: {((data[hoveredDay].absent / data[hoveredDay].total) * 100).toFixed(0)}%
            </div>
          </div>
        )}

        {/* Legend */}
        <div style={{ display: 'flex', gap: '20px', marginTop: '16px', fontSize: '11px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <div style={{ width: '12px', height: '12px', background: '#3b82f6', borderRadius: '2px' }}></div>
            <span>Có mặt</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <div style={{ width: '12px', height: '12px', background: '#ef4444', borderRadius: '2px' }}></div>
            <span>Vắng mặt</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function MonthlyAttendanceTrend({ data, hoveredMonth, setHoveredMonth, avgRate }) {
  const chartHeight = 280;
  const chartWidth = 560;
  const topPadding = 20;
  const bottomPadding = 80;
  const padding = 40;
  const viewBoxHeight = chartHeight + topPadding + bottomPadding;
  const viewBoxWidth = chartWidth + 80;
  const chartAreaLeft = padding;
  const chartAreaRight = chartWidth;

  // Normalize data points for line chart
  const maxRate = 100;
  const points = data.map((item, idx) => ({
    ...item,
    x: padding + (idx / (data.length - 1)) * (chartWidth - padding),
    y: topPadding + chartHeight - (item.rate / maxRate) * chartHeight,
  }));

  // Create SVG path for line
  const pathD = points.map((p, idx) => `${idx === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');

  return (
    <div className="card">
      <div className="card-h">
        <div>
          <div className="card-t">Xu hướng điểm danh</div>
          <div className="card-su" style={{ marginTop: '4px', fontSize: '11px', color: 'var(--tx3)' }}>
            Tỷ lệ % trung bình tháng
          </div>
        </div>
      </div>

      <div style={{ padding: '20px', position: 'relative' }}>
        <svg width="100%" height={viewBoxHeight} viewBox={`0 0 ${viewBoxWidth} ${viewBoxHeight}`} style={{ minWidth: '100%' }}>
          <rect
            x={chartAreaLeft}
            y={topPadding}
            width={chartAreaRight - chartAreaLeft}
            height={chartHeight}
            fill="transparent"
            stroke="#cbd5e1"
            strokeWidth="0.8"
            opacity="0.18"
            rx="10"
          />

          {/* Y-axis labels and horizontal grid lines */}
          {[0, 25, 50, 75, 100].map((val) => {
            const y = topPadding + chartHeight - (val / 100) * chartHeight;
            return (
              <g key={`grid-${val}`}>
                <text x="18" y={y + 5} fontSize="11" fill="var(--tx3)" textAnchor="end" fontWeight="500">
                  {val}%
                </text>
                <line
                  x1={chartAreaLeft}
                  y1={y}
                  x2={chartAreaRight}
                  y2={y}
                  stroke="#cbd5e1"
                  strokeWidth="0.75"
                  strokeDasharray="4,4"
                  opacity="0.45"
                />
              </g>
            );
          })}

          <line
            x1={chartAreaLeft}
            y1={topPadding + chartHeight}
            x2={chartAreaRight}
            y2={topPadding + chartHeight}
            stroke="#9ca3af"
            strokeWidth="1"
            opacity="0.45"
          />

          {/* Vertical grid lines */}
          {points.map((point, idx) => (
            <line
              key={`grid-v-${idx}`}
              x1={point.x}
              y1={topPadding}
              x2={point.x}
              y2={topPadding + chartHeight}
              stroke="#cbd5e1"
              strokeWidth="0.75"
              strokeDasharray="4,4"
              opacity="0.25"
            />
          ))}

          {/* Line */}
          <path d={pathD} stroke="#6366f1" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round" />

          {/* Data points */}
          {points.map((point, idx) => (
            <g key={`point-${idx}`}>
              <circle
                cx={point.x}
                cy={point.y}
                r={hoveredMonth === idx ? 5 : 4}
                fill={hoveredMonth === idx ? '#6366f1' : '#818cf8'}
                style={{ cursor: 'pointer', transition: 'all 0.2s' }}
                onMouseEnter={() => setHoveredMonth(idx)}
                onMouseLeave={() => setHoveredMonth(null)}
              />
              <text x={point.x} y={topPadding + chartHeight + 30} fontSize="11" fill="var(--tx2)" textAnchor="middle" fontWeight="600">
                {point.month}
              </text>
            </g>
          ))}
        </svg>

        {/* Tooltip */}
        {hoveredMonth !== null && (
          <div style={{
            position: 'absolute',
            top: topPadding + chartHeight - (data[hoveredMonth].rate / 100) * chartHeight - 40,
            left: padding + (hoveredMonth / (data.length - 1)) * (chartWidth - padding),
            transform: 'translateX(-50%)',
            background: '#fff',
            border: '1px solid var(--bd)',
            borderRadius: '8px',
            padding: '8px 12px',
            fontSize: '11px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
            zIndex: 10,
            whiteSpace: 'nowrap'
          }}>
            <div style={{ fontWeight: 700, color: 'var(--tx)' }}>{data[hoveredMonth].name}</div>
            <div style={{ color: 'var(--tx2)', marginTop: '2px' }}>
              Điểm danh: {data[hoveredMonth].rate}%
            </div>
          </div>
        )}

        {/* Progress bar */}
        <div style={{ marginTop: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '11px', color: 'var(--tx3)', fontWeight: 600 }}>Trung bình tháng</span>
            <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--tx)' }}>{avgRate}%</span>
          </div>
          <div style={{ width: '100%', height: '8px', background: 'var(--bg3)', borderRadius: '4px', overflow: 'hidden' }}>
            <div style={{
              width: `${avgRate}`,
              height: '100%',
              background: 'linear-gradient(90deg, #6366f1, #8b5cf6)',
              borderRadius: '4px',
              transition: 'width 0.3s'
            }}></div>
          </div>
        </div>
      </div>
    </div>
  );
}