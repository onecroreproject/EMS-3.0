import { useState, useEffect, useRef } from 'react';
import { 
  LogIn, Monitor, CheckCircle2, AppWindow, Clock, UserPlus, 
  ChevronRight, Check
} from 'lucide-react';
import type { NotificationItem } from './types';

interface NotificationDropdownProps {
  isOpen: boolean;
  onClose: () => void;
  notifications: NotificationItem[];
  unreadCount: number;
  onMarkAllAsRead: () => void;
  onItemClick: (id: string) => void;
  onViewAll: () => void;
}

export default function NotificationDropdown({
  isOpen,
  onClose,
  notifications,
  unreadCount,
  onMarkAllAsRead,
  onItemClick,
  onViewAll,
}: NotificationDropdownProps) {
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [isRendered, setIsRendered] = useState(isOpen);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    let animId: number;
    let timerId: ReturnType<typeof setTimeout>;

    if (isOpen) {
      setIsRendered(true);
      animId = requestAnimationFrame(() => {
        timerId = setTimeout(() => {
          setIsVisible(true);
        }, 16);
      });
    } else {
      setIsVisible(false);
      timerId = setTimeout(() => {
        setIsRendered(false);
      }, 200);
    }

    return () => {
      cancelAnimationFrame(animId);
      clearTimeout(timerId);
    };
  }, [isOpen]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        onClose();
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen, onClose]);

  if (!isRendered) return null;

  const getNotificationIcon = (item: NotificationItem) => {
    switch (item.iconType) {
      case 'login':
        return (
          <div className="w-10 h-10 rounded-xl bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400 flex items-center justify-center shrink-0">
            <LogIn className="w-5 h-5" />
          </div>
        );
      case 'offline':
        return (
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-500 dark:bg-blue-500/10 dark:text-blue-400 flex items-center justify-center shrink-0">
            <Monitor className="w-5 h-5" />
          </div>
        );
      case 'task':
        return (
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-500 dark:bg-emerald-500/10 dark:text-emerald-400 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-5 h-5" />
          </div>
        );
      case 'usage':
        return (
          <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-500 dark:bg-purple-500/10 dark:text-purple-400 flex items-center justify-center shrink-0">
            <AppWindow className="w-5 h-5" />
          </div>
        );
      case 'idle':
        return (
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-500 dark:bg-amber-500/10 dark:text-amber-400 flex items-center justify-center shrink-0">
            <Clock className="w-5 h-5" />
          </div>
        );
      case 'user':
        return (
          <div className="w-10 h-10 rounded-xl bg-orange-50 text-orange-500 dark:bg-orange-500/10 dark:text-orange-400 flex items-center justify-center shrink-0">
            <UserPlus className="w-5 h-5" />
          </div>
        );
      default:
        return (
          <div className="w-10 h-10 rounded-xl bg-slate-50 text-slate-500 dark:bg-slate-700/50 dark:text-slate-300 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-5 h-5" />
          </div>
        );
    }
  };

  const previewItems = notifications.slice(0, 5);

  return (
    <div
      ref={dropdownRef}
      className={`absolute right-0 top-full mt-2 w-80 sm:w-96 bg-white dark:bg-[#1E2B3C] rounded-2xl shadow-2xl border border-slate-100 dark:border-slate-700/80 overflow-hidden z-50 text-slate-800 dark:text-slate-100 transition-all duration-200 ease-out origin-top-right will-change-[opacity,transform] ${
        isVisible
          ? 'opacity-100 scale-100 translate-y-0'
          : 'opacity-0 scale-95 -translate-y-2 pointer-events-none'
      }`}
    >
      {/* Header */}
      <div className="px-5 py-4 border-b border-slate-100 dark:border-slate-700/70 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h3 className="font-bold text-slate-900 dark:text-white text-base">Notifications</h3>
          {unreadCount > 0 && (
            <span className="bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">
              {unreadCount}
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={onMarkAllAsRead}
            className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:text-blue-700 flex items-center gap-1 transition-colors"
          >
            <Check className="w-3.5 h-3.5" /> Mark all as read
          </button>
        )}
      </div>

      {/* Notification items */}
      <div className="divide-y divide-slate-100 dark:divide-slate-700/60 max-h-[420px] overflow-y-auto no-scrollbar">
        {previewItems.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-sm">No notifications available</div>
        ) : (
          previewItems.map((item) => (
            <div
              key={item.id}
              onClick={() => onItemClick(item.id)}
              className={`p-4 flex items-start gap-3.5 cursor-pointer transition-colors ${
                !item.isRead
                  ? 'bg-slate-50/70 dark:bg-slate-800/40 hover:bg-slate-100/80 dark:hover:bg-slate-800/70'
                  : 'hover:bg-slate-50 dark:hover:bg-slate-800/30'
              }`}
            >
              {getNotificationIcon(item)}

              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-1 mb-0.5">
                  <h4 className="text-sm font-bold text-slate-800 dark:text-slate-100 truncate">
                    {item.title}
                  </h4>
                  <div className="flex items-center gap-1.5 shrink-0">
                    <span className="text-[11px] text-slate-400 dark:text-slate-400">
                      {item.timeAgo}
                    </span>
                    {!item.isRead && (
                      <span className="w-2 h-2 rounded-full bg-blue-600 shrink-0" />
                    )}
                  </div>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2 leading-relaxed">
                  {item.description}
                </p>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Footer Button: View All Notifications */}
      <div className="p-3 border-t border-slate-100 dark:border-slate-700/70 bg-slate-50/60 dark:bg-slate-800/40">
        <button
          onClick={() => {
            onClose();
            onViewAll();
          }}
          className="w-full py-2.5 px-4 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl flex items-center justify-center gap-1.5 transition-colors shadow-sm"
        >
          View All Notifications <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
