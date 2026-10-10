import { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';

export default function Layout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const token = localStorage.getItem('adminToken') || sessionStorage.getItem('adminToken');
    if (!token && location.pathname !== '/login') {
      navigate('/login');
    }
  }, [navigate, location]);

  return (
    <div className="flex flex-col h-screen bg-slate-50">
      {/* Top Orange Header */}
      <Header toggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)} />
      
      {/* Below Header: Sidebar + Main Content */}
      <div className="flex flex-1 overflow-hidden relative">
        <Sidebar isOpen={isSidebarOpen} setIsOpen={setIsSidebarOpen} />
        
        <main className="flex-1 overflow-y-auto p-4 md:p-6 w-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
