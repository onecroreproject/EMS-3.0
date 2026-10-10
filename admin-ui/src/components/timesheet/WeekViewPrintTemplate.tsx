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
  return `${h}h ${m.toString().padStart(2, '0')}m`;
};

const formatText = (seconds: number) => {
  if (!seconds) return '0h 00m';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m.toString().padStart(2, '0')}m`;
};

const WeekViewPrintTemplate = forwardRef<HTMLDivElement, PrintProps>(({ data, date }, ref) => {
  if (!data || !data.rows) return null;

  const generatedOn = new Date().toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' });
  const weekPeriod = `${data?.startDateLabel || ''} - ${data?.endDateLabel || ''}`;

  // Chart data
  const chartData = data.rows.map((row: any) => ({
    name: row.label, // e.g. "05 Oct"
    workVal: row.workSeconds / 3600,
    formattedWork: formatText(row.workSeconds)
  }));

  const renderEfficiency = (eff: number) => {
    if (eff >= 70) return <span className="text-[#10b981]">{eff}%</span>;
    return <span className="text-[#f59e0b]">{eff}%</span>;
  };

  return (
    <div ref={ref} className="bg-white px-8 py-6 w-[210mm] min-h-[296mm] max-h-[296mm] mx-auto text-slate-800 flex flex-col box-border overflow-hidden" style={{ fontFamily: 'Arial, sans-serif' }}>
      <style type="text/css" media="print">
        {`@page { size: A4 portrait; margin: 10mm; }`}
      </style>
      
      {/* Header */}
      <div className="flex justify-between items-start border-b border-slate-200 pb-2 mb-3 shrink-0">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1a237e] m-0">Employee Timesheet</h1>
          <p className="text-sm text-slate-600 mt-1">Weekly summary with work, idle, lunch and break details</p>
        </div>
        <div className="text-right">
          <p className="text-xs text-slate-500">Employment Management System (EMS)</p>
          <p className="text-sm font-semibold text-slate-700">Employee Timesheet Report</p>
        </div>
      </div>

      {/* Metadata */}
      <div className="bg-[#f0f4f8] rounded-md p-3 flex justify-between text-[13px] mb-4 border border-slate-200 shrink-0">
        <div className="space-y-1.5">
          <div className="grid grid-cols-[120px_10px_1fr]"><span className="text-slate-500">Employee Name</span><span>:</span><span className="font-medium">{data.employeeName}</span></div>
          <div className="grid grid-cols-[120px_10px_1fr]"><span className="text-slate-500">Employee Code</span><span>:</span><span className="font-medium">{data.employeeCode}</span></div>
          <div className="grid grid-cols-[120px_10px_1fr]"><span className="text-slate-500">Department</span><span>:</span><span className="font-medium">{data.departmentName}</span></div>
        </div>
        <div className="space-y-1.5 border-l border-slate-300 pl-8">
          <div className="grid grid-cols-[100px_10px_1fr]"><span className="text-slate-500">Week Period</span><span>:</span><span className="font-medium">{weekPeriod}</span></div>
          <div className="grid grid-cols-[100px_10px_1fr]"><span className="text-slate-500">Generated On</span><span>:</span><span className="font-medium">{generatedOn}</span></div>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-4 gap-3 mb-4 shrink-0">
        <div className="border border-[#10b981] rounded-lg p-3 bg-white flex items-center gap-3 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1.5 bg-[#10b981] w-3/4 rounded-r"></div>
          <div className="w-10 h-10 rounded bg-[#e0f8ed] flex items-center justify-center text-[#10b981]">
            <CheckCircle2 size={24} />
          </div>
          <div>
            <div className="text-[#10b981] text-[11px] font-bold uppercase">Work Time</div>
            <div className="text-xl font-extrabold">{formatText(data?.summary?.workSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#3b82f6] rounded-lg p-3 bg-white flex items-center gap-3 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1.5 bg-[#3b82f6] w-1/4 rounded-r"></div>
          <div className="w-10 h-10 rounded bg-[#e0f2fe] flex items-center justify-center text-[#3b82f6]">
            <Clock size={24} />
          </div>
          <div>
            <div className="text-[#3b82f6] text-[11px] font-bold uppercase">Idle Time</div>
            <div className="text-xl font-extrabold">{formatText(data?.summary?.idleSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#f59e0b] rounded-lg p-3 bg-white flex items-center gap-3 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1.5 bg-[#f59e0b] w-1/2 rounded-r"></div>
          <div className="w-10 h-10 rounded bg-[#fef3c7] flex items-center justify-center text-[#f59e0b]">
            <Coffee size={24} />
          </div>
          <div>
            <div className="text-[#f59e0b] text-[11px] font-bold uppercase">Lunch Time</div>
            <div className="text-xl font-extrabold">{formatText(data?.summary?.lunchSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#a855f7] rounded-lg p-3 bg-white flex items-center gap-3 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1.5 bg-[#a855f7] w-1/3 rounded-r"></div>
          <div className="w-10 h-10 rounded bg-[#f4e8fd] flex items-center justify-center text-[#a855f7]">
            <Coffee size={24} />
          </div>
          <div>
            <div className="text-[#a855f7] text-[11px] font-bold uppercase">Break Time</div>
            <div className="text-xl font-extrabold">{formatText(data?.summary?.breakSeconds)}</div>
          </div>
        </div>
      </div>

      {/* Weekly Timesheet Table */}
      <div className="mb-4 border border-slate-200 rounded-md overflow-hidden shrink-0">
        <div className="bg-slate-100 px-3 py-2 font-bold flex items-center gap-2 text-slate-800 text-sm">
          <Calendar className="w-4 h-4" /> Weekly Timesheet
        </div>
        <table className="w-full text-center text-[10px]">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-slate-700">
              <th className="px-1.5 py-1.5 font-bold text-left pl-3">Date</th>
              <th className="px-1.5 py-1.5 font-bold text-left">Day</th>
              <th className="px-1.5 py-1.5 font-bold">Work Time</th>
              <th className="px-1.5 py-1.5 font-bold">Idle Time</th>
              <th className="px-1.5 py-1.5 font-bold">Lunch Time</th>
              <th className="px-1.5 py-1.5 font-bold">Break Time</th>
              <th className="px-1.5 py-1.5 font-bold">Total (HH:MM)</th>
              <th className="px-1.5 py-1.5 font-bold pr-3">Efficiency %</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {data.rows.map((row: any, i: number) => (
              <tr key={i} className="bg-white text-slate-600">
                <td className="px-1.5 py-1.5 font-semibold text-slate-800 text-left pl-3">{row.label}</td>
                <td className="px-1.5 py-1.5 text-left">{row.dayName ? row.dayName.substring(0,3) : ''}</td>
                <td className="px-1.5 py-1.5">{formatText(row.workSeconds)}</td>
                <td className="px-1.5 py-1.5">{formatText(row.idleSeconds)}</td>
                <td className="px-1.5 py-1.5">{formatText(row.lunchSeconds)}</td>
                <td className="px-1.5 py-1.5">{formatText(row.breakSeconds)}</td>
                <td className="px-1.5 py-1.5 font-semibold">{formatHHMM(row.totalSeconds)}</td>
                <td className="px-1.5 py-1.5 pr-3 font-bold">{renderEfficiency(row.efficiency)}</td>
              </tr>
            ))}
            <tr className="bg-[#e0f8ed]">
              <td className="px-1.5 py-2 font-bold text-slate-800 text-left pl-3" colSpan={2}>Total (Week)</td>
              <td className="px-1.5 py-2 font-bold text-[#10b981]">{formatText(data?.summary?.workSeconds)}</td>
              <td className="px-1.5 py-2 font-bold text-[#10b981]">{formatText(data?.summary?.idleSeconds)}</td>
              <td className="px-1.5 py-2 font-bold text-[#10b981]">{formatText(data?.summary?.lunchSeconds)}</td>
              <td className="px-1.5 py-2 font-bold text-[#10b981]">{formatText(data?.summary?.breakSeconds)}</td>
              <td className="px-1.5 py-2 font-bold text-[#10b981]">{formatHHMM(data?.summary?.totalSeconds)}</td>
              <td className="px-1.5 py-2 font-bold text-[#10b981] pr-3">{data?.summary?.efficiency || 0}%</td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* Chart */}
      <div className="mb-4 border border-slate-200 rounded-md overflow-hidden shrink-0 flex flex-col bg-white">
        <div className="bg-slate-100 px-3 py-1 font-bold flex items-center gap-2 text-slate-800 text-xs">
          <Calendar className="w-4 h-4" /> Daily Work Hours (HH:MM)
        </div>
        <div className="h-[150px] p-2 w-full flex justify-center">
          <BarChart width={700} height={134} data={chartData} margin={{ top: 15, right: 10, left: -25, bottom: 0 }} barSize={35}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
            <XAxis dataKey="name" tick={{fontSize: 9, fill: '#64748b'}} axisLine={false} tickLine={false} />
            <YAxis tick={{fontSize: 9, fill: '#64748b'}} tickFormatter={(val) => `${val}h`} axisLine={false} tickLine={false} />
            <Bar dataKey="workVal" fill="#10b981" radius={[2, 2, 0, 0]} label={(props: any) => {
              const { x, y, width, payload } = props;
              const textVal = payload?.formattedWork || props?.formattedWork || '';
              return <text x={x + width / 2} y={y - 5} fill="#1e293b" fontSize={9} textAnchor="middle">{textVal}</text>;
            }} />
          </BarChart>
        </div>
      </div>

      {/* Week Summary Total */}
      <div className="mt-auto border border-slate-200 rounded-md overflow-hidden shrink-0">
        <div className="bg-slate-100 px-3 py-1 font-bold flex items-center gap-2 text-slate-800 text-xs">
          <Calendar className="w-4 h-4" /> Week Summary (Total)
        </div>
        <table className="w-full text-center text-xs">
          <thead>
            <tr className="border-b border-slate-200 bg-white text-slate-600">
              <th className="px-1.5 py-2">Work Time</th>
              <th className="px-1.5 py-2">Idle Time</th>
              <th className="px-1.5 py-2">Lunch Time</th>
              <th className="px-1.5 py-2">Break Time</th>
              <th className="px-1.5 py-2">Total (HH:MM)</th>
              <th className="px-1.5 py-2">Efficiency %</th>
            </tr>
          </thead>
          <tbody>
            <tr className="bg-white">
              <td className="px-1.5 py-2">{formatText(data?.summary?.workSeconds)}</td>
              <td className="px-1.5 py-2">{formatText(data?.summary?.idleSeconds)}</td>
              <td className="px-1.5 py-2">{formatText(data?.summary?.lunchSeconds)}</td>
              <td className="px-1.5 py-2">{formatText(data?.summary?.breakSeconds)}</td>
              <td className="px-1.5 py-2 font-bold">{formatHHMM(data?.summary?.totalSeconds)}</td>
              <td className="px-1.5 py-2">
                <span className="px-2 py-0.5 rounded bg-[#e0f8ed] text-[#10b981] font-bold">{data?.summary?.efficiency || 0}%</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* Footer */}
      <div className="mt-3 flex justify-between items-center text-[10px] text-slate-500 border-t border-slate-200 pt-2 shrink-0">
        <div>{generatedOn}</div>
        <div className="text-center">Employment Management System • Employee Timesheet</div>
        <div>Page 1 of 1</div>
      </div>
    </div>
  );
});

WeekViewPrintTemplate.displayName = 'WeekViewPrintTemplate';

// Quick calendar icon
const Calendar = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
);

export default WeekViewPrintTemplate;
