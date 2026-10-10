import { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  ClipboardList, Search, Plus, ListTodo,
  CheckCircle2, Clock, FileWarning, AlertCircle,
  Edit, Trash2, ChevronLeft, ChevronRight,
  TrendingUp
} from 'lucide-react';
import Breadcrumb from '../components/ui/Breadcrumb';
import apiClient from '../lib/axios';
import DeleteModal from '../components/ui/DeleteModal';

interface Task {
  id: string;
  title: string;
  taskType: string;
  assignedTo: string;
  assignedBy: string;
  startDate: string;
  endDate: string;
  status: string;
  priority: string;
}

interface Employee {
  id: string;
  name: string;
}

export default function TaskList() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [filterEmployee, setFilterEmployee] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterPriority, setFilterPriority] = useState('');

  // Pagination
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(7); // Show 7 tasks per page to prevent scrolling as per user request
  
  // Actions
  const [taskToDelete, setTaskToDelete] = useState<string | null>(null);

  useEffect(() => {
    fetchTasks();
    fetchEmployees();
  }, []);

  const fetchTasks = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get('/api/admin/tasks');
      setTasks(res.data);
    } catch (error) {
      console.error("Failed to fetch tasks", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchEmployees = async () => {
    try {
      const res = await apiClient.get('/api/admin/employees?size=1000');
      setEmployees(res.data.content || []);
    } catch (error) {
      console.error("Failed to fetch employees", error);
    }
  };

  const confirmDelete = async () => {
    if (!taskToDelete) return;
    try {
      await apiClient.delete(`/api/admin/tasks/${taskToDelete}`);
      setTaskToDelete(null);
      fetchTasks();
    } catch (error) {
      console.error("Failed to delete task", error);
    }
  };

  const handleResetFilters = () => {
    setSearchQuery('');
    setFilterEmployee('');
    setFilterType('');
    setFilterStatus('');
    setFilterPriority('');
  };

  // Filter tasks locally (since backend API currently doesn't support pagination/filtering natively yet, based on AdminTaskController)
  const filteredTasks = tasks.filter(task => {
    if (searchQuery) {
      const matchTitle = task.title?.toLowerCase().includes(searchQuery.toLowerCase());
      const empName = employees.find(e => e.id === task.assignedTo)?.name.toLowerCase() || '';
      const matchEmp = empName.includes(searchQuery.toLowerCase());
      if (!matchTitle && !matchEmp) return false;
    }
    if (filterEmployee && task.assignedTo !== filterEmployee) return false;
    if (filterType && task.taskType !== filterType) return false;
    if (filterStatus && task.status !== filterStatus) return false;
    if (filterPriority && task.priority !== filterPriority) return false;
    return true;
  });

  const totalPages = Math.ceil(filteredTasks.length / size);
  const paginatedTasks = filteredTasks.slice(page * size, (page + 1) * size);

  // Stats
  const totalTasks = tasks.length;
  const completedTasks = tasks.filter(t => t.status === 'COMPLETED').length;
  const inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS').length;
  const registeredTasks = tasks.filter(t => t.status === 'REGISTERED' || t.status === 'ASSIGNED').length;
  const pendingTasks = tasks.filter(t => t.status === 'PENDING' || t.status === 'ACCEPTED').length;

  const getInitials = (name: string) => {
    if (!name) return 'U';
    const parts = name.split(' ');
    if (parts.length > 1) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.substring(0, 2).toUpperCase();
  };

  const getAvatarColor = (name: string) => {
    if (!name) return 'bg-slate-500';
    const colors = [
      'bg-indigo-500', 'bg-purple-500', 'bg-pink-500', 'bg-rose-500', 
      'bg-orange-500', 'bg-amber-500', 'bg-emerald-500', 'bg-teal-500', 'bg-cyan-500'
    ];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  };

  const getTaskTypeBadge = (type: string) => {
    switch (type) {
      case 'Development': return 'bg-blue-100 text-blue-600';
      case 'Support': return 'bg-red-100 text-red-600';
      case 'Testing': return 'bg-purple-100 text-purple-600';
      case 'Project': return 'bg-sky-100 text-sky-600';
      case 'Training': return 'bg-emerald-100 text-emerald-600';
      case 'Mentoring': return 'bg-fuchsia-100 text-fuchsia-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  };

  const getStatusBadge = (status: string) => {
    const s = (status || '').toUpperCase();
    if (s === 'COMPLETED') return 'bg-emerald-500 text-white';
    if (s === 'IN_PROGRESS' || s === 'IN PROGRESS') return 'bg-blue-500 text-white';
    if (s === 'REGISTERED') return 'bg-slate-500 text-white';
    if (s === 'ASSIGNED') return 'bg-orange-400 text-white';
    return 'bg-slate-500 text-white';
  };

  const getPriorityColor = (priority: string) => {
    const p = (priority || '').toLowerCase();
    if (p === 'high' || p === 'critical') return 'text-red-500 bg-red-50';
    if (p === 'medium') return 'text-orange-500 bg-orange-50';
    if (p === 'low') return 'text-emerald-500 bg-emerald-50';
    return 'text-slate-500 bg-slate-50';
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-12">
      
      <DeleteModal 
        isOpen={!!taskToDelete}
        onClose={() => setTaskToDelete(null)}
        onConfirm={confirmDelete}
      />

      {/* Header & Breadcrumb */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-4">
          <div className="h-14 w-14 bg-blue-100 rounded-2xl flex items-center justify-center text-blue-600">
            <ClipboardList className="h-7 w-7" />
          </div>
          <div>
            <h2 className="text-3xl font-bold text-slate-800">Task List</h2>
            <p className="text-slate-500 text-sm mt-1">View and manage all assigned tasks</p>
          </div>
        </div>
        
        <div className="flex flex-col items-end">
          <div className="-mt-2 mb-3">
            <Breadcrumb items={[
              { label: 'Tasks', href: '/tasks' },
              { label: 'Task List' }
            ]} />
          </div>
          <NavLink 
            to="/tasks/assign" 
            className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded-xl text-sm font-bold hover:bg-blue-700 transition shadow-sm"
          >
            <Plus className="h-4 w-4" /> Create New Task
          </NavLink>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-blue-50 p-3 rounded-full text-blue-500 shrink-0">
            <ListTodo className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase">Total Tasks</p>
            <h3 className="text-xl font-bold text-slate-800">{totalTasks}</h3>
          </div>
        </div>
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-emerald-50 p-3 rounded-full text-emerald-500 shrink-0">
            <CheckCircle2 className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase">Completed</p>
            <div className="flex items-center gap-2">
              <h3 className="text-xl font-bold text-slate-800">{completedTasks}</h3>
              <span className="text-[10px] font-bold text-emerald-600 flex items-center"><TrendingUp className="h-3 w-3 mr-0.5"/> +12%</span>
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-orange-50 p-3 rounded-full text-orange-500 shrink-0">
            <Clock className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase">In Progress</p>
            <h3 className="text-xl font-bold text-slate-800">{inProgressTasks}</h3>
          </div>
        </div>
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-slate-100 p-3 rounded-full text-slate-600 shrink-0">
            <ClipboardList className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase">Registered</p>
            <h3 className="text-xl font-bold text-slate-800">{registeredTasks}</h3>
          </div>
        </div>
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-red-50 p-3 rounded-full text-red-500 shrink-0">
            <AlertCircle className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase">Pending</p>
            <h3 className="text-xl font-bold text-slate-800">{pendingTasks}</h3>
          </div>
        </div>
      </div>

      {/* Main Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100">
        
        {/* Filters */}
        <div className="p-4 border-b border-slate-100 flex flex-col lg:flex-row gap-4 items-center justify-between bg-slate-50/50 rounded-t-2xl">
          
          {/* Search */}
          <div className="w-full lg:w-96">
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <Search className="h-4 w-4" />
              </div>
              <input 
                type="text" 
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="Search by title, employee, type..." 
                className="w-full pl-9 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm transition-all shadow-sm" 
              />
            </div>
          </div>

          {/* Filter Section */}
          <div className="flex flex-wrap lg:flex-nowrap gap-3 items-center">
              
              <div className="w-[140px]">
                <select value={filterEmployee} onChange={e => setFilterEmployee(e.target.value)} className="w-full px-3 py-2.5 border border-slate-200 rounded-xl text-sm outline-none bg-white focus:border-blue-500 shadow-sm">
                  <option value="">All Employees</option>
                  {employees.map(emp => (
                    <option key={emp.id} value={emp.id}>{emp.name}</option>
                  ))}
                </select>
              </div>

              <div className="w-[130px]">
                <select value={filterType} onChange={e => setFilterType(e.target.value)} className="w-full px-3 py-2.5 border border-slate-200 rounded-xl text-sm outline-none bg-white focus:border-blue-500 shadow-sm">
                  <option value="">All Types</option>
                  <option value="Development">Development</option>
                  <option value="Training">Training</option>
                  <option value="Support">Support</option>
                  <option value="Project">Project</option>
                  <option value="Testing">Testing</option>
                  <option value="Mentoring">Mentoring</option>
                </select>
              </div>

              <div className="w-[130px]">
                <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)} className="w-full px-3 py-2.5 border border-slate-200 rounded-xl text-sm outline-none bg-white focus:border-blue-500 shadow-sm">
                  <option value="">All Status</option>
                  <option value="ASSIGNED">ASSIGNED</option>
                  <option value="REGISTERED">REGISTERED</option>
                  <option value="IN_PROGRESS">IN PROGRESS</option>
                  <option value="COMPLETED">COMPLETED</option>
                </select>
              </div>

              <div className="w-[120px]">
                <select value={filterPriority} onChange={e => setFilterPriority(e.target.value)} className="w-full px-3 py-2.5 border border-slate-200 rounded-xl text-sm outline-none bg-white focus:border-blue-500 shadow-sm">
                  <option value="">All Priority</option>
                  <option value="High">High</option>
                  <option value="Medium">Medium</option>
                  <option value="Low">Low</option>
                </select>
              </div>

              <div className="flex gap-2">
                <button onClick={handleResetFilters} className="px-5 py-2.5 text-sm font-semibold text-slate-600 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl transition shadow-sm">
                  Reset
                </button>
                <button className="px-5 py-2.5 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl transition shadow-sm">
                  Apply
                </button>
              </div>

          </div>

        </div>

        {/* Table */}
        <div className="w-full">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-600 text-xs font-bold border-b border-slate-100">
              <tr>
                <th className="px-4 py-3">#</th>
                <th className="px-4 py-3">Title ↕</th>
                <th className="px-4 py-3">Assigned To ↕</th>
                <th className="px-4 py-3">Assigned By ↕</th>
                <th className="px-4 py-3">Task Type ↕</th>
                <th className="px-4 py-3">Start Date ↕</th>
                <th className="px-4 py-3">End Date ↕</th>
                <th className="px-4 py-3">Status ↕</th>
                <th className="px-4 py-3">Priority ↕</th>
                <th className="px-4 py-3 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
              {loading ? (
                <tr><td colSpan={10} className="px-6 py-12 text-center text-slate-400">Loading tasks...</td></tr>
              ) : paginatedTasks.length === 0 ? (
                <tr><td colSpan={10} className="px-6 py-12 text-center text-slate-400">No tasks found.</td></tr>
              ) : (
                paginatedTasks.map((task, index) => {
                  const empName = employees.find(e => e.id === task.assignedTo)?.name || task.assignedTo || 'Unknown';
                  return (
                    <tr key={task.id} className="hover:bg-slate-50 transition-colors">
                      <td className="px-4 py-4">{page * size + index + 1}</td>
                      <td className="px-4 py-4 font-semibold text-slate-800">{task.title || '-'}</td>
                      
                      <td className="px-4 py-4">
                        <div className="flex items-center gap-2">
                          <div className={`w-7 h-7 rounded-full text-white flex items-center justify-center text-[10px] font-bold ${getAvatarColor(empName)}`}>
                            {getInitials(empName)}
                          </div>
                          <span>{empName}</span>
                        </div>
                      </td>
                      
                      <td className="px-4 py-4">
                        <div className="flex items-center gap-2">
                          <div className={`w-7 h-7 rounded-full text-white flex items-center justify-center text-[10px] font-bold ${getAvatarColor(task.assignedBy || 'Admin')}`}>
                            {getInitials(task.assignedBy || 'Admin')}
                          </div>
                          <span>{task.assignedBy || 'Admin'} <span className="text-slate-400 text-xs">(Manager)</span></span>
                        </div>
                      </td>

                      <td className="px-4 py-4">
                        <span className={`px-2.5 py-1 rounded-md text-xs font-bold ${getTaskTypeBadge(task.taskType)}`}>
                          {task.taskType || 'Task'}
                        </span>
                      </td>
                      
                      <td className="px-4 py-4">{task.startDate || '-'}</td>
                      <td className="px-4 py-4">{task.endDate || '-'}</td>
                      
                      <td className="px-4 py-4">
                        <span className={`px-2.5 py-1 rounded-md text-[11px] font-bold tracking-wide uppercase ${getStatusBadge(task.status)}`}>
                          {task.status || 'UNKNOWN'}
                        </span>
                      </td>

                      <td className="px-4 py-4">
                        <span className={`px-3 py-1 rounded-full text-xs font-bold ${getPriorityColor(task.priority)}`}>
                          {task.priority || 'Medium'}
                        </span>
                      </td>

                      <td className="px-4 py-4">
                        <div className="flex items-center justify-center gap-2">
                          <button onClick={() => alert(`View/Edit Task ID: ${task.id}`)} className="p-1.5 bg-yellow-100 text-yellow-700 rounded hover:bg-yellow-200 transition" title="View / Edit">
                            <Edit className="h-4 w-4" />
                          </button>
                          <button onClick={() => setTaskToDelete(task.id)} className="p-1.5 bg-red-100 text-red-600 rounded hover:bg-red-200 transition" title="Delete">
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {filteredTasks.length > 0 && (
          <div className="p-4 border-t border-slate-100 flex flex-col sm:flex-row justify-between items-center gap-4 rounded-b-2xl">
            <p className="text-sm text-slate-500">
              Showing {page * size + 1} to {Math.min((page + 1) * size, filteredTasks.length)} of {filteredTasks.length} tasks
            </p>
            <div className="flex items-center gap-1">
              <button 
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 border border-slate-200 rounded text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50"
              >
                Previous
              </button>
              
              <button className="w-8 h-8 flex items-center justify-center rounded bg-blue-600 text-white text-sm font-bold shadow-sm">
                {page + 1}
              </button>
              {page + 1 < totalPages && (
                <button 
                  onClick={() => setPage(page + 1)}
                  className="w-8 h-8 flex items-center justify-center rounded border border-slate-200 text-slate-600 text-sm font-bold hover:bg-slate-50"
                >
                  {page + 2}
                </button>
              )}

              <button 
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 border border-slate-200 rounded text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

    </div>
  );
}
