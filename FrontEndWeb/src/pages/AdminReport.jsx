import '../css/AdminReport.css';

const reportStats = [
  {
    color: 'bl',
    label: 'Tổng số lớp',
    value: '124',
  },
  {
    color: 'gr',
    label: 'Tỉ lệ đi học trung bình',
    value: '89%',
  },
  {
    color: 'am',
    label: 'Lớp dưới chuẩn (<80%)',
    value: '8',
  },
  {
    color: 'pu',
    label: 'Tổng số buổi đã dạy',
    value: '1,850',
  },
];

const reportRows = [
  {
    classCode: 'CNTT-K22A',
    subject: 'Cấu trúc dữ liệu',
    lecturer: 'Nguyễn Minh',
    totalStudents: 40,
    completedSessions: '5/15',
    attendanceRate: '92%',
    status: '-',
    warning: false,
  },
  {
    classCode: 'CNTT-K22B',
    subject: 'Lập trình Web',
    lecturer: 'Trần Văn A',
    totalStudents: 42,
    completedSessions: '4/15',
    attendanceRate: '85%',
    status: '-',
    warning: false,
  },
  {
    classCode: 'KT-K23A',
    subject: 'Kinh tế vĩ mô',
    lecturer: 'Lê Thị B',
    totalStudents: 60,
    completedSessions: '6/15',
    attendanceRate: '65%',
    status: 'Cần chú ý',
    warning: true,
  },
  {
    classCode: 'CNTT-K21C',
    subject: 'Trí tuệ nhân tạo',
    lecturer: 'Phạm Văn C',
    totalStudents: 35,
    completedSessions: '10/15',
    attendanceRate: '95%',
    status: '-',
    warning: false,
  },
];

export default function AdminReport() {
  return (
    <div className="page active">
      <ReportHeader />

      <ReportToolbar />

      <ReportStats />

      <ReportTable />
    </div>
  );
}

function ReportHeader() {
  return (
    <div className="adm-bar">
      <div className="adm-ic">📊</div>

      <div>
        <div className="ar-title">Báo cáo Điểm danh Toàn trường</div>
        <div className="ar-subtitle">
          Tổng hợp tình hình điểm danh của tất cả các lớp trong học kỳ
        </div>
      </div>
    </div>
  );
}

function ReportToolbar() {
  return (
    <div className="ar-toolbar">
      <select className="fi ar-filter" defaultValue="HK1-2024-2025">
        <option>HK1-2024-2025</option>
      </select>

      <select className="fi ar-filter" defaultValue="Tất cả Khoa">
        <option>Tất cả Khoa</option>
        <option>Khoa CNTT</option>
        <option>Khoa Kinh tế</option>
      </select>

      <div className="ar-export-actions">
        <button className="btn btn-s btn-sm">📥 Xuất Excel (Toàn trường)</button>
        <button className="btn btn-s btn-sm">📄 Xuất PDF</button>
      </div>
    </div>
  );
}

function ReportStats() {
  return (
    <div className="sg">
      {reportStats.map((item) => (
        <div key={item.label} className={`sc ${item.color}`}>
          <div className="sc-lb">{item.label}</div>
          <div className={`sc-vl ${item.color}`}>{item.value}</div>
        </div>
      ))}
    </div>
  );
}

function ReportTable() {
  return (
    <div className="card">
      <div className="card-h">
        <div>
          <div className="card-t">Danh sách Lớp học</div>
          <div className="card-su">Thống kê điểm danh theo từng lớp</div>
        </div>

        <div className="srch">
          <span className="srch-ic">🔍</span>
          <input aria-label="Tìm lớp hoặc giảng viên" placeholder="Tìm lớp, giảng viên..." />
        </div>
      </div>

      <table className="tbl">
        <thead>
          <tr>
            <th>Mã Lớp</th>
            <th>Môn học</th>
            <th>Giảng viên</th>
            <th>Sĩ số</th>
            <th>Buổi hoàn thành</th>
            <th>Tỉ lệ đi học</th>
            <th>Cảnh báo</th>
          </tr>
        </thead>

        <tbody>
          {reportRows.map((row) => (
            <tr
              key={`${row.classCode}-${row.subject}`}
              className={row.warning ? 'ar-warning-row' : ''}
            >
              <td className="ar-code">{row.classCode}</td>
              <td>{row.subject}</td>
              <td>{row.lecturer}</td>
              <td>{row.totalStudents}</td>
              <td>{row.completedSessions}</td>
              <td>
                <span
                  className={
                    row.warning
                      ? 'ar-attendance-rate ar-attendance-danger'
                      : 'ar-attendance-rate ar-attendance-good'
                  }
                >
                  {row.attendanceRate}
                </span>
              </td>
              <td>
                {row.warning ? (
                  <span className="bdg b-ab">{row.status}</span>
                ) : (
                  row.status
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}