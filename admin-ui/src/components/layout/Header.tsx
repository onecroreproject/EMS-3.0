import { Search, Bell, User, Menu, LogOut, KeyRound, X, Loader2, ChevronRight } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../lib/axios';
import NotificationDropdown from '../notifications/NotificationDropdown';
import AllNotificationsDrawer from '../notifications/AllNotificationsDrawer';
import { INITIAL_NOTIFICATIONS } from '../notifications/notificationData';
import type { NotificationItem } from '../notifications/types';
import { notify } from '../../utils/toast';

interface SearchEmployee {
  id: string;
  name: string;
  email: string;
  employeeCode?: string;
  designation?: string;
  departmentName?: string;
  teamName?: string;
  status?: string;
}

export default function Header({ toggleSidebar }: { toggleSidebar: () => void }) {
  const navigate = useNavigate();
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [isMobileSearchOpen, setIsMobileSearchOpen] = useState(false);
  const [searchValue, setSearchValue] = useState('');
  const [searchResults, setSearchResults] = useState<SearchEmployee[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isSearchDropdownOpen, setIsSearchDropdownOpen] = useState(false);
  const mobileInputRef = useRef<HTMLInputElement>(null);
  const desktopInputRef = useRef<HTMLInputElement>(null);
  const [isDarkMode, setIsDarkMode] = useState(() => {
    if (typeof window !== 'undefined') {
      return document.documentElement.classList.contains('dark');
    }
    return false;
  });

  // Dynamic Live Search: Fetch from Backend on debounced input
  useEffect(() => {
    const query = searchValue.trim();
    if (!query) {
      setSearchResults([]);
      setIsSearching(false);
      setIsSearchDropdownOpen(false);
      return;
    }

    setIsSearching(true);
    setIsSearchDropdownOpen(true);

    const timer = setTimeout(async () => {
      try {
        const response = await apiClient.get(`/api/admin/employees?search=${encodeURIComponent(query)}&size=6`);
        const content = response.data?.content || [];
        setSearchResults(content);
      } catch (err) {
        console.error("Failed to fetch search results:", err);
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [searchValue]);

  // Notification States & Dynamic Helpers
  const formatTimeAgo = (dateInput?: string | Date): string => {
    if (!dateInput) return 'Recently';
    const date = new Date(dateInput);
    if (isNaN(date.getTime())) return 'Recently';
    
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return 'Just now';
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) return `${diffInMinutes} minute${diffInMinutes > 1 ? 's' : ''} ago`;
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays === 1) return 'Yesterday';
    if (diffInDays < 7) return `${diffInDays} days ago`;
    const diffInWeeks = Math.floor(diffInDays / 7);
    if (diffInWeeks < 4) return `${diffInWeeks} week${diffInWeeks > 1 ? 's' : ''} ago`;
    return date.toLocaleDateString();
  };

  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    const saved = localStorage.getItem('admin_notifications');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      } catch (e) {}
    }
    return INITIAL_NOTIFICATIONS;
  });
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [isAllNotificationsOpen, setIsAllNotificationsOpen] = useState(false);

  const unreadCount = notifications.filter(n => !n.isRead).length;

  const fetchNotifications = async () => {
    try {
      const response = await apiClient.get('/api/admin/notifications');
      const rawList = response.data?.notifications;
      if (Array.isArray(rawList)) {
        const mapped: NotificationItem[] = rawList.map((item: any) => ({
          id: item.id || item._id,
          title: item.title,
          description: item.description,
          category: item.category || 'System',
          timestamp: item.createdAt || new Date().toISOString(),
          timeAgo: formatTimeAgo(item.createdAt),
          isRead: !!item.read || !!item.isRead,
          iconType: item.iconType || 'system',
          link: item.link,
          createdAt: item.createdAt,
        }));
        setNotifications(mapped);
        localStorage.setItem('admin_notifications', JSON.stringify(mapped));
      }
    } catch (err) {
      console.error("Failed to fetch notifications from backend:", err);
    }
  };

  useEffect(() => {
    fetchNotifications();
    // Auto-poll notifications every 30 seconds
    const interval = setInterval(fetchNotifications, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleMarkAllAsRead = async () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    notify.success('All notifications marked as read');
    try {
      await apiClient.put('/api/admin/notifications/mark-all-read');
    } catch (err) {
      console.error("Failed to mark all notifications read on backend:", err);
    }
  };

  const handleToggleRead = async (id: string) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: !n.isRead } : n));
    try {
      await apiClient.put(`/api/admin/notifications/${id}/read`);
    } catch (err) {
      console.error("Failed to toggle notification read on backend:", err);
    }
  };

  const handleDeleteNotification = async (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
    notify.delete('Notification removed');
    try {
      await apiClient.delete(`/api/admin/notifications/${id}`);
    } catch (err) {
      console.error("Failed to delete notification on backend:", err);
    }
  };

  const handleItemClick = async (id: string) => {
    const item = notifications.find(n => n.id === id);
    if (item && !item.isRead) {
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
      try {
        await apiClient.put(`/api/admin/notifications/${id}/read`);
      } catch (err) {
        console.error("Failed to mark notification read on backend:", err);
      }
    }
    if (item?.link) {
      setIsNotificationOpen(false);
      navigate(item.link);
    }
  };

  const profileRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLFormElement>(null);
  const mobileSearchRef = useRef<HTMLDivElement>(null);
  const mobileSearchButtonRef = useRef<HTMLButtonElement>(null);

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

  // Close dropdown or mobile search when clicking outside or pressing Escape
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setIsProfileOpen(false);
      }
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsSearchFocused(false);
        setIsSearchDropdownOpen(false);
      }
      if (
        mobileSearchRef.current &&
        !mobileSearchRef.current.contains(event.target as Node) &&
        mobileSearchButtonRef.current &&
        !mobileSearchButtonRef.current.contains(event.target as Node)
      ) {
        setIsMobileSearchOpen(false);
        setIsSearchDropdownOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsMobileSearchOpen(false);
        setIsSearchFocused(false);
        setIsSearchDropdownOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      window.removeEventListener("keydown", handleKeyDown);
    };
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

  const handleSelectEmployee = (emp: SearchEmployee) => {
    setIsSearchDropdownOpen(false);
    setIsMobileSearchOpen(false);
    setIsSearchFocused(false);
    navigate(`/employees/view?search=${encodeURIComponent(emp.name || emp.employeeCode || '')}`);
  };

  const handleSearchSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const query = searchValue.trim();
    setIsSearchDropdownOpen(false);
    setIsMobileSearchOpen(false);
    setIsSearchFocused(false);
    if (query) {
      navigate(`/employees/view?search=${encodeURIComponent(query)}`);
    } else {
      navigate('/employees/view');
    }
  };

  const renderSearchDropdown = () => {
    if (!isSearchDropdownOpen || !searchValue.trim()) return null;

    return (
      <div 
        onMouseDown={(e) => e.stopPropagation()} 
        className="absolute left-0 right-0 top-full mt-2 bg-white dark:bg-[#1E2B3C] rounded-2xl shadow-2xl border border-slate-100 dark:border-slate-700/80 overflow-hidden z-50 text-slate-800 dark:text-slate-100 animate-in fade-in slide-in-from-top-2 duration-200"
      >
        {/* Header summary */}
        <div className="px-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border-b border-slate-100 dark:border-slate-700/70 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
          <span className="font-semibold">Employees</span>
          {isSearching ? (
            <span className="flex items-center gap-1.5 text-blue-600 dark:text-blue-400 font-medium">
              <Loader2 className="w-3.5 h-3.5 animate-spin" /> Fetching from backend...
            </span>
          ) : (
            <span>{searchResults.length} match{searchResults.length === 1 ? '' : 'es'} found</span>
          )}
        </div>

        {/* Results List */}
        <div className="max-h-72 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-700/50">
          {isSearching && searchResults.length === 0 ? (
            <div className="p-6 text-center text-sm text-slate-400 flex flex-col items-center justify-center gap-2.5">
              <Loader2 className="w-6 h-6 animate-spin text-orange-500 dark:text-blue-400" />
              <span className="text-xs font-medium">Searching backend database...</span>
            </div>
          ) : searchResults.length === 0 ? (
            <div className="p-6 text-center text-sm text-slate-400">
              <p className="font-semibold text-slate-700 dark:text-slate-300 mb-0.5">No employees found</p>
              <p className="text-xs">No matching employee for "{searchValue}"</p>
            </div>
          ) : (
            searchResults.map((emp) => (
              <div
                key={emp.id}
                onClick={() => handleSelectEmployee(emp)}
                className="p-3 sm:p-3.5 flex items-center justify-between gap-3 hover:bg-slate-50 dark:hover:bg-slate-800/70 cursor-pointer transition-colors group text-left"
              >
                <div className="flex items-center gap-3 min-w-0">
                  {/* Avatar Initial with Status Dot */}
                  <div className="relative shrink-0">
                    <div className="w-9 h-9 rounded-xl bg-orange-100 dark:bg-blue-500/20 text-[#F96D3E] dark:text-blue-400 font-bold text-sm flex items-center justify-center">
                      {(emp.name || 'E').charAt(0).toUpperCase()}
                    </div>
                    <span 
                      className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-white dark:border-[#1E2B3C] ${
                        emp.status === 'Active' ? 'bg-green-500' : 'bg-slate-400'
                      }`} 
                      title={emp.status || 'Active'}
                    />
                  </div>

                  {/* Employee Details */}
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold text-slate-900 dark:text-white truncate group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                        {emp.name}
                      </p>
                      {emp.employeeCode && (
                        <span className="text-[11px] font-mono font-medium px-1.5 py-0.2 rounded bg-slate-100 dark:bg-slate-700/80 text-slate-600 dark:text-slate-300 shrink-0">
                          {emp.employeeCode}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 truncate flex items-center gap-1.5 mt-0.5">
                      <span>{emp.departmentName || 'No Department'}</span>
                      {emp.designation && (
                        <>
                          <span className="opacity-60">•</span>
                          <span>{emp.designation}</span>
                        </>
                      )}
                    </p>
                  </div>
                </div>

                <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-blue-500 group-hover:translate-x-0.5 transition-all shrink-0" />
              </div>
            ))
          )}
        </div>

        {/* Footer Link */}
        {searchResults.length > 0 && (
          <div className="p-2.5 bg-slate-50/80 dark:bg-slate-800/40 border-t border-slate-100 dark:border-slate-700/60 text-center">
            <button
              type="button"
              onClick={handleSearchSubmit}
              className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline cursor-pointer"
            >
              View all results in Employee List &rarr;
            </button>
          </div>
        )}
      </div>
    );
  };

  return (
    <header 
      className={`h-16 text-white flex items-center justify-between px-4 md:px-6 shadow-md dark:shadow-none dark:border-b dark:border-slate-800/80 z-10 shrink-0 transition-colors duration-300 ease-in-out relative ${
        isDarkMode ? 'bg-[#0F172A]' : 'bg-[#F96D3E]'
      }`}
    >
      {/* Mobile Expanding Search Bar (Smooth Slide & Expand Transitions) */}
      <div 
        ref={mobileSearchRef}
        className={`sm:hidden absolute inset-0 z-40 flex items-center px-3.5 gap-2 transition-all duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] ${
          isDarkMode ? 'bg-[#0F172A]' : 'bg-[#F96D3E]'
        } ${
          isMobileSearchOpen 
            ? 'opacity-100 translate-y-0 scale-100 pointer-events-auto' 
            : 'opacity-0 -translate-y-2 scale-[0.98] pointer-events-none'
        }`}
      >
        <form onSubmit={handleSearchSubmit} className="flex-1 relative flex items-center">
          <button
            type="submit"
            className="absolute inset-y-0 left-0 pl-3.5 flex items-center cursor-pointer text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            title="Search"
          >
            <Search className={`h-4 w-4 transition-all duration-300 ${
              isDarkMode ? 'text-blue-400 scale-110' : 'text-[#F96D3E] scale-110'
            }`} />
          </button>
          <input
            ref={mobileInputRef}
            type="text"
            enterKeyHint="search"
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            placeholder="Search employee by name or email..."
            className={`w-full pl-10 pr-9 py-2 rounded-full text-sm outline-none transition-all duration-300 ${
              isDarkMode 
                ? 'bg-[#1E2B3C] border-2 border-blue-500 shadow-[0_0_0_4px_rgba(59,130,246,0.35)] text-white placeholder-slate-400' 
                : 'bg-white border-2 border-[#F96D3E] shadow-[0_0_0_4px_rgba(249,109,62,0.3)] text-gray-900 placeholder-gray-400'
            }`}
          />
          {searchValue && (
            <button
              type="button"
              onClick={() => {
                setSearchValue('');
                mobileInputRef.current?.focus();
              }}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 p-0.5 cursor-pointer"
              title="Clear search"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
          {/* Dynamic Live Backend Search Results */}
          {renderSearchDropdown()}
        </form>
        <button
          type="button"
          onClick={() => {
            setIsMobileSearchOpen(false);
            setIsSearchDropdownOpen(false);
          }}
          className={`p-2 rounded-full transition-all duration-200 cursor-pointer shrink-0 ${
            isDarkMode ? 'hover:bg-slate-800 text-slate-300' : 'hover:bg-orange-600 text-white'
          }`}
          title="Close search"
        >
          <X className="w-5 h-5" />
        </button>
      </div>
      
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
        <form 
          onSubmit={handleSearchSubmit}
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
            ref={desktopInputRef}
            type="text" 
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            onFocus={() => {
              setIsSearchFocused(true);
              if (searchValue.trim().length > 0) setIsSearchDropdownOpen(true);
            }}
            placeholder="Search employee by name or email..." 
            className={`w-full pl-10 pr-9 py-2 rounded-full transition-all duration-300 text-sm md:text-base outline-none ${
              isDarkMode 
                ? (isSearchFocused 
                    ? 'bg-[#1E2B3C] border-2 border-blue-500 shadow-[0_0_0_4px_rgba(59,130,246,0.35)] text-white placeholder-slate-400' 
                    : 'bg-[#1E2B3C] border-2 border-slate-700/80 text-slate-200 placeholder-slate-400 hover:border-slate-600')
                : (isSearchFocused 
                    ? 'bg-white border-2 border-[#F96D3E] shadow-[0_0_0_4px_rgba(249,109,62,0.3)] text-gray-900 placeholder-gray-400' 
                    : 'bg-white border-2 border-transparent text-gray-800 placeholder-gray-400 shadow-sm hover:border-orange-200')
            }`}
          />
          {searchValue && (
            <button
              type="button"
              onClick={() => {
                setSearchValue('');
                setIsSearchDropdownOpen(false);
              }}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 p-0.5 cursor-pointer"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
          {/* Dynamic Live Backend Search Results */}
          {renderSearchDropdown()}
        </form>
      </div>

      {/* Right: Profile & Notifications */}
      <div className="flex items-center gap-3 md:gap-6 ml-auto sm:ml-0">
        <button 
          ref={mobileSearchButtonRef}
          onClick={() => {
            setIsMobileSearchOpen(prev => {
              const next = !prev;
              if (next) {
                setTimeout(() => mobileInputRef.current?.focus(), 80);
              }
              return next;
            });
          }}
          className={`sm:hidden p-2 rounded-full transition-all duration-200 cursor-pointer ${
            isDarkMode 
              ? (isMobileSearchOpen ? 'bg-slate-800 text-blue-400' : 'hover:bg-slate-800 text-slate-200') 
              : (isMobileSearchOpen ? 'bg-orange-700 text-white' : 'hover:bg-orange-600 text-white')
          }`}
          title="Search"
        >
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

        {/* Notification Bell & Dropdown */}
        <div className="relative">
          <button 
            onClick={() => setIsNotificationOpen(!isNotificationOpen)}
            className={`relative p-2 hover:scale-105 rounded-full transition-all duration-300 cursor-pointer ${
              isDarkMode ? 'hover:bg-slate-800' : 'hover:bg-orange-600'
            }`}
            title="Notifications"
          >
            <Bell className="h-5 w-5" />
            {unreadCount > 0 && (
              <span className={`absolute top-0 right-0 h-4 min-w-4 px-1 bg-red-600 border-2 rounded-full text-[9px] flex items-center justify-center font-bold text-white shadow-sm ${
                isDarkMode ? 'border-[#0F172A]' : 'border-[#F96D3E]'
              }`}>
                {unreadCount}
              </span>
            )}
          </button>

          <NotificationDropdown
            isOpen={isNotificationOpen}
            onClose={() => setIsNotificationOpen(false)}
            notifications={notifications}
            unreadCount={unreadCount}
            onMarkAllAsRead={handleMarkAllAsRead}
            onItemClick={handleItemClick}
            onViewAll={() => setIsAllNotificationsOpen(true)}
          />
        </div>
        
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
      
      {/* All Notifications Slide-Over Drawer */}
      <AllNotificationsDrawer
        isOpen={isAllNotificationsOpen}
        onClose={() => setIsAllNotificationsOpen(false)}
        notifications={notifications}
        onToggleRead={handleToggleRead}
        onDeleteNotification={handleDeleteNotification}
        onMarkAllAsRead={handleMarkAllAsRead}
      />
    </header>
  );
}
