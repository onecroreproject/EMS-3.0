import React, { forwardRef } from 'react';
import { CheckCircle2, Clock, Coffee } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, ResponsiveContainer } from 'recharts';

interface PrintProps {
  data: any;
  date: string;
}

const formatHHMM = (seconds: number) => {
  if (!seconds) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m.toString().padStart(2, '0')}m`;
};

const formatText = (seconds: number) => {
  if (!seconds) return '0h 00m';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m.toString().padStart(2, '0')}m`;
};

const MonthViewPrintTemplate = forwardRef<HTMLDivElement, PrintProps>(({ data, date }, ref) => {
  if (!data || !data.rows) return null;

  const generatedOn = new Date().toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' });
  const monthPeriod = `${data?.startDateLabel || ''} - ${data?.endDateLabel || ''}`;
  const displayMonth = data.rows.length > 0 && data.rows[0].label ? new Date(data.rows[0].label).toLocaleDateString('en-US', { month: 'long', year: 'numeric' }) : 'This Month';

  // Chart data
  const chartData = data.rows.map((row: any) => ({
    name: row.label ? row.label.split(' ')[0] : '', // e.g. "01 Oct" -> "01"
    workVal: row.workSeconds / 3600,
    formattedWork: formatText(row.workSeconds)
  }));

  const renderEfficiency = (eff: number) => {
    if (eff >= 70) return <span className="text-[#10b981]">{eff}%</span>;
    return <span className="text-[#f59e0b]">{eff}%</span>;
  };

  return (
    <div ref={ref} className="bg-white px-5 py-3 w-[210mm] min-h-[297mm] max-h-[297mm] mx-auto text-slate-800 flex flex-col box-border overflow-hidden" style={{ fontFamily: 'Arial, sans-serif' }}>
      <style type="text/css" media="print">
        {`@page { size: A4 portrait; margin: 5mm; }`}
      </style>
      
      {/* Header */}
      <div className="flex justify-between items-start border-b border-slate-200 pb-1 mb-2 shrink-0">
        <div>
          <h1 className="text-2xl font-extrabold text-[#1a237e] m-0 leading-tight">Employee Timesheet</h1>
          <p className="text-xs text-slate-600 mt-0.5">Monthly summary with work, idle, lunch and break details</p>
        </div>
        <div className="text-right">
          <p className="text-[10px] text-slate-500">Employment Management System (EMS)</p>
          <p className="text-xs font-semibold text-slate-700">Employee Timesheet Report</p>
        </div>
      </div>

      {/* Metadata */}
      <div className="bg-[#f0f4f8] rounded-md p-2 flex justify-between text-[11px] mb-2 border border-slate-200 shrink-0">
        <div className="space-y-0.5">
          <div className="grid grid-cols-[100px_10px_1fr]"><span className="text-slate-500">Employee Name</span><span>:</span><span className="font-medium">{data.employeeName}</span></div>
          <div className="grid grid-cols-[100px_10px_1fr]"><span className="text-slate-500">Employee Code</span><span>:</span><span className="font-medium">{data.employeeCode}</span></div>
          <div className="grid grid-cols-[100px_10px_1fr]"><span className="text-slate-500">Department</span><span>:</span><span className="font-medium">{data.departmentName}</span></div>
        </div>
        <div className="space-y-0.5 border-l border-slate-300 pl-6">
          <div className="grid grid-cols-[80px_10px_1fr]"><span className="text-slate-500">Month</span><span>:</span><span className="font-medium">{displayMonth}</span></div>
          <div className="grid grid-cols-[80px_10px_1fr]"><span className="text-slate-500">Period</span><span>:</span><span className="font-medium">{monthPeriod}</span></div>
          <div className="grid grid-cols-[80px_10px_1fr]"><span className="text-slate-500">Generated On</span><span>:</span><span className="font-medium">{generatedOn}</span></div>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-4 gap-2 mb-2 shrink-0">
        <div className="border border-[#10b981] rounded p-1.5 bg-white flex items-center gap-2 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1 bg-[#10b981] w-3/4 rounded-r"></div>
          <div className="w-6 h-6 rounded bg-[#e0f8ed] flex items-center justify-center text-[#10b981]">
            <CheckCircle2 size={14} />
          </div>
          <div>
            <div className="text-[#10b981] text-[9px] font-bold uppercase leading-none">Work Time</div>
            <div className="text-sm font-extrabold leading-tight">{formatText(data?.summary?.workSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#3b82f6] rounded p-1.5 bg-white flex items-center gap-2 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1 bg-[#3b82f6] w-1/4 rounded-r"></div>
          <div className="w-6 h-6 rounded bg-[#e0f2fe] flex items-center justify-center text-[#3b82f6]">
            <Clock size={14} />
          </div>
          <div>
            <div className="text-[#3b82f6] text-[9px] font-bold uppercase leading-none">Idle Time</div>
            <div className="text-sm font-extrabold leading-tight">{formatText(data?.summary?.idleSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#f59e0b] rounded p-1.5 bg-white flex items-center gap-2 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1 bg-[#f59e0b] w-1/2 rounded-r"></div>
          <div className="w-6 h-6 rounded bg-[#fef3c7] flex items-center justify-center text-[#f59e0b]">
            <Coffee size={14} />
          </div>
          <div>
            <div className="text-[#f59e0b] text-[9px] font-bold uppercase leading-none">Lunch Time</div>
            <div className="text-sm font-extrabold leading-tight">{formatText(data?.summary?.lunchSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#a855f7] rounded p-1.5 bg-white flex items-center gap-2 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1 bg-[#a855f7] w-1/3 rounded-r"></div>
          <div className="w-6 h-6 rounded bg-[#f4e8fd] flex items-center justify-center text-[#a855f7]">
            <Coffee size={14} />
          </div>
          <div>
            <div className="text-[#a855f7] text-[9px] font-bold uppercase leading-none">Break Time</div>
            <div className="text-sm font-extrabold leading-tight">{formatText(data?.summary?.breakSeconds)}</div>
          </div>
        </div>
      </div>

      {/* Monthly Timesheet Table */}
      <div className="mb-2 border border-slate-200 rounded-md overflow-hidden flex-1 flex flex-col">
        <div className="bg-[#f8fafc] px-3 py-1 font-bold flex items-center gap-2 text-[#1e293b] text-[10px] shrink-0 border-b border-slate-200">
          <Calendar className="w-3.5 h-3.5" /> Monthly Timesheet - {displayMonth}
        </div>
        <div className="flex-1 overflow-hidden flex flex-col justify-between">
          <table className="w-full text-center text-[9px]">
            <thead>
              <tr className="border-b border-slate-200 bg-white text-slate-700">
                <th className="px-1 py-0.5 font-bold text-left pl-3">Date</th>
                <th className="px-1 py-0.5 font-bold text-left">Day</th>
                <th className="px-1 py-0.5 font-bold">Work Time</th>
                <th className="px-1 py-0.5 font-bold">Idle Time</th>
                <th className="px-1 py-0.5 font-bold">Lunch Time</th>
                <th className="px-1 py-0.5 font-bold">Break Time</th>
                <th className="px-1 py-0.5 font-bold">Total (HH:MM)</th>
                <th className="px-1 py-0.5 font-bold pr-3">Efficiency %</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {data.rows.map((row: any, i: number) => (
                <tr key={i} className="bg-white text-slate-600">
                  <td className="px-1 py-[1.5px] font-medium text-slate-800 text-left pl-3">{row.label}</td>
                  <td className="px-1 py-[1.5px] text-left">{row.dayName ? row.dayName.substring(0,3) : ''}</td>
                  <td className="px-1 py-[1.5px]">{formatText(row.workSeconds)}</td>
                  <td className="px-1 py-[1.5px]">{formatText(row.idleSeconds)}</td>
                  <td className="px-1 py-[1.5px]">{formatText(row.lunchSeconds)}</td>
                  <td className="px-1 py-[1.5px]">{formatText(row.breakSeconds)}</td>
                  <td className="px-1 py-[1.5px] font-medium">{formatHHMM(row.totalSeconds)}</td>
                  <td className="px-1 py-[1.5px] pr-3 font-bold">{renderEfficiency(row.efficiency)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-[#e0f8ed] border-t border-emerald-200">
                <td className="px-1 py-1 font-bold text-slate-800 text-left pl-3" colSpan={2}>Total (Month)</td>
                <td className="px-1 py-1 font-bold text-[#10b981]">{formatText(data?.summary?.workSeconds)}</td>
                <td className="px-1 py-1 font-bold text-[#10b981]">{formatText(data?.summary?.idleSeconds)}</td>
                <td className="px-1 py-1 font-bold text-[#10b981]">{formatText(data?.summary?.lunchSeconds)}</td>
                <td className="px-1 py-1 font-bold text-[#10b981]">{formatText(data?.summary?.breakSeconds)}</td>
                <td className="px-1 py-1 font-bold text-[#10b981]">{formatHHMM(data?.summary?.totalSeconds)}</td>
                <td className="px-1 py-1 font-bold bg-[#d1fae5] text-[#10b981] pr-3">{data?.summary?.efficiency || 0}%</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      {/* Bottom Section: Chart + Summary */}
      <div className="grid grid-cols-[2fr_1fr] gap-2 shrink-0 mt-auto">
        
        {/* Chart */}
        <div className="border border-slate-200 rounded-md overflow-hidden bg-white flex flex-col">
          <div className="bg-[#f8fafc] px-3 py-1 font-bold flex items-center gap-2 text-[#1e293b] text-[10px] border-b border-slate-200">
            <Calendar className="w-3.5 h-3.5" /> Daily Work Hours (HH:MM)
          </div>
          <div className="flex-1 w-full flex justify-center items-end relative min-h-[105px]">
            <div className="absolute inset-x-0 bottom-0 flex justify-center overflow-hidden">
              <BarChart width={520} height={105} data={chartData} margin={{ top: 15, right: 0, left: -15, bottom: 0 }} barSize={10}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{fontSize: 6, fill: '#64748b'}} axisLine={false} tickLine={false} interval={0} />
                <YAxis tick={{fontSize: 7, fill: '#64748b'}} tickFormatter={(val) => `${val}h`} axisLine={false} tickLine={false} />
                <Bar dataKey="workVal" fill="#10b981" radius={[1, 1, 0, 0]} isAnimationActive={false} label={(props: any) => {
                  const { x, y, width, payload } = props;
                  const textVal = payload?.formattedWork || props?.formattedWork || '';
                  if (textVal === '0h 00m') return null;
                  return <text x={x + width / 2} y={y - 3} fill="#1e293b" fontSize={5} textAnchor="middle">{textVal.replace('0h 00m', '')}</text>;
                }} />
              </BarChart>
            </div>
          </div>
        </div>

        {/* Month Summary Total */}
        <div className="border border-slate-200 rounded-md overflow-hidden bg-white flex flex-col">
          <div className="bg-[#f8fafc] px-3 py-1 font-bold flex items-center gap-2 text-[#1e293b] text-[10px] border-b border-slate-200">
            <BarIcon className="w-3.5 h-3.5" /> Monthly Summary (Total)
          </div>
          <div className="flex-1 flex flex-col p-1.5 space-y-0.5 text-[9px]">
            <div className="flex justify-between py-0.5 border-b border-slate-100">
              <span className="font-bold text-[#1e293b]">Work Time</span>
              <span className="bg-[#e0f8ed] text-[#10b981] px-1.5 py-0.5 rounded font-bold">{formatText(data?.summary?.workSeconds)}</span>
            </div>
            <div className="flex justify-between py-0.5 border-b border-slate-100">
              <span className="font-bold text-[#1e293b]">Idle Time</span>
              <span className="text-slate-700 font-medium">{formatText(data?.summary?.idleSeconds)}</span>
            </div>
            <div className="flex justify-between py-0.5 border-b border-slate-100">
              <span className="font-bold text-[#f59e0b]">Lunch Time</span>
              <span className="bg-[#fef3c7] text-[#f59e0b] px-1.5 py-0.5 rounded font-bold">{formatText(data?.summary?.lunchSeconds)}</span>
            </div>
            <div className="flex justify-between py-0.5 border-b border-slate-100">
              <span className="font-bold text-[#a855f7]">Break Time</span>
              <span className="bg-[#f4e8fd] text-[#a855f7] px-1.5 py-0.5 rounded font-bold">{formatText(data?.summary?.breakSeconds)}</span>
            </div>
            <div className="flex justify-between py-0.5 border-b border-emerald-100 bg-[#d1fae5] -mx-1.5 px-1.5 mt-0.5">
              <span className="font-bold text-[#1e293b]">Total (HH:MM)</span>
              <span className="text-[#10b981] font-bold">{formatHHMM(data?.summary?.totalSeconds)}</span>
            </div>
            <div className="flex justify-between py-0.5 mt-0.5">
              <span className="font-bold text-[#1e293b]">Efficiency %</span>
              <span className="text-[#10b981] font-bold border border-[#10b981] px-2 py-0.5 rounded">{data?.summary?.efficiency || 0}%</span>
            </div>
          </div>
        </div>

      </div>

      {/* Footer */}
      <div className="mt-2 flex justify-between items-center text-[8px] text-slate-500 border-t border-slate-200 pt-1.5 shrink-0">
        <div>{generatedOn}</div>
        <div className="text-center font-medium">Employment Management System • Employee Timesheet</div>
        <div>Page 1 of 1</div>
      </div>
    </div>
  );
});

MonthViewPrintTemplate.displayName = 'MonthViewPrintTemplate';

// Quick calendar icon
const Calendar = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
);

const BarIcon = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 3v18h18"/><path d="M18 17V9"/><path d="M13 17V5"/><path d="M8 17v-3"/></svg>
);

export default MonthViewPrintTemplate;
