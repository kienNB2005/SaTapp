import React, { useState, useEffect, useRef } from 'react';
import {
  UploadCloud, FileSpreadsheet, CheckCircle2, AlertTriangle,
  ArrowLeft, Download, RefreshCw, Home, Search, Calendar,
  Edit, Trash2, X, MapPin, Navigation
} from 'lucide-react';
import api from '../utils/api';
import { MapContainer, TileLayer, Marker, useMapEvents, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix marker icon issue with webpack/vite
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconAnchor: [12, 41]
});
L.Marker.prototype.options.icon = DefaultIcon;

function LocationMarker({ latitude, longitude, radius, setLat, setLng, disabled, onMapClick }) {
  const map = useMapEvents({
    click(e) {
      if(disabled) return;
      const lat = e.latlng.lat;
      const lng = e.latlng.lng;
      setLat(lat);
      setLng(lng);
      if (onMapClick) onMapClick(lat, lng);
    },
  });

  useEffect(() => {
    if (latitude && longitude) {
      map.flyTo([parseFloat(latitude), parseFloat(longitude)], map.getZoom() < 15 ? 15 : map.getZoom());
    }
  }, [latitude, longitude, map]);

  if (!latitude || !longitude) return null;
  
  return (
    <>
      <Marker position={[parseFloat(latitude), parseFloat(longitude)]} />
      {radius > 0 && (
        <Circle 
          center={[parseFloat(latitude), parseFloat(longitude)]} 
          radius={parseInt(radius)} 
          pathOptions={{ fillColor: 'var(--bl)', color: 'var(--bl)' }} 
        />
      )}
    </>
  );
}

