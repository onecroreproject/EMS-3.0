import React, { useState } from 'react';
import { Building2, Save, X, Info, Tag, MapPin, User, FileText, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Breadcrumb from '../components/ui/Breadcrumb';
import { notify } from '../utils/toast';

export default function AddDepartment() {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    name: '',
    departmentCode: '',
    status: 'ACTIVE'
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleToggleStatus = () => {
    setFormData(prev => ({
      ...prev,
      status: prev.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: Record<string, string> = {};
    if (!formData.name) newErrors.name = 'Department Name is required';
    if (!formData.departmentCode) newErrors.departmentCode = 'Department Code is required';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setLoading(true);
    const toastId = notify.loading('Creating department...');
    try {
      const apiClient = (await import('../lib/axios')).default;
      
      const response = await apiClient.post('/api/admin/departments', formData);
      if (response.status === 200 || response.status === 201) {
        notify.update(toastId, {
          render: `Department "${formData.name}" created successfully! 🎉`,
          type: 'success',
          autoClose: 3000
        });
        navigate('/departments/view');
      }
    } catch (error: any) {
      console.error('Error saving department:', error);
      notify.update(toastId, {
        render: error.response?.data?.message || 'Failed to save department',
        type: 'error',
        autoClose: 4000
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-4xl mx-auto">
      {/* Breadcrumb Navigation */}
      <div className="-mb-2">
        <Breadcrumb items={[
          { label: 'Organization Management', href: '#' },
          { label: 'Departments', href: '/departments/view' },
          { label: 'Add Department' }
        ]} />
      </div>

      {/* Header */}
      <div className="mb-6 mt-4">
        <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100 flex items-center gap-3">
          <div className="p-2 bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-lg">
            <Building2 className="w-6 h-6" />
          </div>
          Add New Department
        </h1>
        <p className="text-slate-500 dark:text-slate-400 mt-2 text-sm">
          Create a new organizational department and set its operational parameters.
        </p>
      </div>

      <div className="bg-white dark:bg-[#1E293B] rounded-xl shadow-sm border border-slate-200 dark:border-slate-700/80 overflow-hidden transition-colors">
        {/* Form Container */}
        <form onSubmit={handleSubmit} noValidate>
          
          <div className="p-6 sm:p-8 space-y-6">
            
            {/* Section 1: Basic Info */}
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-100 border-b border-slate-100 dark:border-slate-700/80 pb-2 flex items-center gap-2">
                <Info className="w-5 h-5 text-blue-500" />
                Basic Information
              </h2>

              {/* Status Toggle moved to top */}
              <div className="flex items-center justify-between p-4 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/80 rounded-lg mb-6">
                <div>
                  <h3 className="text-sm font-medium text-slate-800 dark:text-slate-200">Department Status</h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Determine if this department is active and visible to employees.</p>
                </div>
                <button
                  type="button"
                  onClick={handleToggleStatus}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                    formData.status === 'ACTIVE' ? 'bg-emerald-500' : 'bg-slate-300'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      formData.status === 'ACTIVE' ? 'translate-x-6' : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
                
                {/* Department Name */}
                <div className="space-y-2">
                  <label htmlFor="name" className="block text-sm font-medium text-slate-700 dark:text-slate-300">
                    Department Name <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Building2 className="h-4 w-4 text-slate-400 dark:text-slate-500" />
                    </div>
                    <input
                      type="text"
                      id="name"
                      name="name"
                      required
                      value={formData.name}
                      onChange={handleChange}
                      placeholder="e.g., Engineering"
                      className={`block w-full pl-10 pr-3 py-2 border rounded-lg outline-none text-sm transition-shadow bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 ${errors.name ? 'border-red-500 focus:ring-2 focus:ring-red-500 focus:border-red-500' : 'border-slate-300 dark:border-slate-600 focus:ring-2 focus:ring-blue-500 focus:border-blue-500'}`}
                    />
                  </div>
                  {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name}</p>}
                </div>

                {/* Department Code */}
                <div className="space-y-2">
                  <label htmlFor="departmentCode" className="block text-sm font-medium text-slate-700 dark:text-slate-300">
                    Department Code <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Tag className="h-4 w-4 text-slate-400 dark:text-slate-500" />
                    </div>
                    <input
                      type="text"
                      id="departmentCode"
                      name="departmentCode"
                      required
                      value={formData.departmentCode}
                      onChange={handleChange}
                      placeholder="e.g., ENG-01"
                      className={`block w-full pl-10 pr-3 py-2 border rounded-lg outline-none text-sm transition-shadow uppercase bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 ${errors.departmentCode ? 'border-red-500 focus:ring-2 focus:ring-red-500 focus:border-red-500' : 'border-slate-300 dark:border-slate-600 focus:ring-2 focus:ring-blue-500 focus:border-blue-500'}`}
                    />
                  </div>
                  {errors.departmentCode ? (
                    <p className="text-red-500 text-xs mt-1">{errors.departmentCode}</p>
                  ) : (
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">A unique shortcode for tracking.</p>
                  )}
                </div>

              </div>
            </div>

          </div>

          {/* Form Actions / Footer */}
          <div className="px-6 py-4 bg-slate-50/70 dark:bg-slate-800/60 border-t border-slate-200 dark:border-slate-700/80 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={() => navigate('/departments/view')}
              className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-lg shadow-sm hover:bg-slate-50 dark:hover:bg-slate-700/60 focus:outline-none focus:ring-2 focus:ring-slate-200 transition-colors flex items-center gap-2"
            >
              <X className="w-4 h-4" />
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors flex items-center gap-2 disabled:opacity-70"
            >
              <Save className="w-4 h-4" />
              {loading ? 'Saving...' : 'Save Department'}
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}
