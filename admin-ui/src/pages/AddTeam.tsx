import React, { useState, useEffect } from 'react';
import { Users, Info, Building2 } from 'lucide-react';
import Breadcrumb from '../components/ui/Breadcrumb';
import { useNavigate } from 'react-router-dom';
import apiClient from '../lib/axios';

export default function AddTeam() {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    name: '',
    departmentId: ''
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [departments, setDepartments] = useState<{id: string, name: string}[]>([]);
  const [loading, setLoading] = useState(false);
  const [deptLoading, setDeptLoading] = useState(true);

  useEffect(() => {
    const fetchDepartments = async () => {
      try {
        const res = await apiClient.get('/api/admin/departments');
        setDepartments(res.data);
      } catch (err) {
        console.error("Failed to fetch departments", err);
      } finally {
        setDeptLoading(false);
      }
    };
    fetchDepartments();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: Record<string, string> = {};
    if (!formData.name) newErrors.name = 'Team Name is required';
    if (!formData.departmentId) newErrors.departmentId = 'Department is required';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setLoading(true);
    try {
      const response = await apiClient.post('/api/admin/teams', formData);
      if (response.status === 200 || response.status === 201) {
        navigate('/teams/view');
      }
    } catch (error: any) {
      console.error('Error saving team:', error);
      alert('Failed to save team: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-4xl mx-auto">
      <div className="-mb-2">
         <Breadcrumb items={[
          { label: 'Organization Management', href: '#' },
          { label: 'Teams', href: '/teams/view' },
          { label: 'Add Team' }
        ]} />
      </div>
      <div className="mb-6 mt-4">
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-3">
          <div className="p-2 bg-blue-100 text-blue-600 rounded-lg">
            <Users className="w-6 h-6" />
          </div>
          Add New Team
        </h1>
        <p className="text-slate-500 mt-2 text-sm">
          Create a new team and assign it to a specific department.
        </p>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <form onSubmit={handleSubmit} noValidate>
          
          <div className="p-6 sm:p-8 space-y-6">
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-slate-800 border-b border-slate-100 pb-2 flex items-center gap-2">
                <Info className="w-5 h-5 text-blue-500" />
                Team Details
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
                <div className="space-y-2">
                  <label htmlFor="name" className="block text-sm font-medium text-slate-700">
                    Team Name <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Users className="h-4 w-4 text-slate-400" />
                    </div>
                    <input
                      type="text"
                      id="name"
                      name="name"
                      required
                      value={formData.name}
                      onChange={handleChange}
                      placeholder="e.g., Frontend Developers"
                      className={`block w-full pl-10 pr-3 py-2 border rounded-lg outline-none text-sm transition-shadow ${errors.name ? 'border-red-500 focus:ring-2 focus:ring-red-500 focus:border-red-500' : 'border-slate-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500'}`}
                    />
                  </div>
                  {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name}</p>}
                </div>

                <div className="space-y-2">
                  <label htmlFor="departmentId" className="block text-sm font-medium text-slate-700">
                    Select Department <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Building2 className="h-4 w-4 text-slate-400" />
                    </div>
                    <select
                      id="departmentId"
                      name="departmentId"
                      required
                      value={formData.departmentId}
                      onChange={handleChange}
                      disabled={deptLoading}
                      className={`block w-full pl-10 pr-3 py-2 border rounded-lg appearance-none bg-white outline-none text-sm transition-shadow ${errors.departmentId ? 'border-red-500 focus:ring-2 focus:ring-red-500 focus:border-red-500' : 'border-slate-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500'}`}
                    >
                      {deptLoading ? (
                        <option value="">Loading Departments...</option>
                      ) : departments.length === 0 ? (
                        <option value="">No Departments Found</option>
                      ) : (
                        <>
                          <option value="" disabled>Select a department</option>
                          {departments.map(dept => (
                            <option key={dept.id} value={dept.id}>
                              {dept.name}
                            </option>
                          ))}
                        </>
                      )}
                    </select>
                  </div>
                  {errors.departmentId && <p className="text-red-500 text-xs mt-1">{errors.departmentId}</p>}
                </div>
              </div>
            </div>
          </div>

          <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-3">
            <button
              type="button"
              onClick={() => navigate('/teams/view')}
              className="px-5 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm flex items-center gap-2"
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Saving...
                </>
              ) : (
                'Save Team'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
