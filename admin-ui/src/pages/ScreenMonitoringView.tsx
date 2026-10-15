import React, { useState, useEffect } from 'react';
import { 
  Monitor, Search, RotateCcw, Calendar, User, 
  Laptop, Clock, LayoutGrid, List, ChevronLeft, 
  ChevronRight, Download, Maximize2, X, Image as ImageIcon,
  CheckCircle2, Coffee, Users, ChevronDown, Check, ExternalLink
} from 'lucide-react';
import { toast } from 'react-toastify';
import apiClient from '../lib/axios';

interface ScreenshotItem {
  id: string;
  employeeId: string;
  employeeName: string;
  employeeCode: string;
  deviceId: string;
  hostname: string;
  timestamp: string;
  timeFormatted: string;
  dateFormatted: string;
  shortTime: string;
  status: 'Working' | 'Idle' | 'Break' | string;
  appName: string;
  windowTitle: string;
  activityDuration: string;
  fileName: string;
  hasImage: boolean;
  imageUrl: string;
}

interface ScreenshotMetrics {
  totalScreenshots: number;
  activeEmployees: number;
  workingCount: number;
  idleCount: number;
  workingPercentage: number;
  idlePercentage: number;
}

export default function ScreenMonitoringView() {
  const [screenshots, setScreenshots] = useState<ScreenshotItem[]>([]);
  const [metrics, setMetrics] = useState<ScreenshotMetrics>({
    totalScreenshots: 0,
    activeEmployees: 0,
    workingCount: 0,
    idleCount: 0,
    workingPercentage: 0,
    idlePercentage: 0
  });

  const [loading, setLoading] = useState(false);
  const [selectedScreenshot, setSelectedScreenshot] = useState<ScreenshotItem | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [sortOption, setSortOption] = useState<'Newest First' | 'Oldest First'>('Newest First');
  const [fullSizeModal, setFullSizeModal] = useState<ScreenshotItem | null>(null);

  // Filters
  const [dateFilter, setDateFilter] = useState(() => new Date().toISOString().split('T')[0]);
  const [employeeFilter, setEmployeeFilter] = useState('');
  const [deviceFilter, setDeviceFilter] = useState('All Devices');
  const [timeRangeFilter, setTimeRangeFilter] = useState('All Time');
  const [isTimeDropdownOpen, setIsTimeDropdownOpen] = useState(false);
  const [customStartTime, setCustomStartTime] = useState('09:00');
  const [customEndTime, setCustomEndTime] = useState('17:00');
  const timeDropdownRef = React.useRef<HTMLDivElement>(null);

  // Click outside listener for custom time popover
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (timeDropdownRef.current && !timeDropdownRef.current.contains(event.target as Node)) {
        setIsTimeDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Employee list for dropdown
  const [employees, setEmployees] = useState<Array<{ id: string; name: string; code: string }>>([]);

  // Devices list for dropdown
  const [devices, setDevices] = useState<string[]>([
    'All Devices'
  ]);

  // Pagination
  const [page, setPage] = useState(1);
  const totalPages = Math.max(1, Math.ceil((metrics.totalScreenshots || 1) / 12));

  useEffect(() => {
    fetchEmployeesList();
  }, []);

  useEffect(() => {
    if (employeeFilter) {
      fetchScreenshots();
    }
  }, [sortOption, page, employeeFilter, dateFilter, timeRangeFilter, deviceFilter]);

  const fetchEmployeesList = async () => {
    try {
      const res = await apiClient.get('/api/admin/employees');
      const list = Array.isArray(res.data) ? res.data : (res.data?.content || []);
      if (Array.isArray(list) && list.length > 0) {
        const mapped = list.map((e: any) => ({
          id: e.id,
          name: e.name,
          code: e.employeeCode || e.id
        }));
        setEmployees(mapped);
      }
    } catch (err) {
      console.warn('Could not fetch employees list for screen monitoring filter', err);
    }
  };

  const fetchScreenshots = async () => {
    // REQUIREMENT: Do not load all screenshots automatically. Only load when employee is selected.
    if (!employeeFilter || employeeFilter === 'All Employees' || employeeFilter === 'All') {
      setScreenshots([]);
      setSelectedScreenshot(null);
      setMetrics({
        totalScreenshots: 0,
        activeEmployees: 0,
        workingCount: 0,
        idleCount: 0,
        workingPercentage: 0,
        idlePercentage: 0
      });
      return;
    }

    try {
      setLoading(true);
      const params: any = {
        sort: sortOption,
        page: page - 1,
        size: 12
      };

      if (dateFilter.trim()) {
        params.date = dateFilter.trim();
      }

      if (employeeFilter) {
        const matched = employees.find(e => `${e.name} (${e.code})` === employeeFilter || e.code === employeeFilter || e.id === employeeFilter);
        params.employeeId = matched ? matched.code : employeeFilter;
      }

      if (deviceFilter && deviceFilter !== 'All Devices') {
        params.device = deviceFilter;
      }

      if (timeRangeFilter && timeRangeFilter !== 'All Time') {
        params.timeRange = timeRangeFilter;
      }

      const res = await apiClient.get('/api/admin/screenshots', { params });

      if (res.data && res.data.content) {
        let items: ScreenshotItem[] = res.data.content;

        // Double check time filter on client-side to ensure strict adherence
        if (timeRangeFilter && timeRangeFilter !== 'All Time') {
          let startH = -1;
          let startM = 0;
          let endH = -1;
          let endM = 0;

          if (timeRangeFilter.toLowerCase() === 'morning') {
            startH = 6; endH = 12;
          } else if (timeRangeFilter.toLowerCase() === 'afternoon') {
            startH = 12; endH = 18;
          } else if (timeRangeFilter.toLowerCase() === 'evening') {
            startH = 18; endH = 24;
          } else if (timeRangeFilter.toLowerCase() === 'night') {
            startH = 0; endH = 6;
          } else if (timeRangeFilter.includes('-')) {
            const parts = timeRangeFilter.split('-');
            if (parts.length === 2) {
              const p1 = parts[0].trim();
              const p2 = parts[1].trim();
              startH = parseInt(p1.includes(':') ? p1.split(':')[0] : p1, 10);
              startM = p1.includes(':') ? parseInt(p1.split(':')[1], 10) : 0;
              endH = parseInt(p2.includes(':') ? p2.split(':')[0] : p2, 10);
              endM = p2.includes(':') ? parseInt(p2.split(':')[1], 10) : 0;
            }
          }

          if (startH !== -1 && endH !== -1) {
            items = items.filter(it => {
              const dt = new Date(it.timestamp);
              if (isNaN(dt.getTime())) return true;
              const h = dt.getHours();
              const m = dt.getMinutes();
              const currentMinutes = h * 60 + m;
              const startMinutes = startH * 60 + startM;
              const endMinutes = endH * 60 + endM;
              return currentMinutes >= startMinutes && currentMinutes < endMinutes;
            });
          }
        }

        setScreenshots(items);
        const wCount = items.filter(s => s.status !== 'Idle').length;
        const iCount = items.filter(s => s.status === 'Idle').length;
        setMetrics({
          totalScreenshots: timeRangeFilter !== 'All Time' ? items.length : (res.data.totalScreenshots || items.length),
          activeEmployees: items.length > 0 ? 1 : 0,
          workingCount: wCount,
          idleCount: iCount,
          workingPercentage: items.length > 0 ? Math.round((wCount / items.length) * 100) : 0,
          idlePercentage: items.length > 0 ? Math.round((iCount / items.length) * 100) : 0
        });

        // Update dynamic devices list from actual screenshots
        if (items.length > 0) {
          const foundDevices = new Set<string>();
          foundDevices.add('All Devices');
          items.forEach(it => {
            if (it.hostname) foundDevices.add(it.hostname);
            else if (it.deviceId) foundDevices.add(it.deviceId);
          });
          setDevices(Array.from(foundDevices));
        }

        if (items.length > 0) {
          setSelectedScreenshot(items[0]);
        } else {
          setSelectedScreenshot(null);
        }
      } else {
        setScreenshots([]);
        setSelectedScreenshot(null);
        setMetrics({
          totalScreenshots: 0,
          activeEmployees: 0,
          workingCount: 0,
          idleCount: 0,
          workingPercentage: 0,
          idlePercentage: 0
        });
      }
    } catch (err) {
      console.error('Failed to load screenshots from backend', err);
      setScreenshots([]);
      setSelectedScreenshot(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(1);
    fetchScreenshots();
    toast.success('Filtered screenshots loaded');
  };

  const handleReset = () => {
    setDateFilter(new Date().toISOString().split('T')[0]);
    setEmployeeFilter('');
    setDeviceFilter('All Devices');
    setTimeRangeFilter('All Time');
    setIsTimeDropdownOpen(false);
    setCustomStartTime('09:00');
    setCustomEndTime('17:00');
    setSortOption('Newest First');
    setPage(1);
    toast.info('Filters reset to today');
  };

  const handleDownload = (shot: ScreenshotItem) => {
    const link = document.createElement('a');
    link.href = shot.imageUrl;
    link.download = shot.fileName || `screenshot_${shot.id}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    toast.success(`Downloading ${shot.fileName}...`);
  };

  // Helper icon for application
  const getAppIcon = (appName: string) => {
    const name = (appName || '').toLowerCase();
    if (name.includes('intellij') || name.includes('code') || name.includes('studio') || name.includes('eclipse')) {
      return (
        <div className="w-5 h-5 rounded bg-blue-600 flex items-center justify-center text-white text-[10px] font-bold">
          &lt;/&gt;
        </div>
      );
    }
    if (name.includes('chrome') || name.includes('edge') || name.includes('firefox') || name.includes('browser')) {
      return (
        <div className="w-5 h-5 rounded-full bg-red-500 flex items-center justify-center text-white text-[10px] font-bold">
          🌐
        </div>
      );
    }
    if (name.includes('teams') || name.includes('slack') || name.includes('chat')) {
      return (
        <div className="w-5 h-5 rounded bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">
          T
        </div>
      );
    }
    if (name.includes('excel') || name.includes('calc')) {
      return (
        <div className="w-5 h-5 rounded bg-emerald-600 flex items-center justify-center text-white text-[10px] font-bold">
          X
        </div>
      );
    }
    if (name.includes('word') || name.includes('doc')) {
      return (
        <div className="w-5 h-5 rounded bg-blue-700 flex items-center justify-center text-white text-[10px] font-bold">
          W
        </div>
      );
    }
    return (
      <div className="w-5 h-5 rounded bg-slate-600 flex items-center justify-center text-white text-[10px] font-bold">
        💻
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] dark:bg-[#0B1120] text-slate-800 dark:text-slate-100 pb-16 transition-colors">
      <div className="max-w-[1536px] mx-auto px-4 sm:px-6 lg:px-8 pt-6">
        
        {/* Top Breadcrumb */}
        <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400 mb-2">
          <span>Tracking &amp; Monitoring</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="font-semibold text-slate-800 dark:text-slate-200">Screen Monitoring</span>
        </div>

        {/* Page Title */}
        <div className="flex items-center gap-3.5 mb-6">
          <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800/60 flex items-center justify-center text-blue-600 dark:text-blue-400 shadow-sm">
            <Monitor className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
              Screen Monitoring
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-0.5">
              View screenshots captured from employee devices.
            </p>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800/80 rounded-2xl p-4 mb-6 shadow-sm">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-6 gap-3 items-end">
            
            {/* Date Input */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1.5">Date</label>
              <div className="relative">
                <input 
                  type="date" 
                  value={dateFilter} 
                  onChange={e => {
                    setDateFilter(e.target.value);
                    setPage(1);
                  }}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-[#1C2638] border border-slate-200 dark:border-slate-700/80 rounded-xl text-xs font-medium text-slate-800 dark:text-slate-100 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
                />
              </div>
            </div>

            {/* Employee Dropdown */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1.5">Employee</label>
              <select 
                value={employeeFilter} 
                onChange={e => {
                  setEmployeeFilter(e.target.value);
                  setPage(1);
                }}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-[#1C2638] border border-slate-200 dark:border-slate-700/80 rounded-xl text-xs font-medium text-slate-800 dark:text-slate-100 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
              >
                <option value="" className="dark:bg-[#151E2E] text-slate-400">-- Select an Employee --</option>
                {employees.map(emp => (
                  <option key={emp.id} value={`${emp.name} (${emp.code})`} className="dark:bg-[#151E2E] text-slate-100">
                    {emp.name} ({emp.code})
                  </option>
                ))}
              </select>
            </div>

            {/* Device Dropdown */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1.5">Device</label>
              <select 
                value={deviceFilter} 
                onChange={e => setDeviceFilter(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-[#1C2638] border border-slate-200 dark:border-slate-700/80 rounded-xl text-xs font-medium text-slate-800 dark:text-slate-100 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                {devices.map((d, i) => (
                  <option key={i} value={d} className="dark:bg-[#151E2E] text-slate-100">{d}</option>
                ))}
              </select>
            </div>

            {/* Time Range Custom Popover */}
            <div className="relative" ref={timeDropdownRef}>
              <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1.5 flex items-center justify-between">
                <span>Time Range</span>
                {timeRangeFilter !== 'All Time' && (
                  <span className="text-[10px] text-blue-500 dark:text-blue-400 font-semibold">Active</span>
                )}
              </label>
              
              <button
                type="button"
                onClick={() => setIsTimeDropdownOpen(!isTimeDropdownOpen)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-[#1C2638] border border-slate-200 dark:border-slate-700/80 rounded-xl text-xs font-medium text-slate-800 dark:text-slate-100 hover:border-blue-400 dark:hover:border-blue-500 flex items-center justify-between transition cursor-pointer"
              >
                <div className="flex items-center gap-1.5 truncate">
                  <Clock className="w-3.5 h-3.5 text-slate-400 dark:text-slate-400 shrink-0" />
                  <span className="truncate">
                    {timeRangeFilter === 'All Time' 
                      ? 'All Day (24h)' 
                      : timeRangeFilter.includes('-') 
                        ? timeRangeFilter 
                        : `${timeRangeFilter}`}
                  </span>
                </div>
                <ChevronDown className={`w-3.5 h-3.5 text-slate-400 transition-transform ${isTimeDropdownOpen ? 'rotate-180 text-blue-500' : ''}`} />
              </button>

              {/* Compact Floating Popover Menu */}
              {isTimeDropdownOpen && (
                <div className="absolute top-full left-0 mt-1.5 w-64 bg-white dark:bg-[#1E293B] border border-slate-200 dark:border-slate-700 rounded-2xl shadow-xl z-50 p-2.5 space-y-2 animate-in fade-in zoom-in-95 duration-100">
                  {/* Quick Filters */}
                  <div>
                    <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 px-1 mb-1 block">Quick Shifts</span>
                    <div className="grid grid-cols-2 gap-1">
                      {[
                        { label: 'All Day', val: 'All Time' },
                        { label: 'Morning (6-12)', val: 'Morning' },
                        { label: 'Afternoon (12-18)', val: 'Afternoon' },
                        { label: 'Evening (18-24)', val: 'Evening' },
                      ].map((item) => (
                        <button
                          key={item.val}
                          type="button"
                          onClick={() => {
                            setTimeRangeFilter(item.val);
                            setIsTimeDropdownOpen(false);
                            setPage(1);
                          }}
                          className={`px-2 py-1.5 rounded-lg text-left text-xs font-medium transition ${
                            timeRangeFilter === item.val
                              ? 'bg-blue-600 text-white font-semibold shadow-xs'
                              : 'text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700/60'
                          }`}
                        >
                          {item.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="h-px bg-slate-100 dark:bg-slate-700/60" />

                  {/* Hourly Scroll List */}
                  <div>
                    <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 px-1 mb-1 block">Hourly Window</span>
                    <div className="max-h-32 overflow-y-auto space-y-0.5 pr-1 text-xs">
                      {[
                        '08:00 - 09:00', '09:00 - 10:00', '10:00 - 11:00', 
                        '11:00 - 12:00', '12:00 - 13:00', '13:00 - 14:00', 
                        '14:00 - 15:00', '15:00 - 16:00', '16:00 - 17:00', 
                        '17:00 - 18:00', '18:00 - 19:00', '19:00 - 20:00'
                      ].map((hr) => (
                        <button
                          key={hr}
                          type="button"
                          onClick={() => {
                            setTimeRangeFilter(hr);
                            setIsTimeDropdownOpen(false);
                            setPage(1);
                          }}
                          className={`w-full px-2 py-1 rounded-md text-left transition flex items-center justify-between ${
                            timeRangeFilter === hr
                              ? 'bg-blue-50 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 font-semibold'
                              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800'
                          }`}
                        >
                          <span>{hr}</span>
                          {timeRangeFilter === hr && <Check className="w-3 h-3 text-blue-600 dark:text-blue-400" />}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="h-px bg-slate-100 dark:bg-slate-700/60" />

                  {/* Custom Range Section */}
                  <div>
                    <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 px-1 mb-1 block">Custom Window</span>
                    <div className="flex items-center gap-1.5">
                      <input
                        type="time"
                        value={customStartTime}
                        onChange={e => setCustomStartTime(e.target.value)}
                        className="w-1/2 px-2 py-1 bg-slate-50 dark:bg-[#1C2638] border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                      />
                      <span className="text-slate-400 text-xs">to</span>
                      <input
                        type="time"
                        value={customEndTime}
                        onChange={e => setCustomEndTime(e.target.value)}
                        className="w-1/2 px-2 py-1 bg-slate-50 dark:bg-[#1C2638] border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setTimeRangeFilter(`${customStartTime} - ${customEndTime}`);
                        setIsTimeDropdownOpen(false);
                        setPage(1);
                      }}
                      className="w-full mt-2 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold transition"
                    >
                      Apply Custom Time
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-2 xl:col-span-2">
              <button 
                onClick={handleSearch}
                className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition shadow-sm cursor-pointer"
              >
                <Search className="w-3.5 h-3.5" />
                <span>Search</span>
              </button>
              <button 
                onClick={handleReset}
                className="px-4 py-2 border border-slate-200 dark:border-slate-700/80 bg-slate-50 dark:bg-[#1C2638] hover:bg-slate-100 dark:hover:bg-slate-700/60 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5 text-slate-500 dark:text-slate-400" />
                <span>Reset</span>
              </button>
            </div>

          </div>
        </div>

        {/* 4 Metric Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          
          {/* Card 1: Total Screenshots */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800/80 rounded-2xl p-4 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center">
              <ImageIcon className="w-6 h-6" />
            </div>
            <div>
              <div className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Total Screenshots</div>
              <div className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">{metrics.totalScreenshots}</div>
              <div className="text-[11px] text-slate-400 dark:text-slate-500 mt-0.5">Selected Date</div>
            </div>
          </div>

          {/* Card 2: Active Employees */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800/80 rounded-2xl p-4 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center">
              <Users className="w-6 h-6" />
            </div>
            <div>
              <div className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Active Employees</div>
              <div className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">{metrics.activeEmployees}</div>
              <div className="text-[11px] text-slate-400 dark:text-slate-500 mt-0.5">On Selected Date</div>
            </div>
          </div>

          {/* Card 3: Working Screenshots */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800/80 rounded-2xl p-4 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <Clock className="w-6 h-6" />
            </div>
            <div>
              <div className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Working Screenshots</div>
              <div className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">{metrics.workingCount}</div>
              <div className="text-[11px] text-emerald-600 dark:text-emerald-400 font-semibold mt-0.5">{metrics.workingPercentage}%</div>
            </div>
          </div>

          {/* Card 4: Idle Screenshots */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800/80 rounded-2xl p-4 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 flex items-center justify-center">
              <Coffee className="w-6 h-6" />
            </div>
            <div>
              <div className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Idle Screenshots</div>
              <div className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">{metrics.idleCount}</div>
              <div className="text-[11px] text-amber-600 dark:text-amber-400 font-semibold mt-0.5">{metrics.idlePercentage}%</div>
            </div>
          </div>

        </div>

        {/* Main Content: Gallery Grid (Left) + Detail Panel (Right) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* Left Grid: Screenshots List */}
          <div className={`${selectedScreenshot ? 'lg:col-span-8 xl:col-span-8' : 'lg:col-span-12'} bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm`}>
            
            {/* Top Toolbar */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 gap-3">
              <h2 className="text-base font-bold text-slate-900 dark:text-white">
                Screenshots ({metrics.totalScreenshots})
              </h2>

              <div className="flex items-center gap-2.5">
                <select 
                  value={sortOption} 
                  onChange={e => setSortOption(e.target.value as any)}
                  className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium text-slate-700 dark:text-slate-300 focus:outline-none"
                >
                  <option value="Newest First">Newest First</option>
                  <option value="Oldest First">Oldest First</option>
                </select>

                <div className="flex items-center border border-slate-200 dark:border-slate-700 rounded-lg p-0.5 bg-slate-50 dark:bg-slate-800">
                  <button 
                    onClick={() => setViewMode('grid')}
                    className={`p-1.5 rounded-md transition ${viewMode === 'grid' ? 'bg-blue-600 text-white shadow-xs' : 'text-slate-400 hover:text-slate-600'}`}
                    title="Grid View"
                  >
                    <LayoutGrid className="w-3.5 h-3.5" />
                  </button>
                  <button 
                    onClick={() => setViewMode('list')}
                    className={`p-1.5 rounded-md transition ${viewMode === 'list' ? 'bg-blue-600 text-white shadow-xs' : 'text-slate-400 hover:text-slate-600'}`}
                    title="List View"
                  >
                    <List className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>

            {/* Screenshots Cards Grid */}
            {loading ? (
              <div className="py-16 flex flex-col items-center justify-center gap-3">
                <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                <p className="text-xs text-slate-400 font-medium">Loading screenshots...</p>
              </div>
            ) : !employeeFilter ? (
              <div className="py-20 text-center text-slate-400 flex flex-col items-center justify-center">
                <div className="w-16 h-16 rounded-2xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center mb-4 shadow-sm">
                  <User className="w-8 h-8" />
                </div>
                <h3 className="text-base font-bold text-slate-800 dark:text-slate-100">Select an Employee to View Screenshots</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1.5 max-w-sm leading-relaxed">
                  To ensure ultra-fast performance across all monitored PCs, select an employee from the dropdown above to stream their captured screens.
                </p>
              </div>
            ) : screenshots.length === 0 ? (
              <div className="py-16 text-center text-slate-400">
                <ImageIcon className="w-12 h-12 mx-auto text-slate-300 mb-2" />
                <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">No Screenshots Found</p>
                <p className="text-xs text-slate-400 mt-1">Try adjusting the filter criteria or date range.</p>
              </div>
            ) : viewMode === 'grid' ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4 pt-4">
                {screenshots.map(shot => {
                  const isSelected = selectedScreenshot?.id === shot.id;
                  return (
                    <div 
                      key={shot.id} 
                      onClick={() => setSelectedScreenshot(shot)}
                      className={`group rounded-xl border p-2.5 transition cursor-pointer flex flex-col justify-between ${
                        isSelected 
                          ? 'border-blue-500 ring-2 ring-blue-500/30 bg-blue-50/30 dark:bg-blue-900/20 shadow-md' 
                          : 'border-slate-200 dark:border-slate-800/90 hover:border-slate-300 dark:hover:border-slate-700 hover:shadow-sm bg-white dark:bg-[#151E2E]'
                      }`}
                    >
                      {/* Image Thumbnail */}
                      <div className="aspect-[16/10] bg-slate-900 rounded-lg overflow-hidden relative border border-slate-100 dark:border-slate-800/80">
                        <img 
                          src={shot.imageUrl} 
                          alt={shot.windowTitle}
                          className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                          onError={(e: any) => {
                            // Fallback to sample placeholder
                            e.target.onerror = null;
                            e.target.src = 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=900&auto=format&fit=crop&q=80';
                          }}
                        />
                        {/* Expand View Icon Button */}
                        <div className="absolute top-2 right-2 transition">
                          <button 
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              setFullSizeModal(shot);
                            }}
                            className="p-1.5 rounded-lg bg-black/60 hover:bg-black/85 text-white/90 hover:text-white backdrop-blur-md text-xs font-semibold shadow-sm transition border border-white/10"
                            title="Expand View"
                          >
                            <Maximize2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>

                      {/* Time & Status Row */}
                      <div className="flex items-center justify-between mt-2.5 px-1">
                        <span className="text-[11px] font-bold text-slate-700 dark:text-slate-200">{shot.timeFormatted}</span>
                        <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                          shot.status === 'Idle' 
                            ? 'bg-amber-100 text-amber-700 dark:bg-amber-950/60 dark:text-amber-400 border border-amber-200/50 dark:border-amber-800/40' 
                            : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400 border border-emerald-200/50 dark:border-emerald-800/40'
                        }`}>
                          {shot.status}
                        </span>
                      </div>

                      {/* App & Window Title Row */}
                      <div className="flex items-center gap-2 mt-2 pt-2 border-t border-slate-100 dark:border-slate-800 px-1">
                        {getAppIcon(shot.appName)}
                        <div className="truncate">
                          <div className="text-xs font-bold text-slate-800 dark:text-slate-200 truncate">{shot.appName}</div>
                          <div className="text-[10px] text-slate-400 dark:text-slate-400 truncate">{shot.windowTitle}</div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              /* List View */
              <div className="divide-y divide-slate-100 dark:divide-slate-800 pt-2 text-xs">
                {screenshots.map(shot => {
                  const isSelected = selectedScreenshot?.id === shot.id;
                  return (
                    <div 
                      key={shot.id}
                      onClick={() => setSelectedScreenshot(shot)}
                      className={`py-3 px-3 rounded-xl flex items-center justify-between gap-4 transition cursor-pointer ${
                        isSelected 
                          ? 'bg-blue-50/60 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800' 
                          : 'hover:bg-slate-50 dark:hover:bg-slate-800/50'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-16 h-10 rounded-lg overflow-hidden bg-slate-900 shrink-0 border border-slate-200 dark:border-slate-700">
                          <img src={shot.imageUrl} alt="" className="w-full h-full object-cover" />
                        </div>
                        <div>
                          <div className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                            <span>{shot.appName}</span>
                            <span className={`text-[10px] px-2 py-0.2 rounded-full font-semibold ${
                              shot.status === 'Idle' 
                                ? 'bg-amber-100 text-amber-700' 
                                : 'bg-emerald-100 text-emerald-700'
                            }`}>
                              {shot.status}
                            </span>
                          </div>
                          <div className="text-[11px] text-slate-500 truncate max-w-md">{shot.windowTitle}</div>
                        </div>
                      </div>

                      <div className="text-right">
                        <div className="font-semibold text-slate-800 dark:text-slate-200">{shot.timeFormatted}</div>
                        <div className="text-[10px] text-slate-400">{shot.dateFormatted}</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Pagination Controls */}
            {metrics.totalScreenshots > 0 && (
              <div className="flex flex-col sm:flex-row items-center justify-between pt-5 mt-4 border-t border-slate-100 dark:border-slate-800 gap-3 text-xs text-slate-500">
                <div>
                  Showing {screenshots.length > 0 ? (page - 1) * 12 + 1 : 0} to {Math.min(page * 12, metrics.totalScreenshots)} of {metrics.totalScreenshots} screenshots
                </div>
                {totalPages > 1 && (
                  <div className="flex items-center gap-1">
                    <button 
                      disabled={page <= 1}
                      onClick={() => setPage(p => Math.max(1, p - 1))}
                      className="p-1.5 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40 cursor-pointer"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    {Array.from({ length: totalPages }, (_, i) => i + 1)
                      .filter(num => num === 1 || num === totalPages || Math.abs(num - page) <= 1)
                      .map((num, idx, arr) => {
                        const prev = arr[idx - 1];
                        return (
                          <React.Fragment key={num}>
                            {prev && num - prev > 1 && <span className="px-1 text-slate-400">...</span>}
                            <button 
                              onClick={() => setPage(num)}
                              className={`w-8 h-8 rounded-lg font-semibold text-xs transition cursor-pointer ${
                                page === num 
                                  ? 'bg-blue-600 text-white shadow-xs' 
                                  : 'border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                              }`}
                            >
                              {num}
                            </button>
                          </React.Fragment>
                        );
                      })}
                    <button 
                      disabled={page >= totalPages}
                      onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                      className="p-1.5 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40 cursor-pointer"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>
            )}

          </div>

          {/* Right Panel: Screenshot Details (Exactly as shown in Image 3) */}
          {selectedScreenshot && (
            <div className="lg:col-span-4 xl:col-span-4 bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800/90 rounded-2xl p-5 shadow-sm sticky top-6">
              
              {/* Header */}
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800/80">
                <h3 className="font-bold text-sm text-slate-900 dark:text-white">Screenshot Details</h3>
                <button 
                  onClick={() => setSelectedScreenshot(null)}
                  className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Large Image Preview */}
              <div className="aspect-[16/10] bg-slate-900 rounded-xl overflow-hidden my-4 border border-slate-200 dark:border-slate-800/80 relative group">
                <img 
                  src={selectedScreenshot.imageUrl} 
                  alt={selectedScreenshot.windowTitle} 
                  className="w-full h-full object-cover"
                />
                <button 
                  type="button"
                  onClick={() => setFullSizeModal(selectedScreenshot)}
                  className="absolute bottom-2.5 right-2.5 px-2.5 py-1 rounded-lg bg-black/60 hover:bg-black/80 text-white text-[11px] font-medium flex items-center gap-1.5 backdrop-blur-xs transition"
                >
                  <Maximize2 className="w-3 h-3" />
                  <span>Full View</span>
                </button>
              </div>

              {/* Metadata Fields List */}
              <div className="divide-y divide-slate-100 dark:divide-slate-800/70 text-xs">
                
                {/* Employee */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    <span>Employee</span>
                  </div>
                  <span className="font-semibold text-slate-800 dark:text-slate-100">
                    {selectedScreenshot.employeeName} ({selectedScreenshot.employeeCode})
                  </span>
                </div>

                {/* Device */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <Laptop className="w-3.5 h-3.5 text-slate-400" />
                    <span>Device</span>
                  </div>
                  <span className="font-semibold text-slate-800 dark:text-slate-100">
                    {selectedScreenshot.hostname}
                  </span>
                </div>

                {/* Captured At */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span>Captured At</span>
                  </div>
                  <span className="font-semibold text-slate-800 dark:text-slate-100">
                    {selectedScreenshot.dateFormatted} {selectedScreenshot.timeFormatted}
                  </span>
                </div>

                {/* Application */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <Monitor className="w-3.5 h-3.5 text-slate-400" />
                    <span>Application</span>
                  </div>
                  <span className="font-semibold text-slate-800 dark:text-slate-100">
                    {selectedScreenshot.appName}
                  </span>
                </div>

                {/* Window Title */}
                <div className="py-2.5 flex flex-col gap-1">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <ExternalLink className="w-3.5 h-3.5 text-slate-400" />
                    <span>Window Title</span>
                  </div>
                  <span className="font-medium text-slate-800 dark:text-slate-200 pl-5 break-words">
                    {selectedScreenshot.windowTitle}
                  </span>
                </div>

                {/* Status */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <CheckCircle2 className="w-3.5 h-3.5 text-slate-400" />
                    <span>Status</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-semibold text-slate-800 dark:text-slate-100">
                    <span className={`w-2 h-2 rounded-full ${selectedScreenshot.status === 'Idle' ? 'bg-amber-500' : 'bg-emerald-500'}`}></span>
                    <span>{selectedScreenshot.status}</span>
                  </div>
                </div>

                {/* Activity Duration */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span>Activity Duration</span>
                  </div>
                  <span className="font-semibold text-slate-800 dark:text-slate-100">
                    {selectedScreenshot.activityDuration}
                  </span>
                </div>

                {/* File Name */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                    <ImageIcon className="w-3.5 h-3.5 text-slate-400" />
                    <span>File Name</span>
                  </div>
                  <span className="font-mono text-[11px] text-slate-600 dark:text-slate-400 truncate max-w-[180px]">
                    {selectedScreenshot.fileName}
                  </span>
                </div>

              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-3 mt-5 pt-4 border-t border-slate-100 dark:border-slate-800/80">
                <button 
                  onClick={() => setFullSizeModal(selectedScreenshot)}
                  className="flex-1 py-2 px-3 border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800/80 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition shadow-xs cursor-pointer"
                >
                  <Maximize2 className="w-3.5 h-3.5" />
                  <span>View Full Size</span>
                </button>

                <button 
                  onClick={() => handleDownload(selectedScreenshot)}
                  className="flex-1 py-2 px-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition shadow-sm cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download</span>
                </button>
              </div>

            </div>
          )}

        </div>

      </div>

      {/* Full Size Lightbox Modal */}
      {fullSizeModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-in fade-in duration-200">
          <div className="relative max-w-6xl w-full bg-slate-900 rounded-2xl overflow-hidden border border-slate-800 shadow-2xl flex flex-col">
            
            {/* Modal Top Bar */}
            <div className="px-6 py-3.5 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between text-white">
              <div>
                <div className="text-sm font-bold">{fullSizeModal.appName} • {fullSizeModal.windowTitle}</div>
                <div className="text-xs text-slate-400">{fullSizeModal.employeeName} ({fullSizeModal.employeeCode}) • {fullSizeModal.dateFormatted} {fullSizeModal.timeFormatted}</div>
              </div>
              <div className="flex items-center gap-2">
                <button 
                  onClick={() => handleDownload(fullSizeModal)}
                  className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold flex items-center gap-1.5 transition"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download</span>
                </button>
                <button 
                  onClick={() => setFullSizeModal(null)}
                  className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Modal Image Display */}
            <div className="p-4 max-h-[80vh] flex items-center justify-center overflow-auto">
              <img 
                src={fullSizeModal.imageUrl} 
                alt="" 
                className="max-h-[75vh] w-auto object-contain rounded-lg shadow-lg"
              />
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
