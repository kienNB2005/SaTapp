import {
  Users,
  GraduationCap,
  BookMarked,
  QrCode,
  TrendingUp,
  Calendar,
  Clock,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line } from "recharts";

const stats = [
  { label: "Tổng Sinh viên", value: "4,283", icon: <Users size={20} />, color: "from-blue-500 to-blue-600", bg: "bg-blue-50", text: "text-blue-600", change: "+12%" },
  { label: "Giảng viên", value: "186", icon: <GraduationCap size={20} />, color: "from-violet-500 to-violet-600", bg: "bg-violet-50", text: "text-violet-600", change: "+3%" },
  { label: "Môn học", value: "92", icon: <BookMarked size={20} />, color: "from-emerald-500 to-emerald-600", bg: "bg-emerald-50", text: "text-emerald-600", change: "+8%" },
  { label: "Lượt điểm danh QR", value: "21,540", icon: <QrCode size={20} />, color: "from-orange-500 to-orange-600", bg: "bg-orange-50", text: "text-orange-600", change: "+24%" },
];

const attendanceData = [
  { day: "T2", present: 85, absent: 15 },
  { day: "T3", present: 78, absent: 22 },
  { day: "T4", present: 91, absent: 9 },
  { day: "T5", present: 88, absent: 12 },
  { day: "T6", present: 74, absent: 26 },
  { day: "T7", present: 65, absent: 35 },
];

const trendData = [
  { month: "T1", value: 72 },
  { month: "T2", value: 75 },
  { month: "T3", value: 80 },
  { month: "T4", value: 78 },
  { month: "T5", value: 85 },
  { month: "T6", value: 89 },
];

const recentActivities = [
  { type: "success", text: "Sinh viên Nguyễn Văn A đã điểm danh", time: "2 phút trước", icon: <CheckCircle2 size={14} /> },
  { type: "warning", text: "Lớp CNTT-K21A vắng 5 sinh viên", time: "15 phút trước", icon: <AlertCircle size={14} /> },
  { type: "success", text: "Học kỳ 2 - 2024/2025 đã được tạo", time: "1 giờ trước", icon: <CheckCircle2 size={14} /> },
  { type: "success", text: "Giảng viên Trần Thị B đã phê duyệt", time: "2 giờ trước", icon: <CheckCircle2 size={14} /> },
  { type: "warning", text: "Cần phê duyệt 3 lịch giảng dạy mới", time: "3 giờ trước", icon: <AlertCircle size={14} /> },
];

export function DashboardPage() {
  return (
    <div className="p-6 space-y-6">
      {/* Page title */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Dashboard Admin</h1>
          <p className="text-sm text-gray-500 mt-0.5">Tổng quan hệ thống điểm danh QR</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500 bg-gray-50 border border-gray-200 rounded-xl px-3 py-2">
          <Calendar size={14} className="text-blue-500" />
          <span>Học kỳ 2 · 2024/2025</span>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4">
        {stats.map((s) => (
          <div key={s.label} className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-start justify-between">
              <div className={`w-11 h-11 rounded-xl ${s.bg} ${s.text} flex items-center justify-center`}>
                {s.icon}
              </div>
              <span className="text-xs font-medium text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full flex items-center gap-1">
                <TrendingUp size={10} />
                {s.change}
              </span>
            </div>
            <div className="mt-3">
              <div className="text-2xl font-bold text-gray-800">{s.value}</div>
              <div className="text-sm text-gray-500 mt-0.5">{s.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-3 gap-4">
        {/* Attendance chart */}
        <div className="col-span-2 bg-white rounded-2xl p-5 border border-gray-100 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-semibold text-gray-800">Tỷ lệ điểm danh tuần này</h3>
              <p className="text-xs text-gray-400 mt-0.5">Số lượng có mặt / vắng mặt theo ngày</p>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={attendanceData} barGap={4}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="day" tick={{ fontSize: 12, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 12, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ borderRadius: "12px", border: "none", boxShadow: "0 4px 20px rgba(0,0,0,0.1)" }}
                cursor={{ fill: "#f8fafc" }}
              />
              <Bar dataKey="present" fill="#3b82f6" name="Có mặt" radius={[4, 4, 0, 0]} />
              <Bar dataKey="absent" fill="#fca5a5" name="Vắng mặt" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Trend chart */}
        <div className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm">
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-gray-800">Xu hướng điểm danh</h3>
            <p className="text-xs text-gray-400 mt-0.5">Tỷ lệ % trung bình tháng</p>
          </div>
          <ResponsiveContainer width="100%" height={160}>
            <LineChart data={trendData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="month" tick={{ fontSize: 11, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: "#9ca3af" }} axisLine={false} tickLine={false} domain={[60, 100]} />
              <Tooltip
                contentStyle={{ borderRadius: "12px", border: "none", boxShadow: "0 4px 20px rgba(0,0,0,0.1)" }}
              />
              <Line
                type="monotone"
                dataKey="value"
                stroke="#6366f1"
                strokeWidth={2.5}
                dot={{ fill: "#6366f1", r: 4 }}
                name="Tỷ lệ %"
              />
            </LineChart>
          </ResponsiveContainer>
          <div className="mt-3 flex items-center gap-2">
            <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
              <div className="h-full bg-linear-to-r from-violet-500 to-blue-500 rounded-full" style={{ width: "89%" }} />
            </div>
            <span className="text-sm font-semibold text-gray-700">89%</span>
          </div>
        </div>
      </div>

      {/* Recent activity */}
      <div className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-lg font-semibold text-gray-800">Hoạt động gần đây</h3>
            <p className="text-xs text-gray-400 mt-0.5">Các sự kiện mới nhất trong hệ thống</p>
          </div>
          <button className="text-xs text-blue-600 hover:text-blue-700 font-medium">Xem tất cả</button>
        </div>
        <div className="space-y-3">
          {recentActivities.map((a, i) => (
            <div key={i} className="flex items-start gap-3 p-3 rounded-xl hover:bg-gray-50 transition-colors">
              <div className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 mt-0.5 ${
                a.type === "success" ? "bg-emerald-50 text-emerald-600" : "bg-orange-50 text-orange-600"
              }`}>
                {a.icon}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-700">{a.text}</p>
                <div className="flex items-center gap-1 mt-0.5">
                  <Clock size={10} className="text-gray-400" />
                  <span className="text-xs text-gray-400">{a.time}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default DashboardPage;
