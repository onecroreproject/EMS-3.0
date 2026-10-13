import { useState, useMemo, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { 
  X, Search, Calendar, MoreVertical, LogIn, Monitor, 
  CheckCircle2, AppWindow, Clock, UserPlus, ChevronLeft, ChevronRight,
  Shield, Check, Trash2
} from 'lucide-react';
import type { NotificationItem, NotificationCategory } from './types';

interface AllNotificationsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  notifications: NotificationItem[];
  onToggleRead: (id: string) => void;
  onDeleteNotification: (id: string) => void;
  onMarkAllAsRead: () => void;
}

export default function AllNotificationsDrawer({
  isOpen,
  onClose,
  notifications,
  onToggleRead,
  onDeleteNotification,
  onMarkAllAsRead,
}: AllNotificationsDrawerProps) {
  const [activeCategory, setActiveCategory] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState('');
  const [timeFilter, setTimeFilter] = useState('All Time');
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 6;

  // Animation lifecycle states
  const [isRendered, setIsRendered] = useState(isOpen);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    let animId: number;
    let timerId: ReturnType<typeof setTimeout>;

    if (isOpen) {
      setIsRendered(true);
      // Ensure the element is mounted in DOM before toggling visibility for smooth slide
      animId = requestAnimationFrame(() => {
        timerId = setTimeout(() => {
          setIsVisible(true);
        }, 16);
      });
      document.body.style.overflow = 'hidden';
    } else {
      setIsVisible(false);
      timerId = setTimeout(() => {
        setIsRendered(false);
      }, 420); // Matches transition duration
      document.body.style.overflow = '';
    }

    return () => {
      cancelAnimationFrame(animId);
      clearTimeout(timerId);
    };
  }, [isOpen]);

  // Clean up body overflow on unmount
  useEffect(() => {
    return () => {
      document.body.style.overflow = '';
    };
  }, []);

  const handleClose = () => {
    setIsVisible(false);
    setTimeout(() => {
      onClose();
    }, 400);
  };

  // Category counts
  const categoryCounts = useMemo(() => {
    const list = notifications || [];
    const counts: Record<string, number> = {
      All: list.length,
      Security: 0,
      Employee: 0,
      Tasks: 0,
      System: 0,
    };
    list.forEach((item) => {
      if (item && item.category && counts[item.category] !== undefined) {
        counts[item.category]++;
      }
    });
    return counts;
  }, [notifications]);

  // Filtered notifications
  const filteredNotifications = useMemo(() => {
    const list = notifications || [];
    return list.filter((item) => {
      if (!item) return false;
      // Category filter
      if (activeCategory !== 'All' && item.category !== activeCategory) {
        return false;
      }
      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchTitle = (item.title || '').toLowerCase().includes(q);
        const matchDesc = (item.description || '').toLowerCase().includes(q);
        const matchCat = (item.category || '').toLowerCase().includes(q);
        if (!matchTitle && !matchDesc && !matchCat) return false;
      }
      // Time filter
      const timeAgo = (item.timeAgo || '').toLowerCase();
      if (timeFilter === 'Today') {
        const isToday = timeAgo.includes('minute') || timeAgo.includes('hour') || timeAgo.includes('just now');
        if (!isToday) return false;
      } else if (timeFilter === 'This Week') {
        const isWeek = !timeAgo.includes('month');
        if (!isWeek) return false;
      }
      return true;
    });
  }, [notifications, activeCategory, searchQuery, timeFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredNotifications.length / ITEMS_PER_PAGE));
  const paginatedItems = filteredNotifications.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  if (!isRendered || typeof document === 'undefined' || !document.body) return null;

  const getCategoryBadge = (category: NotificationCategory) => {
    switch (category) {
      case 'Security':
        return (
          <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400">
            Security
          </span>
        );
      case 'Employee':
        return (
          <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-blue-100 text-blue-600 dark:bg-blue-500/20 dark:text-blue-400">
            Employee
          </span>
        );
      case 'Tasks':
        return (
          <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-600 dark:bg-emerald-500/20 dark:text-emerald-400">
            Tasks
          </span>
        );
      case 'System':
        return (
          <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-purple-100 text-purple-600 dark:bg-purple-500/20 dark:text-purple-400">
            System
          </span>
        );
    }
  };

  const getNotificationIcon = (item: NotificationItem) => {
    switch (item.iconType) {
      case 'login':
        return (
          <div className="w-11 h-11 rounded-2xl bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400 flex items-center justify-center shrink-0 shadow-sm">
            <LogIn className="w-5 h-5" />
          </div>
        );
      case 'offline':
        return (
          <div className="w-11 h-11 rounded-2xl bg-blue-50 text-blue-500 dark:bg-blue-500/10 dark:text-blue-400 flex items-center justify-center shrink-0 shadow-sm">
            <Monitor className="w-5 h-5" />
          </div>
        );
      case 'task':
        return (
          <div className="w-11 h-11 rounded-2xl bg-emerald-50 text-emerald-500 dark:bg-emerald-500/10 dark:text-emerald-400 flex items-center justify-center shrink-0 shadow-sm">
            <CheckCircle2 className="w-5 h-5" />
          </div>
        );
      case 'usage':
        return (
          <div className="w-11 h-11 rounded-2xl bg-purple-50 text-purple-500 dark:bg-purple-500/10 dark:text-purple-400 flex items-center justify-center shrink-0 shadow-sm">
            <AppWindow className="w-5 h-5" />
          </div>
        );
      case 'idle':
        return (
          <div className="w-11 h-11 rounded-2xl bg-amber-50 text-amber-500 dark:bg-amber-500/10 dark:text-amber-400 flex items-center justify-center shrink-0 shadow-sm">
            <Clock className="w-5 h-5" />
          </div>
        );
      case 'user':
        return (
          <div className="w-11 h-11 rounded-2xl bg-orange-50 text-orange-500 dark:bg-orange-500/10 dark:text-orange-400 flex items-center justify-center shrink-0 shadow-sm">
            <UserPlus className="w-5 h-5" />
          </div>
        );
      default:
        return (
          <div className="w-11 h-11 rounded-2xl bg-slate-50 text-slate-500 dark:bg-slate-700/50 dark:text-slate-300 flex items-center justify-center shrink-0 shadow-sm">
            <Shield className="w-5 h-5" />
          </div>
        );
    }
  };

  const categories: { label: string; count: number }[] = [
    { label: 'All', count: categoryCounts.All },
    { label: 'Security', count: categoryCounts.Security },
    { label: 'Employee', count: categoryCounts.Employee },
    { label: 'Tasks', count: categoryCounts.Tasks },
    { label: 'System', count: categoryCounts.System },
  ];

  return createPortal(
    <div
      className={`fixed inset-0 z-[9999] overflow-hidden text-slate-800 dark:text-slate-100 font-sans ${
        isVisible ? 'pointer-events-auto' : 'pointer-events-none'
      }`}
    >
      {/* Backdrop with smooth blur and fade */}
      <div
        className={`fixed inset-0 bg-slate-900/60 backdrop-blur-md cursor-pointer transition-all duration-400 ease-[cubic-bezier(0.22,1,0.36,1)] ${
          isVisible ? 'opacity-100' : 'opacity-0'
        }`}
        onClick={handleClose}
      />

      {/* Slide-over Right Drawer Container */}
      <div className="fixed inset-y-0 right-0 max-w-full flex pl-6 sm:pl-10 h-full pointer-events-none">
        <div
          className={`w-screen max-w-xl sm:max-w-2xl bg-white dark:bg-[#111827] shadow-2xl flex flex-col h-full border-l border-slate-100 dark:border-slate-800 relative z-10 pointer-events-auto transition-transform duration-400 ease-[cubic-bezier(0.22,1,0.36,1)] will-change-transform ${
            isVisible ? 'translate-x-0' : 'translate-x-full'
          }`}
        >
          
          {/* Header */}
          <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-start justify-between shrink-0">
            <div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-white">All Notifications</h2>
              <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
                Stay updated with the latest activities and alerts.
              </p>
            </div>
            <button
              onClick={handleClose}
              className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Category Tabs */}
          <div className="px-6 border-b border-slate-100 dark:border-slate-800 flex items-center gap-6 overflow-x-auto no-scrollbar shrink-0">
            {categories.map((cat) => {
              const isActive = activeCategory === cat.label;
              return (
                <button
                  key={cat.label}
                  onClick={() => {
                    setActiveCategory(cat.label);
                    setCurrentPage(1);
                  }}
                  className={`py-3.5 text-sm font-semibold border-b-2 flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap ${
                    isActive
                      ? 'border-blue-600 text-blue-600 dark:text-blue-400'
                      : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
                  }`}
                >
                  {cat.label} <span className="text-xs opacity-80">({cat.count})</span>
                </button>
              );
            })}
          </div>

          {/* Search & Filter Bar */}
          <div className="p-6 pb-4 flex flex-col sm:flex-row gap-3 items-center justify-between shrink-0">
            <div className="relative w-full sm:flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setCurrentPage(1);
                }}
                placeholder="Search notifications..."
                className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white dark:focus:bg-[#1E2B3C] text-slate-800 dark:text-slate-100 transition shadow-sm"
              />
            </div>

            <div className="relative w-full sm:w-auto">
              <div className="flex items-center gap-2 px-3.5 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-700 dark:text-slate-200 shadow-sm cursor-pointer">
                <Calendar className="w-4 h-4 text-slate-400 shrink-0" />
                <select
                  value={timeFilter}
                  onChange={(e) => {
                    setTimeFilter(e.target.value);
                    setCurrentPage(1);
                  }}
                  className="bg-transparent outline-none cursor-pointer text-xs sm:text-sm font-medium"
                >
                  <option value="All Time">All Time</option>
                  <option value="Today">Today</option>
                  <option value="This Week">This Week</option>
                  <option value="This Month">This Month</option>
                </select>
              </div>
            </div>
          </div>

          {/* Notifications List */}
          <div className="flex-1 overflow-y-auto px-6 py-2 divide-y divide-slate-100 dark:divide-slate-800/80 custom-scrollbar">
            {paginatedItems.length === 0 ? (
              <div className="py-16 text-center text-slate-400">
                <p className="text-base font-semibold">No notifications found</p>
                <p className="text-xs text-slate-400 mt-1">Try adjusting your filters or search keywords.</p>
              </div>
            ) : (
              paginatedItems.map((item) => (
                <div
                  key={item.id}
                  className={`py-4.5 flex items-start gap-4 transition-colors relative group ${
                    !item.isRead ? 'bg-slate-50/50 dark:bg-slate-800/20' : ''
                  }`}
                >
                  {getNotificationIcon(item)}

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <h4 className="text-sm font-bold text-slate-900 dark:text-white">
                        {item.title}
                      </h4>
                      {getCategoryBadge(item.category)}
                    </div>
                    
                    <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed mb-1.5">
                      {item.description}
                    </p>

                    <div className="flex items-center gap-2 text-[11px] text-slate-400">
                      <span>{item.timeAgo}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 pt-0.5">
                    {!item.isRead && (
                      <span className="w-2 h-2 rounded-full bg-blue-600" title="Unread" />
                    )}

                    <div className="relative">
                      <button
                        onClick={() => setActiveMenuId(activeMenuId === item.id ? null : item.id)}
                        className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition"
                      >
                        <MoreVertical className="w-4 h-4" />
                      </button>

                      {activeMenuId === item.id && (
                        <div className="absolute right-0 mt-1 w-40 bg-white dark:bg-[#1E2B3C] border border-slate-100 dark:border-slate-700 rounded-xl shadow-xl py-1 z-30 animate-in fade-in zoom-in-95 duration-150">
                          <button
                            onClick={() => {
                              onToggleRead(item.id);
                              setActiveMenuId(null);
                            }}
                            className="w-full text-left px-3 py-2 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700/50 flex items-center gap-2 cursor-pointer"
                          >
                            <Check className="w-3.5 h-3.5" />
                            {item.isRead ? 'Mark as unread' : 'Mark as read'}
                          </button>
                          <button
                            onClick={() => {
                              onDeleteNotification(item.id);
                              setActiveMenuId(null);
                            }}
                            className="w-full text-left px-3 py-2 text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10 flex items-center gap-2 cursor-pointer"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                            Delete
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-center gap-2 shrink-0">
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="w-8 h-8 rounded-lg border border-slate-200 dark:border-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40 transition"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                <button
                  key={page}
                  onClick={() => setCurrentPage(page)}
                  className={`w-8 h-8 rounded-lg text-xs font-bold transition ${
                    currentPage === page
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800'
                  }`}
                >
                  {page}
                </button>
              ))}

              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="w-8 h-8 rounded-lg border border-slate-200 dark:border-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40 transition"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}

        </div>
      </div>
    </div>,
    document.body
  );
}
