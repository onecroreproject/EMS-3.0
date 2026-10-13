import React, { useState, useEffect, useRef } from 'react';
import { 
  Users, CheckCircle2, XCircle, Clock, Calendar, Download, 
  Search, Filter, MoreHorizontal, ChevronRight, ChevronLeft,
  MapPin, Settings, Layout, Globe, Activity, Eye, FileText, Edit, RefreshCw
} from 'lucide-react';
import Breadcrumb from '../components/ui/Breadcrumb';

interface EmployeeAttendance {
  id: number;
  name: string;
  code: string;
  department: string;
  status: 'Present' | 'Absent' | 'On Leave';
  checkIn: string;
  checkOut: string;
  workHours: string;
}

const DUMMY_DATA: EmployeeAttendance[] = [
  { id: 1, name: 'Santhosh', code: '26@DLK001', department: 'IT Operations', status: 'Present', checkIn: '09:05 AM', checkOut: '-', workHours: '2h 30m' },
  { id: 2, name: 'Jeeva', code: '26@DLK002', department: 'Development', status: 'Present', checkIn: '08:50 AM', checkOut: '-', workHours: '4h 10m' },
  { id: 3, name: 'Priya', code: '26@DLK003', department: 'QA', status: 'Present', checkIn: '09:10 AM', checkOut: '-', workHours: '2h 45m' },
  { id: 4, name: 'Ram Srinivashr', code: '26@DLK004', department: 'Development', status: 'Absent', checkIn: '-', checkOut: '-', workHours: '0h 0m' },
  { id: 5, name: 'Arun Kumar', code: '26@DLK005', department: 'IT Operations', status: 'Present', checkIn: '08:55 AM', checkOut: '-', workHours: '4h 05m' },
  { id: 6, name: 'Kavya', code: '26@DLK006', department: 'HR', status: 'On Leave', checkIn: '-', checkOut: '-', workHours: '0h 0m' },
  { id: 7, name: 'Manoj', code: '26@DLK007', department: 'Development', status: 'Present', checkIn: '09:00 AM', checkOut: '-', workHours: '3h 20m' },
  { id: 8, name: 'Divya', code: '26@DLK008', department: 'Support', status: 'Present', checkIn: '09:15 AM', checkOut: '-', workHours: '2h 10m' },
  { id: 9, name: 'Vignesh', code: '26@DLK009', department: 'IT Operations', status: 'Absent', checkIn: '-', checkOut: '-', workHours: '0h 0m' },
  { id: 10, name: 'Sathya', code: '26@DLK010', department: 'QA', status: 'Present', checkIn: '08:48 AM', checkOut: '-', workHours: '4h 12m' }
];

