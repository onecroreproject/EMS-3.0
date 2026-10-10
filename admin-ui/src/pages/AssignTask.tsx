import { useState, useEffect, type FormEvent, useRef } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  FileText, Users, Calendar as CalendarIcon, 
  Info, CheckCircle2, ChevronRight, ChevronDown, X, Save,
  Code, BookOpen, Headset, Briefcase
} from 'lucide-react';
import Breadcrumb from '../components/ui/Breadcrumb';
import apiClient from '../lib/axios';

interface Employee {
  id: string;
  name: string;
  employeeCode: string;
  email: string;
  designation: string;
  departmentName: string;
  teamName: string;
  status: string;
}

// Custom Select Component for neater dropdowns
function CustomSelect({ 
  value, 
  onChange, 
  options, 
  placeholder,
  ringColor = "focus:ring-blue-500"
}: { 
  value: string, 
  onChange: (val: string) => void, 
  options: { label: string, value: string, icon?: React.ReactNode, colorClass?: string }[], 
  placeholder: string,
  ringColor?: string
}) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selectedOption = options.find(o => o.value === value);

  return (
    <div className="relative" ref={dropdownRef}>
      <div 
        className={`w-full px-4 py-2.5 bg-white border ${isOpen ? 'border-blue-400 ring-2 ' + ringColor.replace('focus:', '') : 'border-slate-200 hover:border-slate-300'} rounded-xl cursor-pointer flex justify-between items-center transition-all`}
        onClick={() => setIsOpen(!isOpen)}
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setIsOpen(!isOpen) }}
      >
        <span className={selectedOption ? "text-slate-700 font-medium flex items-center gap-2" : "text-slate-400"}>
          {selectedOption?.icon}
          {selectedOption ? selectedOption.label : placeholder}
        </span>
        <ChevronDown className={`h-4 w-4 text-slate-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </div>
      
      {isOpen && (
        <div className="absolute z-50 w-full mt-2 bg-white border border-slate-100 rounded-xl shadow-xl shadow-slate-200/50 max-h-60 overflow-auto py-1 animate-in fade-in slide-in-from-top-2 duration-200">
          {options.length === 0 ? (
             <div className="px-4 py-3 text-sm text-slate-400 text-center">No options available</div>
          ) : (
            options.map((option) => (
              <div
                key={option.value}
                className={`px-4 py-2.5 text-sm cursor-pointer transition-colors flex items-center gap-2 ${
                  value === option.value 
                    ? (option.colorClass ? `font-bold ${option.colorClass}` : 'bg-blue-50/80 text-blue-700 font-bold') 
                    : `text-slate-600 hover:bg-slate-50 hover:text-slate-900 ${option.colorClass ? `hover:${option.colorClass.split(' ')[0]}` : ''}`
                }`}
                onClick={() => {
                  onChange(option.value);
                  setIsOpen(false);
                }}
              >
                {option.icon}
                {option.label}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

export default function AssignTask() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('');
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);

  // Form state
  const [title, setTitle] = useState('');
  const [taskType, setTaskType] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [priority, setPriority] = useState('Medium');
  const [remarks, setRemarks] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<{type: 'success' | 'error', text: string} | null>(null);

  useEffect(() => {
    apiClient.get('/api/admin/employees?size=1000')
      .then(res => {
        setEmployees(res.data.content || []);
      })
      .catch(err => console.error("Failed to fetch employees", err));
  }, []);

  useEffect(() => {
    if (selectedEmployeeId) {
      const emp = employees.find(e => 
        e.id === selectedEmployeeId || 
        e.email === selectedEmployeeId || 
        e.employeeCode === selectedEmployeeId || 
        e.name === selectedEmployeeId
      );
      setSelectedEmployee(emp || null);
    } else {
      setSelectedEmployee(null);
    }
  }, [selectedEmployeeId, employees]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!title || !taskType || !selectedEmployeeId) {
      setMessage({ type: 'error', text: 'Please fill in all required fields' });
      return;
    }

    setIsSubmitting(true);
    setMessage(null);
    try {
      await apiClient.post('/api/admin/tasks', {
        title,
        taskType,
        description,
        assignedTo: selectedEmployeeId,
        assignedBy: 'Admin', // Or get from auth context
        startDate: startDate || undefined, // Send undefined if empty so backend sets it
        endDate,
        priority,
        remarks,
        status: 'ASSIGNED'
      });
      setMessage({ type: 'success', text: 'Task successfully created and assigned!' });
      // Reset form
      setTitle('');
      setTaskType('');
      setDescription('');
      setSelectedEmployeeId('');
      setStartDate('');
      setEndDate('');
      setPriority('Medium');
      setRemarks('');
    } catch (error) {
      console.error(error);
      setMessage({ type: 'error', text: 'Failed to create task. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-12">
      {/* Header & Breadcrumb - Right Aligned Breadcrumbs */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-2">
        <div className="flex items-center gap-4">
          <div className="h-14 w-14 bg-blue-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-blue-600/20">
            <FileText className="h-7 w-7" />
          </div>
          <div>
            <h2 className="text-3xl font-bold text-slate-800">Create New Task</h2>
            <p className="text-slate-500 text-sm mt-1">Assign a new task to an employee and set expectations</p>
          </div>
        </div>

        <div className="flex flex-col items-end">
          <div className="-mt-2 mb-3">
            <Breadcrumb items={[
              { label: 'Tasks', href: '/tasks' },
              { label: 'Create Task' }
            ]} />
          </div>
        </div>
      </div>

      {message && (
        <div className={`p-4 rounded-xl text-sm font-bold ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left Form Area - 2/3 width */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Task Information */}
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative z-30">
            <div className="bg-blue-50/50 px-6 py-4 flex items-center gap-2 border-b border-blue-100/50 rounded-t-2xl">
              <FileText className="h-5 w-5 text-blue-600" />
              <h3 className="font-bold text-blue-900">Task Information</h3>
            </div>
            <div className="p-6 space-y-5">
              <div className="grid grid-cols-2 gap-5">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-1.5">Task Title <span className="text-red-500">*</span></label>
                  <input type="text" placeholder="Enter task title" value={title} onChange={e => setTitle(e.target.value)} required className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-1.5">Task Type <span className="text-red-500">*</span></label>
                  <CustomSelect 
                    value={taskType} 
                    onChange={setTaskType} 
                    placeholder="Select Task Type"
                    ringColor="ring-blue-500"
                    options={[
                      { label: 'Development', value: 'Development', icon: <Code className="w-4 h-4" />, colorClass: 'bg-blue-50 text-blue-700' },
                      { label: 'Training', value: 'Training', icon: <BookOpen className="w-4 h-4" />, colorClass: 'bg-emerald-50 text-emerald-700' },
                      { label: 'Support', value: 'Support', icon: <Headset className="w-4 h-4" />, colorClass: 'bg-red-50 text-red-700' },
                      { label: 'Project', value: 'Project', icon: <Briefcase className="w-4 h-4" />, colorClass: 'bg-purple-50 text-purple-700' },
                      { label: 'Mentoring', value: 'Mentoring', icon: <Users className="w-4 h-4" />, colorClass: 'bg-orange-50 text-orange-700' }
                    ]}
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-1.5">Description <span className="text-slate-400 font-normal text-xs ml-1">(Optional)</span></label>
                <textarea rows={3} value={description} onChange={e => setDescription(e.target.value)} placeholder="Enter detailed description of the task..." className="w-full px-4 py-3 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm resize-none"></textarea>
                <div className="text-right text-xs text-slate-400 mt-1">{description.length}/500</div>
              </div>
            </div>
          </div>

          {/* Employee Details */}
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative z-20">
            <div className="bg-emerald-50/50 px-6 py-4 flex items-center gap-2 border-b border-emerald-100/50 rounded-t-2xl">
              <Users className="h-5 w-5 text-emerald-600" />
              <h3 className="font-bold text-emerald-900">Employee Details</h3>
            </div>
            <div className="p-6 space-y-5">
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-1.5">Select Employee <span className="text-red-500">*</span></label>
                <CustomSelect 
                  value={selectedEmployeeId} 
                  onChange={setSelectedEmployeeId} 
                  placeholder="Select Employee"
                  ringColor="ring-emerald-500"
                  options={employees.map(emp => ({ 
                    label: `${emp.name} ${emp.employeeCode ? `(${emp.employeeCode})` : ''}`, 
                    value: emp.id || emp.email || emp.employeeCode || emp.name 
                  }))}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">Employee Code</label>
                  <div className="px-4 py-2 bg-slate-50 border border-slate-100 rounded-lg text-sm text-slate-600 min-h-[38px] flex items-center">
                    {selectedEmployee?.employeeCode || '-'}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">Email</label>
                  <div className="px-4 py-2 bg-slate-50 border border-slate-100 rounded-lg text-sm text-slate-600 min-h-[38px] flex items-center truncate">
                    {selectedEmployee?.email || '-'}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">Designation</label>
                  <div className="px-4 py-2 bg-slate-50 border border-slate-100 rounded-lg text-sm text-slate-600 min-h-[38px] flex items-center truncate">
                    {selectedEmployee?.designation || '-'}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">Department</label>
                  <div className="px-4 py-2 bg-slate-50 border border-slate-100 rounded-lg text-sm text-slate-600 min-h-[38px] flex items-center truncate">
                    {selectedEmployee?.departmentName || '-'}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">Team</label>
                  <div className="px-4 py-2 bg-slate-50 border border-slate-100 rounded-lg text-sm text-slate-600 min-h-[38px] flex items-center truncate">
                    {selectedEmployee?.teamName || '-'}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">Status</label>
                  <div className="px-4 py-2 bg-slate-50 border border-slate-100 rounded-lg text-sm text-slate-600 min-h-[38px] flex items-center">
                    {selectedEmployee ? (
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-bold ${
                        selectedEmployee.status.toLowerCase() === 'active' ? 'bg-emerald-100 text-emerald-700' : 
                        selectedEmployee.status.toLowerCase() === 'inactive' ? 'bg-red-100 text-red-700' : 'bg-slate-200 text-slate-700'
                      }`}>
                        {selectedEmployee.status}
                      </span>
                    ) : (
                      '-'
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Assignment Details */}
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative z-10">
            <div className="bg-purple-50/50 px-6 py-4 flex items-center gap-2 border-b border-purple-100/50 rounded-t-2xl">
              <CalendarIcon className="h-5 w-5 text-purple-600" />
              <h3 className="font-bold text-purple-900">Assignment Details</h3>
            </div>
            <div className="p-6">
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-1.5">Assigned By <span className="text-red-500">*</span></label>
                  <input type="text" value="Admin" disabled className="w-full px-4 py-2.5 bg-slate-100 border border-slate-200 rounded-xl text-sm text-slate-500" />
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-1.5">Start Date <span className="text-red-500">*</span></label>
                  <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm text-slate-700" />
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-1.5">End Date <span className="text-slate-400 font-normal text-xs ml-1">(Optional)</span></label>
                  <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm text-slate-700" />
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-1.5">Priority <span className="text-red-500">*</span></label>
                  <CustomSelect 
                    value={priority} 
                    onChange={setPriority} 
                    placeholder="Select Priority"
                    ringColor="ring-purple-500"
                    options={[
                      { label: 'Low', value: 'Low', icon: <div className="w-2 h-2 rounded-full bg-slate-400"></div> },
                      { label: 'Medium', value: 'Medium', icon: <div className="w-2 h-2 rounded-full bg-blue-500"></div> },
                      { label: 'High', value: 'High', icon: <div className="w-2 h-2 rounded-full bg-orange-500"></div> },
                      { label: 'Critical', value: 'Critical', icon: <div className="w-2 h-2 rounded-full bg-red-600 animate-pulse"></div> }
                    ]}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Additional Information */}
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative">
            <div className="bg-orange-50/50 px-6 py-4 flex items-center gap-2 border-b border-orange-100/50 rounded-t-2xl">
              <Info className="h-5 w-5 text-orange-600" />
              <h3 className="font-bold text-orange-900">Additional Information</h3>
            </div>
            <div className="p-6">
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-1.5">Remarks / Instructions</label>
                <textarea rows={2} value={remarks} onChange={e => setRemarks(e.target.value)} placeholder="Enter any notes..." className="w-full px-4 py-3 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-orange-500 text-sm resize-none"></textarea>
                <div className="text-right text-xs text-slate-400 mt-1">{remarks.length}/300</div>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-between items-center bg-white p-4 rounded-2xl border border-slate-100 shadow-sm">
            <button type="button" onClick={() => window.history.back()} className="flex items-center gap-2 px-5 py-2.5 text-slate-600 font-bold hover:bg-slate-50 rounded-xl transition">
              <X className="h-4 w-4" /> Cancel
            </button>
            <button type="submit" disabled={isSubmitting} className="flex items-center gap-2 bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold hover:bg-blue-700 transition shadow-sm disabled:opacity-50">
              <Save className="h-4 w-4" /> {isSubmitting ? 'Saving...' : 'Save Task'}
            </button>
          </div>

        </div>

        {/* Right Sidebar Area - 1/3 width */}
        <div className="space-y-6">
          
          {/* Task Workflow */}
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="bg-slate-50/80 px-5 py-4 flex items-center gap-2 border-b border-slate-100">
              <FileText className="h-4 w-4 text-blue-600" />
              <h3 className="font-bold text-slate-800 text-sm">Task Workflow</h3>
            </div>
            <div className="p-5">
              <div className="space-y-4">
                <div className="flex gap-4">
                  <div className="flex flex-col items-center">
                    <div className="h-8 w-8 rounded-full bg-blue-500 text-white flex items-center justify-center font-bold text-sm shrink-0">1</div>
                    <div className="w-0.5 h-6 bg-slate-200 my-1"></div>
                  </div>
                  <div className="pt-1">
                    <h4 className="font-bold text-slate-800 text-sm">Assigned</h4>
                    <p className="text-xs text-slate-500 mt-0.5">Task is assigned to employee</p>
                  </div>
                </div>
                
                <div className="flex gap-4">
                  <div className="flex flex-col items-center">
                    <div className="h-8 w-8 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold text-sm shrink-0">2</div>
                    <div className="w-0.5 h-6 bg-slate-200 my-1"></div>
                  </div>
                  <div className="pt-1">
                    <h4 className="font-bold text-slate-800 text-sm">Accepted</h4>
                    <p className="text-xs text-slate-500 mt-0.5">Employee accepts the task</p>
                  </div>
                </div>

                <div className="flex gap-4">
                  <div className="flex flex-col items-center">
                    <div className="h-8 w-8 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-sm shrink-0">3</div>
                    <div className="w-0.5 h-6 bg-slate-200 my-1"></div>
                  </div>
                  <div className="pt-1">
                    <h4 className="font-bold text-slate-800 text-sm">In Progress</h4>
                    <p className="text-xs text-slate-500 mt-0.5">Employee works on the task</p>
                  </div>
                </div>

                <div className="flex gap-4">
                  <div className="flex flex-col items-center">
                    <div className="h-8 w-8 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-bold text-sm shrink-0">4</div>
                  </div>
                  <div className="pt-1">
                    <h4 className="font-bold text-slate-800 text-sm">Completed</h4>
                    <p className="text-xs text-slate-500 mt-0.5">Task is completed and reviewed</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Task Types */}
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="bg-slate-50/80 px-5 py-4 flex items-center gap-2 border-b border-slate-100">
              <FileText className="h-4 w-4 text-blue-600" />
              <h3 className="font-bold text-slate-800 text-sm">Task Types</h3>
            </div>
            <div className="p-5 space-y-4">
              <div className="flex items-center gap-3">
                <span className="w-24 px-2 py-1 bg-blue-50 text-blue-600 rounded-md text-xs font-bold text-center shrink-0">Development</span>
                <span className="text-xs text-slate-500">Feature development</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="w-24 px-2 py-1 bg-emerald-50 text-emerald-600 rounded-md text-xs font-bold text-center shrink-0">Training</span>
                <span className="text-xs text-slate-500">Skill development</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="w-24 px-2 py-1 bg-red-50 text-red-600 rounded-md text-xs font-bold text-center shrink-0">Support</span>
                <span className="text-xs text-slate-500">Bug fixes, support</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="w-24 px-2 py-1 bg-purple-50 text-purple-600 rounded-md text-xs font-bold text-center shrink-0">Project</span>
                <span className="text-xs text-slate-500">Project tasks</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="w-24 px-2 py-1 bg-orange-50 text-orange-600 rounded-md text-xs font-bold text-center shrink-0">Mentoring</span>
                <span className="text-xs text-slate-500">Guidance</span>
              </div>
            </div>
          </div>

          {/* Tips */}
          <div className="bg-blue-50/50 rounded-2xl shadow-sm border border-blue-100 overflow-hidden">
            <div className="px-5 py-4 flex items-center gap-2 border-b border-blue-100/50">
              <Info className="h-4 w-4 text-blue-600" />
              <h3 className="font-bold text-blue-900 text-sm">Tips for Creating Tasks</h3>
            </div>
            <div className="p-5">
              <ul className="space-y-3">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-blue-500 shrink-0 mt-0.5" />
                  <span className="text-xs text-blue-800">Provide a clear and specific title</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-blue-500 shrink-0 mt-0.5" />
                  <span className="text-xs text-blue-800">Include detailed description</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-blue-500 shrink-0 mt-0.5" />
                  <span className="text-xs text-blue-800">Set realistic start and end dates</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-blue-500 shrink-0 mt-0.5" />
                  <span className="text-xs text-blue-800">Choose appropriate task priority</span>
                </li>
              </ul>
            </div>
          </div>

        </div>

      </form>
    </div>
  );
}
