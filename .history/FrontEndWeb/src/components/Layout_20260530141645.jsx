import React, { useState, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

export default function Layout() {
  const location = useLocation();
  const [role, setRole] = useState('gv');

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      try {
        const decoded = jwtDecode(token);
        const userRoles = decoded.roles;
        const rolesArray = Array.isArray(userRoles) ? userRoles : (userRoles ? [userRoles] : []);
        
        if (rolesArray.some(r => r && (String(r).toUpperCase() === 'ADMIN' || String(r).toUpperCase() === 'ROLE_ADMIN'))) {
          setRole('admin');
        } else {
          setRole('gv');
        }
      } catch (error) {
        console.error("Lỗi giải mã token trong Layout:", error);
      }
    }
  }, []);

  return (
    <div className="flex min-h-screen bg-gray-50">
      <Sidebar role={role} />
      <div className="flex flex-col flex-1">
        <Topbar />
        <main className="flex-1 overflow-auto">
          <Outlet context={{ role }} />
        </main>
      </div>
    </div>
  );
}
