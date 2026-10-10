import { useState, useEffect } from 'react';
import { Building2, Edit2, Trash2, Search, Plus, Filter, AlertCircle, X, Save, User, FileText, Tag, CheckCircle, Info } from 'lucide-react';
import { Link } from 'react-router-dom';
import apiClient from '../lib/axios';
import Modal from '../components/ui/Modal';

interface Department {
  id: number;
  name: string;
  departmentCode: string;
  departmentHead: string;
  description: string;
  status: string;
}

export default function ViewDepartments() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Edit Modal State
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingDept, setEditingDept] = useState<Department | null>(null);
  const [editLoading, setEditLoading] = useState(false);

  const fetchDepartments = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get('/api/admin/departments');
      setDepartments(res.data);
      setError(null);
    } catch (err: any) {
      console.error(err);
      setError('Failed to fetch departments. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDepartments();
  }, []);

  const handleDelete = async (id: number, name: string) => {
    if (window.confirm(`Are you sure you want to delete the department "${name}"?`)) {
      try {
        await apiClient.delete(`/api/admin/departments/${id}`);
        setDepartments(prev => prev.filter(dept => dept.id !== id));
      } catch (err: any) {
        console.error(err);
        alert('Failed to delete department. It might be in use.');
      }
    }
  };

  const handleEditClick = (dept: Department) => {
    setEditingDept({ ...dept }); // Create a copy
    setIsEditModalOpen(true);
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    if (editingDept) {
      setEditingDept(prev => ({ ...prev!, [name]: value }));
    }
  };

  const handleEditToggleStatus = () => {
    if (editingDept) {
      setEditingDept(prev => ({
        ...prev!,
        status: prev!.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      }));
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingDept) return;
    
    setEditLoading(true);
    try {
      const res = await apiClient.put(`/api/admin/departments/${editingDept.id}`, editingDept);
      if (res.status === 200) {
        setDepartments(prev => prev.map(d => d.id === editingDept.id ? res.data : d));
        setIsEditModalOpen(false);
      }
    } catch (err: any) {
      console.error('Error updating department:', err);
      alert('Failed to update department: ' + (err.response?.data?.message || err.message));
    } finally {
      setEditLoading(false);
    }
  };

  const filteredDepartments = departments.filter(dept => 
    dept.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    dept.departmentCode.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-3">
            <div className="p-2 bg-blue-100 text-blue-600 rounded-lg">
              <Building2 className="w-6 h-6" />
            </div>
            Departments
          </h1>
          <p className="text-slate-500 mt-2 text-sm">
            Manage your organization's departments and their settings.
          </p>
        </div>
        <Link 
          to="/departments/add" 
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 shadow-sm"
        >
          <Plus className="w-4 h-4" /> Add Department
        </Link>
      </div>

      {/* Controls: Search and Filter */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 flex flex-col sm:flex-row justify-between gap-4">
        <div className="relative w-full sm:max-w-md">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-4 w-4 text-slate-400" />
          </div>
          <input
            type="text"
            placeholder="Search by name or code..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm outline-none transition-shadow"
          />
        </div>
        <button className="flex items-center justify-center gap-2 px-4 py-2 bg-slate-50 border border-slate-200 text-slate-600 rounded-lg hover:bg-slate-100 transition-colors text-sm font-medium w-full sm:w-auto">
          <Filter className="w-4 h-4" /> Filter
        </button>
      </div>

      {/* Table Container */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-slate-500 space-y-3">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="text-sm font-medium">Loading departments...</p>
          </div>
        ) : error ? (
          <div className="p-8 text-center text-red-500 space-y-3">
            <AlertCircle className="w-8 h-8 mx-auto" />
            <p className="text-sm font-medium">{error}</p>
          </div>
        ) : filteredDepartments.length === 0 ? (
          <div className="p-12 text-center text-slate-500 space-y-4">
            <Building2 className="w-12 h-12 mx-auto text-slate-300" />
            <h3 className="text-lg font-bold text-slate-700">No Departments Found</h3>
            <p className="text-sm">There are no departments matching your search criteria.</p>
            {searchTerm === '' && (
              <Link 
                to="/departments/add" 
                className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700 font-medium text-sm mt-2"
              >
                <Plus className="w-4 h-4" /> Create your first department
              </Link>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  <th className="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Department Info</th>
                  <th className="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Code</th>
                  <th className="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Manager</th>
                  <th className="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredDepartments.map((dept) => (
                  <tr key={dept.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center">
                        <div className="h-10 w-10 flex-shrink-0 bg-blue-50 text-blue-600 rounded-lg flex items-center justify-center font-bold text-sm">
                          {dept.name.charAt(0)}
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-bold text-slate-800">{dept.name}</div>
                          <div className="text-xs text-slate-500 max-w-[200px] truncate" title={dept.description}>
                            {dept.description || 'No description provided.'}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="px-2.5 py-1 text-xs font-semibold bg-slate-100 text-slate-700 rounded-md uppercase border border-slate-200">
                        {dept.departmentCode}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-700 font-medium">
                      {dept.departmentHead || <span className="text-slate-400 italic">Not Assigned</span>}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold ${
                        dept.status === 'ACTIVE' 
                          ? 'bg-emerald-100 text-emerald-700' 
                          : 'bg-slate-100 text-slate-600'
                      }`}>
                        {dept.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-medium">
                      <div className="flex justify-end gap-2">
                        <button 
                          className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          title="Edit Department"
                          onClick={() => handleEditClick(dept)}
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button 
                          className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Delete Department"
                          onClick={() => handleDelete(dept.id, dept.name)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Edit Department Modal */}
      <Modal isOpen={isEditModalOpen} onClose={() => setIsEditModalOpen(false)} title="Edit Department">
        {editingDept && (
          <div>
            <p className="text-slate-500 mb-6 text-sm">Make modifications to {editingDept.name}'s details below.</p>
            <form onSubmit={handleEditSubmit} className="space-y-5">
                <div className="grid grid-cols-2 gap-4">
                  {/* Department Name */}
                  <div className="space-y-1.5">
                    <label htmlFor="name" className="text-xs font-bold text-slate-500 uppercase">
                      Department Name
                    </label>
                      <input
                        type="text"
                        id="name"
                        name="name"
                        required
                        value={editingDept.name}
                        onChange={handleEditChange}
                        className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                      />
                  </div>

                  {/* Department Code */}
                  <div className="space-y-1.5">
                    <label htmlFor="departmentCode" className="text-xs font-bold text-slate-500 uppercase">
                      Department Code
                    </label>
                      <input
                        type="text"
                        id="departmentCode"
                        name="departmentCode"
                        required
                        value={editingDept.departmentCode}
                        onChange={handleEditChange}
                        className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium uppercase"
                      />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  {/* Department Head */}
                  <div className="space-y-1.5">
                    <label htmlFor="departmentHead" className="text-xs font-bold text-slate-500 uppercase">
                      Department Head
                    </label>
                      <select
                        id="departmentHead"
                        name="departmentHead"
                        value={editingDept.departmentHead || ''}
                        onChange={handleEditChange}
                        className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium text-slate-700"
                      >
                        <option value="" disabled>Select a Manager</option>
                        <option value="EMP001">John Doe</option>
                        <option value="EMP002">Jane Smith</option>
                        <option value="EMP003">Santhosh Kumar</option>
                        <option value="EMP004">Alice Johnson</option>
                      </select>
                  </div>
                  
                  {/* Status */}
                  <div className="space-y-1.5">
                    <label htmlFor="status" className="text-xs font-bold text-slate-500 uppercase">Status</label>
                    <select 
                       name="status" 
                       value={editingDept.status} 
                       onChange={handleEditChange} 
                       className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium text-slate-700"
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                    </select>
                  </div>
                </div>

                {/* Description */}
                <div className="space-y-1.5">
                  <label htmlFor="description" className="text-xs font-bold text-slate-500 uppercase">
                    Description
                  </label>
                    <textarea
                      id="description"
                      name="description"
                      rows={2}
                      value={editingDept.description || ''}
                      onChange={handleEditChange}
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium resize-none"
                    ></textarea>
                </div>

              <div className="pt-6 mt-6 border-t border-slate-100 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-5 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-50 transition shadow-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={editLoading}
                  className="px-5 py-2.5 bg-blue-600 rounded-xl text-sm font-bold text-white hover:bg-blue-700 transition shadow-sm disabled:opacity-70"
                >
                  {editLoading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        )}
      </Modal>

    </div>
  );
}
