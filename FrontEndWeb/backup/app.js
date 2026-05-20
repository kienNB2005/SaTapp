/* ============================================================
   QRAttend — Main Script
   ============================================================ */

/* ── Page title map ── */
const PAGE_TITLES = {
  dashboard:  ['Dashboard',                   'Thứ 3, 02/09/2025 · HK1-2024-2025'],
  sessions:   ['Buổi học · CTDL · K22A',      '15 buổi · HK1-2024-2025'],
  qr:         ['QR Điểm danh · Buổi 5/15',    'CTDL · K22A · Đang diễn ra · B201'],
  attendance: ['Danh sách điểm danh',          'Buổi 5/15 · 02/09/2025 · CTDL'],
  report:     ['Báo cáo lớp',                  'CTDL · CNTT-K22A · HK1-2024-2025'],
  adash:      ['Dashboard Admin',              'HK1-2024-2025 · Toàn trường'],
  atkb:       ['Nhập Thời khóa biểu',          'Upload file TKB từ trường'],
  ausers:     ['Quản lý Tài khoản',            'Admin · Giảng viên · Sinh viên'],
  arooms:     ['Quản lý Phòng học',            '4 phòng · 1 chưa có GPS'],
  asubjects:  ['Quản lý Môn học',              '48 môn học'],
  asemesters: ['Quản lý Học kỳ',               '1 học kỳ đang active'],
  areport:    ['Báo cáo Toàn trường',          'HK1-2024-2025'],
};

/* ── Navigation ── */
function go(id, el) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('p-' + id).classList.add('active');

  document.querySelectorAll('.sb-it').forEach(i => i.classList.remove('on'));
  if (el) el.classList.add('on');

  const [title, sub] = PAGE_TITLES[id] || [id, ''];
  document.getElementById('pgTitle').textContent = title;
  document.getElementById('pgSub').textContent   = sub;
}

/* ── Role switcher ── */
function role(r, btn) {
  document.querySelectorAll('.sb-tb').forEach(b => b.classList.remove('on'));
  btn.classList.add('on');

  const isAdmin = r === 'admin';
  document.getElementById('nav-gv').style.display    = isAdmin ? 'none'  : 'block';
  document.getElementById('nav-admin').style.display = isAdmin ? 'block' : 'none';

  document.getElementById('sbNm').textContent = isAdmin ? 'Quản trị viên' : 'Nguyễn Minh';
  document.getElementById('sbRl').textContent = isAdmin ? 'Admin · Hệ thống' : 'Giảng viên · CNTT';
  document.getElementById('sbAv').textContent = isAdmin ? 'AD' : 'NM';
  document.getElementById('sbAv').style.background = isAdmin
    ? 'linear-gradient(135deg,#3B82F6,#A855F7)'
    : 'linear-gradient(135deg,#22C55E,#14B8A6)';

  const firstItem = document.querySelector(isAdmin ? '#nav-admin .sb-it' : '#nav-gv .sb-it');
  go(isAdmin ? 'adash' : 'dashboard', firstItem);
  if (firstItem) firstItem.classList.add('on');
}

/* ── Checkout modal ── */
function openCheckoutModal()  { document.getElementById('coModal').classList.add('open'); }
function closeCheckoutModal() { document.getElementById('coModal').classList.remove('open'); }

document.addEventListener('DOMContentLoaded', () => {
  const coModal = document.getElementById('coModal');
  coModal.addEventListener('click', e => {
    if (e.target === coModal) closeCheckoutModal();
  });

  /* ── QR token countdown ── */
  let timerVal = 42;
  setInterval(() => {
    timerVal = timerVal <= 1 ? 60 : timerVal - 1;
    const numEl    = document.getElementById('tnum');
    const dispEl   = document.getElementById('tdisp');
    const circleEl = document.getElementById('tcircle');

    if (numEl)    numEl.textContent  = timerVal;
    if (dispEl)   dispEl.textContent = timerVal + 's';
    if (circleEl) {
      const pct  = timerVal / 60;
      const circ = 151;
      circleEl.style.strokeDashoffset = circ * (1 - pct);
      circleEl.style.stroke = pct > .4 ? '#22C55E' : pct > .2 ? '#F59E0B' : '#EF4444';
    }
  }, 1000);

  /* ── Live attendance simulation ── */
  const svData = [
    { code: '2251012345', nm: 'Nguyễn Văn An',   t: '07:02', late: false },
    { code: '2251012346', nm: 'Trần Thị Bình',   t: '07:08', late: false },
    { code: '2251012350', nm: 'Vũ Thị Hoa',      t: '07:05', late: false },
    { code: '2251012351', nm: 'Đinh Văn Khoa',   t: '07:11', late: false },
    { code: '2251012352', nm: 'Ngô Thị Lan',     t: '07:14', late: false },
    { code: '2251012353', nm: 'Bùi Minh Long',   t: '07:16', late: false },
    { code: '2251012347', nm: 'Lê Minh Cường',   t: '07:19', late: true  },
    { code: '2251012354', nm: 'Đỗ Thị Mai',      t: '07:22', late: true  },
  ];

  function renderAttendance() {
    const body = document.getElementById('attBody');
    if (!body) return;
    body.innerHTML = svData.map(s => `
      <div class="att-row">
        <div class="att-av">${s.nm.split(' ').pop()[0]}</div>
        <div style="flex:1">
          <div class="att-nm">${s.nm}</div>
          <div style="font-size:10px;color:var(--tx3);font-family:var(--mo)">${s.code}</div>
        </div>
        ${s.late ? '<span class="bdg b-lt" style="font-size:10px;margin-right:4px">Muộn</span>' : ''}
        <span class="att-ti">${s.t}</span>
      </div>
    `).join('');
  }

  renderAttendance();

  let attCount = 22;
  const extraNames = ['Phạm Quang Minh', 'Lưu Thị Oanh', 'Trịnh Văn Phúc', 'Cao Thị Quỳnh', 'Đặng Minh Sơn'];

  setInterval(() => {
    if (attCount < 38 && Math.random() < 0.15) {
      attCount++;
      const el = document.getElementById('attCnt');
      if (el) el.textContent = attCount;

      const name = extraNames[Math.floor(Math.random() * extraNames.length)];
      const now  = new Date();
      const h    = now.getHours().toString().padStart(2, '0');
      const m    = now.getMinutes().toString().padStart(2, '0');
      svData.unshift({ code: '225101' + (2355 + attCount), nm: name, t: `${h}:${m}`, late: false });
      renderAttendance();
    }
  }, 3000);
});
