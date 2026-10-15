import { useState, useEffect } from 'react';
import { 
  Building2, Edit2, Trash2, Search, Plus, Filter, AlertCircle, X, Save, 
  User, FileText, Tag, CheckCircle, Info, ChevronLeft, ChevronRight 
} from 'lucide-react';
import { Link } from 'react-router-dom';
import Breadcrumb from '../components/ui/Breadcrumb';
import apiClient from '../lib/axios';
import Modal from '../components/ui/Modal';
import { notify } from '../utils/toast';

interface Department {
  id: number;
  name: string;
  departmentCode: string;
  status: string;
}

export default function ViewDepartments() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 6;

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
      const toastId = notify.loading(`Deleting department "${name}"...`);
      try {
        await apiClient.delete(`/api/admin/departments/${id}`);
        setDepartments(prev => prev.filter(dept => dept.id !== id));
        notify.update(toastId, {
          render: `Department "${name}" deleted successfully!`,
          type: 'delete',
          autoClose: 3000
        });
      } catch (err: any) {
        console.error(err);
        notify.update(toastId, {
          render: 'Failed to delete department. It might be in use.',
          type: 'error',
          autoClose: 4000
        });
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
    const toastId = notify.loading(`Updating department "${editingDept.name}"...`);
    try {
      const res = await apiClient.put(`/api/admin/departments/${editingDept.id}`, editingDept);
      if (res.status === 200) {
        setDepartments(prev => prev.map(d => d.id === editingDept.id ? res.data : d));
        setIsEditModalOpen(false);
        notify.update(toastId, {
          render: `Department "${editingDept.name}" updated successfully! ✨`,
          type: 'success',
          autoClose: 3000
        });
      }
    } catch (err: any) {
      console.error('Error updating department:', err);
      notify.update(toastId, {
        render: 'Failed to update department: ' + (err.response?.data?.message || err.message),
        type: 'error',
        autoClose: 4000
      });
    } finally {
      setEditLoading(false);
    }
  };

  const filteredDepartments = departments.filter(dept => {
    const search = searchTerm.toLowerCase();
    const nameMatch = (dept.name || '').toLowerCase().includes(search);
    const codeMatch = (dept.departmentCode || '').toLowerCase().includes(search);
    return nameMatch || codeMatch;
  });

  // Pagination calculation - 6 items per page
  const totalItems = filteredDepartments.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = Math.min(startIndex + itemsPerPage, totalItems);
  const paginatedDepartments = filteredDepartments.slice(startIndex, endIndex);

  // Reset to page 1 if current page exceeds total pages
  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(1);
    }
  }, [searchTerm, totalPages, currentPage]);

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="-mb-2">
        <Breadcrumb items={[
          { label: 'Organization Management', href: '#' },
          { label: 'Departments' }
        ]} />
      </div>

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100 flex items-center gap-3">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-lg">
              <Building2 className="w-6 h-6" />
            </div>
            Departments
          </h1>
          <p className="text-slate-500 dark:text-slate-400 mt-2 text-sm">
            Manage your organization's departments and their settings.
          </p>
        </div>
        <Link
          to="/departments/add"
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium shadow-sm whitespace-nowrap"
        >
          <Plus className="w-4 h-4" />
          Add New Department
        </Link>
      </div>

      {/* Controls: Search and Filter */}
      <div className="bg-white dark:bg-[#1E293B] p-4 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700/80 flex flex-col sm:flex-row justify-between gap-4 transition-colors">
        <div className="relative w-full sm:max-w-md">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-4 w-4 text-slate-400 dark:text-slate-500" />
          </div>
          <input
            type="text"
            placeholder="Search by name or code..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="block w-full pl-10 pr-3 py-2 border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm outline-none transition-shadow"
          />
        </div>
        <button className="flex items-center justify-center gap-2 px-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors text-sm font-medium w-full sm:w-auto">
          <Filter className="w-4 h-4" /> Filter
        </button>
      </div>

      {/* Table Container */}
      <div className="bg-white dark:bg-[#1E293B] rounded-xl shadow-sm border border-slate-200 dark:border-slate-700/80 overflow-hidden transition-colors">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400 space-y-3">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="text-sm font-medium">Loading departments...</p>
          </div>
        ) : error ? (
          <div className="p-8 text-center text-red-500 space-y-3">
            <AlertCircle className="w-8 h-8 mx-auto" />
            <p className="text-sm font-medium">{error}</p>
          </div>
        ) : filteredDepartments.length === 0 ? (
          <div className="p-12 text-center text-slate-500 dark:text-slate-400 space-y-4">
            <Building2 className="w-12 h-12 mx-auto text-slate-300 dark:text-slate-600" />
            <h3 className="text-lg font-bold text-slate-700 dark:text-slate-200">No Departments Found</h3>
            <p className="text-sm">There are no departments matching your search criteria.</p>
            {searchTerm === '' && (
              <Link 
                to="/departments/add" 
                className="inline-flex items-center gap-2 text-blue-600 dark:text-blue-400 hover:underline font-medium text-sm mt-2"
              >
                <Plus className="w-4 h-4" /> Create your first department
              </Link>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto thin-scrollbar">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/70 dark:bg-slate-800/60 border-b border-slate-200 dark:border-slate-700/80">
                  <th className="px-4 sm:px-6 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Department Info</th>
                  <th className="px-4 sm:px-6 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Code</th>
                  <th className="px-4 sm:px-6 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Status</th>
                  <th className="px-4 sm:px-6 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700/70">
                {paginatedDepartments.map((dept) => (
                  <tr key={dept.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors">
                    <td className="px-4 sm:px-6 py-3.5 sm:py-4">
                      <div className="flex items-center gap-3">
                        <div className="h-9 w-9 sm:h-10 sm:w-10 flex-shrink-0 bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-lg flex items-center justify-center font-bold text-xs sm:text-sm">
                          {dept.name ? dept.name.charAt(0).toUpperCase() : 'D'}
                        </div>
                        <div>
                          <div className="text-sm font-bold text-slate-800 dark:text-slate-100">{dept.name}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 sm:px-6 py-3.5 sm:py-4">
                      {dept.departmentCode ? (
                        <span className="px-2.5 py-1 text-xs font-semibold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-md uppercase border border-slate-200 dark:border-slate-700">
                          {dept.departmentCode}
                        </span>
                      ) : (
                        <span className="text-xs text-slate-400 dark:text-slate-500 italic">None</span>
                      )}
                    </td>
                    <td className="px-4 sm:px-6 py-3.5 sm:py-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        dept.status === 'ACTIVE' 
                          ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400 border border-emerald-200/50 dark:border-emerald-800/40' 
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-700'
                      }`}>
                        {dept.status || 'ACTIVE'}
                      </span>
                    </td>
                    <td className="px-4 sm:px-6 py-3.5 sm:py-4 text-right text-sm font-medium whitespace-nowrap">
                      <div className="flex justify-end gap-1 sm:gap-2">
                        <button 
                          className="p-1.5 sm:p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded-lg transition-colors inline-flex items-center justify-center"
                          title="Edit Department"
                          onClick={() => handleEditClick(dept)}
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button 
                          className="p-1.5 sm:p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors inline-flex items-center justify-center"
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

        {/* Pagination Bar - Showing only 6 per page */}
        {!loading && !error && totalItems > 0 && (
          <div className="p-4 border-t border-slate-100 dark:border-slate-700/80 flex flex-col sm:flex-row justify-between items-center gap-4 bg-white dark:bg-[#1E293B]">
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Showing <span className="font-semibold text-slate-700 dark:text-slate-200">{startIndex + 1}</span> to{' '}
              <span className="font-semibold text-slate-700 dark:text-slate-200">{endIndex}</span> of{' '}
              <span className="font-semibold text-slate-700 dark:text-slate-200">{totalItems}</span> departments
            </p>
            <div className="flex items-center gap-1.5">
              <button 
                onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                disabled={currentPage === 1}
                className="flex items-center gap-1 px-3 py-1.5 text-xs sm:text-sm font-medium text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer"
              >
                <ChevronLeft className="h-4 w-4" /> Previous
              </button>
              
              {Array.from({ length: totalPages }, (_, idx) => idx + 1).map((pg) => (
                <button
                  key={pg}
                  onClick={() => setCurrentPage(pg)}
                  className={`w-8 h-8 flex items-center justify-center rounded-lg text-xs sm:text-sm font-bold transition cursor-pointer ${
                    currentPage === pg
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700'
                  }`}
                >
                  {pg}
                </button>
              ))}

              <button 
                onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                disabled={currentPage >= totalPages}
                className="flex items-center gap-1 px-3 py-1.5 text-xs sm:text-sm font-medium text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer"
              >
                Next <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Edit Department Modal */}
      <Modal isOpen={isEditModalOpen} onClose={() => setIsEditModalOpen(false)} title="Edit Department">
        {editingDept && (
          <div>
            <p className="text-slate-500 dark:text-slate-400 mb-6 text-sm">Make modifications to {editingDept.name}'s details below.</p>
            <form onSubmit={handleEditSubmit} className="space-y-5">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Department Name */}
                  <div className="space-y-1.5">
                    <label htmlFor="name" className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase">
                      Department Name
                    </label>
                    <input
                      type="text"
                      id="name"
                      name="name"
                      required
                      value={editingDept.name}
                      onChange={handleEditChange}
                      className="w-full px-4 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-900 dark:text-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                    />
                  </div>

                  {/* Department Code */}
                  <div className="space-y-1.5">
                    <label htmlFor="departmentCode" className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase">
                      Department Code
                    </label>
                    <input
                      type="text"
                      id="departmentCode"
                      name="departmentCode"
                      required
                      value={editingDept.departmentCode}
                      onChange={handleEditChange}
                      className="w-full px-4 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-900 dark:text-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium uppercase"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-4">
                  {/* Status */}
                  <div className="space-y-1.5">
                    <label htmlFor="status" className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase">Status</label>
                    <select 
                       name="status" 
                       value={editingDept.status} 
                       onChange={handleEditChange} 
                       className="w-full px-4 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium text-slate-700 dark:text-slate-200"
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                    </select>
                  </div>
                </div>

              <div className="pt-6 mt-6 border-t border-slate-100 dark:border-slate-700/80 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-5 py-2.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700/60 transition shadow-sm"
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
