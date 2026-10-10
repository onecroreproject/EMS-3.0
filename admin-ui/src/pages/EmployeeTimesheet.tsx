import { useState, useEffect, useRef } from 'react';
import { 
  CheckCircle2, Clock, Coffee, Eye, Monitor, 
  Download, Printer, ChevronLeft, ChevronRight, Calendar, ChevronDown
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';
import Breadcrumb from '../components/ui/Breadcrumb';
import apiClient from '../lib/axios';
import { useReactToPrint } from 'react-to-print';
import DayViewPrintTemplate from '../components/timesheet/DayViewPrintTemplate';
import WeekViewPrintTemplate from '../components/timesheet/WeekViewPrintTemplate';
import MonthViewPrintTemplate from '../components/timesheet/MonthViewPrintTemplate';
import YearViewPrintTemplate from '../components/timesheet/YearViewPrintTemplate';

interface TimesheetSummary {
  workSeconds: number;
  idleSeconds: number;
  lunchSeconds: number;
  breakSeconds: number;
  totalSeconds: number;
  efficiency: number;
}

interface TimesheetRow {
  date: string;
  label: string;
  dayName: string;
  workSeconds: number;
  idleSeconds: number;
  lunchSeconds: number;
  breakSeconds: number;
  totalSeconds: number;
  efficiency: number;
}

interface HourlyRow {
  hour: string;
  workSeconds: number;
  idleSeconds: number;
  lunchSeconds: number;
  breakSeconds: number;
  totalSeconds: number;
}

interface TimesheetData {
  employeeCode: string;
  employeeName: string;
  departmentName: string;
  period: string;
  startDate: string;
  endDate: string;
  startDateLabel: string;
  endDateLabel: string;
  summary: TimesheetSummary;
  rows: TimesheetRow[];
  hourly?: HourlyRow[];
}

export default function EmployeeTimesheet() {
  const [viewMode, setViewMode] = useState('DAY');
  const [date, setDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [employeeCode, setEmployeeCode] = useState('');
  
  const [employees, setEmployees] = useState<any[]>([]);
  const [data, setData] = useState<TimesheetData | null>(null);
  const [loading, setLoading] = useState(false);
  
  const [currentPage, setCurrentPage] = useState(1);
  const ROWS_PER_PAGE = 10;

  const printRef = useRef<HTMLDivElement>(null);
  const handlePrint = useReactToPrint({
    contentRef: printRef,
    documentTitle: `Timesheet_${data?.employeeName}_${date}`,
  });

  const weekPrintRef = useRef<HTMLDivElement>(null);
  const handleWeekPrint = useReactToPrint({
    contentRef: weekPrintRef,
    documentTitle: `Timesheet_Week_${data?.employeeName}_${date}`,
  });

  const monthPrintRef = useRef<HTMLDivElement>(null);
  const handleMonthPrint = useReactToPrint({
    contentRef: monthPrintRef,
    documentTitle: `Timesheet_Month_${data?.employeeName}_${date}`,
  });

  const yearPrintRef = useRef<HTMLDivElement>(null);
  const handleYearPrint = useReactToPrint({
    contentRef: yearPrintRef,
    documentTitle: `Timesheet_Year_${data?.employeeName}_${date}`,
  });

  useEffect(() => {
    apiClient.get('/api/v1/employees')
      .then(res => {
        if (Array.isArray(res.data)) {
          setEmployees(res.data);
        } else {
          setEmployees([]);
        }
      })
      .catch(console.error);
  }, []);

  useEffect(() => {
    fetchData();
  }, [viewMode, date, employeeCode]);

  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!employeeCode) {
      setData(null);
      setError(null);
      return;
    }
    setData(null);
    setError(null);
    setLoading(true);
    try {
      const res = await apiClient.get('/api/admin/timesheet', {
        params: { employeeCode, period: viewMode, date }
      });
      if (typeof res.data === 'object' && res.data !== null && !Array.isArray(res.data)) {
        setData(res.data);
        setCurrentPage(1); // Reset page on new data
      } else {
        setData(null);
        setError("Invalid data format received.");
      }
    } catch (err: any) {
      console.error(err);
      setData(null);
      setError(err.response?.data?.message || err.message || "An error occurred");
    } finally {
      setLoading(false);
    }
  };

  const formatHHMM = (seconds: number) => {
    if (!seconds) return '-';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
  };

  const formatText = (seconds: number) => {
    if (!seconds) return '0h 00m';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return `${h}h ${m.toString().padStart(2, '0')}m`;
  };

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const fullLabel = payload[0].payload.fullName || label;
      return (
        <div className="bg-white p-2 border border-slate-200 shadow-lg rounded text-xs z-50 relative">
          <p className="font-bold mb-1 text-slate-800">{fullLabel}</p>
          {payload.map((entry: any, index: number) => (
            <div key={index} className="flex items-center gap-1.5 mb-0.5">
              <div className="w-2 h-2 rounded-sm" style={{ backgroundColor: entry.color }}></div>
              <span className="text-slate-600 capitalize">{entry.name}:</span>
              <span className="font-semibold">{entry.value}</span>
            </div>
          ))}
        </div>
      );
    }
    return null;
  };

  // Mock table data structure for the 1 Day view to match the exact screenshot
  // since the backend only provides aggregated hourly seconds
  const buildDayViewRows = (type: 'WORK' | 'BREAK' | 'LUNCH' | 'IDLE') => {
    if (!data || !data.hourly) return null;
    
    // Dynamically calculate columns based on the employee's active hours
    let targetHours = ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00'];
    
    if (data.hourly && data.hourly.length > 0) {
      const hours = data.hourly.map((h: any) => parseInt(h.hour.split(':')[0], 10));
      const minHour = Math.min(...hours);
      const maxHour = Math.max(...hours);
      
      targetHours = [];
      for (let i = minHour; i <= maxHour; i++) {
        targetHours.push(`${i.toString().padStart(2, '0')}:00`);
      }
    }

    const displayHours = targetHours.map(h => {
      let hour = parseInt(h.split(':')[0], 10);
      hour = hour > 12 ? hour - 12 : hour;
      return `${hour.toString().padStart(2, '0')}:00`;
    });
    
    const getHourSecs = (h: string) => {
      const row = data.hourly?.find(r => r.hour === h);
      if (!row) return 0;
      if (type === 'WORK') return row.workSeconds;
      if (type === 'BREAK') return row.breakSeconds;
      if (type === 'LUNCH') return row.lunchSeconds;
      if (type === 'IDLE') return row.idleSeconds;
      return 0;
    };

    const rowData = targetHours.map(h => getHourSecs(h));
    const totalSecs = rowData.reduce((a, b) => a + b, 0);

    let client = 'System', project = 'Idle', task = 'No Activity';
    if (type === 'WORK') { client = 'ABC Technologies'; project = 'Web Application'; task = 'Development'; }
    if (type === 'BREAK') { client = 'System'; project = 'Break'; task = 'Tea Break'; }
    if (type === 'LUNCH') { client = 'Office'; project = 'Lunch'; task = 'Lunch Break'; }

    let bgClass = '', textClass = '', icon = <CheckCircle2 className="w-3.5 h-3.5" />;
    if (type === 'WORK') { bgClass = 'bg-emerald-50'; textClass = 'text-emerald-600'; icon = <CheckCircle2 className="w-3.5 h-3.5" />; }
    if (type === 'BREAK') { bgClass = 'bg-purple-50'; textClass = 'text-purple-600'; icon = <Coffee className="w-3.5 h-3.5" />; }
    if (type === 'LUNCH') { bgClass = 'bg-amber-50'; textClass = 'text-amber-600'; icon = <Coffee className="w-3.5 h-3.5" />; }
    if (type === 'IDLE') { bgClass = 'bg-blue-50'; textClass = 'text-blue-600'; icon = <Clock className="w-3.5 h-3.5" />; }

    const title = type === 'WORK' ? 'Work Time' : type === 'BREAK' ? 'Break Time' : type === 'LUNCH' ? 'Lunch Time' : 'Idle Time';

    return (
      <div className="border border-slate-200 rounded-lg overflow-hidden bg-white mt-4">
        <div className={`${bgClass} px-3 py-2 flex items-center gap-1.5 border-b border-slate-200`}>
          <div className={`p-0.5 rounded-full bg-white ${textClass}`}>{icon}</div>
          <h3 className={`text-[13px] font-bold ${textClass}`}>{title}</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-[11px] whitespace-nowrap">
            <thead className="text-slate-700 font-bold border-b border-slate-200 bg-white">
              <tr>
                <th className="px-3 py-2">Client Name</th>
                <th className="px-3 py-2">Project Name</th>
                <th className="px-3 py-2">Task Name</th>
                {displayHours.map((h, i) => <th key={i} className="px-2 py-2 text-center">{h}</th>)}
                <th className="px-3 py-2 text-center font-bold">Total (HH:MM)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-600 bg-white">
              <tr className="hover:bg-slate-50">
                <td className="px-3 py-2">{client}</td>
                <td className="px-3 py-2">{project}</td>
                <td className="px-3 py-2">{task}</td>
                {rowData.map((secs, i) => <td key={i} className="px-2 py-2 text-center">{formatHHMM(secs)}</td>)}
                <td className="px-3 py-2 text-center font-bold text-slate-800">{formatHHMM(totalSecs)}</td>
              </tr>
              <tr className={`${bgClass} border-t border-slate-200`}>
                <td colSpan={3} className="px-3 py-2 text-right font-bold text-slate-800">Total {title}</td>
                {rowData.map((secs, i) => <td key={i} className="px-2 py-2 text-center font-bold text-slate-800">{formatHHMM(secs)}</td>)}
                <td className="px-3 py-2 text-center font-bold text-slate-800">{formatHHMM(totalSecs)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  const renderSummaryInline = () => {
    if (!data) return null;
    return (
      <div className="flex flex-wrap items-center gap-2 mt-4 lg:mt-0">
        <div className="bg-emerald-50/50 rounded-lg px-3 py-1.5 flex items-center gap-2 border border-emerald-100 min-w-[140px]">
          <div className="w-7 h-7 rounded-full bg-emerald-500 flex items-center justify-center shrink-0">
            <CheckCircle2 className="h-4 w-4 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.workSeconds || 0)}</h3>
            <p className="text-[10px] text-slate-500 uppercase">Work Time</p>
          </div>
        </div>
        <div className="bg-blue-50/50 rounded-lg px-3 py-1.5 flex items-center gap-2 border border-blue-100 min-w-[140px]">
          <div className="w-7 h-7 rounded-full bg-blue-500 flex items-center justify-center shrink-0">
            <Clock className="h-4 w-4 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.idleSeconds || 0)}</h3>
            <p className="text-[10px] text-slate-500 uppercase">Idle Time</p>
          </div>
        </div>
        <div className="bg-amber-50/50 rounded-lg px-3 py-1.5 flex items-center gap-2 border border-amber-100 min-w-[140px]">
          <div className="w-7 h-7 rounded-full bg-amber-400 flex items-center justify-center shrink-0">
            <Coffee className="h-4 w-4 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.lunchSeconds || 0)}</h3>
            <p className="text-[10px] text-slate-500 uppercase">Lunch Time</p>
          </div>
        </div>
        <div className="bg-purple-50/50 rounded-lg px-3 py-1.5 flex items-center gap-2 border border-purple-100 min-w-[140px]">
          <div className="w-7 h-7 rounded-full bg-purple-400 flex items-center justify-center shrink-0">
            <Coffee className="h-4 w-4 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.breakSeconds || 0)}</h3>
            <p className="text-[10px] text-slate-500 uppercase">Break Time</p>
          </div>
        </div>
      </div>
    );
  };

  const renderMultiDaySummary = () => {
    if (!data) return null;
    return (
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mt-4">
        <div className="bg-white rounded-lg p-3 border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex justify-between items-start mb-2">
            <div className="flex items-center gap-2">
               <div className="w-7 h-7 rounded-full bg-emerald-500 flex items-center justify-center shrink-0">
                <CheckCircle2 className="h-3.5 w-3.5 text-white" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.workSeconds || 0)}</h3>
                <p className="text-[10px] text-slate-500 uppercase tracking-wider">Work Time</p>
              </div>
            </div>
            <span className="font-bold text-slate-700 text-xs">{(((data.summary?.workSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100).toFixed(0)}%</span>
          </div>
          <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
            <div className="bg-emerald-500 h-full rounded-full" style={{ width: `${(((data.summary?.workSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100)}%` }}></div>
          </div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex justify-between items-start mb-2">
            <div className="flex items-center gap-2">
               <div className="w-7 h-7 rounded-full bg-blue-500 flex items-center justify-center shrink-0">
                <Clock className="h-3.5 w-3.5 text-white" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.idleSeconds || 0)}</h3>
                <p className="text-[10px] text-slate-500 uppercase tracking-wider">Idle Time</p>
              </div>
            </div>
            <span className="font-bold text-slate-700 text-xs">{(((data.summary?.idleSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100).toFixed(0)}%</span>
          </div>
          <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
            <div className="bg-blue-500 h-full rounded-full" style={{ width: `${(((data.summary?.idleSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100)}%` }}></div>
          </div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex justify-between items-start mb-2">
            <div className="flex items-center gap-2">
               <div className="w-7 h-7 rounded-full bg-amber-400 flex items-center justify-center shrink-0">
                <Coffee className="h-3.5 w-3.5 text-white" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.lunchSeconds || 0)}</h3>
                <p className="text-[10px] text-slate-500 uppercase tracking-wider">Lunch Time</p>
              </div>
            </div>
            <span className="font-bold text-slate-700 text-xs">{(((data.summary?.lunchSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100).toFixed(0)}%</span>
          </div>
          <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
            <div className="bg-amber-400 h-full rounded-full" style={{ width: `${(((data.summary?.lunchSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100)}%` }}></div>
          </div>
        </div>
        <div className="bg-white rounded-lg p-3 border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex justify-between items-start mb-2">
            <div className="flex items-center gap-2">
               <div className="w-7 h-7 rounded-full bg-purple-400 flex items-center justify-center shrink-0">
                <Coffee className="h-3.5 w-3.5 text-white" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-800">{formatText(data.summary?.breakSeconds || 0)}</h3>
                <p className="text-[10px] text-slate-500 uppercase tracking-wider">Break Time</p>
              </div>
            </div>
            <span className="font-bold text-slate-700 text-xs">{(((data.summary?.breakSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100).toFixed(0)}%</span>
          </div>
          <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
            <div className="bg-purple-400 h-full rounded-full" style={{ width: `${(((data.summary?.breakSeconds || 0) / Math.max(1, data.summary?.totalSeconds || 1)) * 100)}%` }}></div>
          </div>
        </div>
      </div>
    );
  }

  const renderMultiDayView = () => {
    if (!data || !data.rows || !Array.isArray(data.rows)) return null;
    
    // Transform rows for chart
    const chartData = data.rows.map(r => ({
      name: viewMode === 'YEAR' ? (r.label || '') : viewMode === 'MONTH' ? (r.label ? r.label.split(' ')[0] : '') : (r.dayName || ''),
      fullName: viewMode === 'YEAR' ? (r.label || '') : `${r.dayName || ''}, ${r.label || ''}`,
      work: formatText(r.workSeconds || 0),
      idle: formatText(r.idleSeconds || 0),
      lunch: formatText(r.lunchSeconds || 0),
      break: formatText(r.breakSeconds || 0),
      workVal: Number(((r.workSeconds || 0) / 3600).toFixed(2)),
      idleVal: Number(((r.idleSeconds || 0) / 3600).toFixed(2)),
      lunchVal: Number(((r.lunchSeconds || 0) / 3600).toFixed(2)),
      breakVal: Number(((r.breakSeconds || 0) / 3600).toFixed(2)),
    }));

    let title = viewMode === 'WEEK' ? 'Weekly Timesheet' : viewMode === 'MONTH' ? 'Monthly Timesheet' : 'Yearly Timesheet';
    if (data.startDateLabel) title += ` (${data.startDateLabel} - ${data.endDateLabel})`;

    const totalPages = Math.ceil(data.rows.length / ROWS_PER_PAGE);
    const paginatedRows = data.rows.slice((currentPage - 1) * ROWS_PER_PAGE, currentPage * ROWS_PER_PAGE);

    return (
      <>
        {renderMultiDaySummary()}
        <div className="mt-4 bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
            <h3 className="font-bold text-[13px] text-slate-800">{title}</h3>
            <div className="flex flex-wrap items-center gap-3 text-[10px] font-medium">
              <div className="flex items-center gap-1"><div className="w-2.5 h-2.5 rounded-[2px] bg-emerald-500"></div> Work Time</div>
              <div className="flex items-center gap-1"><div className="w-2.5 h-2.5 rounded-[2px] bg-blue-500"></div> Idle Time</div>
              <div className="flex items-center gap-1"><div className="w-2.5 h-2.5 rounded-[2px] bg-amber-400"></div> Lunch Time</div>
              <div className="flex items-center gap-1"><div className="w-2.5 h-2.5 rounded-[2px] bg-purple-400"></div> Break Time</div>
            </div>
          </div>

          <div className="p-3 h-[250px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 10, right: 10, left: -25, bottom: 0 }} barSize={35}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{fontSize: 10, fill: '#64748b'}} axisLine={false} tickLine={false} />
                <YAxis tick={{fontSize: 10, fill: '#64748b'}} tickFormatter={(val) => `${val}h`} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} cursor={{fill: '#f8fafc'}} />
                <Bar dataKey="workVal" stackId="a" fill="#10b981" name="Work" />
                <Bar dataKey="idleVal" stackId="a" fill="#3b82f6" name="Idle" />
                <Bar dataKey="lunchVal" stackId="a" fill="#fbbf24" name="Lunch" />
                <Bar dataKey="breakVal" stackId="a" fill="#c084fc" name="Break" radius={[2, 2, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="overflow-x-auto border-t border-slate-100">
            <table className="w-full text-left text-[11px] whitespace-nowrap">
              <thead className="bg-slate-50 text-slate-700 font-bold border-b border-slate-200">
                <tr>
                  <th className="px-4 py-2">{viewMode === 'YEAR' ? 'Month' : 'Date'}</th>
                  <th className="px-4 py-2 text-center">Work Time</th>
                  <th className="px-4 py-2 text-center">Idle Time</th>
                  <th className="px-4 py-2 text-center">Lunch Time</th>
                  <th className="px-4 py-2 text-center">Break Time</th>
                  <th className="px-4 py-2 text-center font-bold">Total (HH:MM)</th>
                  <th className="px-4 py-2 text-center">Efficiency %</th>
                  <th className="px-4 py-2 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-600">
                {paginatedRows.map((day, i) => (
                  <tr key={i} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-2 font-medium text-slate-800">{viewMode === 'YEAR' ? (day.label || '') : `${day.dayName || ''}, ${day.label || ''}`}</td>
                    <td className="px-4 py-2 text-center">{formatText(day.workSeconds || 0)}</td>
                    <td className="px-4 py-2 text-center">{formatText(day.idleSeconds || 0)}</td>
                    <td className="px-4 py-2 text-center">{formatText(day.lunchSeconds || 0)}</td>
                    <td className="px-4 py-2 text-center">{formatText(day.breakSeconds || 0)}</td>
                    <td className="px-4 py-2 text-center font-bold text-slate-800">{formatHHMM(day.totalSeconds || 0)}</td>
                    <td className="px-4 py-2 text-center">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        (day.efficiency || 0) >= 70 ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
                      }`}>
                        {day.efficiency || 0}%
                      </span>
                    </td>
                    <td className="px-4 py-2 text-center">
                      <button className="text-blue-500 hover:bg-blue-50 p-1 rounded transition-colors inline-flex justify-center">
                        <Eye className="h-3.5 w-3.5" />
                      </button>
                    </td>
                  </tr>
                ))}
                <tr className="bg-slate-50 font-bold border-t border-slate-200 text-slate-800">
                  <td className="px-4 py-3">Total</td>
                  <td className="px-4 py-3 text-center">{formatText(data.summary?.workSeconds || 0)}</td>
                  <td className="px-4 py-3 text-center">{formatText(data.summary?.idleSeconds || 0)}</td>
                  <td className="px-4 py-3 text-center">{formatText(data.summary?.lunchSeconds || 0)}</td>
                  <td className="px-4 py-3 text-center">{formatText(data.summary?.breakSeconds || 0)}</td>
                  <td className="px-4 py-3 text-center">{formatHHMM(data.summary?.totalSeconds || 0)}</td>
                  <td className="px-4 py-3 text-center">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-700">{data.summary?.efficiency || 0}%</span>
                  </td>
                  <td className="px-4 py-3 text-center text-blue-500 font-bold">-</td>
                </tr>
              </tbody>
            </table>
          </div>
          {totalPages > 1 && (
            <div className="flex flex-col sm:flex-row items-center justify-between px-4 py-3 bg-white border-t border-slate-100 gap-3">
              <span className="text-[11px] text-slate-500 font-medium">
                Showing {(currentPage - 1) * ROWS_PER_PAGE + 1} to {Math.min(currentPage * ROWS_PER_PAGE, data.rows.length)} of {data.rows.length} entries
              </span>
              <div className="flex gap-1 items-center">
                <button
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                  className="px-2 py-1 rounded border border-slate-200 text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed text-[11px] font-medium hover:bg-slate-50 transition-colors"
                >
                  Prev
                </button>
                <div className="flex items-center gap-1 mx-1">
                  {[...Array(totalPages)].map((_, idx) => {
                    const page = idx + 1;
                    // Only show a few pages around current to avoid long pagination
                    if (page === 1 || page === totalPages || (page >= currentPage - 1 && page <= currentPage + 1)) {
                      return (
                        <button
                          key={idx}
                          onClick={() => setCurrentPage(page)}
                          className={`w-6 h-6 rounded text-[11px] font-bold flex items-center justify-center transition-colors ${
                            currentPage === page ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100'
                          }`}
                        >
                          {page}
                        </button>
                      );
                    } else if (page === currentPage - 2 || page === currentPage + 2) {
                      return <span key={idx} className="text-slate-400 text-xs">...</span>;
                    }
                    return null;
                  })}
                </div>
                <button
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                  className="px-2 py-1 rounded border border-slate-200 text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed text-[11px] font-medium hover:bg-slate-50 transition-colors"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      </>
    );
  };

  return (
    <div className="max-w-[1600px] mx-auto space-y-4 pb-10">
      
      {/* Header Row */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
        <div>
          <div className="-mb-2">
             <Breadcrumb items={[
              { label: 'Tracking & Monitoring', href: '#' },
              { label: 'Employee Timesheet' }
            ]} />
          </div>
          <h2 className="text-xl font-bold text-slate-800 mt-2">Employee Timesheet</h2>
          <p className="text-slate-500 text-[11px] mt-0.5">View employee wise efficient work, idle time, lunch and break details.</p>
        </div>
        
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex bg-white rounded-md border border-slate-200 overflow-hidden shadow-sm h-8">
            {['DAY', 'WEEK', 'MONTH', 'YEAR'].map(mode => (
              <button
                key={mode}
                onClick={() => setViewMode(mode)}
                className={`px-3 text-[11px] font-semibold transition-colors flex items-center h-full ${
                  viewMode === mode 
                    ? 'bg-blue-600 text-white' 
                    : 'text-slate-600 hover:bg-slate-50 border-r border-slate-200 last:border-0'
                }`}
              >
                {mode === 'DAY' ? '1 Day' : mode === 'WEEK' ? '1 Week' : mode === 'MONTH' ? '1 Month' : '1 Year'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Filters Toolbar */}
      <div className="bg-white p-3 rounded-xl shadow-sm border border-slate-100 flex flex-col md:flex-row gap-3 justify-between items-end">
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          <div className="flex flex-col gap-1 w-full md:w-auto">
            <label className="text-[10px] font-bold text-slate-700">Employee</label>
            <div className="relative">
              <select 
                value={employeeCode} 
                onChange={e => setEmployeeCode(e.target.value)}
                className="appearance-none px-3 py-1.5 pr-8 bg-white border border-slate-200 rounded-md text-[11px] font-medium text-slate-700 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 shadow-sm min-w-[180px] w-full transition-all cursor-pointer"
              >
                <option value="" disabled>Select Employee</option>
                {employees.map(e => <option key={e.employeeCode} value={e.employeeCode}>{e.name} ({e.employeeCode})</option>)}
              </select>
              <div className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                <ChevronDown className="w-3.5 h-3.5" />
              </div>
            </div>
          </div>
          <div className="flex flex-col gap-1 w-full md:w-auto">
            <label className="text-[10px] font-bold text-slate-700">Department</label>
            <div className="relative">
              <select className="appearance-none px-3 py-1.5 pr-8 bg-slate-50/50 border border-slate-200 rounded-md text-[11px] font-medium text-slate-700 outline-none shadow-sm min-w-[160px] w-full cursor-not-allowed" disabled>
                <option>{data ? data.departmentName : 'All Departments'}</option>
              </select>
              <div className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                <ChevronDown className="w-3.5 h-3.5" />
              </div>
            </div>
          </div>
          <div className="flex flex-col gap-1 w-full md:w-auto">
            <label className="text-[10px] font-bold text-slate-700">Date</label>
            <div className="flex items-center bg-white border border-slate-200 rounded-md shadow-sm overflow-hidden h-[30px]">
              <button className="px-2 border-r border-slate-200 hover:bg-slate-50 text-slate-600 h-full flex items-center" onClick={() => {
                const d = new Date(date);
                if (viewMode === 'DAY') d.setDate(d.getDate() - 1);
                if (viewMode === 'WEEK') d.setDate(d.getDate() - 7);
                if (viewMode === 'MONTH') d.setMonth(d.getMonth() - 1);
                if (viewMode === 'YEAR') d.setFullYear(d.getFullYear() - 1);
                setDate(d.toISOString().split('T')[0]);
              }}>
                <ChevronLeft className="h-3.5 w-3.5" />
              </button>
              <input 
                type="date"
                value={date}
                onChange={e => setDate(e.target.value)}
                className="px-2 text-center text-[11px] font-medium text-slate-700 outline-none w-[110px]"
              />
              <button className="px-2 border-l border-slate-200 hover:bg-slate-50 text-slate-600 h-full flex items-center" onClick={() => {
                const d = new Date(date);
                if (viewMode === 'DAY') d.setDate(d.getDate() + 1);
                if (viewMode === 'WEEK') d.setDate(d.getDate() + 7);
                if (viewMode === 'MONTH') d.setMonth(d.getMonth() + 1);
                if (viewMode === 'YEAR') d.setFullYear(d.getFullYear() + 1);
                setDate(d.toISOString().split('T')[0]);
              }}>
                <ChevronRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto shrink-0 mt-2 md:mt-0">
          <button className="flex-1 md:flex-none flex items-center justify-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-md text-[11px] font-semibold transition shadow-sm h-[30px]">
            <Monitor className="h-3.5 w-3.5" /> Generate Report
          </button>
          <button className="flex-1 md:flex-none flex items-center justify-center gap-1.5 px-3 py-1.5 bg-white border border-slate-200 rounded-md text-[11px] font-medium text-slate-700 hover:bg-slate-50 transition shadow-sm h-[30px]">
            <Download className="h-3.5 w-3.5" /> Export to Excel
          </button>
          <button 
            className="flex-1 md:flex-none flex items-center justify-center gap-1.5 px-3 py-1.5 bg-white border border-slate-200 rounded-md text-[11px] font-medium text-slate-700 hover:bg-slate-50 transition shadow-sm h-[30px]"
            onClick={() => {
              if (!data) return;
              if (viewMode === 'DAY') {
                handlePrint();
              } else if (viewMode === 'WEEK') {
                handleWeekPrint();
              } else if (viewMode === 'MONTH') {
                handleMonthPrint();
              } else if (viewMode === 'YEAR') {
                handleYearPrint();
              } else {
                alert(`Print layout for ${viewMode} view is coming soon!`);
              }
            }}
          >
            <Printer className="h-3.5 w-3.5" /> Print
          </button>
        </div>
      </div>

      {loading ? (
        <div className="w-full mt-4 space-y-4 animate-pulse">
          {/* Summary Cards Skeleton */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="bg-white rounded-lg p-3 border border-slate-200 shadow-sm h-20">
                <div className="flex justify-between items-start mb-2">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-full bg-slate-200"></div>
                    <div className="space-y-2">
                      <div className="h-3 w-16 bg-slate-200 rounded"></div>
                      <div className="h-2 w-12 bg-slate-200 rounded"></div>
                    </div>
                  </div>
                  <div className="h-3 w-8 bg-slate-200 rounded"></div>
                </div>
                <div className="w-full h-1.5 bg-slate-100 rounded-full mt-3"></div>
              </div>
            ))}
          </div>
          {/* Main Content Skeleton */}
          <div className="bg-white border border-slate-200 rounded-lg h-[300px] w-full"></div>
        </div>
      ) : !data ? (
        <div className="py-20 flex flex-col items-center justify-center text-slate-500 animate-fade-in">
          <Monitor className="h-12 w-12 text-slate-300 mb-3" />
          <h3 className="text-lg font-bold text-slate-700">
            {!employeeCode ? "Select an Employee" : error ? "Error Loading Timesheet" : "No Timesheet Data Available"}
          </h3>
          <p className="text-sm mt-1 text-center">
            {!employeeCode 
              ? "Please select an employee from the dropdown above to view their timesheet." 
              : error ? <span className="text-red-500 font-medium">{error}</span> 
              : "No data found for the selected dates. Please adjust the filters."}
          </p>
        </div>
      ) : (
        <div className="animate-fade-in">
          {viewMode === 'DAY' && (
            <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between mt-4">
              <h2 className="text-[15px] font-bold text-slate-800">
                Timesheet for {data?.employeeName} ({data?.employeeCode}) - {data?.startDateLabel}
              </h2>
              {renderSummaryInline()}
            </div>
          )}

          {viewMode === 'DAY' ? (
            <div className="space-y-1 mt-2">
              {buildDayViewRows('WORK')}
              {buildDayViewRows('BREAK')}
              {buildDayViewRows('LUNCH')}
              {buildDayViewRows('IDLE')}
            </div>
          ) : (
            renderMultiDayView()
          )}
        </div>
      )}

      {/* Hidden Print Templates */}
      <div className="hidden">
        {data && viewMode === 'DAY' && (
          <DayViewPrintTemplate ref={printRef} data={data} date={date} />
        )}
        {data && viewMode === 'WEEK' && (
          <WeekViewPrintTemplate ref={weekPrintRef} data={data} date={date} />
        )}
        {data && viewMode === 'MONTH' && (
          <MonthViewPrintTemplate ref={monthPrintRef} data={data} date={date} />
        )}
        {data && viewMode === 'YEAR' && (
          <YearViewPrintTemplate ref={yearPrintRef} data={data} date={date} />
        )}
      </div>
    </div>
  );
}
