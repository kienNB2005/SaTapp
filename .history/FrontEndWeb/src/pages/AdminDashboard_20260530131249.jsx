// AdminDashboard.jsx
import '../css/AdminDashboard.css';

export default function AdminDashboard() {
  return (
    <div className="page active admin-dashboard">
      <div className="adm-hero">
        <div className="adm-hero-copy">
          <div className="adm-title">Dashboard Admin</div>
          <div className="adm-subtitle">Tổng quan hệ thống điểm danh QR</div>
        </div>
        <div className="adm-hero-actions">
          <button className="btn btn-s sem-btn">Học kỳ 2 · 2024/2025</button>
        </div>
      </div>

      <StatisticCards />

      <div className="adm-grid">
        <AttendanceChart />
        <TrendCard />
      </div>

      <RecentActivities />
    </div>
  );
}

function StatisticCards() {
  return (
    <div className="sg admin-stats">
      <div className="sc bl">
        <div className="sc-head">
          <div className="sc-ic">👥</div>
          <span className="sc-rate positive">+12%</span>
        </div>
        <div className="sc-vl bl">4,283</div>
        <div className="sc-lb">Tổng Sinh viên</div>
      </div>

      <div className="sc gr">
        <div className="sc-head">
          <div className="sc-ic">🎓</div>
          <span className="sc-rate positive">+3%</span>
        </div>
        <div className="sc-vl gr">186</div>
        <div className="sc-lb">Giảng viên</div>
      </div>

      <div className="sc am">
        <div className="sc-head">
          <div className="sc-ic">📚</div>
          <span className="sc-rate positive">+8%</span>
        </div>
        <div className="sc-vl am">92</div>
        <div className="sc-lb">Môn học</div>
      </div>

      <div className="sc pu">
        <div className="sc-head">
          <div className="sc-ic">🔲</div>
          <span className="sc-rate positive">+24%</span>
        </div>
        <div className="sc-vl pu">21,540</div>
        <div className="sc-lb">Lượt điểm danh QR</div>
      </div>
    </div>
  );
}

function AttendanceChart() {
  return (
    <div className="card chart-card">
      <div className="card-h card-h-spaced">
        <div>
          <div className="card-t">Tỷ lệ điểm danh tuần này</div>
          <div className="card-su">Số lượng có mặt / vắng mặt theo ngày</div>
        </div>
      </div>

      <div className="chart-bars">
        {[
          { label: 'T2', value: 82 },
          { label: 'T3', value: 75 },
          { label: 'T4', value: 92 },
          { label: 'T5', value: 88 },
          { label: 'T6', value: 73 },
          { label: 'T7', value: 63 },
        ].map((item) => (
          <div key={item.label} className="chart-bar">
            <div className="chart-bar-fill" style={{ height: `${item.value}%` }} />
            <span>{item.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function TrendCard() {
  return (
    <div className="card chart-card">
      <div className="card-h card-h-spaced">
        <div>
          <div className="card-t">Xu hướng điểm danh</div>
          <div className="card-su">Tỷ lệ % trung bình tháng</div>
        </div>
      </div>

      <div className="line-chart">
        <div className="line-grid" />
        <svg viewBox="0 0 240 120" className="line-svg" preserveAspectRatio="none">
          <polyline
            fill="none"
            stroke="#4F46E5"
            strokeWidth="4"
            points="0,80 40,68 80,60 120,62 160,46 200,28 240,22"
          />
          {[0, 1, 2, 3, 4, 5, 6].map((idx) => (
            <circle
              key={idx}
              cx={idx * 40}
              cy={[80, 68, 60, 62, 46, 28, 22][idx]}
              r="4"
              fill="#4F46E5"
            />
          ))}
        </svg>
      </div>

      <div className="trend-footer">
        <span>89%</span>
        <div className="progress-bar">
          <div className="progress-fill" style={{ width: '89%' }} />
        </div>
      </div>
    </div>
  );
}

function RecentActivities() {
  return (
    <div className="card activity-card">
      <div className="card-h card-h-spaced">
        <div>
          <div className="card-t">Hoạt động gần đây</div>
          <div className="card-su">Các sự kiện mới nhất trong hệ thống</div>
        </div>
        <button className="btn btn-s">Xem tất cả</button>
      </div>

      <div className="tl-list">
        {[
          {
            title: 'Sinh viên Nguyễn Văn A đã điểm danh',
            desc: '2 phút trước',
            variant: 'success',
          },
          {
            title: 'Lớp CNTT-K21A vắng 5 sinh viên',
            desc: '15 phút trước',
            variant: 'warning',
          },
          {
            title: 'Học kỳ 2 - 2024/2025 đã được tạo',
            desc: '1 giờ trước',
            variant: 'success',
          },
          {
            title: 'Giảng viên Trần Thị B đã phê duyệt',
            desc: '2 giờ trước',
            variant: 'success',
          },
          {
            title: 'Cần phê duyệt 3 lịch giảng dạy mới',
            desc: '3 giờ trước',
            variant: 'warning',
          },
        ].map((item) => (
          <div key={item.title} className="tl-item">
            <div className={`tl-dot ${item.variant === 'success' ? 'tl-dot-success' : 'tl-dot-warning'}`} />
            <div>
              <div className="tl-title">{item.title}</div>
              <div className="tl-desc">{item.desc}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}