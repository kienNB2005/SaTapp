// AdminDashboard.jsx
import '../css/AdminDashboard.css';

export default function AdminDashboard() {
  return (
    <div className="page active">
      <AdminHeader />

      <StatisticCards />

      <div className="g2">
        <TodayClasses />
        <RecentActivities />
      </div>
    </div>
  );
}

function AdminHeader() {
  return (
    <div className="adm-bar">
      <div className="adm-ic">🛡️</div>

      <div>
        <div className="adm-title">Quản trị hệ thống</div>
        <div className="adm-subtitle">
          HK1-2024-2025 · 62 lớp · 2,450 sinh viên · 128 giảng viên
        </div>
      </div>
    </div>
  );
}

function StatisticCards() {
  return (
    <div className="sg">
      <div className="sc bl">
        <div className="sc-ic">👥</div>
        <div className="sc-lb">Sinh viên</div>
        <div className="sc-vl bl">2,450</div>
      </div>

      <div className="sc gr">
        <div className="sc-ic">👨‍🏫</div>
        <div className="sc-lb">Giảng viên</div>
        <div className="sc-vl gr">128</div>
      </div>

      <div className="sc am">
        <div className="sc-ic">🏫</div>
        <div className="sc-lb">Lớp HC</div>
        <div className="sc-vl am">62</div>
      </div>

      <div className="sc pu">
        <div className="sc-ic">📊</div>
        <div className="sc-lb">Tb chuyên cần</div>
        <div className="sc-vl pu">83%</div>
      </div>
    </div>
  );
}

function TodayClasses() {
  return (
    <div className="card">
      <div className="card-h">
        <div className="card-t">Buổi học hôm nay</div>
      </div>

      <table className="tbl">
        <thead>
          <tr>
            <th>Giảng viên</th>
            <th>Môn · Lớp</th>
            <th>Phòng</th>
            <th>Tiết</th>
            <th>TT</th>
          </tr>
        </thead>

        <tbody>
          <tr>
            <td>Nguyễn Minh</td>
            <td className="small-text">CTDL · K22A</td>
            <td className="room-text">B201</td>
            <td className="small-text">1–3</td>
            <td>
              <span className="bdg b-op">Đang mở</span>
            </td>
          </tr>

          <tr>
            <td>Trần Hương</td>
            <td className="small-text">Giải tích · K23A</td>
            <td className="room-text">A101</td>
            <td className="small-text">4–6</td>
            <td>
              <span className="bdg b-sc">Sắp tới</span>
            </td>
          </tr>

          <tr>
            <td>Lê Quốc</td>
            <td className="small-text">Vật lý · K23C</td>
            <td className="room-text">C201</td>
            <td className="small-text">1–3</td>
            <td>
              <span className="bdg b-cl">Xong</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

function RecentActivities() {
  return (
    <div className="card">
      <div className="card-h">
        <div className="card-t">Hoạt động gần đây</div>
      </div>

      <div className="tl-list">
        <div className="tl-item">
          <div className="tl-dot"></div>

          <div>
            <div className="tl-title">GV Nguyễn Minh mở buổi CTDL</div>
            <div className="tl-desc">
              07:01 · Phòng B201 · 22/40 SV đã điểm danh
            </div>
          </div>
        </div>

        <div className="tl-item">
          <div className="tl-dot tl-dot-blue"></div>

          <div>
            <div className="tl-title">Admin nhập TKB HK1-2024-2025</div>
            <div className="tl-desc">
              12/08 · 142 dòng · 1,840 buổi học
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}