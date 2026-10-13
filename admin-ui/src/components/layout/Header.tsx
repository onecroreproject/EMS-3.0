import { Search, Bell, User, Menu, LogOut, KeyRound } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Header({ toggleSidebar }: { toggleSidebar: () => void }) {
  const navigate = useNavigate();
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(() => {
    if (typeof window !== 'undefined') {
      return document.documentElement.classList.contains('dark');
    }
    return false;
  });

  const profileRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLDivElement>(null);

  // Sync theme with document element
  useEffect(() => {
    const handleThemeChange = () => {
      setIsDarkMode(document.documentElement.classList.contains('dark'));
    };
    window.addEventListener('theme-change', handleThemeChange);
    setIsDarkMode(document.documentElement.classList.contains('dark'));
    return () => window.removeEventListener('theme-change', handleThemeChange);
  }, []);

  const handleToggleTheme = () => {
    const nextDark = !isDarkMode;
    setIsDarkMode(nextDark);
    if (nextDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
    window.dispatchEvent(new Event('theme-change'));
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setIsProfileOpen(false);
      }
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsSearchFocused(false);
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
    <header 
      className={`h-16 text-white flex items-center justify-between px-4 md:px-6 shadow-md dark:shadow-none dark:border-b dark:border-slate-800/80 z-10 shrink-0 transition-colors duration-300 ease-in-out ${
        isDarkMode ? 'bg-[#0F172A]' : 'bg-[#F96D3E]'
      }`}
    >
      
      {/* Left: Mobile Menu & Logo */}
      <div className="flex items-center gap-3 w-auto md:w-64">
        <button 
          onClick={toggleSidebar}
          className={`md:hidden p-1 rounded text-white transition-colors duration-300 ${
            isDarkMode ? 'hover:bg-slate-800' : 'hover:bg-orange-600'
          }`}
        >
          <Menu className="h-6 w-6" />
        </button>

        <div className={`hidden md:flex p-2 rounded-full transition-colors duration-300 ${
          isDarkMode ? 'bg-blue-500/20' : 'bg-white/20'
        }`}>
          <svg className={`w-6 h-6 ${isDarkMode ? 'text-blue-300' : 'text-white'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div className="hidden md:block">
          <h1 className="text-xl font-bold leading-tight">EmpMonitor</h1>
          <p className={`text-[10px] transition-colors duration-300 ${isDarkMode ? 'text-slate-400' : 'text-orange-100'}`}>
            Employee Monitoring System
          </p>
        </div>
      </div>

      {/* Middle: Search Bar with Smooth Expanding Transition & Dynamic Color Shift */}
      <div className="flex-1 px-2 md:px-4 hidden sm:flex justify-center">
        <div 
          ref={searchRef}
          className={`relative transition-all duration-300 ease-in-out ${isSearchFocused ? 'w-full max-w-2xl' : 'w-full max-w-md hover:max-w-lg'}`}
        >
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none">
            <Search className={`h-4 w-4 transition-all duration-300 ${
              isSearchFocused 
                ? (isDarkMode ? 'text-blue-400 scale-110' : 'text-[#F96D3E] scale-110') 
                : (isDarkMode ? 'text-slate-400' : 'text-gray-400')
            }`} />
          </div>
          <input 
            type="text" 
            onFocus={() => setIsSearchFocused(true)}
            onBlur={() => setIsSearchFocused(false)}
            placeholder="Search employee by name or email..." 
            className={`w-full pl-10 pr-4 py-2 rounded-full transition-all duration-300 text-sm md:text-base outline-none ${
              isDarkMode 
                ? (isSearchFocused 
                    ? 'bg-[#1E2B3C] border-2 border-blue-500 shadow-[0_0_0_4px_rgba(59,130,246,0.35)] text-white placeholder-slate-400' 
                    : 'bg-[#1E2B3C] border-2 border-slate-700/80 text-slate-200 placeholder-slate-400 hover:border-slate-600')
                : (isSearchFocused 
                    ? 'bg-white border-2 border-[#F96D3E] shadow-[0_0_0_4px_rgba(249,109,62,0.3)] text-gray-900 placeholder-gray-400' 
                    : 'bg-white border-2 border-transparent text-gray-800 placeholder-gray-400 shadow-sm hover:border-orange-200')
            }`}
          />
        </div>
      </div>

      {/* Right: Profile & Notifications */}
      <div className="flex items-center gap-3 md:gap-6 ml-auto sm:ml-0">
        <button className={`sm:hidden p-1 rounded-full transition ${isDarkMode ? 'hover:bg-slate-800' : 'hover:bg-orange-600'}`}>
          <Search className="h-5 w-5" />
        </button>

        {/* Dark Mode Toggle */}
        <button 
          onClick={handleToggleTheme}
          className={`p-2 hover:scale-110 rounded-full transition-all duration-300 cursor-pointer ${
            isDarkMode ? 'hover:bg-slate-800 text-yellow-300' : 'hover:bg-orange-600 text-white'
          }`}
          title={isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode"}
        >
          {isDarkMode ? (
            <svg className="h-5 w-5 text-yellow-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          ) : (
            <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
          )}
        </button>

        <button className={`relative p-2 hover:scale-110 rounded-full transition-all duration-300 cursor-pointer ${
          isDarkMode ? 'hover:bg-slate-800' : 'hover:bg-orange-600'
        }`}>
          <Bell className="h-5 w-5" />
          <span className={`absolute top-0 right-0 h-4 w-4 bg-red-600 border-2 rounded-full text-[8px] flex items-center justify-center font-bold ${
            isDarkMode ? 'border-[#0F172A]' : 'border-[#F96D3E]'
          }`}>
            3
          </span>
        </button>
        
        {/* Profile Dropdown */}
        <div className="relative" ref={profileRef}>
          <div 
            onClick={() => setIsProfileOpen(!isProfileOpen)}
            className={`flex items-center gap-2 md:gap-3 cursor-pointer p-1 md:pr-3 rounded-full transition-colors duration-300 ${
              isDarkMode ? 'hover:bg-slate-800' : 'hover:bg-orange-600'
            }`}
          >
            <div className="h-8 w-8 bg-white text-[#F96D3E] rounded-full flex items-center justify-center shadow-sm">
              <User className="h-5 w-5" />
            </div>
            <div className="hidden md:block text-left">
              <p className="text-sm font-semibold leading-none text-white">Admin</p>
              <p className={`text-xs transition-colors duration-300 ${isDarkMode ? 'text-slate-400' : 'text-orange-100'}`}>
                Administrator
              </p>
            </div>
          </div>

          {/* Dropdown Menu */}
          {isProfileOpen && (
            <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-[#1E2B3C] rounded-xl shadow-lg border border-slate-100 dark:border-slate-700/80 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-200">
              <div className="px-4 py-3 border-b border-slate-100 dark:border-slate-700/80">
                <p className="text-sm text-slate-800 dark:text-slate-100 font-semibold">Signed in as Admin</p>
              </div>
              <div className="py-1">
                <button 
                  onClick={() => { setIsProfileOpen(false); navigate('/admin/profile'); }}
                  className="w-full text-left px-4 py-2 text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700/50 flex items-center gap-2 transition-colors cursor-pointer"
                >
                  <User className="h-4 w-4 text-slate-400" />
                  My Profile
                </button>
                <button 
                  onClick={handleChangePassword}
                  className="w-full text-left px-4 py-2 text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700/50 flex items-center gap-2 transition-colors cursor-pointer"
                >
                  <KeyRound className="h-4 w-4 text-slate-400" />
                  Change Password
                </button>
                <button 
                  onClick={handleLogout}
                  className="w-full text-left px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10 flex items-center gap-2 transition-colors cursor-pointer"
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
