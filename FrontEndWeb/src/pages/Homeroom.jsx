import React from 'react';

export default function Homeroom() {
  return (
    <div className="page active">
      <div style={{ display: 'flex', gap: '10px', marginBottom: '18px', alignItems: 'center' }}>
        <select className="fi" style={{ width: '160px' }} defaultValue="CNTT-K22A"><option>CNTT-K22A</option></select>
        <select className="fi" style={{ width: '220px' }} defaultValue="Cấu trúc dữ liệu"><option>Cấu trúc dữ liệu</option><option>Lập trình Web</option><option>Cơ sở dữ liệu</option></select>
        <select className="fi" style={{ width: '160px' }} defaultValue="HK1-2024-2025"><option>HK1-2024-2025</option></select>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '8px' }}>
          <button className="btn btn-s btn-sm">📥 Excel</button>
          <button className="btn btn-s btn-sm">📄 PDF</button>
        </div>
      </div>
      <div className="sg">
        <div className="sc gr"><div className="sc-lb">Tổng SV</div><div className="sc-vl">60</div></div>
        <div className="sc gr"><div className="sc-lb">Tb có mặt</div><div className="sc-vl gr">92%</div></div>
        <div className="sc am"><div className="sc-lb">Dưới ngưỡng</div><div className="sc-vl am">4</div><div className="sc-su">Vắng ≥ 20%</div></div>
        <div className="sc bl"><div className="sc-lb">Buổi đã xong</div><div className="sc-vl bl">10/15</div></div>
      </div>
      <div className="card">
        <table className="tbl">
          <thead><tr><th>MSSV</th><th>Họ tên</th><th>Có mặt</th><th>Vắng</th><th>Có phép</th><th>Muộn</th><th>Về sớm</th><th>Tỉ lệ</th></tr></thead>
          <tbody>
            <tr><td style={{ fontFamily: 'var(--mo)' }}>2251012345</td><td>Nguyễn Văn An</td><td>10</td><td>0</td><td>0</td><td>0</td><td>0</td><td><span style={{ color: 'var(--gr)', fontWeight: '700', fontFamily: 'var(--mo)' }}>100%</span></td></tr>
            <tr><td style={{ fontFamily: 'var(--mo)' }}>2251012347</td><td>Lê Minh Cường</td><td>8</td><td>1</td><td>1</td><td>0</td><td>0</td><td><span style={{ color: 'var(--gr)', fontWeight: '700', fontFamily: 'var(--mo)' }}>80%</span></td></tr>
            <tr style={{ background: 'rgba(239,68,68,.04)' }}><td style={{ fontFamily: 'var(--mo)' }}>2251012350</td><td>Vũ Thị Hoa</td><td>5</td><td>3</td><td>0</td><td>2</td><td>0</td><td><span style={{ color: 'var(--rd)', fontWeight: '700', fontFamily: 'var(--mo)' }}>50%</span></td></tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
