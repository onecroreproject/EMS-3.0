import { 
  Search, Bell, User, Menu, LogOut, KeyRound, X, Loader2, ChevronRight,
  Zap, ChevronDown, Sun, Moon, UserPlus, CheckCircle2,
  Building2, Users, BarChart2, Monitor
} from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
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
  const [isQuickActionsOpen, setIsQuickActionsOpen] = useState(false);
  const quickActionsRef = useRef<HTMLDivElement>(null);
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
      if (quickActionsRef.current && !quickActionsRef.current.contains(event.target as Node)) {
        setIsQuickActionsOpen(false);
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
        setIsQuickActionsOpen(false);
      }
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        desktopInputRef.current?.focus();
        setIsSearchFocused(true);
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
      className="h-[74px] text-white flex items-center justify-between px-4 md:px-6 shadow-md z-50 shrink-0 transition-colors duration-300 relative overflow-visible"
    >
      {/* Background Wave Design - Matches Reference Image Exactly */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0">
        {/* Dynamic Gradient Mesh Base */}
        <div 
          className={`absolute inset-0 transition-all duration-500 ${
            isDarkMode 
              ? 'bg-gradient-to-r from-[#0B132B] via-[#1C2541] to-[#0F172A]' 
              : 'bg-gradient-to-r from-[#F0381B] via-[#FA5428] via-[#FF6E38] to-[#EB381B]'
          }`} 
        />

        {/* Vector Flowing Liquid Waves */}
        <svg 
          className="absolute inset-0 w-full h-full object-cover" 
          viewBox="0 0 1440 90" 
          preserveAspectRatio="none"
        >
          <defs>
            <linearGradient id="waveCrimsonGrad" x1="0%" y1="100%" x2="50%" y2="0%">
              <stop offset="0%" stopColor="#B71C1C" stopOpacity={isDarkMode ? 0.35 : 0.85} />
              <stop offset="55%" stopColor="#D32F2F" stopOpacity={isDarkMode ? 0.2 : 0.55} />
              <stop offset="100%" stopColor="#F44336" stopOpacity={0} />
            </linearGradient>

            <linearGradient id="waveAmberGrad" x1="0%" y1="0%" x2="60%" y2="100%">
              <stop offset="0%" stopColor={isDarkMode ? '#38BDF8' : '#FFA24C'} stopOpacity={isDarkMode ? 0.25 : 0.65} />
              <stop offset="60%" stopColor={isDarkMode ? '#0284C7' : '#FF7033'} stopOpacity={isDarkMode ? 0.15 : 0.4} />
              <stop offset="100%" stopColor="#F85429" stopOpacity={0} />
            </linearGradient>

            <linearGradient id="waveRightGrad" x1="30%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FFA050" stopOpacity={0} />
              <stop offset="50%" stopColor={isDarkMode ? '#1E293B' : '#E0321A'} stopOpacity={isDarkMode ? 0.35 : 0.4} />
              <stop offset="100%" stopColor={isDarkMode ? '#0F172A' : '#B71C1C'} stopOpacity={isDarkMode ? 0.8 : 0.75} />
            </linearGradient>
          </defs>

          {/* Top-Left Amber Wave (flows down behind Teratime logo) */}
          <path d="M0,0 L440,0 C370,44 280,70 190,56 C110,44 50,66 0,80 Z" fill="url(#waveAmberGrad)" />

          {/* Bottom-Left Crimson Flowing Liquid Wave */}
          <path d="M0,32 C80,38 170,60 270,66 C370,72 450,46 530,54 C590,60 640,78 690,90 L0,90 Z" fill="url(#waveCrimsonGrad)" />

          {/* Center Subtle Rolling Wave under Search Bar */}
          <path d="M350,0 C480,24 630,44 790,34 C950,22 1090,48 1250,36 C1360,26 1410,10 1440,0 Z" fill="#FFFFFF" opacity={isDarkMode ? 0.04 : 0.12} />

          {/* Right Fluid Wave behind Quick Actions and Avatar */}
          <path d="M960,90 C1080,56 1220,42 1350,56 C1390,60 1420,72 1440,82 L1440,90 Z" fill="url(#waveRightGrad)" />

          {/* Bottom Edge Subtle Liquid Crest Line */}
          <path d="M0,84 C240,89 480,80 720,85 C960,90 1200,83 1440,87 L1440,90 L0,90 Z" fill={isDarkMode ? '#000000' : '#8B180A'} opacity={0.4} />
        </svg>
      </div>

      {/* Mobile Expanding Search Bar */}
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
            placeholder="Search employees, departments, tasks, or reports..."
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
      
      {/* Left: Mobile Menu & Teratime Brand Logo */}
      <div className="flex items-center gap-3 w-auto md:w-72 relative z-10 shrink-0">
        <button 
          onClick={toggleSidebar}
          className={`md:hidden p-1.5 rounded-lg text-white transition-colors duration-200 ${
            isDarkMode ? 'hover:bg-slate-800' : 'hover:bg-white/20'
          }`}
        >
          <Menu className="h-6 w-6" />
        </button>

        <Link to="/" className="hidden md:flex items-center gap-3 group cursor-pointer text-white">
          <div className="flex items-center justify-center shrink-0">
            <img 
              src="/teratime-logo-white.png" 
              alt="Teratime Logo" 
              className="h-10 w-10 object-contain drop-shadow-md group-hover:scale-105 transition-transform duration-200" 
            />
          </div>
          <div>
            <h1 className="text-xl font-bold leading-tight tracking-tight text-white drop-shadow-xs">Teratime</h1>
            <p className="text-[10px] sm:text-[11px] font-medium tracking-wide text-white/90 leading-tight">
              Employment Management System
            </p>
          </div>
        </Link>
      </div>

      {/* Middle: Expanding Transition Search Bar */}
      <div 
        className={`flex-1 px-3 sm:px-4 hidden sm:flex justify-center mx-auto relative z-10 transition-all duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] ${
          isSearchFocused ? 'max-w-2xl' : 'max-w-md hover:max-w-lg'
        }`}
      >
        <form 
          onSubmit={handleSearchSubmit}
          ref={searchRef}
          className="relative w-full"
        >
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none">
            {isSearching ? (
              <Loader2 className="h-4 w-4 text-[#F96D3E] animate-spin" />
            ) : (
              <Search className={`h-4 w-4 transition-colors duration-200 ${isSearchFocused ? 'text-[#F96D3E]' : 'text-slate-400'}`} />
            )}
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
            onBlur={() => {
              setTimeout(() => {
                if (!searchValue.trim()) {
                  setIsSearchFocused(false);
                }
              }, 200);
            }}
            placeholder="Search employees, departments, tasks, or reports..." 
            className={`w-full pl-10 pr-10 py-2.5 rounded-full text-sm outline-none transition-all duration-300 ${
              isDarkMode
                ? (isSearchFocused 
                    ? 'bg-[#1E293B] text-white placeholder-slate-400 border border-blue-500/80 shadow-[0_4px_20px_rgba(0,0,0,0.35),0_0_0_3px_rgba(59,130,246,0.35)] scale-[1.01]' 
                    : 'bg-[#1E293B]/80 text-slate-200 placeholder-slate-400 border border-slate-700/60 shadow-md hover:bg-[#1E293B] hover:border-slate-600')
                : (isSearchFocused
                    ? 'bg-white text-slate-900 placeholder-slate-400 border border-white/90 shadow-[0_8px_25px_-4px_rgba(0,0,0,0.2),0_0_0_3px_rgba(255,255,255,0.75)] ring-2 ring-white/95 scale-[1.01]'
                    : 'bg-white text-slate-800 placeholder-slate-400 border border-transparent shadow-md hover:shadow-lg')
            }`}
          />
          {searchValue && (
            <button
              type="button"
              onClick={() => {
                setSearchValue('');
                setIsSearchDropdownOpen(false);
                desktopInputRef.current?.focus();
              }}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 p-0.5 cursor-pointer transition-colors"
              title="Clear search"
            >
              <X className="w-4 h-4" />
            </button>
          )}
          {renderSearchDropdown()}
        </form>
      </div>

      {/* Right: Quick Actions, Theme Toggle, Notifications, Profile */}
      <div className="flex items-center gap-2.5 sm:gap-4 ml-auto sm:ml-0 relative z-10 shrink-0">
        {/* Mobile Search Icon */}
        <button 
          ref={mobileSearchButtonRef}
          onClick={() => {
            setIsMobileSearchOpen(prev => {
              const next = !prev;
              if (next) setTimeout(() => mobileInputRef.current?.focus(), 80);
              return next;
            });
          }}
          className="sm:hidden p-2 rounded-full hover:bg-white/15 text-white transition-colors"
          title="Search"
        >
          <Search className="h-5 w-5" />
        </button>

        {/* Light / Dark Mode Toggle Switch (Matches Reference Exactly: Light = Sun, Dark = Moon) */}
        <div className="hidden sm:flex items-center gap-2" title={isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode"}>
          {isDarkMode ? (
            <Moon className="h-4.5 w-4.5 text-amber-300 fill-amber-300/40 drop-shadow-sm select-none" />
          ) : (
            <Sun className="h-4.5 w-4.5 text-white drop-shadow-sm select-none" />
          )}
          <button 
            type="button"
            onClick={handleToggleTheme}
            aria-label="Toggle light/dark theme"
            className={`w-12 h-6.5 rounded-full p-0.5 transition-all duration-300 cursor-pointer flex items-center relative shadow-inner ${
              isDarkMode 
                ? 'bg-[#0B1120] hover:bg-[#070b14] border border-slate-600/70' 
                : 'bg-[#8E1C0A]/90 hover:bg-[#7D1708] border border-white/30'
            }`}
          >
            {/* Ambient Sparkle Inside Track (like reference UI) */}
            {isDarkMode ? (
              <span className="absolute right-2 text-[10px] text-slate-400 font-bold select-none leading-none pointer-events-none">
                ✦
              </span>
            ) : (
              <span className="absolute left-2 text-[10px] text-white/85 font-bold select-none leading-none pointer-events-none">
                ✦
              </span>
            )}
            
            {/* Smooth Sliding Pristine White Knob */}
            <div 
              className={`w-5 h-5 rounded-full bg-white shadow-md transform transition-transform duration-300 flex items-center justify-center ${
                isDarkMode ? 'translate-x-0.5' : 'translate-x-6'
              }`}
            />
          </button>
        </div>

        {/* Notification Bell with Dynamic Unread Badge */}
        <div className="relative">
          <button 
            onClick={() => setIsNotificationOpen(!isNotificationOpen)}
            className="relative p-2 rounded-full hover:bg-white/15 transition-all text-white cursor-pointer"
            title="Notifications"
          >
            <Bell className="h-5 w-5 text-white" />
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 h-4 min-w-4 px-1 rounded-full bg-[#E53935] text-white text-[10px] font-bold flex items-center justify-center ring-2 ring-[#FA502A] shadow-sm">
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

        {/* Quick Actions Button & Floating Dropdown */}
        <div className="relative" ref={quickActionsRef}>
          <button
            onClick={() => setIsQuickActionsOpen(!isQuickActionsOpen)}
            className="flex items-center gap-1.5 px-3 py-1.5 sm:px-4 sm:py-2 rounded-full bg-gradient-to-r from-[#FF4E1E] to-[#E6350E] hover:from-[#FF5F2E] hover:to-[#F24217] text-white text-xs sm:text-sm font-semibold shadow-md border border-white/25 transition-all cursor-pointer"
          >
            <Zap className="h-3.5 w-3.5 sm:h-4 sm:w-4 fill-white text-white" />
            <span className="hidden xs:inline">Quick Actions</span>
            <ChevronDown className={`h-3.5 w-3.5 sm:h-4 sm:w-4 transition-transform duration-200 ${isQuickActionsOpen ? 'rotate-180' : ''}`} />
          </button>

          {isQuickActionsOpen && (
            <div className="absolute right-0 top-full mt-2 w-72 bg-white dark:bg-[#1E2B3C] rounded-2xl shadow-2xl border border-slate-100 dark:border-slate-700/80 p-3 z-[100] text-slate-800 dark:text-slate-100 animate-in fade-in zoom-in-95 duration-150">
              <div className="px-3 pt-2 pb-2 border-b border-slate-100 dark:border-slate-700/70 mb-1.5">
                <p className="text-[11px] font-bold text-slate-400 dark:text-slate-400 tracking-wider uppercase">QUICK ACTIONS</p>
                <p className="text-[11px] text-slate-400 dark:text-slate-400">Common tasks, one click away</p>
              </div>
              
              <div className="space-y-1">
                <button 
                  onClick={() => { setIsQuickActionsOpen(false); navigate('/employees/add'); }}
                  className="w-full flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors text-left group cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-blue-50 dark:bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0">
                    <UserPlus className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-200 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">Add Employee</span>
                </button>

                <button 
                  onClick={() => { setIsQuickActionsOpen(false); navigate('/tasks/assign'); }}
                  className="w-full flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors text-left group cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-emerald-50 dark:bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                    <CheckCircle2 className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-200 group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors">Create Task</span>
                </button>

                <button 
                  onClick={() => { setIsQuickActionsOpen(false); navigate('/departments/add'); }}
                  className="w-full flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors text-left group cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-orange-50 dark:bg-orange-500/10 text-orange-600 dark:text-orange-400 flex items-center justify-center shrink-0">
                    <Building2 className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-200 group-hover:text-orange-600 dark:group-hover:text-orange-400 transition-colors">Add Department</span>
                </button>

                <button 
                  onClick={() => { setIsQuickActionsOpen(false); navigate('/teams/add'); }}
                  className="w-full flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors text-left group cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-purple-50 dark:bg-purple-500/10 text-purple-600 dark:text-purple-400 flex items-center justify-center shrink-0">
                    <Users className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-200 group-hover:text-purple-600 dark:group-hover:text-purple-400 transition-colors">Invite Team Member</span>
                </button>

                <button 
                  onClick={() => { setIsQuickActionsOpen(false); navigate('/tracking/timesheet'); }}
                  className="w-full flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors text-left group cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-sky-50 dark:bg-sky-500/10 text-sky-600 dark:text-sky-400 flex items-center justify-center shrink-0">
                    <BarChart2 className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-200 group-hover:text-sky-600 dark:group-hover:text-sky-400 transition-colors">View Reports</span>
                </button>

                <button 
                  onClick={() => { setIsQuickActionsOpen(false); navigate('/tracking/screen-monitoring'); }}
                  className="w-full flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors text-left group cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-pink-50 dark:bg-pink-500/10 text-pink-600 dark:text-pink-400 flex items-center justify-center shrink-0">
                    <Monitor className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-200 group-hover:text-pink-600 dark:group-hover:text-pink-400 transition-colors">Capture Screenshot</span>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Profile Dropdown (Avatar preserved as-is) */}
        <div className="relative" ref={profileRef}>
          <div 
            onClick={() => setIsProfileOpen(!isProfileOpen)}
            className="flex items-center gap-2 cursor-pointer p-1 rounded-full hover:bg-white/10 transition-colors"
          >
            <div className="h-9 w-9 bg-white text-[#F96D3E] rounded-full flex items-center justify-center shadow-md ring-2 ring-white/60 shrink-0">
              <User className="h-5 w-5" />
            </div>
            <div className="hidden md:block text-left">
              <p className="text-sm font-bold leading-tight text-white">Admin</p>
              <p className="text-[11px] text-white/80 leading-tight">
                Administrator
              </p>
            </div>
            <ChevronDown className="h-3.5 w-3.5 text-white/80 hidden md:block" />
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
