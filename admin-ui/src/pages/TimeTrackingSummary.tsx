import { useState, useEffect, useRef } from 'react';
import { 
  Monitor, MonitorOff, Users, Smartphone, RefreshCw, 
  Search, Eye, X, ChevronLeft, ChevronRight, Activity, 
  MapPin, Clock, Info, CheckCircle2, Layout, Globe, Image as ImageIcon, Settings
} from 'lucide-react';
import Breadcrumb from '../components/ui/Breadcrumb';
import apiClient from '../lib/axios';

interface Employee {
  id: string;
  name: string;
  employeeCode: string;
}

interface DeviceTracking {
  id: string;
  employee: Employee;
  deviceId: string;
  hostname: string;
  os: string;
  status: 'ONLINE' | 'OFFLINE';
  activity: 'WORKING' | 'IDLE' | 'MEETING' | 'BREAK' | 'LUNCH' | '-';
  lastSeen: string;
  agentVersion: string;
  registeredAt: string;
  ipAddress: string;
  location: string;
}

interface SummaryData {
  topApplications: Array<{name: string, durationSeconds: number}>;
  topWebsites: Array<{name: string, durationSeconds: number}>;
  timeline: Array<{time: string, title: string, color: string, duration?: number, type: string}>;
  stats: {
    workSeconds: number;
    idleSeconds: number;
    breakSeconds: number;
    lunchSeconds: number;
    requiredSeconds: number;
    remainingSeconds: number;
    efficiency: number;
    firstWorkStart: string | null;
    lastWorkEnd: string | null;
  };
  date?: string;
}

