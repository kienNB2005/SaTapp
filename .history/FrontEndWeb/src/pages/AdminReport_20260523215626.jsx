import React from 'react';

export default function AdminReport() {
  return (
    <div className="page active">
      <div className="adm-bar">
        <div className="adm-ic">📊</div>
        <div>
          <div style={{ fontSize: '15px', fontWeight: '700' }}>Báo cáo Điểm danh Toàn trường</div>
          <div style={{ fontSize: '12px', color: 'var(--tx3)', marginTop: '2px' }}>Tổng hợp tình hình điểm danh của tất cả các lớp trong học kỳ</div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '10px', marginBottom: '18px', alignItems: 'center' }}>
        <select className="fi" style={{ width: '180px' }} defaultValue="HK1-2024-2025"><option>HK1-2024-2025</option></select>
        <select className="fi" style={{ width: '180px' }} defaultValue="Tất cả Khoa"><option>Tất cả Khoa</option><option>Khoa CNTT</option><option>Khoa Kinh tế</option></select>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '8px' }}>
          <button className="btn btn-s btn-sm">📥 Xuất Excel (Toàn trường)</button>
          <button className="btn btn-s btn-sm">📄 Xuất PDF</button>
        </div>
      </div>
      
      <div className="sg">
        <div className="sc bl"><div className="sc-lb">Tổng số lớp</div><div className="sc-vl bl">124</div></div>
        <div className="sc gr"><div className="sc-lb">Tỉ lệ đi học trung bình</div><div className="sc-vl gr">89%</div></div>
        <div className="sc am"><div className="sc-lb">Lớp dưới chuẩn (&lt;80%)</div><div className="sc-vl am">8</div></div>
        <div className="sc pu"><div className="sc-lb">Tổng số buổi đã dạy</div><div className="sc-vl pu">1,850</div></div>
      </div>
      
      <div className="card">
        <div className="card-h">
          <div>
            <div className="card-t">Danh sách Lớp học</div>
            <div className="card-su">Thống kê điểm danh theo từng lớp</div>
          </div>
          <div className="srch">
            <span className="srch-ic">🔍</span>
            <input placeholder="Tìm lớp, giảng viên..." />
          </div>
        </div>
        <table className="tbl">
          <thead><tr><th>Mã Lớp</th><th>Môn học</th><th>Giảng viên</th><th>Sĩ số</th><th>Buổi hoàn thành</th><th>Tỉ lệ đi học</th><th>Cảnh báo</th></tr></thead>
          <tbody>
            <tr><td style={{ fontFamily: 'var(--mo)' }}>CNTT-K22A</td><td>Cấu trúc dữ liệu</td><td>Nguyễn Minh</td><td>40</td><td>5/15</td><td><span style={{ color: 'var(--gr)', fontWeight: '700', fontFamily: 'var(--mo)' }}>92%</span></td><td>-</td></tr>
            <tr><td style={{ fontFamily: 'var(--mo)' }}>CNTT-K22B</td><td>Lập trình Web</td><td>Trần Văn A</td><td>42</td><td>4/15</td><td><span style={{ color: 'var(--gr)', fontWeight: '700', fontFamily: 'var(--mo)' }}>85%</span></td><td>-</td></tr>
            <tr style={{ background: 'rgba(239,68,68,.04)' }}><td style={{ fontFamily: 'var(--mo)' }}>KT-K23A</td><td>Kinh tế vĩ mô</td><td>Lê Thị B</td><td>60</td><td>6/15</td><td><span style={{ color: 'var(--rd)', fontWeight: '700', fontFamily: 'var(--mo)' }}>65%</span></td><td><span className="bdg b-ab">Cần chú ý</span></td></tr>
            <tr><td style={{ fontFamily: 'var(--mo)' }}>CNTT-K21C</td><td>Trí tuệ nhân tạo</td><td>Phạm Văn C</td><td>35</td><td>10/15</td><td><span style={{ color: 'var(--gr)', fontWeight: '700', fontFamily: 'var(--mo)' }}>95%</span></td><td>-</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
