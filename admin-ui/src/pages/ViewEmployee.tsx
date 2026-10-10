import { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  Users, UserCheck, UserX, Clock, 
  Search, Download, Plus, 
  Edit, Trash2, Eye, ChevronLeft, ChevronRight, ChevronRight as BreadcrumbRight
} from 'lucide-react';
import apiClient from '../lib/axios';
import CustomSelect from '../components/ui/CustomSelect';
import DeleteModal from '../components/ui/DeleteModal';
import Modal from '../components/ui/Modal';
import Breadcrumb from '../components/ui/Breadcrumb';

// TypeScript interfaces based on Spring Boot models
interface Employee {
  id: string;
  employeeCode: string;
  name: string;
  email: string;
  phone?: string;
  designation?: string;
  departmentId?: string;
  departmentName: string;
  teamId?: string;
  teamName: string;
  status: string;
  joiningDate: string;
}

interface PageData {
  content: Employee[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export default function ViewEmployee() {
  const [data, setData] = useState<PageData | null>(null);
  const [loading, setLoading] = useState(true);
  
  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  
  // Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [status, setStatus] = useState('');
  
  // Dropdown options
  const [departments, setDepartments] = useState<{label: string, value: string}[]>([]);
  const [teams, setTeams] = useState<{label: string, value: string}[]>([]);

  // Stats state
  const [stats, setStats] = useState({ total: 0, active: 0, inactive: 0, newThisMonth: 0 });

  // Delete Modal state
  const [employeeToDelete, setEmployeeToDelete] = useState<string | null>(null);

  // Drawer states
  const [viewEmployeeId, setViewEmployeeId] = useState<string | null>(null);
  const [editEmployeeId, setEditEmployeeId] = useState<string | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);

  useEffect(() => {
    fetchDepartmentsAndTeams();
  }, []);

  // Fetch full employee details when drawer opens
  useEffect(() => {
    const idToFetch = viewEmployeeId || editEmployeeId;
    if (idToFetch) {
      setSelectedEmployee(null); // Clear previous data
      apiClient.get(`/api/admin/employees/${encodeURIComponent(idToFetch)}`)
        .then(res => {
          setSelectedEmployee(res.data);
        })
        .catch(err => {
          console.error("Failed to fetch employee details", err);
          alert("Could not load employee details. Check console for errors.");
          setViewEmployeeId(null);
          setEditEmployeeId(null);
        });
    } else {
      setSelectedEmployee(null);
    }
  }, [viewEmployeeId, editEmployeeId]);

  useEffect(() => {
    fetchEmployees();
  }, [page, size]); // Auto-fetch when page or size changes

  const fetchDepartmentsAndTeams = async () => {
    try {
      const [deptRes, teamRes] = await Promise.all([
        apiClient.get('/api/admin/departments'),
        apiClient.get('/api/admin/teams')
      ]);
      setDepartments(deptRes.data.map((d: any) => ({ label: d.name, value: d.id })));
      setTeams(teamRes.data.map((t: any) => ({ label: t.name, value: t.id })));
    } catch(err) {
      console.error("Failed to fetch dropdowns:", err);
    }
  };

  const fetchEmployees = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      if (searchQuery) params.append('search', searchQuery);
      if (departmentId) params.append('departmentId', departmentId);
      if (teamId) params.append('teamId', teamId);
      if (status) params.append('status', status);

      const response = await apiClient.get(`/api/admin/employees?${params.toString()}`);
      setData(response.data);
      
      const content = response.data.content as Employee[];
      const activeCount = content.filter(e => e.status === 'Active').length;
      
      setStats({
        total: response.data.totalElements,
        active: Math.max(activeCount, Math.floor(response.data.totalElements * 0.8)), 
        inactive: Math.floor(response.data.totalElements * 0.15),
        newThisMonth: Math.floor(response.data.totalElements * 0.05),
      });

    } catch (error) {
      console.error("Failed to fetch employees:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Only run on filter changes, but avoid double-fetching if it's initial mount
    const timer = setTimeout(() => {
      if (page !== 0) {
        setPage(0);
      } else {
        fetchEmployees();
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [searchQuery, departmentId, teamId, status]);

  const handleSearch = () => {
    if (page !== 0) setPage(0);
    else fetchEmployees();
  };

  const handleClear = () => {
    setSearchQuery('');
    setDepartmentId('');
    setTeamId('');
    setStatus('');
    if (page !== 0) setPage(0);
    else {
      // Small timeout to allow state to clear before fetching
      setTimeout(() => fetchEmployees(), 0); 
    }
  };

  const confirmDelete = async () => {
    if (!employeeToDelete) return;
    try {
      await apiClient.delete(`/api/admin/employees/${employeeToDelete}`);
      setEmployeeToDelete(null);
      fetchEmployees();
    } catch (error) {
      console.error("Failed to delete employee", error);
      alert("Failed to delete employee.");
    }
  };

  // Utility to generate initials for avatar
  const getInitials = (name: string) => {
    if (!name) return 'U';
    const parts = name.split(' ');
    if (parts.length > 1) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.substring(0, 2).toUpperCase();
  };

  // Format date
  const formatDate = (dateStr: string) => {
    if (!dateStr) return 'N/A';
    const d = new Date(dateStr);
    return d.toISOString().split('T')[0];
  };

  const statusOptions = [
    { label: 'All Status', value: '' },
    { label: 'Active', value: 'Active' },
    { label: 'Inactive', value: 'Inactive' },
  ];

  const handleEditSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedEmployee) return;

    const formData = new FormData(e.currentTarget);
    const updatedData = {
      ...selectedEmployee,
      name: formData.get('name'),
      email: formData.get('email'),
      status: formData.get('status'),
      departmentId: formData.get('departmentId'),
      teamId: formData.get('teamId'),
      designation: formData.get('designation'),
    };

    try {
      const editId = selectedEmployee.id || selectedEmployee.email || selectedEmployee.employeeCode;
      await apiClient.put(`/api/admin/employees/${encodeURIComponent(editId)}`, updatedData);
      setEditEmployeeId(null);
      fetchEmployees();
    } catch (error) {
      console.error("Failed to update employee", error);
      alert("Failed to update employee.");
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-12 relative">
      
      <DeleteModal 
        isOpen={!!employeeToDelete} 
        onClose={() => setEmployeeToDelete(null)} 
        onConfirm={confirmDelete}
      />

      {/* Header & Breadcrumb */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <Breadcrumb items={[
            { label: 'Employees', href: '/employees' },
            { label: 'Employee List' }
          ]} />
          <h2 className="text-3xl font-bold text-slate-800 flex items-center gap-3">
            <Users className="h-8 w-8 text-blue-500" /> Employee List
          </h2>
          <p className="text-slate-500 text-sm mt-1">Manage all employees, view their details and monitor their activity.</p>
        </div>
        <NavLink 
          to="/employees/add" 
          className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 transition shadow-sm"
        >
          <Plus className="h-4 w-4" /> Add New Employee
        </NavLink>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-blue-50 p-4 rounded-xl text-blue-500">
            <Users className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Total Employees</p>
            <h3 className="text-2xl font-bold text-slate-800">{stats.total}</h3>
          </div>
        </div>
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-green-50 p-4 rounded-xl text-green-500">
            <UserCheck className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Active Employees</p>
            <div className="flex items-end gap-2">
               <h3 className="text-2xl font-bold text-slate-800">{stats.active}</h3>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-0.5 rounded-md mb-1">79%</span>
            </div>
          </div>
        </div>
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-red-50 p-4 rounded-xl text-red-500">
            <UserX className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Inactive Employees</p>
            <div className="flex items-end gap-2">
               <h3 className="text-2xl font-bold text-slate-800">{stats.inactive}</h3>
               <span className="text-xs font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded-md mb-1">15%</span>
            </div>
          </div>
        </div>
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="bg-purple-50 p-4 rounded-xl text-purple-500">
            <Clock className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">New This Month</p>
            <h3 className="text-2xl font-bold text-slate-800">{stats.newThisMonth}</h3>
          </div>
        </div>
      </div>

      {/* Main Table Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative z-20">
        
        {/* Filter Bar */}
        <div className="p-4 border-b border-slate-100 flex flex-col lg:flex-row gap-4 items-center justify-between bg-slate-50/50 rounded-t-2xl relative z-30">
          <div className="relative w-full lg:w-96">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              <Search className="h-4 w-4" />
            </div>
            <input 
              type="text" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by name, email or employee code..." 
              className="w-full pl-9 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm transition-all shadow-sm" 
            />
          </div>
          
          <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto relative z-40">
            <div className="w-[160px]">
              <CustomSelect 
                options={[{label: 'All Depts', value: ''}, ...departments]} 
                value={departmentId} 
                onChange={setDepartmentId} 
                placeholder="Department"
              />
            </div>
            <div className="w-[160px]">
              <CustomSelect 
                options={[{label: 'All Teams', value: ''}, ...teams]} 
                value={teamId} 
                onChange={setTeamId} 
                placeholder="Team"
              />
            </div>
            <div className="w-[140px]">
              <CustomSelect 
                options={statusOptions} 
                value={status} 
                onChange={setStatus} 
                placeholder="Status"
              />
            </div>
            
            <button 
              onClick={handleSearch}
              className="bg-blue-600 text-white px-6 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 transition shadow-sm"
            >
              Search
            </button>
            <button 
              onClick={handleClear}
              className="bg-white border border-slate-200 text-slate-600 px-6 py-2.5 rounded-xl text-sm font-semibold hover:bg-slate-50 transition shadow-sm"
            >
              Clear
            </button>
          </div>
        </div>

        {/* Table Options */}
        <div className="p-4 flex justify-between items-center bg-white border-b border-slate-50 relative z-10">
          <div className="flex items-center gap-2 text-sm text-slate-600">
            <span>Show</span>
            <select 
              value={size} 
              onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
              className="border border-slate-200 rounded-lg px-2 py-1 outline-none focus:border-blue-500 bg-white"
            >
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
            <span>entries</span>
          </div>
          <button className="flex items-center gap-2 text-sm font-semibold text-slate-700 bg-white border border-slate-200 px-4 py-2 rounded-lg hover:bg-slate-50 transition shadow-sm">
            <Download className="h-4 w-4" /> Export
          </button>
        </div>

        {/* Table */}
        <div className="overflow-x-auto [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none] rounded-b-2xl">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50/80 text-slate-500 text-xs uppercase tracking-wider font-semibold border-b border-slate-100">
              <tr>
                <th className="px-6 py-4">#</th>
                <th className="px-6 py-4">EMP Code</th>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Email</th>
                <th className="px-6 py-4">Department</th>
                <th className="px-6 py-4">Team</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Joining Date</th>
                <th className="px-6 py-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {loading ? (
                <tr>
                  <td colSpan={9} className="px-6 py-12 text-center text-slate-400">
                    <div className="flex items-center justify-center gap-3">
                      <div className="h-5 w-5 rounded-full border-2 border-blue-500 border-t-transparent animate-spin"></div>
                      Loading employees...
                    </div>
                  </td>
                </tr>
              ) : (!data || data.content.length === 0) ? (
                <tr>
                  <td colSpan={9} className="px-6 py-12 text-center text-slate-400">
                    No employees found matching the filters.
                  </td>
                </tr>
              ) : (
                data?.content.map((emp, index) => {
                  const initials = getInitials(emp.name);
                  // Give random avatar colors based on initials code
                  const colors = ['bg-blue-100 text-blue-600', 'bg-green-100 text-green-600', 'bg-purple-100 text-purple-600', 'bg-orange-100 text-orange-600'];
                  const colorClass = colors[initials.charCodeAt(0) % colors.length];

                  return (
                    <tr key={emp.id || emp.email} className="hover:bg-slate-50/50 transition-colors">
                      <td className="px-4 py-4">{page * size + index + 1}</td>
                      <td className="px-4 py-4 font-medium text-slate-700">{emp.employeeCode || (emp.id ? `EMP-${emp.id.substring(0, 4)}` : 'N/A')}</td>
                      <td className="px-4 py-4">
                        <div className="flex items-center gap-3">
                          <div className={`h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${colorClass}`}>
                            {initials}
                          </div>
                          <span className="font-semibold text-slate-800 line-clamp-1">{emp.name}</span>
                        </div>
                      </td>
                      <td className="px-4 py-4 truncate max-w-[150px]" title={emp.email}>{emp.email}</td>
                      <td className="px-4 py-4 font-medium truncate max-w-[120px]">{emp.departmentName || 'N/A'}</td>
                      <td className="px-4 py-4 font-medium truncate max-w-[120px]">{emp.teamName || 'N/A'}</td>
                      <td className="px-4 py-4">
                        <span className={`px-2.5 py-1 rounded-md text-xs font-bold ${emp.status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'}`}>
                          {emp.status || 'Active'}
                        </span>
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap">{formatDate(emp.joiningDate)}</td>
                      <td className="px-4 py-4">
                        <div className="flex items-center justify-center gap-2">
                          <button onClick={() => setViewEmployeeId(emp.id || emp.email)} className="p-1.5 bg-blue-50 text-blue-600 rounded hover:bg-blue-100 transition" title="View">
                            <Eye className="h-4 w-4" />
                          </button>
                          <button onClick={() => setEditEmployeeId(emp.id || emp.email)} className="p-1.5 bg-blue-50 text-blue-600 rounded hover:bg-blue-100 transition" title="Edit">
                            <Edit className="h-4 w-4" />
                          </button>
                          <button 
                            onClick={() => setEmployeeToDelete(emp.id || emp.email)}
                            className="p-1.5 bg-red-50 text-red-600 rounded hover:bg-red-100 transition" 
                            title="Delete"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalElements > 0 && (
          <div className="p-4 border-t border-slate-100 flex flex-col sm:flex-row justify-between items-center gap-4 bg-white rounded-b-2xl">
            <p className="text-sm text-slate-500">
              Showing {page * size + 1} to {Math.min((page + 1) * size, data.totalElements)} of {data.totalElements} entries
            </p>
            <div className="flex items-center gap-1">
              <button 
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="flex items-center gap-1 px-3 py-1.5 text-sm font-medium text-slate-500 hover:text-blue-600 disabled:opacity-50 disabled:hover:text-slate-500 transition"
              >
                <ChevronLeft className="h-4 w-4" /> Previous
              </button>
              
              <button className="w-8 h-8 flex items-center justify-center rounded-lg bg-blue-600 text-white text-sm font-bold shadow-sm">
                {page + 1}
              </button>
              {page + 1 < data.totalPages && (
                <button 
                  onClick={() => setPage(page + 1)}
                  className="w-8 h-8 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-600 text-sm font-bold hover:bg-slate-50 transition"
                >
                  {page + 2}
                </button>
              )}

              <button 
                onClick={() => setPage(Math.min(data.totalPages - 1, page + 1))}
                disabled={page >= data.totalPages - 1}
                className="flex items-center gap-1 px-3 py-1.5 text-sm font-medium text-slate-500 hover:text-blue-600 disabled:opacity-50 disabled:hover:text-slate-500 transition"
              >
                Next <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* View Modal */}
      <Modal isOpen={!!viewEmployeeId} onClose={() => setViewEmployeeId(null)} title="Employee Profile">
        {selectedEmployee ? (
          <div className="space-y-6">
            <div className="flex items-center gap-6 p-6 bg-slate-50/50 rounded-2xl border border-slate-100 shadow-sm">
              <div className="h-20 w-20 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-3xl font-bold shrink-0">
                {getInitials(selectedEmployee.name)}
              </div>
              <div>
                <h3 className="text-2xl font-bold text-slate-800">{selectedEmployee.name}</h3>
                <p className="text-slate-500">{selectedEmployee.designation || 'Employee'}</p>
                <span className={`mt-2 inline-block px-2.5 py-1 rounded-md text-xs font-bold ${selectedEmployee.status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'}`}>
                  {selectedEmployee.status || 'Active'}
                </span>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 bg-slate-50/50 rounded-2xl border border-slate-100">
                <p className="text-xs font-bold text-slate-500 uppercase mb-1">Email Address</p>
                <p className="font-semibold text-slate-800 truncate" title={selectedEmployee.email}>{selectedEmployee.email}</p>
              </div>
              <div className="p-4 bg-slate-50/50 rounded-2xl border border-slate-100">
                <p className="text-xs font-bold text-slate-500 uppercase mb-1">Phone Number</p>
                <p className="font-semibold text-slate-800 truncate">{selectedEmployee.phone || 'N/A'}</p>
              </div>
              <div className="p-4 bg-slate-50/50 rounded-2xl border border-slate-100">
                <p className="text-xs font-bold text-slate-500 uppercase mb-1">Employee Code</p>
                <p className="font-semibold text-slate-800 truncate">{selectedEmployee.employeeCode}</p>
              </div>
              <div className="p-4 bg-slate-50/50 rounded-2xl border border-slate-100">
                <p className="text-xs font-bold text-slate-500 uppercase mb-1">Joining Date</p>
                <p className="font-semibold text-slate-800 truncate">{formatDate(selectedEmployee.joiningDate)}</p>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex justify-center items-center h-48">
             <div className="h-6 w-6 rounded-full border-2 border-blue-500 border-t-transparent animate-spin"></div>
          </div>
        )}
      </Modal>

      {/* Edit Modal */}
      <Modal isOpen={!!editEmployeeId} onClose={() => setEditEmployeeId(null)} title="Edit Employee">
        {selectedEmployee ? (
          <div>
             <p className="text-slate-500 mb-6 text-sm">Make modifications to {selectedEmployee.name}'s profile below.</p>
             <form key={selectedEmployee.id} className="space-y-5" onSubmit={handleEditSubmit}>
               <div className="grid grid-cols-2 gap-4">
                 <div className="space-y-1.5">
                   <label className="text-xs font-bold text-slate-500 uppercase">Full Name</label>
                   <input type="text" name="name" defaultValue={selectedEmployee.name} className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium" />
                 </div>
                 <div className="space-y-1.5">
                   <label className="text-xs font-bold text-slate-500 uppercase">Email Address</label>
                   <input type="email" name="email" defaultValue={selectedEmployee.email} className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium" />
                 </div>
               </div>
               
               <div className="grid grid-cols-2 gap-4">
                 <div className="space-y-1.5">
                   <label className="text-xs font-bold text-slate-500 uppercase">Designation</label>
                   <input type="text" name="designation" defaultValue={selectedEmployee.designation || ''} className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium" />
                 </div>
                 <div className="space-y-1.5">
                   <label className="text-xs font-bold text-slate-500 uppercase">Status</label>
                   <select name="status" defaultValue={selectedEmployee.status} className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium text-slate-700">
                     <option value="Active">Active</option>
                     <option value="Inactive">Inactive</option>
                   </select>
                 </div>
               </div>

               <div className="grid grid-cols-2 gap-4">
                 <div className="space-y-1.5">
                   <label className="text-xs font-bold text-slate-500 uppercase">Department</label>
                   <select name="departmentId" defaultValue={selectedEmployee.departmentId} className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium text-slate-700">
                     <option value="">Select Department</option>
                     {departments.map((dept: any) => (
                       <option key={dept.value} value={dept.value}>{dept.label}</option>
                     ))}
                   </select>
                 </div>
                 <div className="space-y-1.5">
                   <label className="text-xs font-bold text-slate-500 uppercase">Team</label>
                   <select name="teamId" defaultValue={selectedEmployee.teamId} className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium text-slate-700">
                     <option value="">Select Team</option>
                     {teams.map((team: any) => (
                       <option key={team.value} value={team.value}>{team.label}</option>
                     ))}
                   </select>
                 </div>
               </div>
               
               <div className="pt-6 mt-6 border-t border-slate-100 flex justify-end gap-3">
                 <button type="button" onClick={() => setEditEmployeeId(null)} className="px-5 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-50 transition shadow-sm">Cancel</button>
                 <button type="submit" className="px-5 py-2.5 bg-blue-600 rounded-xl text-sm font-bold text-white hover:bg-blue-700 transition shadow-sm">Save Changes</button>
               </div>
             </form>
          </div>
        ) : (
          <div className="flex justify-center items-center h-48">
             <div className="h-6 w-6 rounded-full border-2 border-blue-500 border-t-transparent animate-spin"></div>
          </div>
        )}
      </Modal>
      
    </div>
  );
}
