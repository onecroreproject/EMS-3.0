import { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';

export default function Layout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Check dark mode preference: only dark if explicitly saved as 'dark'
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }

    const token = localStorage.getItem('adminToken') || sessionStorage.getItem('adminToken');
    if (!token && location.pathname !== '/login') {
      navigate('/login');
    }
  }, [navigate, location]);

  return (
    <div className="flex flex-col h-screen w-full max-w-full overflow-x-hidden bg-slate-50 dark:bg-[#0b0f19] transition-colors duration-300">
      {/* Top Orange Header */}
      <Header toggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)} />

      {/* Below Header: Sidebar + Main Content */}
      <div className="flex flex-1 overflow-hidden relative z-0 w-full max-w-full">
        <Sidebar isOpen={isSidebarOpen} setIsOpen={setIsSidebarOpen} />

        <main className="flex-1 overflow-y-auto overflow-x-hidden p-3 sm:p-4 md:p-6 w-full max-w-full no-scrollbar">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
