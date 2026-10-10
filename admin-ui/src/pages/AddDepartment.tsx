import React, { useState } from 'react';
import { Building2, Save, X, Info, Tag, MapPin, User, FileText, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function AddDepartment() {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    name: '',
    departmentCode: '',
    departmentHead: '',
    description: '',
    status: 'ACTIVE'
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleToggleStatus = () => {
    setFormData(prev => ({
      ...prev,
      status: prev.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    }));
  };

  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      // Import apiClient from axios configuration
      // We will assume it's imported at the top, I'll add the import as well.
      const apiClient = (await import('../lib/axios')).default;
      
      const response = await apiClient.post('/api/admin/departments', formData);
      if (response.status === 200 || response.status === 201) {
        navigate('/departments/view');
      }
    } catch (error: any) {
      console.error('Error saving department:', error);
      alert('Failed to save department: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-3">
          <div className="p-2 bg-blue-100 text-blue-600 rounded-lg">
            <Building2 className="w-6 h-6" />
          </div>
          Add New Department
        </h1>
        <p className="text-slate-500 mt-2 text-sm">
          Create a new organizational department and set its operational parameters.
        </p>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        {/* Form Container */}
        <form onSubmit={handleSubmit}>
          
          <div className="p-6 sm:p-8 space-y-6">
            
            {/* Section 1: Basic Info */}
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-slate-800 border-b border-slate-100 pb-2 flex items-center gap-2">
                <Info className="w-5 h-5 text-blue-500" />
                Basic Information
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
                
                {/* Department Name */}
                <div className="space-y-2">
                  <label htmlFor="name" className="block text-sm font-medium text-slate-700">
                    Department Name <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Building2 className="h-4 w-4 text-slate-400" />
                    </div>
                    <input
                      type="text"
                      id="name"
                      name="name"
                      required
                      value={formData.name}
                      onChange={handleChange}
                      placeholder="e.g., Engineering"
                      className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm transition-shadow outline-none"
                    />
                  </div>
                </div>

                {/* Department Code */}
                <div className="space-y-2">
                  <label htmlFor="departmentCode" className="block text-sm font-medium text-slate-700">
                    Department Code <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Tag className="h-4 w-4 text-slate-400" />
                    </div>
                    <input
                      type="text"
                      id="departmentCode"
                      name="departmentCode"
                      required
                      value={formData.departmentCode}
                      onChange={handleChange}
                      placeholder="e.g., ENG-01"
                      className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm transition-shadow outline-none uppercase"
                    />
                  </div>
                  <p className="text-xs text-slate-500 mt-1">A unique shortcode for tracking.</p>
                </div>

              </div>
            </div>

            {/* Section 2: Operational Details */}
            <div className="space-y-4 pt-4">
              <h2 className="text-lg font-semibold text-slate-800 border-b border-slate-100 pb-2 flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-blue-500" />
                Operational Details
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
                
                {/* Department Head */}
                <div className="space-y-2">
                  <label htmlFor="departmentHead" className="block text-sm font-medium text-slate-700">
                    Department Head
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <User className="h-4 w-4 text-slate-400" />
                    </div>
                    <select
                      id="departmentHead"
                      name="departmentHead"
                      value={formData.departmentHead}
                      onChange={handleChange}
                      className="block w-full pl-10 pr-10 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm appearance-none bg-white transition-shadow outline-none"
                    >
                      <option value="" disabled>Select a Manager</option>
                      <option value="EMP001">John Doe</option>
                      <option value="EMP002">Jane Smith</option>
                      <option value="EMP003">Santhosh Kumar</option>
                      <option value="EMP004">Alice Johnson</option>
                    </select>
                    <div className="absolute inset-y-0 right-0 flex items-center px-2 pointer-events-none text-slate-500">
                      <svg className="w-4 h-4 fill-current" viewBox="0 0 20 20">
                        <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" fillRule="evenodd"></path>
                      </svg>
                    </div>
                  </div>
                </div>

              </div>

              {/* Description */}
              <div className="space-y-2 pt-2">
                <label htmlFor="description" className="block text-sm font-medium text-slate-700">
                  Description
                </label>
                <div className="relative">
                  <div className="absolute top-3 left-3 pointer-events-none">
                    <FileText className="h-4 w-4 text-slate-400" />
                  </div>
                  <textarea
                    id="description"
                    name="description"
                    rows={4}
                    value={formData.description}
                    onChange={handleChange}
                    placeholder="Briefly describe the responsibilities and goals of this department..."
                    className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm transition-shadow outline-none resize-none"
                  ></textarea>
                </div>
              </div>

              {/* Status Toggle */}
              <div className="flex items-center justify-between p-4 bg-slate-50 border border-slate-200 rounded-lg mt-4">
                <div>
                  <h3 className="text-sm font-medium text-slate-800">Department Status</h3>
                  <p className="text-xs text-slate-500 mt-0.5">Determine if this department is active and visible to employees.</p>
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

            </div>
          </div>

          {/* Form Actions / Footer */}
          <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={() => navigate('/departments/view')}
              className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg shadow-sm hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-200 transition-colors flex items-center gap-2"
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