export default function TimeTrackingSummary() {
  const [devices, setDevices] = useState<DeviceTracking[]>([]);
  const [summaryData, setSummaryData] = useState<SummaryData | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<string>('');
  
  // Selected device for side panel
  const [selectedDevice, setSelectedDevice] = useState<DeviceTracking | null>(null);
  const [activeTab, setActiveTab] = useState('Overview');
  
  // Modals
  const [showTimelineModal, setShowTimelineModal] = useState(false);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('All Status');
  const [osFilter, setOsFilter] = useState('All OS');
  const [activityFilter, setActivityFilter] = useState('All Activity');
  
  // Pagination
  const [page, setPage] = useState(0);
  const size = 8;
  
  // Date selection
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const selectedDateRef = useRef<string>(selectedDate);
  useEffect(() => {
    selectedDateRef.current = selectedDate;
    fetchTrackingData();
  }, [selectedDate]);
  
  const selectedDeviceIdRef = useRef<string | null>(null);

  const fetchTrackingData = async () => {
    try {
      // Don't set loading true on every 10s refresh so it doesn't flicker
      if (devices.length === 0) setLoading(true);
      
      const res = await apiClient.get('/api/admin/tracking/devices');
      const fetchedDevices: DeviceTracking[] = res.data || [];
      setDevices(fetchedDevices);

      // Fetch summary (if a device is selected, we could fetch their specific summary, but for now we fetch global or specific)
      const summaryRes = await apiClient.get('/api/admin/tracking/summary?date=' + selectedDateRef.current + (selectedDeviceIdRef.current ? `&employeeCode=${fetchedDevices.find(d => d.id === selectedDeviceIdRef.current)?.employee.employeeCode || ''}` : ''));
      setSummaryData(summaryRes.data);
      
      // Update selected device if panel is open to get latest status/activity
      if (selectedDeviceIdRef.current) {
        const updatedSelected = fetchedDevices.find(d => d.id === selectedDeviceIdRef.current);
        if (updatedSelected) setSelectedDevice(updatedSelected);
      }
      
      setLastUpdated(new Date().toLocaleString('en-US', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true }));
    } catch (error) {
      console.error("Failed to fetch tracking data", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTrackingData();
    
    // Auto refresh every 1 second
    const interval = setInterval(() => {
      fetchTrackingData();
    }, 1000);
    
    return () => clearInterval(interval);
  }, []);

  const handleSelectDevice = (device: DeviceTracking | null) => {
    setSelectedDevice(device);
    selectedDeviceIdRef.current = device ? device.id : null;
    if (device) {
      // Re-fetch summary immediately for this user
      apiClient.get(`/api/admin/tracking/summary?date=${selectedDateRef.current}&employeeCode=${device.employee.employeeCode}`)
        .then(res => setSummaryData(res.data))
        .catch(console.error);
    } else {
      // Re-fetch global summary
      apiClient.get(`/api/admin/tracking/summary?date=${selectedDateRef.current}`)
        .then(res => setSummaryData(res.data))
        .catch(console.error);
    }
  };

  const filteredDevices = devices.filter(d => {
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      if (!d.employee.name.toLowerCase().includes(q) && 
          !d.employee.employeeCode?.toLowerCase().includes(q) &&
          !d.deviceId.toLowerCase().includes(q) &&
          !d.hostname.toLowerCase().includes(q)) {
        return false;
      }
    }
    if (statusFilter !== 'All Status' && d.status !== statusFilter.toUpperCase()) return false;
    if (osFilter !== 'All OS' && d.os !== osFilter) return false;
    if (activityFilter !== 'All Activity' && d.activity !== activityFilter.toUpperCase()) return false;
    return true;
  });

  const totalPages = Math.ceil(filteredDevices.length / size);
  const paginatedDevices = filteredDevices.slice(page * size, (page + 1) * size);

  const onlineCount = devices.filter(d => d.status === 'ONLINE').length;
  const offlineCount = devices.filter(d => d.status === 'OFFLINE').length;
  const onlinePercent = devices.length > 0 ? Math.round((onlineCount / devices.length) * 100) : 0;
  const offlinePercent = devices.length > 0 ? Math.round((offlineCount / devices.length) * 100) : 0;

  const getActivityBadge = (activity: string) => {
    switch(activity) {
      case 'WORKING': return <span className="text-emerald-600 font-bold flex items-center gap-1.5"><div className="w-2 h-2 rounded-full bg-emerald-500"></div> WORKING</span>;
      case 'IDLE': return <span className="text-amber-500 font-bold flex items-center gap-1.5"><div className="w-2 h-2 rounded-full bg-amber-500"></div> IDLE</span>;
      case 'MEETING': return <span className="text-blue-600 font-bold flex items-center gap-1.5"><div className="w-2 h-2 rounded-full bg-blue-500"></div> MEETING</span>;
      case 'BREAK': return <span className="text-orange-500 font-bold flex items-center gap-1.5"><div className="w-2 h-2 rounded-full bg-orange-500"></div> BREAK</span>;
      case 'LUNCH': return <span className="text-violet-600 font-bold flex items-center gap-1.5"><div className="w-2 h-2 rounded-full bg-violet-500"></div> LUNCH</span>;
      default: return <span className="text-slate-400 font-medium">-</span>;
    }
  };

  const getInitials = (name: string) => {
    if (!name) return 'U';
    const parts = name.split(' ');
    if (parts.length > 1) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.substring(0, 2).toUpperCase();
  };

  const getAvatarColor = (name: string) => {
    if (!name) return 'bg-slate-500';
    const colors = ['bg-indigo-500', 'bg-purple-500', 'bg-pink-500', 'bg-rose-500', 'bg-orange-500', 'bg-emerald-500', 'bg-cyan-500'];
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  };

  const formatDuration = (seconds: number) => {
    if (!seconds) return '0m';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
  };
  
  const calculatePct = (duration: number, list: any[]) => {
    if (!list || list.length === 0) return '0%';
    const total = list.reduce((sum, item) => sum + item.durationSeconds, 0);
    if (total === 0) return '0%';
    return Math.round((duration / total) * 100) + '%';
  };

  const getAppBadgeColor = (name: string) => {
    const colors = [
      'bg-indigo-50 text-indigo-600 border-indigo-200', 
      'bg-purple-50 text-purple-600 border-purple-200', 
      'bg-pink-50 text-pink-600 border-pink-200', 
      'bg-rose-50 text-rose-600 border-rose-200', 
      'bg-orange-50 text-orange-600 border-orange-200', 
      'bg-emerald-50 text-emerald-600 border-emerald-200', 
      'bg-cyan-50 text-cyan-600 border-cyan-200',
      'bg-blue-50 text-blue-600 border-blue-200'
    ];
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  };

  const getAppIcon = (appName: string) => {
    const name = appName.toLowerCase();
    if (name.includes('chrome')) return <img src="https://upload.wikimedia.org/wikipedia/commons/8/87/Google_Chrome_icon_%282011%29.png" className="w-4 h-4 shrink-0 object-contain" alt="Chrome" />;
    if (name.includes('idea')) return <img src="https://upload.wikimedia.org/wikipedia/commons/9/9c/IntelliJ_IDEA_Icon.svg" className="w-4 h-4 shrink-0 object-contain" alt="IntelliJ" />;
    if (name.includes('code') || name.includes('vscode')) return <img src="https://upload.wikimedia.org/wikipedia/commons/9/9a/Visual_Studio_Code_1.35_icon.svg" className="w-4 h-4 shrink-0 object-contain" alt="VS Code" />;
    if (name.includes('teams')) return <img src="https://upload.wikimedia.org/wikipedia/commons/c/c9/Microsoft_Office_Teams_%282018%E2%80%93present%29.svg" className="w-4 h-4 shrink-0 object-contain" alt="Teams" />;
    if (name.includes('notion')) return <img src="https://upload.wikimedia.org/wikipedia/commons/e/e9/Notion-logo.svg" className="w-4 h-4 shrink-0 object-contain" alt="Notion" />;
    if (name.includes('excel')) return <img src="https://upload.wikimedia.org/wikipedia/commons/3/34/Microsoft_Office_Excel_%282019%E2%80%93present%29.svg" className="w-4 h-4 shrink-0 object-contain" alt="Excel" />;
    if (name.includes('word')) return <img src="https://upload.wikimedia.org/wikipedia/commons/f/fd/Microsoft_Office_Word_%282019%E2%80%93present%29.svg" className="w-4 h-4 shrink-0 object-contain" alt="Word" />;
    if (name.includes('slack')) return <img src="https://upload.wikimedia.org/wikipedia/commons/d/d5/Slack_icon_2019.svg" className="w-4 h-4 shrink-0 object-contain" alt="Slack" />;
    
    // Dynamic beautiful fallback badge for ANY other app
    const initial = appName.replace('.exe', '').charAt(0).toUpperCase();
    const colorClasses = getAppBadgeColor(appName);
    return <div className={`w-4 h-4 shrink-0 rounded border flex items-center justify-center text-[10px] font-bold ${colorClasses}`}>{initial}</div>;
  };

  const getWebsiteIcon = (siteName: string) => {
    const domain = siteName.replace(/^(?:https?:\/\/)?(?:www\.)?/i, "").split('/')[0];
    if (domain) {
      return <img src={`https://www.google.com/s2/favicons?domain=${domain}&sz=64`} className="w-4 h-4 shrink-0 object-contain rounded-[3px]" alt={domain} />;
    }
    return <Globe className="h-4 w-4 shrink-0 text-slate-400" />;
  };

  return (
    <div className="max-w-[1600px] mx-auto space-y-5 pb-10">
      
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <div className="-mb-2">
            <Breadcrumb items={[
              { label: 'Tracking & Monitoring', href: '#' },
              { label: 'Time Tracking Summary' }
            ]} />
          </div>
          <h2 className="text-2xl font-bold text-slate-800 mt-2">Time Tracking Summary</h2>
          <p className="text-slate-500 text-sm mt-0.5">Monitor employee work hours, activity and live device status in real-time</p>
        </div>
        
        <div className="flex items-center gap-4">
          <input 
            type="date" 
            value={selectedDate} 
            onChange={(e) => setSelectedDate(e.target.value)} 
            className="px-4 py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 bg-white"
          />
          <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 border border-emerald-100 rounded-full">
            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
            <span className="text-xs font-bold text-emerald-700">Live Data</span>
          </div>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-emerald-50 p-4 rounded-xl text-emerald-500 shrink-0">
            <Monitor className="h-7 w-7" />
          </div>
          <div>
            <h3 className="text-2xl font-bold text-slate-800">{onlineCount}</h3>
            <p className="text-sm font-medium text-slate-500">Online Devices</p>
            <p className="text-xs font-bold text-emerald-600 mt-0.5">{onlinePercent}% online</p>
          </div>
        </div>
        
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-red-50 p-4 rounded-xl text-red-500 shrink-0">
            <MonitorOff className="h-7 w-7" />
          </div>
          <div>
            <h3 className="text-2xl font-bold text-slate-800">{offlineCount}</h3>
            <p className="text-sm font-medium text-slate-500">Offline Devices</p>
            <p className="text-xs font-bold text-red-600 mt-0.5">{offlinePercent}% offline</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-blue-50 p-4 rounded-xl text-blue-500 shrink-0">
            <Users className="h-7 w-7" />
          </div>
          <div>
            <h3 className="text-2xl font-bold text-slate-800">{new Set(devices.map(d=>d.employee.id)).size}</h3>
            <p className="text-sm font-medium text-slate-500">Total Employees</p>
            <p className="text-xs font-bold text-emerald-600 mt-0.5">{onlineCount} active now</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-purple-50 p-4 rounded-xl text-purple-500 shrink-0">
            <Smartphone className="h-7 w-7" />
          </div>
          <div>
            <h3 className="text-2xl font-bold text-slate-800">{devices.length}</h3>
            <p className="text-sm font-medium text-slate-500">Total Devices</p>
            <p className="text-xs font-bold text-slate-500 mt-0.5">{devices.length} registered</p>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[250px]">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            <Search className="h-4 w-4" />
          </div>
          <input 
            type="text" 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Search by employee name, code, device ID or hostname..." 
            className="w-full pl-9 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm shadow-sm" 
          />
        </div>
        
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-medium outline-none focus:border-blue-500 shadow-sm min-w-[140px]">
          <option>All Status</option>
          <option>Online</option>
          <option>Offline</option>
        </select>
        
        <select value={osFilter} onChange={e => setOsFilter(e.target.value)} className="px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-medium outline-none focus:border-blue-500 shadow-sm min-w-[140px]">
          <option>All OS</option>
          <option>Windows 11</option>
          <option>Windows 10</option>
          <option>macOS</option>
        </select>
        
        <select value={activityFilter} onChange={e => setActivityFilter(e.target.value)} className="px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-medium outline-none focus:border-blue-500 shadow-sm min-w-[140px]">
          <option>All Activity</option>
          <option>Working</option>
          <option>Idle</option>
          <option>Meeting</option>
          <option>Break</option>
        </select>

        <button className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-medium text-slate-600 hover:bg-slate-50 transition shadow-sm">
          <Clock className="h-4 w-4" /> Select Date Range
        </button>
      </div>

      {/* Main Layout Area */}
      <div className="flex flex-col xl:flex-row gap-5 items-start">
        
        {/* Left Column (Table & Widgets) */}
        <div className="flex-1 min-w-0 space-y-5 w-full">
          
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="w-full overflow-x-auto [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
              <table className="w-full text-left text-sm table-auto lg:table-fixed min-w-[800px] lg:min-w-0">
                <thead className="bg-slate-50 text-slate-600 text-[11px] uppercase tracking-wider font-bold border-b border-slate-100">
                  <tr>
                    <th className="px-2 py-3 w-[4%]">#</th>
                    <th className="px-2 py-3 w-[15%]">Employee</th>
                    <th className="px-2 py-3 w-[10%] truncate">Emp Code</th>
                    <th className="px-2 py-3 w-[12%]">Device ID</th>
                    <th className="px-2 py-3 w-[12%]">Hostname</th>
                    <th className="px-2 py-3 w-[10%]">OS</th>
                    <th className="px-2 py-3 w-[9%]">Status</th>
                    <th className="px-2 py-3 w-[10%]">Activity</th>
                    <th className="px-2 py-3 w-[10%]">Last Seen</th>
                    <th className="px-2 py-3 w-[8%] text-center">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white text-slate-700 text-xs">
                  {paginatedDevices.length === 0 ? (
                    <tr><td colSpan={10} className="px-6 py-12 text-center text-slate-400">No devices found.</td></tr>
                  ) : (
                    paginatedDevices.map((device, index) => (
                      <tr key={device.id} className={`hover:bg-slate-50 transition-colors ${selectedDevice?.id === device.id ? 'bg-blue-50/50' : ''}`}>
                        <td className="px-2 py-2.5">{page * size + index + 1}</td>
                        <td className="px-2 py-2.5 truncate font-medium text-slate-800" title={device.employee.name}>
                          {device.employee.name}
                        </td>
                        <td className="px-2 py-2.5 text-slate-500 truncate" title={device.employee.employeeCode}>
                          {device.employee.employeeCode || '-'}
                        </td>
                        <td className="px-2 py-2.5 font-mono text-[10px] text-slate-500 truncate" title={device.deviceId}>
                          {device.deviceId}
                        </td>
                        <td className="px-2 py-2.5 truncate" title={device.hostname}>{device.hostname}</td>
                        <td className="px-2 py-2.5 truncate" title={device.os}>{device.os}</td>
                        <td className="px-2 py-2.5">
                          <span className={`px-2 py-0.5 rounded text-[9px] font-bold tracking-wider ${device.status === 'ONLINE' ? 'bg-emerald-500 text-white' : 'bg-red-500 text-white'}`}>
                            {device.status}
                          </span>
                        </td>
                        <td className="px-2 py-2.5 truncate">{getActivityBadge(device.activity)}</td>
                        <td className="px-2 py-2.5 text-[9px] text-slate-500 leading-tight truncate">
                          {device.lastSeen && typeof device.lastSeen === 'string' && device.lastSeen.includes(',') 
                            ? device.lastSeen.split(', ').map((p, i) => <div key={i}>{p}</div>)
                            : <div className="truncate">{device.lastSeen}</div>}
                        </td>
                        <td className="px-1 py-2.5 text-center">
                          <button onClick={() => handleSelectDevice(device)} className={`flex items-center justify-center w-full gap-1 px-2 py-1 rounded-md text-[10px] font-bold transition border ${selectedDevice?.id === device.id ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-blue-600 border-blue-200 hover:bg-blue-50'}`}>
                            <Eye className="h-3 w-3" /> View
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            
            {/* Pagination */}
            {filteredDevices.length > 0 && (
              <div className="p-4 border-t border-slate-100 flex justify-between items-center bg-white">
                <p className="text-sm text-slate-500 font-medium">
                  Showing {page * size + 1} to {Math.min((page + 1) * size, filteredDevices.length)} of {filteredDevices.length} entries
                </p>
                <div className="flex items-center gap-1">
                  <button onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0} className="px-3 py-1.5 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition">Previous</button>
                  <button className="w-8 h-8 flex items-center justify-center rounded-lg bg-blue-600 text-white text-sm font-bold shadow-sm">{page + 1}</button>
                  {page + 1 < totalPages && <button onClick={() => setPage(page + 1)} className="w-8 h-8 flex items-center justify-center rounded-lg border border-slate-200 text-slate-600 text-sm font-bold hover:bg-slate-50 transition">{page + 2}</button>}
                  {page + 2 < totalPages && <button onClick={() => setPage(page + 2)} className="w-8 h-8 flex items-center justify-center rounded-lg border border-slate-200 text-slate-600 text-sm font-bold hover:bg-slate-50 transition">{page + 3}</button>}
                  <button onClick={() => setPage(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1} className="px-3 py-1.5 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition">Next</button>
                </div>
              </div>
            )}
          </div>

          {/* Bottom Widgets */}
          <div className="grid grid-cols-1 xl:grid-cols-3 gap-5 items-start">
            {/* Widget 1 */}
            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="font-bold text-slate-800">Today's Activity Timeline</h3>
                  <p className="text-xs text-slate-500 mt-0.5">{selectedDevice ? `Employee ${selectedDevice.employee.employeeCode}` : 'Overview'}</p>
                </div>
              </div>
              <div className="pl-2 ml-2 pr-2">
                {!selectedDevice ? (
                  <div className="h-full flex flex-col items-center justify-center text-slate-400 pb-8">
                    <div className="h-10 w-10 rounded-full border border-slate-200 flex items-center justify-center mb-3">
                      <Monitor className="h-4 w-4 text-slate-400" />
                    </div>
                    <p className="text-sm font-bold text-slate-600">No Employee Selected</p>
                    <p className="text-xs text-center mt-1">Please select an employee to view their timeline.</p>
                  </div>
                ) : (
                  <>
                    <div className="space-y-4 border-l-2 border-slate-100">
                      {summaryData?.timeline && summaryData.timeline.length > 0 ? summaryData.timeline.slice(0, 6).map((event, i) => (
                        <div key={i} className="relative">
                          <div className={`absolute -left-[6px] top-1 w-2.5 h-2.5 rounded-full border-2 border-white ${event.color === 'emerald' ? 'bg-emerald-500' : event.color === 'blue' ? 'bg-blue-500' : event.color === 'slate' ? 'bg-slate-400' : event.color === 'violet' || event.color === 'purple' ? 'bg-violet-500' : 'bg-orange-400'} ${i === Math.min(summaryData.timeline.length, 6)-1 && event.title.includes('Last Active') ? 'animate-pulse' : ''}`}></div>
                          <div className="flex justify-between text-xs pl-4">
                            <span className="font-medium text-slate-500">{event.time}</span>
                            <span className={`font-bold ${event.color === 'emerald' ? 'text-emerald-700' : event.color === 'blue' ? 'text-blue-600' : event.color === 'slate' ? 'text-slate-500' : event.color === 'violet' || event.color === 'purple' ? 'text-violet-600' : 'text-orange-600'}`}>{event.title.split(' (')[0]}</span>
                          </div>
                        </div>
                      )) : (
                        <div className="h-full flex flex-col items-center justify-center text-slate-400 py-8">
                          <div className="h-10 w-10 rounded-full border border-slate-200 flex items-center justify-center mb-3">
                            <Info className="h-4 w-4 text-slate-400" />
                          </div>
                          <p className="text-sm font-bold text-slate-600">No Data Available</p>
                          <p className="text-xs text-center mt-1">No timeline events recorded yet.</p>
                        </div>
                      )}
                    </div>
                    {summaryData?.timeline && summaryData.timeline.length > 0 && (
                      <button onClick={() => setShowTimelineModal(true)} className="text-blue-600 text-xs font-bold mt-5 mb-2 flex items-center gap-1 hover:underline ml-4">
                        View Full Timeline <ChevronRight className="h-3 w-3" />
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>
            
            {/* Widget 2 */}
            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="font-bold text-slate-800">Top Applications</h3>
                  <p className="text-xs text-slate-500 mt-0.5">Most used applications for the selected day.</p>
                </div>
                {selectedDevice && <span className="text-xs text-blue-600 font-bold cursor-pointer hover:underline mt-1 whitespace-nowrap shrink-0">View All &rarr;</span>}
              </div>
              <div className="pr-2">
                {!selectedDevice ? (
                  <div className="h-full flex flex-col items-center justify-center text-slate-400 pb-8">
                    <div className="h-10 w-10 rounded-full border border-slate-200 flex items-center justify-center mb-3">
                      <Layout className="h-4 w-4 text-slate-400" />
                    </div>
                    <p className="text-sm font-bold text-slate-600">No Employee Selected</p>
                    <p className="text-xs text-center mt-1">Please select an employee to view applications.</p>
                  </div>
                ) : (
                  <div className="space-y-2.5 mt-1">
                    {summaryData?.topApplications && summaryData.topApplications.length > 0 ? summaryData.topApplications.slice(0, 6).map((app, i) => (
                      <div key={i} className="flex items-center justify-between text-xs">
                        <div className="flex items-center gap-2">
                          {getAppIcon(app.name)}
                          <span className="font-medium text-slate-700 truncate max-w-[120px]" title={app.name}>{app.name}</span>
                        </div>
                        <div className="flex gap-4 shrink-0">
                          <span className="text-slate-500">{formatDuration(app.durationSeconds)}</span>
                          <span className="font-bold text-slate-800 w-8 text-right">{calculatePct(app.durationSeconds, summaryData.topApplications)}</span>
                        </div>
                      </div>
                    )) : (
                      <div className="h-full flex flex-col items-center justify-center text-slate-400 py-8">
                        <div className="h-10 w-10 rounded-full border border-slate-200 flex items-center justify-center mb-3">
                          <Info className="h-4 w-4 text-slate-400" />
                        </div>
                        <p className="text-sm font-bold text-slate-600">No Data Available</p>
                        <p className="text-xs text-center mt-1">No application usage found.</p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Widget 3 */}
            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="font-bold text-slate-800">Top Websites</h3>
                  <p className="text-xs text-slate-500 mt-0.5">Most visited websites for the selected day.</p>
                </div>
                {selectedDevice && <span className="text-xs text-blue-600 font-bold cursor-pointer hover:underline mt-1 whitespace-nowrap shrink-0">View All &rarr;</span>}
              </div>
              <div className="pr-2">
                {!selectedDevice ? (
                  <div className="h-full flex flex-col items-center justify-center text-slate-400 pb-8">
                    <div className="h-10 w-10 rounded-full border border-slate-200 flex items-center justify-center mb-3">
                      <Globe className="h-4 w-4 text-slate-400" />
                    </div>
                    <p className="text-sm font-bold text-slate-600">No Employee Selected</p>
                    <p className="text-xs text-center mt-1">Please select an employee to view websites.</p>
                  </div>
                ) : (
                  <div className="space-y-2.5 mt-1">
                    {summaryData?.topWebsites && summaryData.topWebsites.length > 0 ? summaryData.topWebsites.slice(0, 6).map((site, i) => (
                      <div key={i} className="flex items-center justify-between text-xs">
                        <div className="flex items-center gap-2">
                          {getWebsiteIcon(site.name)}
                          <span className="font-medium text-slate-700 truncate max-w-[120px]" title={site.name}>{site.name}</span>
                        </div>
                        <div className="flex gap-4 shrink-0">
                          <span className="text-slate-500">{formatDuration(site.durationSeconds)}</span>
                          <span className="font-bold text-slate-800 w-8 text-right">{calculatePct(site.durationSeconds, summaryData.topWebsites)}</span>
                        </div>
                      </div>
                    )) : (
                      <div className="h-full flex flex-col items-center justify-center text-slate-400 py-8">
                        <div className="h-10 w-10 rounded-full border border-slate-200 flex items-center justify-center mb-3">
                          <Info className="h-4 w-4 text-slate-400" />
                        </div>
                        <p className="text-sm font-bold text-slate-600">No Data Available</p>
                        <p className="text-xs text-center mt-1">No website usage found.</p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Right Column - Device Details Panel (Permanently Visible) */}
        <div className="w-[320px] shrink-0 flex flex-col gap-4 sticky top-4">
          {!selectedDevice ? (
            <div className="bg-white rounded-2xl shadow-lg border border-slate-200 flex flex-col items-center justify-center h-[500px] text-center p-8">
              <div className="bg-blue-50 p-6 rounded-full mb-4">
                <Monitor className="h-12 w-12 text-blue-400" />
              </div>
              <h3 className="text-lg font-bold text-slate-800 mb-2">No Device Selected</h3>
              <p className="text-sm text-slate-500">Click on "View" in the table row to see detailed information for a specific device and employee.</p>
            </div>
          ) : (
            <>
            <div className="bg-white rounded-2xl shadow-lg border border-slate-200 animate-in fade-in duration-300">
              {/* Panel Header */}
              <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
                <h3 className="font-bold text-slate-800 text-lg">Device Details</h3>
                <button onClick={() => handleSelectDevice(null)} className="p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition">
                  <X className="h-5 w-5" />
                </button>
              </div>
              
              {/* Panel Profile */}
              <div className="p-5 flex items-center justify-between border-b border-slate-100">
                <div className="flex items-center gap-3">
                  <div className={`h-12 w-12 rounded-xl flex items-center justify-center text-white font-bold text-lg shadow-sm ${getAvatarColor(selectedDevice.employee.name)}`}>
                    {getInitials(selectedDevice.employee.name)}
                  </div>
                  <div>
                    <h4 className="font-bold text-slate-800 text-lg">{selectedDevice.employee.name}</h4>
                    <p className="text-sm text-slate-500">Employee Code: {selectedDevice.employee.employeeCode || '-'}</p>
                  </div>
                </div>
                <span className={`px-2.5 py-1 rounded-md text-[10px] font-bold tracking-wider ${selectedDevice.status === 'ONLINE' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                  {selectedDevice.status}
                </span>
              </div>

              {/* Panel Tabs */}
              <div className="px-5 border-b border-slate-100 flex flex-wrap gap-x-4 gap-y-0 text-xs font-bold text-slate-500">
                {['Overview', 'Activity', 'Applications', 'Websites', 'Screenshots'].map(tab => (
                  <button 
                    key={tab} 
                    onClick={() => setActiveTab(tab)}
                    className={`py-3 border-b-2 transition-colors ${activeTab === tab ? 'border-blue-600 text-blue-600' : 'border-transparent hover:text-slate-700'}`}
                  >
                    {tab}
                  </button>
                ))}
              </div>

              {/* Panel Content (Overview) */}
              <div className="p-5">
                {activeTab === 'Overview' && (
                  <div className="space-y-6">
                    <div className="grid grid-cols-[120px_1fr] gap-y-4 text-sm">
                      
                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Monitor className="h-4 w-4" /> Device ID
                      </div>
                      <div className="text-slate-800 font-mono text-xs mt-0.5">{selectedDevice.deviceId}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Layout className="h-4 w-4" /> Hostname
                      </div>
                      <div className="text-slate-800 font-medium">{selectedDevice.hostname}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Info className="h-4 w-4" /> Operating System
                      </div>
                      <div className="text-slate-800">{selectedDevice.os}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Settings className="h-4 w-4" /> Agent Version
                      </div>
                      <div className="text-slate-800">{selectedDevice.agentVersion}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <CheckCircle2 className="h-4 w-4" /> Status
                      </div>
                      <div className="flex items-center gap-1.5 font-bold text-slate-800">
                        <div className={`w-2 h-2 rounded-full ${selectedDevice.status === 'ONLINE' ? 'bg-emerald-500' : 'bg-red-500'}`}></div>
                        {selectedDevice.status}
                      </div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Activity className="h-4 w-4" /> Activity
                      </div>
                      <div>{getActivityBadge(selectedDevice.activity)}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Clock className="h-4 w-4" /> Registered At
                      </div>
                      <div className="text-slate-800 text-xs mt-0.5">{selectedDevice.registeredAt}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Clock className="h-4 w-4" /> Last Seen
                      </div>
                      <div className="text-slate-800 text-xs mt-0.5">{selectedDevice.lastSeen.replace('T', ' ')}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <Globe className="h-4 w-4" /> IP Address
                      </div>
                      <div className="text-slate-800 font-mono text-xs mt-0.5">{selectedDevice.ipAddress}</div>

                      <div className="flex items-center gap-2 text-slate-500 font-medium">
                        <MapPin className="h-4 w-4" /> Location
                      </div>
                      <div className="text-slate-800 text-xs mt-0.5">{selectedDevice.location}</div>
                    </div>

                    {selectedDevice.status === 'ONLINE' && (
                      <div className="bg-emerald-50 border border-emerald-100 rounded-xl p-4 flex gap-3">
                        <div className="bg-emerald-500 rounded-full h-6 w-6 shrink-0 flex items-center justify-center text-white mt-0.5">
                          <CheckCircle2 className="h-4 w-4" />
                        </div>
                        <div>
                          <h4 className="font-bold text-emerald-800 text-sm">Device is online and tracking activity</h4>
                          <p className="text-xs text-emerald-600 mt-1">Actively tracking.</p>
                        </div>
                      </div>
                    )}

                  </div>
                )}
              </div>
            </div>

            {/* Recent Screenshots Card */}
            {activeTab === 'Overview' && (
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5">
                <div className="flex justify-between items-center mb-3">
                  <h4 className="font-bold text-slate-800 text-sm">Recent Screenshots</h4>
                  <span className="text-xs text-blue-600 font-bold cursor-pointer hover:underline">View All</span>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  {['01:58 PM', '01:45 PM', '01:30 PM', '01:15 PM'].map((time, i) => (
                    <div key={i} className="group cursor-pointer">
                      <div className="aspect-[4/3] bg-slate-900 rounded-lg overflow-hidden relative shadow-sm border border-slate-200 flex items-center justify-center mb-1 group-hover:ring-2 ring-blue-500 transition">
                        <ImageIcon className="h-5 w-5 text-slate-600" />
                      </div>
                      <p className="text-[10px] text-slate-500 text-center font-medium">{time}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
            
          </>
        )}
        </div>
      </div>

      {/* Activity Timeline Modal */}
      {showTimelineModal && summaryData && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200" onClick={() => setShowTimelineModal(false)}>
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-[700px] max-w-[95vw] max-h-[90vh] flex flex-col overflow-hidden" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <div>
                <h3 className="font-bold text-slate-800 text-lg">Activity Timeline</h3>
                <p className="text-xs text-slate-500">
                  {selectedDevice ? `Employee ${selectedDevice.employee.employeeCode} - ${selectedDate}` : `Global Timeline - ${selectedDate}`}
                </p>
              </div>
              <button onClick={() => setShowTimelineModal(false)} className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition">
                <X className="h-5 w-5" />
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto custom-scrollbar flex-1">
              {/* Productivity Overview */}
              <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 mb-4 flex items-center justify-between">
                <div>
                  <h4 className="font-bold text-blue-900">Productivity Score</h4>
                  <p className="text-xs text-blue-700 font-medium mt-1">Efficiency based on 8 hours required</p>
                </div>
                <div className="flex items-end gap-3">
                  <div className="text-right">
                    <p className="text-[10px] uppercase font-bold text-blue-500 mb-0.5">First Start</p>
                    <p className="text-xs font-bold text-blue-900">{summaryData.stats?.firstWorkStart || '--:--'}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-[10px] uppercase font-bold text-blue-500 mb-0.5">Last End</p>
                    <p className="text-xs font-bold text-blue-900">{summaryData.stats?.lastWorkEnd || '--:--'}</p>
                  </div>
                  <div className="text-right ml-4 pl-4 border-l border-blue-200">
                    <p className="text-3xl font-black text-blue-600">{summaryData.stats?.efficiency || 0}%</p>
                  </div>
                </div>
              </div>

              {/* Stats row */}
              <div className="grid grid-cols-4 gap-3 mb-8">
                <div className="bg-slate-50 border border-slate-100 rounded-xl p-3">
                  <p className="text-[10px] uppercase font-bold text-slate-500 mb-1">Working</p>
                  <p className="text-sm font-bold text-slate-800">{(() => {
                    const s = summaryData.stats?.workSeconds || 0;
                    return `${String(Math.floor(s/3600)).padStart(2,'0')}:${String(Math.floor((s%3600)/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
                  })()}</p>
                </div>
                <div className="bg-slate-50 border border-slate-100 rounded-xl p-3">
                  <p className="text-[10px] uppercase font-bold text-slate-500 mb-1">Idle</p>
                  <p className="text-sm font-bold text-slate-800">{(() => {
                    const s = summaryData.stats?.idleSeconds || 0;
                    return `${String(Math.floor(s/3600)).padStart(2,'0')}:${String(Math.floor((s%3600)/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
                  })()}</p>
                </div>
                <div className="bg-slate-50 border border-slate-100 rounded-xl p-3">
                  <p className="text-[10px] uppercase font-bold text-slate-500 mb-1">Break</p>
                  <p className="text-sm font-bold text-slate-800">{(() => {
                    const s = summaryData.stats?.breakSeconds || 0;
                    return `${String(Math.floor(s/3600)).padStart(2,'0')}:${String(Math.floor((s%3600)/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
                  })()}</p>
                </div>
                <div className="bg-slate-50 border border-slate-100 rounded-xl p-3">
                  <p className="text-[10px] uppercase font-bold text-slate-500 mb-1">Lunch</p>
                  <p className="text-sm font-bold text-slate-800">{(() => {
                    const s = summaryData.stats?.lunchSeconds || 0;
                    return `${String(Math.floor(s/3600)).padStart(2,'0')}:${String(Math.floor((s%3600)/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
                  })()}</p>
                </div>
              </div>

              {/* Full Timeline List */}
              <div className="space-y-0 pl-6 relative">
                <div className="absolute top-3 bottom-3 left-[123px] w-[2px] bg-slate-200 z-0"></div>
                {summaryData.timeline.map((event, i) => (
                  <div key={i} className={`relative flex items-start gap-6 py-3 px-4 -ml-4 hover:bg-slate-50 rounded-xl transition-all z-10 ${event.type === 'start' || event.type === 'end' ? 'opacity-80' : ''}`}>
                    <div className="w-[70px] shrink-0 text-right pt-0.5 z-10">
                      <span className="text-xs font-medium text-slate-500 bg-transparent py-1 whitespace-nowrap group-hover:text-slate-800">{event.time}</span>
                    </div>
                    
                    <div className={`w-3 h-3 rounded-full mt-1.5 z-10 border-2 border-white ring-2 ${event.color === 'emerald' ? 'bg-emerald-500 ring-emerald-100' : event.color === 'blue' ? 'bg-blue-500 ring-blue-100' : event.color === 'slate' ? 'bg-slate-400 ring-slate-100' : event.color === 'violet' || event.color === 'purple' ? 'bg-violet-500 ring-violet-100' : 'bg-orange-400 ring-orange-100'}`}></div>
                    
                    <div className="flex-1 pb-4 bg-white z-10">
                      <h4 className={`text-xs font-bold uppercase mb-0.5 ${event.color === 'emerald' ? 'text-slate-800' : event.color === 'blue' ? 'text-slate-800' : event.color === 'slate' ? 'text-slate-600' : 'text-slate-800'}`}>
                        {event.title.split(' (')[0]}
                      </h4>
                      {event.duration && (
                        <p className="text-[10px] text-slate-500">
                          Duration: {(() => {
                            const h = Math.floor(event.duration / 3600);
                            const m = Math.floor((event.duration % 3600) / 60);
                            const s = event.duration % 60;
                            return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
                          })()} • Ended {event.time}
                        </p>
                      )}
                      {!event.duration && event.type !== 'start' && event.type !== 'end' && (
                        <p className="text-[10px] text-slate-500">Started {event.time}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