export default function Attendance() {
  const [activeTab, setActiveTab] = useState('All Employees');
  const [selectedEmp, setSelectedEmp] = useState<EmployeeAttendance | null>(DUMMY_DATA[0]);
  const [rightPanelTab, setRightPanelTab] = useState('Overview');
  const [activeDropdown, setActiveDropdown] = useState<number | null>(null);
  
  const dropdownRef = useRef<HTMLTableCellElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setActiveDropdown(null);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const toggleDropdown = (id: number, e: React.MouseEvent) => {
    e.stopPropagation(); // Prevent row click
    if (activeDropdown === id) {
      setActiveDropdown(null);
    } else {
      setActiveDropdown(id);
    }
  };

  const handleRowClick = (emp: EmployeeAttendance) => {
    setSelectedEmp(emp);
  };

  const getStatusBadge = (status: string) => {
    if (status === 'Present') return <span className="text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-md text-xs font-bold flex items-center gap-1.5"><div className="w-1.5 h-1.5 rounded-full bg-emerald-500"></div> Present</span>;
    if (status === 'Absent') return <span className="text-red-600 bg-red-50 px-2.5 py-1 rounded-md text-xs font-bold flex items-center gap-1.5"><div className="w-1.5 h-1.5 rounded-full bg-red-500"></div> Absent</span>;
    if (status === 'On Leave') return <span className="text-orange-600 bg-orange-50 px-2.5 py-1 rounded-md text-xs font-bold flex items-center gap-1.5"><div className="w-1.5 h-1.5 rounded-full bg-orange-500"></div> On Leave</span>;
    return <span>{status}</span>;
  };

  const getInitials = (name: string) => {
    return name.charAt(0).toUpperCase();
  };

  const getAvatarColor = (name: string) => {
    const colors = ['bg-blue-500', 'bg-emerald-500', 'bg-violet-500', 'bg-orange-500', 'bg-pink-500', 'bg-cyan-500'];
    const index = name.length % colors.length;
    return colors[index];
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-[1600px] mx-auto min-h-screen">
      
      {/* Top Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Attendance</h1>
          <p className="text-sm text-slate-500 mt-1">Monitor employee attendance, work hours and daily status</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Calendar className="h-4 w-4 text-slate-400" />
            </div>
            <select className="pl-9 pr-8 py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700 bg-white shadow-sm outline-none focus:ring-2 focus:ring-blue-500 appearance-none">
              <option>10 Oct 2026</option>
            </select>
          </div>
          <button className="flex items-center gap-2 px-4 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition shadow-sm">
            <Download className="w-4 h-4" /> Export
          </button>
        </div>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-6">
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-blue-50 p-3 rounded-xl text-blue-500">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Total Employees</p>
            <div className="flex items-end gap-3">
              <h3 className="text-2xl font-bold text-slate-800 leading-none">124</h3>
              <div className="flex items-center text-emerald-500 text-xs font-bold bg-emerald-50 px-1.5 py-0.5 rounded">
                ↑ 12%
              </div>
            </div>
            <p className="text-[10px] text-slate-400 mt-1">from last month</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-emerald-50 p-3 rounded-xl text-emerald-500">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Present Today</p>
            <div className="flex items-end gap-3">
              <h3 className="text-2xl font-bold text-slate-800 leading-none">98</h3>
              <div className="flex items-center text-emerald-500 text-xs font-bold bg-emerald-50 px-1.5 py-0.5 rounded">
                ↑ 8%
              </div>
            </div>
            <p className="text-[10px] text-slate-400 mt-1">78.9% attendance</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-red-50 p-3 rounded-xl text-red-500">
            <XCircle className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Absent Today</p>
            <div className="flex items-end gap-3">
              <h3 className="text-2xl font-bold text-slate-800 leading-none">20</h3>
              <div className="flex items-center text-red-500 text-xs font-bold bg-red-50 px-1.5 py-0.5 rounded">
                ↓ 4%
              </div>
            </div>
            <p className="text-[10px] text-slate-400 mt-1">16.1% absence</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-orange-50 p-3 rounded-xl text-orange-500">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">On Leave</p>
            <div className="flex items-end gap-3">
              <h3 className="text-2xl font-bold text-slate-800 leading-none">6</h3>
              <div className="flex items-center text-slate-500 text-xs font-bold bg-slate-50 px-1.5 py-0.5 rounded">
                0%
              </div>
            </div>
            <p className="text-[10px] text-slate-400 mt-1">4.8% on leave</p>
          </div>
        </div>
      </div>

      <div className="flex flex-col xl:flex-row gap-5 items-start">
        {/* Left Area */}
        <div className="flex-1 min-w-0 bg-white rounded-2xl shadow-sm border border-slate-100">
          
          {/* Tabs */}
          <div className="border-b border-slate-100 px-2 flex gap-2">
            {['All Employees', 'Present', 'Absent', 'On Leave', 'Remote', 'Half Day'].map(tab => (
              <button 
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`py-4 px-4 text-sm font-bold border-b-2 transition-colors ${activeTab === tab ? 'border-orange-500 text-orange-500' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
              >
                {tab}
              </button>
            ))}
          </div>

          {/* Filters */}
          <div className="p-4 flex flex-col lg:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input type="text" placeholder="Search by employee name, code or department..." className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow" />
            </div>
            <div className="flex gap-3">
              <select className="border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-600 outline-none">
                <option>All Departments</option>
              </select>
              <select className="border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-600 outline-none">
                <option>All Locations</option>
              </select>
              <select className="border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-600 outline-none">
                <option>All Status</option>
              </select>
              <button className="px-4 py-2 text-slate-600 border border-slate-200 rounded-lg text-sm font-medium hover:bg-slate-50 transition">Reset</button>
              <button className="px-4 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition shadow-sm">Apply Filters</button>
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto min-h-[400px]">
            <table className="w-full text-left text-sm table-fixed">
              <thead className="bg-slate-50 text-slate-600 font-bold text-xs uppercase tracking-wider border-y border-slate-100">
                <tr>
                  <th className="px-4 py-3 w-[5%] text-center"><input type="checkbox" className="rounded border-slate-300" /></th>
                  <th className="px-2 py-3 w-[5%]">#</th>
                  <th className="px-2 py-3 w-[18%]">Employee</th>
                  <th className="px-2 py-3 w-[12%]">Emp Code</th>
                  <th className="px-2 py-3 w-[15%]">Department</th>
                  <th className="px-2 py-3 w-[10%]">Status</th>
                  <th className="px-2 py-3 w-[10%]">Check In</th>
                  <th className="px-2 py-3 w-[10%]">Check Out</th>
                  <th className="px-2 py-3 w-[10%]">Work Hours</th>
                  <th className="px-4 py-3 w-[5%] text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
                {DUMMY_DATA.map((emp, idx) => (
                  <tr 
                    key={emp.id} 
                    onClick={() => handleRowClick(emp)}
                    className={`cursor-pointer transition-colors ${selectedEmp?.id === emp.id ? 'bg-orange-50/50' : 'hover:bg-slate-50'}`}
                  >
                    <td className="px-4 py-3 text-center" onClick={(e) => e.stopPropagation()}><input type="checkbox" className="rounded border-slate-300" /></td>
                    <td className="px-2 py-3">{idx + 1}</td>
                    <td className="px-2 py-3">
                      <div className="flex items-center gap-3">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-white text-[10px] font-bold ${getAvatarColor(emp.name)}`}>
                          {getInitials(emp.name)}
                        </div>
                        <span className="truncate">{emp.name}</span>
                      </div>
                    </td>
                    <td className="px-2 py-3 text-slate-500">{emp.code}</td>
                    <td className="px-2 py-3 text-slate-500">{emp.department}</td>
                    <td className="px-2 py-3">{getStatusBadge(emp.status)}</td>
                    <td className="px-2 py-3 text-slate-500">{emp.checkIn}</td>
                    <td className="px-2 py-3 text-slate-500">{emp.checkOut}</td>
                    <td className="px-2 py-3 text-slate-500">{emp.workHours}</td>
                    <td className="px-4 py-3 text-center relative" ref={activeDropdown === emp.id ? dropdownRef : null}>
                      <button 
                        onClick={(e) => toggleDropdown(emp.id, e)}
                        className={`p-1.5 rounded-lg border transition-colors ${activeDropdown === emp.id ? 'bg-slate-100 border-slate-300' : 'border-slate-200 text-slate-400 hover:text-slate-600 hover:bg-slate-50'}`}
                      >
                        <MoreHorizontal className="w-4 h-4" />
                      </button>
                      
                      {/* DROPDOWN MENU WITH SMOOTH TRANSITION */}
                      <div className={`absolute right-10 top-2 z-50 w-48 bg-white border border-slate-100 rounded-xl shadow-xl transform origin-top-right transition-all duration-200 ease-out flex flex-col py-2 ${
                        activeDropdown === emp.id 
                          ? 'scale-100 opacity-100 translate-y-0 visible'
                          : 'scale-95 opacity-0 -translate-y-2 invisible'
                      }`}>
                        <button className="flex items-center gap-3 px-4 py-2.5 text-sm text-slate-700 font-medium hover:bg-slate-50 hover:text-blue-600 w-full text-left transition-colors">
                          <Eye className="w-4 h-4" /> View Details
                        </button>
                        <button className="flex items-center gap-3 px-4 py-2.5 text-sm text-slate-700 font-medium hover:bg-slate-50 hover:text-blue-600 w-full text-left transition-colors">
                          <Calendar className="w-4 h-4" /> Attendance History
                        </button>
                        <button className="flex items-center gap-3 px-4 py-2.5 text-sm text-slate-700 font-medium hover:bg-slate-50 hover:text-blue-600 w-full text-left transition-colors">
                          <Clock className="w-4 h-4" /> View Timesheet
                        </button>
                        <button className="flex items-center gap-3 px-4 py-2.5 text-sm text-slate-700 font-medium hover:bg-slate-50 hover:text-blue-600 w-full text-left transition-colors">
                          <Edit className="w-4 h-4" /> Edit Attendance
                        </button>
                        <div className="my-1 border-t border-slate-100"></div>
                        <button className="flex items-center gap-3 px-4 py-2.5 text-sm text-slate-700 font-medium hover:bg-slate-50 hover:text-blue-600 w-full text-left transition-colors">
                          <Download className="w-4 h-4" /> Export Record
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="p-4 border-t border-slate-100 flex justify-between items-center bg-white rounded-b-2xl">
            <p className="text-sm text-slate-500">Showing 1 to 10 of 124 entries</p>
            <div className="flex gap-1">
               <button className="px-3 py-1.5 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 bg-white">Previous</button>
               <button className="w-8 h-8 rounded-lg bg-orange-500 text-white text-sm font-bold flex items-center justify-center shadow-sm">1</button>
               <button className="w-8 h-8 rounded-lg border border-slate-200 text-slate-600 text-sm font-bold flex items-center justify-center hover:bg-slate-50">2</button>
               <button className="w-8 h-8 rounded-lg border border-slate-200 text-slate-600 text-sm font-bold flex items-center justify-center hover:bg-slate-50">3</button>
               <button className="w-8 h-8 rounded-lg border border-slate-200 text-slate-600 text-sm font-bold flex items-center justify-center hover:bg-slate-50">4</button>
               <button className="w-8 h-8 rounded-lg border border-slate-200 text-slate-600 text-sm font-bold flex items-center justify-center hover:bg-slate-50">5</button>
               <span className="w-8 h-8 flex items-center justify-center text-slate-400">...</span>
               <button className="w-8 h-8 rounded-lg border border-slate-200 text-slate-600 text-sm font-bold flex items-center justify-center hover:bg-slate-50">13</button>
               <button className="px-3 py-1.5 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 bg-white hover:bg-slate-50">Next</button>
            </div>
          </div>
        </div>

        {/* Right Sidebar - Employee Details */}
        <div className="w-[320px] shrink-0 sticky top-6 space-y-5">
          {selectedEmp && (
            <>
              <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                <div className="p-5 border-b border-slate-100 flex items-center gap-4">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-white text-xl font-bold ${getAvatarColor(selectedEmp.name)}`}>
                    {getInitials(selectedEmp.name)}
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-800 leading-tight">{selectedEmp.name}</h3>
                    <p className="text-xs text-slate-500 mt-0.5">{selectedEmp.code}</p>
                    <p className="text-xs text-slate-500">{selectedEmp.department}</p>
                    <div className="flex items-center gap-1.5 mt-1.5">
                      <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                      <span className="text-xs font-bold text-emerald-600">Online</span>
                    </div>
                  </div>
                </div>

                <div className="flex px-4 border-b border-slate-100">
                  {['Overview', 'Timeline', 'Applications', 'Websites'].map(tab => (
                    <button 
                      key={tab}
                      onClick={() => setRightPanelTab(tab)}
                      className={`flex-1 py-3 text-[11px] font-bold border-b-2 transition-colors ${rightPanelTab === tab ? 'border-orange-500 text-orange-500' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
                    >
                      {tab}
                    </button>
                  ))}
                </div>

                <div className="p-5">
                  <div className="flex justify-between items-center mb-5">
                    <button className="p-1 hover:bg-slate-100 rounded transition"><ChevronLeft className="w-4 h-4 text-slate-500" /></button>
                    <div className="flex items-center gap-2 text-sm font-bold text-slate-700">
                      <Calendar className="w-4 h-4" /> 10 Oct 2026
                    </div>
                    <button className="p-1 hover:bg-slate-100 rounded transition"><ChevronRight className="w-4 h-4 text-slate-500" /></button>
                  </div>

                  <div className="grid grid-cols-2 gap-3 mb-6">
                    <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-3 flex items-center gap-3">
                      <div className="bg-emerald-100 text-emerald-600 p-2 rounded-lg"><Users className="w-5 h-5" /></div>
                      <div>
                        <p className="text-[10px] uppercase font-bold text-slate-500">Check In</p>
                        <p className="font-bold text-slate-800">{selectedEmp.checkIn}</p>
                      </div>
                    </div>
                    <div className="bg-red-50/50 border border-red-100 rounded-xl p-3 flex items-center gap-3">
                      <div className="bg-red-100 text-red-600 p-2 rounded-lg"><Clock className="w-5 h-5" /></div>
                      <div>
                        <p className="text-[10px] uppercase font-bold text-slate-500">Check Out</p>
                        <p className="font-bold text-slate-800">{selectedEmp.checkOut}</p>
                      </div>
                    </div>
                    <div className="bg-blue-50/50 border border-blue-100 rounded-xl p-3 flex items-center gap-3">
                      <div className="bg-blue-100 text-blue-600 p-2 rounded-lg"><Clock className="w-5 h-5" /></div>
                      <div>
                        <p className="text-[10px] uppercase font-bold text-slate-500">Work Hours</p>
                        <p className="font-bold text-slate-800">{selectedEmp.workHours}</p>
                      </div>
                    </div>
                    <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-3 flex items-center gap-3">
                      <div className="bg-emerald-100 text-emerald-600 p-2 rounded-lg"><CheckCircle2 className="w-5 h-5" /></div>
                      <div>
                        <p className="text-[10px] uppercase font-bold text-slate-500">Status</p>
                        <p className="font-bold text-emerald-600">{selectedEmp.status}</p>
                      </div>
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between items-center mb-4">
                      <h4 className="font-bold text-slate-800">Attendance Timeline</h4>
                      <button className="text-xs font-bold text-orange-500 hover:text-orange-600 flex items-center">View Full <ChevronRight className="w-3 h-3" /></button>
                    </div>

                    <div className="relative pl-3 space-y-4 before:absolute before:left-3.5 before:top-2 before:bottom-2 before:w-px before:bg-slate-200">
                      <div className="relative pl-6">
                        <div className="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-emerald-500 ring-4 ring-white"></div>
                        <p className="text-xs font-bold text-slate-800">Work Started</p>
                        <p className="text-[10px] text-slate-500">09:05 AM</p>
                      </div>
                      <div className="relative pl-6 flex justify-between items-start">
                        <div>
                          <div className="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-blue-500 ring-4 ring-white"></div>
                          <p className="text-xs font-bold text-slate-800">Working</p>
                          <p className="text-[10px] text-slate-500">09:05 AM - 11:30 AM</p>
                        </div>
                        <span className="text-[10px] font-bold text-slate-500">2h 25m</span>
                      </div>
                      <div className="relative pl-6">
                        <div className="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-orange-500 ring-4 ring-white"></div>
                        <p className="text-xs font-bold text-slate-800">Break Started</p>
                        <p className="text-[10px] text-slate-500">11:30 AM</p>
                      </div>
                      <div className="relative pl-6 flex justify-between items-start">
                        <div>
                          <div className="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-orange-500 ring-4 ring-white"></div>
                          <p className="text-xs font-bold text-slate-800">Break Ended</p>
                          <p className="text-[10px] text-slate-500">11:45 AM</p>
                        </div>
                        <span className="text-[10px] font-bold text-slate-500">15m</span>
                      </div>
                      <div className="relative pl-6 flex justify-between items-start">
                        <div>
                          <div className="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-blue-500 ring-4 ring-white"></div>
                          <p className="text-xs font-bold text-slate-800">Working</p>
                          <p className="text-[10px] text-slate-500">11:45 AM - 01:00 PM</p>
                        </div>
                        <span className="text-[10px] font-bold text-slate-500">1h 15m</span>
                      </div>
                      <div className="relative pl-6 flex justify-between items-center">
                        <div>
                          <div className="absolute left-[-2px] top-2 w-2 h-2 rounded-full bg-slate-400 ring-4 ring-white animate-pulse"></div>
                          <p className="text-xs font-bold text-slate-800 mt-0.5">Current Status</p>
                          <p className="text-[10px] text-slate-500">01:00 PM - Now</p>
                        </div>
                        <span className="bg-emerald-500 text-white px-2 py-0.5 rounded text-[10px] font-bold">Live</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
