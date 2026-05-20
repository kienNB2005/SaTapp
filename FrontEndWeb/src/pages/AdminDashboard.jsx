import React from 'react';

export default function AdminDashboard() {
  return (
    <div className="page active">
      <div className="adm-bar">
        <div className="adm-ic">🛡️</div>
        <div>
          <div style={{ fontSize: '14px', fontWeight: '700' }}>Quản trị hệ thống</div>
          <div style={{ fontSize: '11px', color: 'var(--tx3)' }}>HK1-2024-2025 · 62 lớp · 2,450 sinh viên · 128 giảng viên</div>
        </div>
      </div>
      <div className="sg">
        <div className="sc bl"><div className="sc-ic">👥</div><div className="sc-lb">Sinh viên</div><div className="sc-vl bl">2,450</div></div>
        <div className="sc gr"><div className="sc-ic">👨‍🏫</div><div className="sc-lb">Giảng viên</div><div className="sc-vl gr">128</div></div>
        <div className="sc am"><div className="sc-ic">🏫</div><div className="sc-lb">Lớp HC</div><div className="sc-vl am">62</div></div>
        <div className="sc pu"><div className="sc-ic">📊</div><div className="sc-lb">Tb chuyên cần</div><div className="sc-vl pu">83%</div></div>
      </div>
      <div className="g2">
        <div className="card">
          <div className="card-h"><div className="card-t">Buổi học hôm nay</div></div>
          <table className="tbl">
            <thead><tr><th>Giảng viên</th><th>Môn · Lớp</th><th>Phòng</th><th>Tiết</th><th>TT</th></tr></thead>
            <tbody>
              <tr><td>Nguyễn Minh</td><td style={{ fontSize: '11px' }}>CTDL · K22A</td><td style={{ fontFamily: 'var(--mo)', fontSize: '11px' }}>B201</td><td style={{ fontSize: '11px' }}>1–3</td><td><span className="bdg b-op">Đang mở</span></td></tr>
              <tr><td>Trần Hương</td><td style={{ fontSize: '11px' }}>Giải tích · K23A</td><td style={{ fontFamily: 'var(--mo)', fontSize: '11px' }}>A101</td><td style={{ fontSize: '11px' }}>4–6</td><td><span className="bdg b-sc">Sắp tới</span></td></tr>
              <tr><td>Lê Quốc</td><td style={{ fontSize: '11px' }}>Vật lý · K23C</td><td style={{ fontFamily: 'var(--mo)', fontSize: '11px' }}>C201</td><td style={{ fontSize: '11px' }}>1–3</td><td><span className="bdg b-cl">Xong</span></td></tr>
            </tbody>
          </table>
        </div>
        <div className="card">
          <div className="card-h"><div className="card-t">Hoạt động gần đây</div></div>
          <div style={{ padding: '10px 14px' }}>
            <div className="tl-item"><div className="tl-dot"></div><div><div style={{ fontSize: '12px', fontWeight: '500' }}>GV Nguyễn Minh mở buổi CTDL</div><div style={{ fontSize: '11px', color: 'var(--tx3)' }}>07:01 · Phòng B201 · 22/40 SV đã điểm danh</div></div></div>
            <div className="tl-item"><div className="tl-dot" style={{ background: 'var(--bl)' }}></div><div><div style={{ fontSize: '12px', fontWeight: '500' }}>Admin nhập TKB HK1-2024-2025</div><div style={{ fontSize: '11px', color: 'var(--tx3)' }}>12/08 · 142 dòng · 1,840 buổi học</div></div></div>
          </div>
        </div>
      </div>
    </div>
  );
}
