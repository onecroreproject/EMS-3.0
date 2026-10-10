import React, { forwardRef } from 'react';
import { CheckCircle2, Clock, Coffee } from 'lucide-react';

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

const DayViewPrintTemplate = forwardRef<HTMLDivElement, PrintProps>(({ data, date }, ref) => {
  if (!data || !data.hourly) return null;

  // Calculate dynamic hours
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

  const getHourSecs = (type: string, h: string) => {
    const row = data.hourly?.find((r: any) => r.hour === h);
    if (!row) return 0;
    if (type === 'WORK') return row.workSeconds;
    if (type === 'BREAK') return row.breakSeconds;
    if (type === 'LUNCH') return row.lunchSeconds;
    if (type === 'IDLE') return row.idleSeconds;
    return 0;
  };

  const renderTable = (type: 'WORK' | 'BREAK' | 'LUNCH' | 'IDLE') => {
    const rowData = targetHours.map(h => getHourSecs(type, h));
    const totalSecs = rowData.reduce((a, b) => a + b, 0);

    let title = 'Work Time', color = 'emerald';
    if (type === 'BREAK') { title = 'Break Time'; color = 'purple'; }
    if (type === 'LUNCH') { title = 'Lunch Time'; color = 'amber'; }
    if (type === 'IDLE') { title = 'Idle Time'; color = 'blue'; }

    const bgMap: Record<string, string> = {
      emerald: 'bg-[#e0f8ed]',
      purple: 'bg-[#f4e8fd]',
      amber: 'bg-[#fef3c7]',
      blue: 'bg-[#e0f2fe]'
    };
    const textMap: Record<string, string> = {
      emerald: 'text-[#10b981]',
      purple: 'text-[#a855f7]',
      amber: 'text-[#f59e0b]',
      blue: 'text-[#3b82f6]'
    };

    return (
      <div className="mb-2 rounded-md overflow-hidden border border-slate-200">
        <div className={`${bgMap[color]} ${textMap[color]} px-3 py-1 font-bold flex items-center gap-2 text-xs`}>
          {type === 'WORK' && <CheckCircle2 className="w-4 h-4" />}
          {type === 'IDLE' && <Clock className="w-4 h-4" />}
          {(type === 'LUNCH' || type === 'BREAK') && <Coffee className="w-4 h-4" />}
          {title}
        </div>
        <table className="w-full text-center text-xs">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50">
              <th className="px-1.5 py-1.5 text-left w-24">Time</th>
              {displayHours.map(h => <th key={h} className="px-1.5 py-1.5 font-semibold">{h}</th>)}
              <th className="px-1.5 py-1.5 font-bold">Total (HH:MM)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td className="px-1.5 py-1.5 text-left">{title}</td>
              {rowData.map((secs, i) => <td key={i} className="px-1.5 py-1.5">{formatHHMM(secs)}</td>)}
              <td className="px-1.5 py-1.5 font-bold">{formatHHMM(totalSecs)}</td>
            </tr>
            <tr className={`${bgMap[color]}`}>
              <td className="px-1.5 py-1.5 text-left font-bold">Total {title}</td>
              {rowData.map((secs, i) => <td key={i} className="px-1.5 py-1.5 font-bold">{formatHHMM(secs)}</td>)}
              <td className="px-1.5 py-1.5 font-bold">{formatHHMM(totalSecs)}</td>
            </tr>
          </tbody>
        </table>
      </div>
    );
  };

  const generatedOn = new Date().toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' });
  const dayName = new Date(date).toLocaleString('en-US', { weekday: 'long' });

  return (
    <div ref={ref} className="bg-white px-8 py-6 w-[210mm] min-h-[296mm] max-h-[296mm] mx-auto text-slate-800 flex flex-col box-border overflow-hidden" style={{ fontFamily: 'Arial, sans-serif' }}>
      <style type="text/css" media="print">
        {`@page { size: A4 portrait; margin: 10mm; }`}
      </style>
      {/* Header */}
      <div className="flex justify-between items-start border-b border-slate-200 pb-2 mb-3 shrink-0">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1a237e] m-0">Employee Timesheet</h1>
          <p className="text-sm text-slate-600 mt-1">Daily activity summary with work, idle, lunch and break details</p>
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
          <div className="grid grid-cols-[100px_10px_1fr]"><span className="text-slate-500">Date</span><span>:</span><span className="font-medium">{data.startDateLabel}</span></div>
          <div className="grid grid-cols-[100px_10px_1fr]"><span className="text-slate-500">Day</span><span>:</span><span className="font-medium">{dayName}</span></div>
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
            <div className="text-xl font-extrabold">{formatText(data.summary.workSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#3b82f6] rounded-lg p-3 bg-white flex items-center gap-3 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1.5 bg-[#3b82f6] w-1/4 rounded-r"></div>
          <div className="w-10 h-10 rounded bg-[#e0f2fe] flex items-center justify-center text-[#3b82f6]">
            <Clock size={24} />
          </div>
          <div>
            <div className="text-[#3b82f6] text-[11px] font-bold uppercase">Idle Time</div>
            <div className="text-xl font-extrabold">{formatText(data.summary.idleSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#f59e0b] rounded-lg p-3 bg-white flex items-center gap-3 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1.5 bg-[#f59e0b] w-1/2 rounded-r"></div>
          <div className="w-10 h-10 rounded bg-[#fef3c7] flex items-center justify-center text-[#f59e0b]">
            <Coffee size={24} />
          </div>
          <div>
            <div className="text-[#f59e0b] text-[11px] font-bold uppercase">Lunch Time</div>
            <div className="text-xl font-extrabold">{formatText(data.summary.lunchSeconds)}</div>
          </div>
        </div>
        <div className="border border-[#a855f7] rounded-lg p-3 bg-white flex items-center gap-3 shadow-sm relative overflow-hidden">
          <div className="absolute bottom-0 left-0 h-1.5 bg-[#a855f7] w-1/3 rounded-r"></div>
          <div className="w-10 h-10 rounded bg-[#f4e8fd] flex items-center justify-center text-[#a855f7]">
            <Coffee size={24} />
          </div>
          <div>
            <div className="text-[#a855f7] text-[11px] font-bold uppercase">Break Time</div>
            <div className="text-xl font-extrabold">{formatText(data.summary.breakSeconds)}</div>
          </div>
        </div>
      </div>

      {/* Detail Tables */}
      {renderTable('WORK')}
      {renderTable('BREAK')}
      {renderTable('LUNCH')}
      {renderTable('IDLE')}

      {/* Daily Summary */}
      <div className="mt-2 border border-slate-200 rounded-md overflow-hidden shrink-0">
        <div className="bg-slate-100 px-3 py-1 font-bold flex items-center gap-2 text-slate-800 text-xs">
          <Calendar className="w-4 h-4" /> Daily Summary
        </div>
        <table className="w-full text-center text-xs">
          <thead>
            <tr className="border-b border-slate-200 bg-white text-slate-600">
              <th className="px-1.5 py-2">Date</th>
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
              <td className="px-1.5 py-2 font-semibold">{data.startDateLabel} ({dayName.substring(0,3)})</td>
              <td className="px-1.5 py-2">{formatText(data.summary.workSeconds)}</td>
              <td className="px-1.5 py-2">{formatText(data.summary.idleSeconds)}</td>
              <td className="px-1.5 py-2">{formatText(data.summary.lunchSeconds)}</td>
              <td className="px-1.5 py-2">{formatText(data.summary.breakSeconds)}</td>
              <td className="px-1.5 py-2 font-bold">{formatHHMM(data.summary.workSeconds + data.summary.idleSeconds + data.summary.lunchSeconds + data.summary.breakSeconds)}</td>
              <td className="px-1.5 py-2">
                <span className="px-2 py-0.5 rounded bg-[#e0f8ed] text-[#10b981] font-bold">{data.summary.efficiency}%</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* Footer */}
      <div className="mt-auto flex justify-between items-center text-[10px] text-slate-500 border-t border-slate-200 pt-2 shrink-0">
        <div>{generatedOn}</div>
        <div className="text-center">Employment Management System • Employee Timesheet</div>
        <div>Page 1 of 1</div>
      </div>
    </div>
  );
});

DayViewPrintTemplate.displayName = 'DayViewPrintTemplate';

// Quick calendar icon since it wasn't imported at top
const Calendar = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
);

export default DayViewPrintTemplate;