export default function AdminRooms() {
  // --- VIEW STATE ---
  const [view, setView] = useState('list'); // 'list' | 'import'

  // --- LIST STATE ---
  const [rooms, setRooms] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [searchText, setSearchText] = useState('');

  // --- EDIT & DELETE STATE ---
  const [editingRoom, setEditingRoom] = useState(null);
  const [editBuilding, setEditBuilding] = useState('');
  const [editCapacity, setEditCapacity] = useState('');
  const [editLatitude, setEditLatitude] = useState('');
  const [editLongitude, setEditLongitude] = useState('');
  const [editGpsRadius, setEditGpsRadius] = useState('');
  const [editClearGps, setEditClearGps] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(null);

  // --- ADD STATE ---
  const [isAdding, setIsAdding] = useState(false);
  const [addCode, setAddCode] = useState('');
  const [addBuilding, setAddBuilding] = useState('');
  const [addCapacity, setAddCapacity] = useState('');
  const [addLatitude, setAddLatitude] = useState('');
  const [addLongitude, setAddLongitude] = useState('');
  const [addGpsRadius, setAddGpsRadius] = useState('');

  // --- MAP SEARCH STATE ---
  const [mapSearchText, setMapSearchText] = useState('');
  const [isSearchingMap, setIsSearchingMap] = useState(false);
  const [isGettingLocation, setIsGettingLocation] = useState(false);

  // --- IMPORT STATE ---
  const [importStep, setImportStep] = useState('upload');
  const [progress, setProgress] = useState(0);
  const [statusText, setStatusText] = useState('');
  const [importResult, setImportResult] = useState(null);
  const [importError, setImportError] = useState('');
  const fileInputRef = useRef(null);

  // =====================
  // FETCH ROOMS LIST
  // =====================
  const fetchRooms = async (signal) => {
    setListLoading(true);
    setListError('');
    try {
      const res = await api.get('/api/v1/rooms', { signal });
      setRooms(res.data.result || []);
    } catch (err) {
      if (err.name === 'AbortError' || err.name === 'CanceledError') return;
      setListError(err.response?.data?.message || 'Không thể tải danh sách phòng.');
    } finally {
      setListLoading(false);
    }
  };

  useEffect(() => {
    if (view !== 'list') return;
    const controller = new AbortController();
    fetchRooms(controller.signal);
    return () => controller.abort();
  }, [view]);

  // =====================
  // EDIT & DELETE HANDLERS
  // =====================
  const handleEditClick = (room) => {
    setEditingRoom(room);
    setEditBuilding(room.building || '');
    setEditCapacity(room.capacity || '');
    setEditLatitude(room.latitude || '');
    setEditLongitude(room.longitude || '');
    setEditGpsRadius(room.gpsRadiusM || '');
    setEditClearGps(false);
  };

  const handleSaveEdit = async () => {
    if (!editBuilding.trim() || !editCapacity) {
      alert('Vui lòng nhập Tòa nhà và Sức chứa!');
      return;
    }
    setIsSaving(true);
    try {
      const payload = {
        building: editBuilding,
        capacity: parseInt(editCapacity),
        latitude: editLatitude ? parseFloat(editLatitude) : null,
        longitude: editLongitude ? parseFloat(editLongitude) : null,
        gpsRadiusM: editGpsRadius ? parseInt(editGpsRadius) : null,
        clearGps: editClearGps
      };
      await api.put(`/api/v1/rooms/${editingRoom.id}`, payload);
      setEditingRoom(null);
      fetchRooms();
    } catch (err) {
      alert(err.response?.data?.message || 'Cập nhật thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (room) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa phòng "${room.code}" không?`)) return;
    setIsDeleting(room.id);
    try {
      await api.delete(`/api/v1/rooms/${room.id}`);
      fetchRooms();
    } catch (err) {
      alert(err.response?.data?.message || 'Xóa thất bại');
    } finally {
      setIsDeleting(null);
    }
  };

  const handleAddClick = () => {
    setIsAdding(true);
    setAddCode('');
    setAddBuilding('');
    setAddCapacity('');
    setAddLatitude('');
    setAddLongitude('');
    setAddGpsRadius('');
  };

  const handleSaveAdd = async () => {
    if (!addCode.trim() || !addBuilding.trim() || !addCapacity) {
      alert('Vui lòng nhập Mã phòng, Tòa nhà và Sức chứa!');
      return;
    }
    setIsSaving(true);
    try {
      const payload = {
        code: addCode,
        building: addBuilding,
        capacity: parseInt(addCapacity),
        latitude: addLatitude ? parseFloat(addLatitude) : null,
        longitude: addLongitude ? parseFloat(addLongitude) : null,
        gpsRadiusM: addGpsRadius ? parseInt(addGpsRadius) : null
      };
      await api.post('/api/v1/rooms', payload);
      setIsAdding(false);
      fetchRooms();
    } catch (err) {
      alert(err.response?.data?.message || 'Thêm mới thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  // =====================
  // MAP SEARCH
  // =====================
  const handleMapClick = (lat, lng) => {
    // Chỉ cập nhật tọa độ, không cập nhật tên Tòa nhà
  };

  const searchLocation = async () => {
    if (!mapSearchText.trim()) return;
    setIsSearchingMap(true);
    try {
      const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(mapSearchText)}&limit=1`);
      const data = await res.json();
      if (data && data.length > 0) {
        const result = data[0];
        if (isAdding) {
          setAddLatitude(parseFloat(result.lat));
          setAddLongitude(parseFloat(result.lon));
        } else {
          setEditLatitude(parseFloat(result.lat));
          setEditLongitude(parseFloat(result.lon));
        }
      } else {
        alert("Không tìm thấy địa điểm nào với từ khóa này.");
      }
    } catch (err) {
      alert("Lỗi khi tìm kiếm địa điểm.");
    } finally {
      setIsSearchingMap(false);
    }
  };

  const getCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert("Trình duyệt của bạn không hỗ trợ định vị GPS hoặc đang truy cập qua HTTP (cần HTTPS hoặc localhost).");
      return;
    }
    
    setIsGettingLocation(true);

    const updateLocation = (lat, lng) => {
      if (isAdding) {
        setAddLatitude(parseFloat(lat.toFixed(6)));
        setAddLongitude(parseFloat(lng.toFixed(6)));
      } else {
        setEditLatitude(parseFloat(lat.toFixed(6)));
        setEditLongitude(parseFloat(lng.toFixed(6)));
      }
    };

    navigator.geolocation.getCurrentPosition(
      (position) => {
        updateLocation(position.coords.latitude, position.coords.longitude);
        setIsGettingLocation(false);
      },
      (error) => {
        console.warn("Lỗi định vị high accuracy:", error);
        // Nếu dùng PC không có GPS hoặc timeout, fallback sang Low Accuracy
        navigator.geolocation.getCurrentPosition(
          (pos2) => {
            updateLocation(pos2.coords.latitude, pos2.coords.longitude);
            setIsGettingLocation(false);
          },
          (err2) => {
            setIsGettingLocation(false);
            console.error("Lỗi định vị low accuracy:", err2);
            alert("Không thể lấy vị trí hiện tại. Vui lòng cấp quyền truy cập vị trí cho trang web hoặc bật Location/GPS trên máy.");
          },
          { enableHighAccuracy: false, timeout: 15000, maximumAge: 60000 }
        );
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
    );
  };

  // =====================
  // IMPORT HANDLERS
  // =====================
  const handleImportClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.name.match(/\.(xlsx|xls|csv)$/)) {
      setImportError('Vui lòng chọn file Excel (.xlsx, .xls) hoặc CSV');
      setImportStep('error');
      e.target.value = null;
      return;
    }

    setImportStep('importing');
    setProgress(10);
    setStatusText('Đang tải file lên...');

    const formData = new FormData();
    formData.append('file', file);

    try {
      setTimeout(() => { setProgress(40); setStatusText('Đang đọc dữ liệu Excel...'); }, 500);

      const res = await api.post('/api/v1/rooms/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setProgress(100);
      setStatusText('Xử lý hoàn tất');
      setImportResult(res.data.result);
      setImportStep('success');

    } catch (err) {
      setProgress(0);
      setImportError(err.response?.data?.message || 'Đã xảy ra lỗi khi import dữ liệu. Vui lòng kiểm tra lại file.');
      setImportStep('error');
    }

    e.target.value = null;
  };

  const downloadTemplate = async () => {
    try {
      const res = await api.get('/api/v1/rooms/template', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Template_Import_Phong.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Không thể tải file mẫu. Vui lòng thử lại sau.');
    }
  };

  const closeImport = () => {
    setView('list');
    setImportStep('upload');
    setImportResult(null);
    setImportError('');
  };

  // =====================
  // RENDER HELPERS
  // =====================
  const formatDate = (isoString) => {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleDateString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
  };

  const filteredRooms = rooms.filter(r =>
    (r.code?.toLowerCase() || '').includes(searchText.toLowerCase()) ||
    (r.building?.toLowerCase() || '').includes(searchText.toLowerCase())
  );

  return (
    <div className="page active">
      {/* HEADER SECTION */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <div className="tb-title" style={{ fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Home size={24} color="var(--bl)" />
            Quản lý Phòng học
          </div>
          <div className="tb-sub">
            {view === 'list' ? `Danh sách Phòng · ${rooms.length} phòng trong hệ thống` : 'Import hàng loạt từ file Excel'}
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {view === 'list' ? (
            <>
              <button className="btn btn-s" onClick={() => fetchRooms()} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <RefreshCw size={14} /> Làm mới
              </button>
              <button className="btn btn-p" onClick={handleAddClick} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                Thêm mới
              </button>
              <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <UploadCloud size={14} /> Import Excel
              </button>
            </>
          ) : (
            <button className="btn btn-s" onClick={closeImport} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <ArrowLeft size={14} /> Quay lại danh sách
            </button>
          )}
        </div>
      </div>

      {/* ======================== */}
      {/* VIEW: IMPORT             */}
      {/* ======================== */}
      {view === 'import' && (
        <>
          {/* UPLOAD */}
          {importStep === 'upload' && (
            <div className="g2">
              <div className="card">
                <div className="card-h">
                  <div className="card-t">Tải lên file Excel</div>
                </div>
                <div style={{ padding: '30px' }}>
                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleFileChange}
                    accept=".xlsx, .xls, .csv"
                    style={{ display: 'none' }}
                  />
                  <div
                    className="upz"
                    onClick={() => fileInputRef.current?.click()}
                    style={{ padding: '50px 30px', cursor: 'pointer' }}
                  >
                    <FileSpreadsheet size={48} color="var(--bl)" style={{ marginBottom: '16px' }} />
                    <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '8px' }}>Nhấn để chọn file Excel</div>
                    <div style={{ fontSize: '13px', color: 'var(--tx3)', marginBottom: '24px' }}>Vui lòng sử dụng đúng file mẫu từ hệ thống</div>
                    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '11px', background: 'var(--bg3)', padding: '6px 12px', borderRadius: '20px', color: 'var(--tx2)' }}>
                      <CheckCircle2 size={12} color="var(--gr)" /> Chỉ hỗ trợ file .xlsx
                    </div>
                  </div>
                </div>
              </div>

              <div className="card" style={{ background: 'linear-gradient(135deg, var(--bg3), var(--bg2))', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '30px', textAlign: 'center' }}>
                <div style={{ width: '64px', height: '64px', borderRadius: '50%', background: 'linear-gradient(135deg, var(--blL), var(--puL))', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px', border: '1px solid var(--bd)' }}>
                  <Download size={32} color="var(--bl)" />
                </div>
                <h3 style={{ fontSize: '16px', marginBottom: '10px' }}>File mẫu Import Phòng học</h3>
                <p style={{ fontSize: '13px', color: 'var(--tx3)', lineHeight: '1.6', maxWidth: '300px' }}>
                  Hệ thống sẽ tự động <strong>thêm mới</strong> những Phòng học chưa có và <strong>cập nhật</strong> những Phòng học đã tồn tại dựa trên mã Phòng học.
                </p>
                <button className="btn btn-p" onClick={downloadTemplate} style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Download size={14} /> Tải file mẫu .xlsx
                </button>
              </div>
            </div>
          )}

          {importStep !== 'upload' && (
            <div className="card" style={{ marginBottom: '20px' }}>
              <div className="card-h" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div className="card-t">Nhập dữ liệu Phòng từ Excel</div>
              <div className="card-su">Hệ thống đang xử lý file tải lên...</div>
            </div>
            <button className="btn btn-s btn-sm" onClick={closeImport}>
              <ArrowLeft size={14} style={{ marginRight: '4px' }} /> Quay lại
            </button>
          </div>

          <div style={{ padding: '30px', maxWidth: '600px', margin: '0 auto' }}>
            {/* Importing State */}
            {importStep === 'importing' && (
              <div style={{ textAlign: 'center' }}>
                <FileSpreadsheet size={48} color="var(--bl)" className="pulse" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '16px', marginBottom: '8px' }}>{statusText}</h3>
                <div className="pb" style={{ marginBottom: '12px' }}>
                  <div className="pf bl" style={{ width: `${progress}%`, transition: 'width 0.3s ease' }}></div>
                </div>
                <p style={{ color: 'var(--tx3)', fontSize: '13px' }}>Vui lòng không đóng trình duyệt trong quá trình này...</p>
              </div>
            )}

            {/* Error State */}
            {importStep === 'error' && (
              <div style={{ textAlign: 'center' }}>
                <AlertTriangle size={48} color="var(--rd)" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '16px', color: 'var(--rd)', marginBottom: '8px' }}>Import thất bại</h3>
                <p style={{ color: 'var(--tx2)', fontSize: '14px', marginBottom: '24px' }}>{importError}</p>
                <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                  <button className="btn btn-s" onClick={closeImport}>Hủy bỏ</button>
                  <button className="btn btn-p" onClick={handleImportClick}>Thử lại file khác</button>
                </div>
              </div>
            )}

            {/* Success State */}
            {importStep === 'success' && importResult && (
              <div>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                  <CheckCircle2 size={48} color="var(--gr)" style={{ marginBottom: '16px' }} />
                  <h3 style={{ fontSize: '16px', color: 'var(--gr)', marginBottom: '8px' }}>Import dữ liệu thành công!</h3>
                  <p style={{ color: 'var(--tx2)', fontSize: '14px' }}>Hệ thống đã đọc và xử lý xong file dữ liệu của bạn.</p>
                </div>

                <div style={{ background: 'var(--bg3)', borderRadius: '12px', padding: '20px', marginBottom: '24px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--bd)' }}>
                    <span style={{ color: 'var(--tx2)' }}>Tổng số dòng hợp lệ:</span>
                    <span style={{ fontWeight: '600' }}>{importResult.totalRowsProcessed || 0} dòng</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--bd)' }}>
                    <span style={{ color: 'var(--tx2)' }}>Thêm mới thành công:</span>
                    <span style={{ fontWeight: '600', color: 'var(--gr)' }}>{importResult.successCount || 0} phòng</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--tx2)' }}>Cập nhật thông tin:</span>
                    <span style={{ fontWeight: '600', color: 'var(--bl)' }}>{importResult.updatedCount || 0} phòng</span>
                  </div>
                </div>

                {importResult.errors && importResult.errors.length > 0 && (
                  <div style={{ background: 'var(--rdL)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: '12px', padding: '20px', marginBottom: '24px' }}>
                    <h4 style={{ color: 'var(--rd)', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '12px' }}>
                      <AlertTriangle size={16} /> Có {importResult.errors.length} cảnh báo/lỗi:
                    </h4>
                    <ul style={{ margin: 0, paddingLeft: '20px', color: 'var(--tx2)', fontSize: '13px' }}>
                      {importResult.errors.map((err, idx) => (
                        <li key={idx} style={{ marginBottom: '4px' }}>{err}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div style={{ textAlign: 'center' }}>
                  <button className="btn btn-p" onClick={closeImport}>Đóng và Xem danh sách</button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
      </>
      )}

      {/* ======================== */}
      {/* VIEW: LIST               */}
      {/* ======================== */}
      {view === 'list' && (
        <>
          <div className="card">
            <div className="card-h">
              <div>
                <div className="card-t">Danh sách Phòng</div>
                <div className="card-su">Tất cả các phòng học hiện có trong hệ thống</div>
              </div>
              <div className="srch">
                <Search className="srch-ic" size={14} />
                <input
                  type="text"
                  placeholder="Tìm phòng, tòa nhà..."
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                />
              </div>
            </div>

            {/* Instructions */}
            <div style={{ padding: '16px 20px', background: 'var(--bg3)', borderBottom: '1px solid var(--bd)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'var(--blL)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <FileSpreadsheet size={16} color="var(--bl)" />
                </div>
                <div>
                  <div style={{ fontSize: '13px', fontWeight: '600' }}>Hướng dẫn nhập liệu</div>
                  <div style={{ fontSize: '12px', color: 'var(--tx3)' }}>Tải file mẫu Excel về, điền dữ liệu theo đúng cột và Upload lên hệ thống.</div>
                </div>
              </div>
              <button className="btn btn-s btn-sm" onClick={downloadTemplate} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Download size={14} /> Tải file mẫu
              </button>
            </div>

            {/* Loading */}
            {listLoading && (
              <div style={{ padding: '60px', textAlign: 'center' }}>
                <RefreshCw size={32} color="var(--bl)" className="pulse" style={{ marginBottom: '12px' }} />
                <p style={{ color: 'var(--tx3)', fontSize: '14px' }}>Đang tải dữ liệu...</p>
              </div>
            )}

            {/* Error */}
            {!listLoading && listError && (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <AlertTriangle size={32} color="var(--rd)" style={{ marginBottom: '12px' }} />
                <p style={{ color: 'var(--rd)', fontSize: '14px', marginBottom: '16px' }}>{listError}</p>
                <button className="btn btn-p" onClick={() => fetchRooms()}>Thử lại</button>
              </div>
            )}

            {/* Empty */}
            {!listLoading && !listError && filteredRooms.length === 0 && (
              <div style={{ padding: '60px', textAlign: 'center' }}>
                <Home size={48} color="var(--tx3)" style={{ marginBottom: '12px', opacity: 0.4 }} />
                <p style={{ color: 'var(--tx3)', fontSize: '14px', marginBottom: '16px' }}>
                  {searchText
                    ? `Không tìm thấy phòng nào với từ khóa "${searchText}"`
                    : 'Chưa có dữ liệu Phòng. Hãy Import file Excel để bắt đầu.'}
                </p>
                {!searchText && (
                  <button className="btn btn-p" onClick={() => setView('import')} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                    <UploadCloud size={14} /> Import Excel ngay
                  </button>
                )}
              </div>
            )}

            {/* Table */}
            {!listLoading && !listError && filteredRooms.length > 0 && (
              <div style={{ overflowX: 'auto' }}>
                <table className="tbl">
                  <thead>
                    <tr>
                      <th style={{ width: '50px' }}>#</th>
                      <th style={{ width: '120px' }}>Mã Phòng</th>
                      <th>Tòa nhà</th>
                      <th style={{ width: '100px', textAlign: 'center' }}>Sức chứa</th>
                      <th><span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><MapPin size={12} /> Tọa độ GPS</span></th>
                      <th style={{ width: '120px' }}><span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><Calendar size={12} /> Ngày tạo</span></th>
                      <th style={{ width: '100px', textAlign: 'center' }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredRooms.map((room, index) => (
                      <tr key={room.id}>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{index + 1}</td>
                        <td>
                          <span style={{
                            background: 'var(--blL)', color: 'var(--bl)',
                            padding: '3px 10px', borderRadius: '6px',
                            fontSize: '12px', fontWeight: '600', letterSpacing: '0.5px'
                          }}>
                            {room.code || '—'}
                          </span>
                        </td>
                        <td style={{ fontWeight: '500' }}>{room.building}</td>
                        <td style={{ textAlign: 'center', fontWeight: '600', color: 'var(--tx2)' }}>
                          {room.capacity}
                        </td>
                        <td>
                          {room.latitude && room.longitude ? (
                            <div style={{ fontSize: '12px', color: 'var(--tx2)' }}>
                              <span style={{ color: 'var(--bl)' }}>Lat:</span> {room.latitude.toFixed(5)}<br/>
                              <span style={{ color: 'var(--bl)' }}>Lng:</span> {room.longitude.toFixed(5)}<br/>
                              <span style={{ color: 'var(--gr)' }}>Bán kính:</span> {room.gpsRadiusM || 0}m
                            </div>
                          ) : (
                            <span style={{ fontSize: '12px', color: 'var(--tx3)', fontStyle: 'italic' }}>Chưa cấu hình GPS</span>
                          )}
                        </td>
                        <td style={{ color: 'var(--tx3)', fontSize: '12px' }}>{formatDate(room.createdAt)}</td>
                        <td style={{ textAlign: 'center' }}>
                          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px' }}
                              title="Sửa"
                              onClick={() => handleEditClick(room)}
                            >
                              <Edit size={14} color="var(--bl)" />
                            </button>
                            <button
                              className="btn btn-s"
                              style={{ padding: '6px', borderRadius: '6px', borderColor: 'rgba(239,68,68,0.3)' }}
                              title="Xóa"
                              onClick={() => handleDelete(room)}
                              disabled={isDeleting === room.id}
                            >
                              {isDeleting === room.id ? (
                                <RefreshCw size={14} color="var(--rd)" className="pulse" />
                              ) : (
                                <Trash2 size={14} color="var(--rd)" />
                              )}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            
            {!listLoading && !listError && filteredRooms.length > 0 && (
              <div style={{ padding: '12px 16px', borderTop: '1px solid var(--bd)', fontSize: '12px', color: 'var(--tx3)' }}>
                Hiển thị {filteredRooms.length}/{rooms.length} phòng
              </div>
            )}
          </div>
        </>
      )}

      {/* ======================== */}
      {/* EDIT MODAL               */}
      {/* ======================== */}
      {editingRoom && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '450px', padding: '24px', margin: '20px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Cập nhật Phòng học</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setEditingRoom(null)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            {/* Read-only Code */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Phòng (Chỉ đọc)</label>
              <div style={{ padding: '10px 14px', background: 'var(--bg3)', borderRadius: '8px', fontSize: '14px', color: 'var(--tx3)', fontWeight: '500' }}>
                {editingRoom.code}
              </div>
            </div>

            <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tòa nhà</label>
                <input
                  type="text"
                  value={editBuilding}
                  onChange={(e) => setEditBuilding(e.target.value)}
                  placeholder="Ví dụ: Tòa A"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)', background: 'var(--bg2)', color: 'var(--tx)' }}
                  autoFocus
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Sức chứa</label>
                <input
                  type="number"
                  min="1"
                  value={editCapacity}
                  onChange={(e) => setEditCapacity(e.target.value)}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
            </div>

            <div style={{ borderTop: '1px dashed var(--bd)', paddingTop: '16px', marginTop: '8px', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                <MapPin size={16} color="var(--tx2)" />
                <span style={{ fontSize: '14px', fontWeight: '600' }}>Cấu hình điểm danh GPS</span>
              </div>
              
              <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                <input 
                  type="text" 
                  placeholder="Tìm kiếm địa điểm (VD: Đại học Bách Khoa Hà Nội)..." 
                  value={mapSearchText}
                  onChange={(e) => setMapSearchText(e.target.value)}
                  onKeyDown={(e) => {
                    if(e.key === 'Enter') {
                      e.preventDefault();
                      searchLocation();
                    }
                  }}
                  style={{ flex: 1, padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                  disabled={editClearGps}
                />
                <button 
                  className="btn btn-s" 
                  onClick={(e) => { e.preventDefault(); searchLocation(); }} 
                  disabled={editClearGps || isSearchingMap}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  {isSearchingMap ? <RefreshCw size={14} className="pulse" /> : <Search size={14} />} Tìm
                </button>
                <button 
                  className="btn btn-p" 
                  onClick={(e) => { e.preventDefault(); getCurrentLocation(); }} 
                  disabled={editClearGps || isGettingLocation}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 12px' }}
                  title="Lấy vị trí của tôi"
                >
                  {isGettingLocation ? <RefreshCw size={14} className="pulse" /> : <Navigation size={14} />}
                </button>
              </div>

              <div style={{ width: '100%', height: '250px', borderRadius: '12px', overflow: 'hidden', marginBottom: '16px', border: '1px solid var(--bd)', position: 'relative', zIndex: 1 }}>
                <MapContainer 
                  center={editLatitude && editLongitude ? [editLatitude, editLongitude] : [21.028511, 105.804817]} 
                  zoom={15} 
                  style={{ width: '100%', height: '100%' }}
                >
                  <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                  />
                  <LocationMarker 
                    latitude={editLatitude} 
                    longitude={editLongitude} 
                    radius={editGpsRadius}
                    setLat={(lat) => setEditLatitude(lat.toFixed(6))}
                    setLng={(lng) => setEditLongitude(lng.toFixed(6))}
                    disabled={editClearGps}
                    onMapClick={handleMapClick}
                  />
                </MapContainer>
                {editClearGps && (
                  <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(255,255,255,0.7)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <span style={{ fontWeight: '600', color: 'var(--tx2)' }}>Đang tắt GPS cho phòng này</span>
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '16px', marginBottom: '12px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Vĩ độ (Latitude)</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={editLatitude}
                    onChange={(e) => setEditLatitude(e.target.value)}
                    placeholder="VD: 21.0382"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                    disabled={editClearGps}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Kinh độ (Longitude)</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={editLongitude}
                    onChange={(e) => setEditLongitude(e.target.value)}
                    placeholder="VD: 105.7827"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                    disabled={editClearGps}
                  />
                </div>
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Bán kính cho phép (mét)</label>
                <input
                  type="number"
                  min="0"
                  value={editGpsRadius}
                  onChange={(e) => setEditGpsRadius(e.target.value)}
                  placeholder="VD: 50"
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                  disabled={editClearGps}
                />
              </div>

              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '13px', color: 'var(--rd)' }}>
                <input 
                  type="checkbox" 
                  checked={editClearGps} 
                  onChange={(e) => setEditClearGps(e.target.checked)} 
                  style={{ cursor: 'pointer' }}
                />
                Xóa dữ liệu GPS hiện tại (tắt điểm danh bằng GPS cho phòng này)
              </label>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '24px' }}>
              <button className="btn btn-s" onClick={() => setEditingRoom(null)} disabled={isSaving}>Hủy</button>
              <button className="btn btn-p" onClick={handleSaveEdit} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <CheckCircle2 size={14} />} Lưu thay đổi
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================== */}
      {/* ADD MODAL                */}
      {/* ======================== */}
      {isAdding && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '450px', padding: '24px', margin: '20px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm Phòng học mới</h3>
              <button className="btn btn-s" style={{ padding: '4px', border: 'none' }} onClick={() => setIsAdding(false)}>
                <X size={20} color="var(--tx3)" />
              </button>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Mã Phòng</label>
              <input
                type="text"
                value={addCode}
                onChange={(e) => setAddCode(e.target.value)}
                placeholder="Ví dụ: P201"
                style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bl)', background: 'var(--bg3)', color: 'var(--tx)' }}
                autoFocus
              />
            </div>

            <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Tòa nhà</label>
                <input
                  type="text"
                  value={addBuilding}
                  onChange={(e) => setAddBuilding(e.target.value)}
                  placeholder="Ví dụ: Tòa A"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Sức chứa</label>
                <input
                  type="number"
                  min="1"
                  value={addCapacity}
                  onChange={(e) => setAddCapacity(e.target.value)}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)' }}
                />
              </div>
            </div>

            <div style={{ borderTop: '1px dashed var(--bd)', paddingTop: '16px', marginTop: '8px', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                <MapPin size={16} color="var(--tx2)" />
                <span style={{ fontSize: '14px', fontWeight: '600' }}>Cấu hình điểm danh GPS (tùy chọn)</span>
              </div>
              
              <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                <input 
                  type="text" 
                  placeholder="Tìm kiếm địa điểm (VD: Đại học Bách Khoa Hà Nội)..." 
                  value={mapSearchText}
                  onChange={(e) => setMapSearchText(e.target.value)}
                  onKeyDown={(e) => {
                    if(e.key === 'Enter') {
                      e.preventDefault();
                      searchLocation();
                    }
                  }}
                  style={{ flex: 1, padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                />
                <button 
                  className="btn btn-s" 
                  onClick={(e) => { e.preventDefault(); searchLocation(); }} 
                  disabled={isSearchingMap}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  {isSearchingMap ? <RefreshCw size={14} className="pulse" /> : <Search size={14} />} Tìm
                </button>
                <button 
                  className="btn btn-p" 
                  onClick={(e) => { e.preventDefault(); getCurrentLocation(); }} 
                  disabled={isGettingLocation}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 12px' }}
                  title="Lấy vị trí của tôi"
                >
                  {isGettingLocation ? <RefreshCw size={14} className="pulse" /> : <Navigation size={14} />}
                </button>
              </div>

              <div style={{ width: '100%', height: '250px', borderRadius: '12px', overflow: 'hidden', marginBottom: '16px', border: '1px solid var(--bd)', position: 'relative', zIndex: 1 }}>
                <MapContainer 
                  center={addLatitude && addLongitude ? [addLatitude, addLongitude] : [21.028511, 105.804817]} 
                  zoom={15} 
                  style={{ width: '100%', height: '100%' }}
                >
                  <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                  />
                  <LocationMarker 
                    latitude={addLatitude} 
                    longitude={addLongitude} 
                    radius={addGpsRadius}
                    setLat={(lat) => setAddLatitude(parseFloat(lat).toFixed(6))}
                    setLng={(lng) => setAddLongitude(parseFloat(lng).toFixed(6))}
                    disabled={false}
                    onMapClick={handleMapClick}
                  />
                </MapContainer>
              </div>

              <div style={{ display: 'flex', gap: '16px', marginBottom: '12px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Vĩ độ (Latitude)</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={addLatitude}
                    onChange={(e) => setAddLatitude(e.target.value)}
                    placeholder="VD: 21.0382"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Kinh độ (Longitude)</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={addLongitude}
                    onChange={(e) => setAddLongitude(e.target.value)}
                    placeholder="VD: 105.7827"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', color: 'var(--tx2)', marginBottom: '8px' }}>Bán kính cho phép (mét)</label>
                <input
                  type="number"
                  min="0"
                  value={addGpsRadius}
                  onChange={(e) => setAddGpsRadius(e.target.value)}
                  placeholder="VD: 50"
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--bd)', background: 'var(--bg2)', color: 'var(--tx)', fontSize: '13px' }}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '24px' }}>
              <button className="btn btn-s" onClick={() => setIsAdding(false)} disabled={isSaving}>Hủy</button>
              <button className="btn btn-p" onClick={handleSaveAdd} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isSaving ? <RefreshCw size={14} className="pulse" /> : <CheckCircle2 size={14} />} Thêm mới
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
