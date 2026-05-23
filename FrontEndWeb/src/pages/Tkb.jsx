import React, { useState } from 'react';

export default function Tkb() {
  const [viewMode, setViewMode] = useState('week'); // 'today' | 'week'

  const allDays = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật'];
  
  // Calculate today's "Thứ"
  const currentDayNum = new Date().getDay(); // 0 is Sunday, 1 is Monday
  const todayStr = currentDayNum === 0 ? 'Chủ Nhật' : `Thứ ${currentDayNum + 1}`;
  
  const displayDays = viewMode === 'today' ? [todayStr] : allDays;

  const periods = [
    { label: 'Sáng (T.1-3)', key: '1-3' },
    { label: 'Sáng (T.4-6)', key: '4-6' },
    { label: 'Chiều (T.7-9)', key: '7-9' },
    { label: 'Chiều (T.10-12)', key: '10-12' },
  ];

  const schedule = {
    'Thứ 3_1-3': { subject: 'Cấu trúc dữ liệu', class: 'CNTT-K22A', room: 'B201', startWeek: 1, endWeek: 15, color: '#22C55E' },
    'Thứ 5_4-6': { subject: 'Lập trình Web', class: 'CNTT-K23B', room: 'A105', startWeek: 1, endWeek: 15, color: '#3B82F6' },
    'Thứ 2_7-9': { subject: 'Mạng máy tính', class: 'ATTT-K22', room: 'C101', startWeek: 2, endWeek: 15, color: '#F59E0B' },
  };

  return (
    <div className="page active">
      <div style={{ display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <label style={{ fontSize: '11px', color: 'var(--tx3)', fontWeight: '600' }}>Học kỳ</label>
          <select className="fi" style={{ width: '200px' }} defaultValue="HK1">
            <option value="HK1">Học kỳ 1 - Năm 2025-2026</option>
            <option value="HK2">Học kỳ 2 - Năm 2024-2025</option>
          </select>
        </div>

        {/* View Toggle */}
        <div style={{ display: 'flex', background: 'var(--bg3)', padding: '4px', borderRadius: '8px', border: '1px solid var(--bd)', marginLeft: 'auto' }}>
          <button style={{ padding: '8px 24px', borderRadius: '6px', border: 'none', background: viewMode === 'today' ? 'var(--bg)' : 'transparent', color: viewMode === 'today' ? 'var(--tx)' : 'var(--tx3)', fontWeight: '600', fontSize: '13px', cursor: 'pointer', boxShadow: viewMode === 'today' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none' }} onClick={() => setViewMode('today')}>Hôm nay</button>
          <button style={{ padding: '8px 24px', borderRadius: '6px', border: 'none', background: viewMode === 'week' ? 'var(--bg)' : 'transparent', color: viewMode === 'week' ? 'var(--tx)' : 'var(--tx3)', fontWeight: '600', fontSize: '13px', cursor: 'pointer', boxShadow: viewMode === 'week' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none' }} onClick={() => setViewMode('week')}>Tuần</button>
        </div>
      </div>

      <div className="card" style={{ padding: '0', overflowX: 'auto', borderRadius: '12px' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: viewMode === 'week' ? '900px' : 'auto' }}>
          <thead>
            <tr style={{ background: 'var(--bg3)' }}>
              <th style={{ padding: '16px 12px', borderBottom: '1px solid var(--bd)', textAlign: 'left', width: '100px', color: 'var(--tx2)', fontSize: '12px', fontWeight: '600' }}>Ca học</th>
              {displayDays.map(d => (
                <th key={d} style={{ padding: '16px 12px', borderBottom: '1px solid var(--bd)', borderLeft: '1px solid var(--bd)', textAlign: 'center', width: viewMode === 'week' ? '15%' : 'auto', color: 'var(--tx2)', fontSize: '12px', fontWeight: '600' }}>{d}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {periods.map((p, idx) => (
              <React.Fragment key={p.key}>
                <tr>
                  <td style={{ padding: '15px 10px', borderBottom: '1px solid var(--bd)', fontWeight: '600', fontSize: '12px', color: 'var(--tx3)', verticalAlign: 'top', background: 'var(--bg3)' }}>
                    {p.label}
                  </td>
                  {displayDays.map(d => {
                    const cls = schedule[`${d}_${p.key}`];
                    const isActive = !!cls;
                    return (
                      <td key={`${d}_${p.key}`} style={{ padding: '8px', borderBottom: '1px solid var(--bd)', borderLeft: '1px solid var(--bd)', verticalAlign: 'top', height: '120px' }}>
                        {isActive && (
                          <div style={{ background: `${cls.color}15`, borderLeft: `4px solid ${cls.color}`, padding: '12px', borderRadius: '6px', height: '100%', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <div style={{ fontSize: '13px', fontWeight: '700', color: 'var(--tx)' }}>{cls.subject}</div>
                            <div style={{ fontSize: '11px', color: 'var(--tx3)' }}>Lớp: <span style={{ fontFamily: 'var(--mo)', fontWeight: '600', color: 'var(--tx2)' }}>{cls.class}</span></div>
                            <div style={{ fontSize: '11px', color: 'var(--tx3)' }}>Phòng: <span style={{ fontFamily: 'var(--mo)', fontWeight: '600', color: 'var(--tx2)' }}>{cls.room}</span></div>
                            <div style={{ marginTop: 'auto', paddingTop: '6px', borderTop: `1px dashed ${cls.color}40`, fontSize: '11px', color: cls.startWeek > 1 ? '#EF4444' : 'var(--tx3)', fontWeight: cls.startWeek > 1 ? '600' : '500' }}>
                              Tuần học: {cls.startWeek}–{cls.endWeek}
                            </div>
                          </div>
                        )}
                      </td>
                    );
                  })}
                </tr>
                {idx === 1 && (
                  <tr>
                    <td colSpan={displayDays.length + 1} style={{ padding: '8px', background: 'rgba(255,255,255,0.02)', borderBottom: '1px solid var(--bd)', textAlign: 'center', fontSize: '11px', color: 'var(--tx3)', fontWeight: '600', letterSpacing: '6px' }}>
                      NGHỈ TRƯA
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
