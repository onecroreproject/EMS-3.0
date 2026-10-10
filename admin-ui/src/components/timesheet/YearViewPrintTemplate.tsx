import React, { forwardRef } from 'react';
import { CheckCircle2, Clock, Coffee } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts';

interface PrintProps {
  data: any;
  date: string;
}

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

const YearViewPrintTemplate = forwardRef<HTMLDivElement, PrintProps>(({ data, date }, ref) => {
  if (!data || !data.rows) return null;

  const generatedOn = new Date().toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' });
  const displayYear = date.split('-')[0] || new Date().getFullYear().toString();
  const periodText = data.startDateLabel ? `${data.startDateLabel} - ${data.endDateLabel}` : `Jan ${displayYear} - Dec ${displayYear}`;

  // Chart data
  const chartData = data.rows.map((row: any) => ({
    name: row.label ? row.label.substring(0, 3) : '', // "January" -> "Jan"
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
          <p className="text-xs text-slate-600 mt-0.5">Yearly summary with work, idle, lunch and break details</p>
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
          <div className="grid grid-cols-[80px_10px_1fr]"><span className="text-slate-500">Year</span><span>:</span><span className="font-medium">{displayYear}</span></div>
          <div className="grid grid-cols-[80px_10px_1fr]"><span className="text-slate-500">Generated On</span><span>:</span><span className="font-medium">{generatedOn}</span></div>
          <div className="grid grid-cols-[80px_10px_1fr]"><span className="text-slate-500">Report Type</span><span>:</span><span className="font-medium">Yearly Summary</span></div>
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

      {/* Yearly Timesheet Table */}
      <div className="mb-2 border border-slate-200 rounded-md overflow-hidden flex-1 flex flex-col min-h-[300px]">
        <div className="bg-[#f8fafc] px-3 py-1 font-bold flex items-center gap-2 text-[#1e293b] text-[10px] shrink-0 border-b border-slate-200">
          <Calendar className="w-3.5 h-3.5" /> Yearly Timesheet - {displayYear}
        </div>
        <div className="flex-1 flex flex-col justify-between">
          <table className="w-full text-center text-[9px]">
            <thead>
              <tr className="border-b border-slate-200 bg-white text-slate-700">
                <th className="px-1 py-1.5 font-bold text-left pl-4">Month</th>
                <th className="px-1 py-1.5 font-bold">Work Time</th>
                <th className="px-1 py-1.5 font-bold">Idle Time</th>
                <th className="px-1 py-1.5 font-bold">Lunch Time</th>
                <th className="px-1 py-1.5 font-bold">Break Time</th>
                <th className="px-1 py-1.5 font-bold">Total (HH:MM)</th>
                <th className="px-1 py-1.5 font-bold pr-4">Efficiency %</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {data.rows.map((row: any, i: number) => (
                <tr key={i} className="bg-white text-slate-600">
                  <td className="px-1 py-[3px] font-medium text-slate-800 text-left pl-4">{row.label} {displayYear}</td>
                  <td className="px-1 py-[3px]">{formatText(row.workSeconds)}</td>
                  <td className="px-1 py-[3px]">{formatText(row.idleSeconds)}</td>
                  <td className="px-1 py-[3px]">{formatText(row.lunchSeconds)}</td>
                  <td className="px-1 py-[3px]">{formatText(row.breakSeconds)}</td>
                  <td className="px-1 py-[3px] font-medium">{formatHHMM(row.totalSeconds)}</td>
                  <td className="px-1 py-[3px] pr-4 font-bold">{renderEfficiency(row.efficiency)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-[#e0f8ed] border-t border-emerald-200">
                <td className="px-1 py-1.5 font-bold text-slate-800 text-left pl-4">Total (Year)</td>
                <td className="px-1 py-1.5 font-bold text-[#10b981]">{formatText(data?.summary?.workSeconds)}</td>
                <td className="px-1 py-1.5 font-bold text-[#10b981]">{formatText(data?.summary?.idleSeconds)}</td>
                <td className="px-1 py-1.5 font-bold text-[#10b981]">{formatText(data?.summary?.lunchSeconds)}</td>
                <td className="px-1 py-1.5 font-bold text-[#10b981]">{formatText(data?.summary?.breakSeconds)}</td>
                <td className="px-1 py-1.5 font-bold text-[#10b981]">{formatHHMM(data?.summary?.totalSeconds)}</td>
                <td className="px-1 py-1.5 font-bold bg-[#d1fae5] text-[#10b981] pr-4">{data?.summary?.efficiency || 0}%</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      {/* Bottom Section: Stacked Chart & Summary */}
      <div className="flex flex-col gap-2 shrink-0 mt-auto">
        
        {/* Chart */}
        <div className="border border-slate-200 rounded-md overflow-hidden bg-white flex flex-col h-[115px]">
          <div className="bg-[#f8fafc] px-3 py-1 font-bold flex items-center gap-2 text-[#1e293b] text-[10px] border-b border-slate-200 shrink-0">
            <BarIcon className="w-3.5 h-3.5" /> Monthly Work Hours (HH:MM) - {displayYear}
          </div>
          <div className="flex-1 w-full flex justify-center items-end relative overflow-hidden px-2 pt-2 pb-1">
            <BarChart width={730} height={85} data={chartData} margin={{ top: 10, right: 0, left: -20, bottom: 0 }} barSize={16}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
              <XAxis dataKey="name" tick={{fontSize: 7, fill: '#64748b'}} axisLine={false} tickLine={false} interval={0} />
              <YAxis tick={{fontSize: 7, fill: '#64748b'}} tickFormatter={(val) => `${val}h`} axisLine={false} tickLine={false} />
              <Bar dataKey="workVal" fill="#10b981" radius={[2, 2, 0, 0]} isAnimationActive={false} label={(props: any) => {
                const { x, y, width, payload } = props;
                const textVal = payload?.formattedWork || props?.formattedWork || '';
                if (textVal === '0h 00m') return null;
                return <text x={x + width / 2} y={y - 5} fill="#1e293b" fontSize={7} textAnchor="middle">{textVal.replace('0h 00m', '')}</text>;
              }} />
            </BarChart>
          </div>
        </div>

        {/* Yearly Summary (Total) - Full width table-like row */}
        <div className="border border-slate-200 rounded-md overflow-hidden bg-white">
          <div className="bg-[#f8fafc] px-3 py-1 font-bold flex items-center gap-2 text-[#1e293b] text-[10px] border-b border-slate-200">
            <Calendar className="w-3.5 h-3.5" /> Yearly Summary (Total)
          </div>
          <div className="bg-white">
            <table className="w-full text-center text-[9px]">
              <thead className="border-b border-slate-100">
                <tr>
                  <th className="py-2 font-bold text-slate-700">Work Time</th>
                  <th className="py-2 font-bold text-slate-700">Idle Time</th>
                  <th className="py-2 font-bold text-slate-700">Lunch Time</th>
                  <th className="py-2 font-bold text-slate-700">Break Time</th>
                  <th className="py-2 font-bold text-slate-700">Total (HH:MM)</th>
                  <th className="py-2 font-bold text-slate-700">Efficiency %</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="py-2 font-bold text-slate-800">{formatText(data?.summary?.workSeconds)}</td>
                  <td className="py-2 font-bold text-slate-800">{formatText(data?.summary?.idleSeconds)}</td>
                  <td className="py-2 font-bold text-slate-800">{formatText(data?.summary?.lunchSeconds)}</td>
                  <td className="py-2 font-bold text-slate-800">{formatText(data?.summary?.breakSeconds)}</td>
                  <td className="py-2 font-bold text-slate-800">{formatHHMM(data?.summary?.totalSeconds)}</td>
                  <td className="py-2 font-bold bg-[#d1fae5] text-[#10b981] text-[11px]">{data?.summary?.efficiency || 0}%</td>
                </tr>
              </tbody>
            </table>
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

YearViewPrintTemplate.displayName = 'YearViewPrintTemplate';

// Quick calendar icon
const Calendar = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
);

const BarIcon = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 3v18h18"/><path d="M18 17V9"/><path d="M13 17V5"/><path d="M8 17v-3"/></svg>
);

export default YearViewPrintTemplate;
