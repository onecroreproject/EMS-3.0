import { Search, Bell, User, Menu, LogOut, KeyRound } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Header({ toggleSidebar }: { toggleSidebar: () => void }) {
  const navigate = useNavigate();
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setIsProfileOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('adminToken');
    sessionStorage.removeItem('adminToken');
    navigate('/login');
  };

  const handleChangePassword = () => {
    setIsProfileOpen(false);
    navigate('/admin/profile');
  };

  return (
    <header className="h-16 bg-[#F96D3E] dark:bg-[#0F172A] text-white flex items-center justify-between px-4 md:px-6 shadow-md z-10 shrink-0 transition-colors duration-300">
      
      {/* Left: Mobile Menu & Logo */}
      <div className="flex items-center gap-3 w-auto md:w-64">
        <button 
          onClick={toggleSidebar}
          className="md:hidden p-1 hover:bg-orange-600 rounded text-white"
        >
          <Menu className="h-6 w-6" />
        </button>

        <div className="hidden md:flex bg-white/20 p-2 rounded-full">
          <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div className="hidden md:block">
          <h1 className="text-xl font-bold leading-tight">EmpMonitor</h1>
          <p className="text-[10px] text-orange-100">Employee Monitoring System</p>
        </div>
      </div>

      {/* Middle: Search Bar */}
      <div className="flex-1 max-w-2xl px-2 md:px-4 hidden sm:block">
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-4 w-4 text-gray-400" />
          </div>
          <input 
            type="text" 
            placeholder="Search employee, device or report..." 
            className="w-full pl-10 pr-4 py-2 rounded-full bg-white text-gray-800 focus:outline-none focus:ring-2 focus:ring-orange-300 text-sm md:text-base"
          />
        </div>
      </div>

      {/* Right: Profile & Notifications */}
      <div className="flex items-center gap-3 md:gap-6 ml-auto sm:ml-0">
        <button className="sm:hidden p-1 hover:bg-orange-600 rounded-full transition">
          <Search className="h-5 w-5" />
        </button>

        {/* Dark Mode Toggle */}
        <button 
          onClick={() => {
            const isDark = document.documentElement.classList.toggle('dark');
            localStorage.setItem('theme', isDark ? 'dark' : 'light');
            // Trigger a custom event in case other components need to know
            window.dispatchEvent(new Event('theme-change'));
          }}
          className="p-2 hover:bg-orange-600 hover:scale-110 rounded-full transition-all duration-300"
          title="Toggle Dark/Light Mode"
        >
          <svg className="h-5 w-5 block dark:hidden" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            {/* Moon icon for Light Mode (to switch to dark) */}
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
          </svg>
          <svg className="h-5 w-5 hidden dark:block text-yellow-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            {/* Sun icon for Dark Mode (to switch to light) */}
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
        </button>

        <button className="relative p-2 hover:bg-orange-600 hover:scale-110 rounded-full transition-all duration-300">
          <Bell className="h-5 w-5" />
          <span className="absolute top-0 right-0 h-4 w-4 bg-red-600 border-2 border-[#F96D3E] rounded-full text-[8px] flex items-center justify-center font-bold">
            3
          </span>
        </button>
        
        {/* Profile Dropdown */}
        <div className="relative" ref={profileRef}>
          <div 
            onClick={() => setIsProfileOpen(!isProfileOpen)}
            className="flex items-center gap-2 md:gap-3 cursor-pointer hover:bg-orange-600 p-1 md:pr-3 rounded-full transition"
          >
            <div className="h-8 w-8 bg-white rounded-full flex items-center justify-center text-[#F96D3E]">
              <User className="h-5 w-5" />
            </div>
            <div className="hidden md:block text-left">
              <p className="text-sm font-semibold leading-none">Admin</p>
              <p className="text-xs text-orange-100">Administrator</p>
            </div>
          </div>

          {/* Dropdown Menu */}
          {isProfileOpen && (
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-lg border border-slate-100 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-200">
              <div className="px-4 py-3 border-b border-slate-100">
                <p className="text-sm text-slate-800 font-semibold">Signed in as Admin</p>
              </div>
              <div className="py-1">
                <button 
                  onClick={() => { setIsProfileOpen(false); navigate('/admin/profile'); }}
                  className="w-full text-left px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 flex items-center gap-2 transition-colors"
                >
                  <User className="h-4 w-4 text-slate-400" />
                  My Profile
                </button>
                <button 
                  onClick={handleChangePassword}
                  className="w-full text-left px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 flex items-center gap-2 transition-colors"
                >
                  <KeyRound className="h-4 w-4 text-slate-400" />
                  Change Password
                </button>
                <button 
                  onClick={handleLogout}
                  className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center gap-2 transition-colors"
                >
                  <LogOut className="h-4 w-4 text-red-400" />
                  Sign out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
      
    </header>
  );
}
