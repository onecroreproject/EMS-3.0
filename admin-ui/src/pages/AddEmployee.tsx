import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../lib/axios';
import { Save, X, User, Users, UserPlus, Mail, Phone, Lock, Briefcase, Calendar as CalendarIcon, Building, MapPin, Activity, ShieldCheck, ChevronRight } from 'lucide-react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import CustomSelect from '../components/ui/CustomSelect';
import Breadcrumb from '../components/ui/Breadcrumb';
import { notify } from '../utils/toast';

export default function AddEmployee() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '', email: '', phone: '',
    password: '', employeeCode: '', designation: '',
    department: '', team: '',
    permanentAddress: '', currentAddress: '', bloodGroup: '',
    alternativePhone: '', altPhoneOwner: '',
    relationType: '', status: 'Active'
  });
  
  const [joiningDate, setJoiningDate] = useState<Date | null>(null);
  const [dateOfBirth, setDateOfBirth] = useState<Date | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  
  const [departments, setDepartments] = useState<{label: string, value: string}[]>([]);
  const [teams, setTeams] = useState<{label: string, value: string}[]>([]);

  useEffect(() => {
    const fetchDropdowns = async () => {
      try {
        const [deptRes, teamRes] = await Promise.all([
          apiClient.get('/api/admin/departments'),
          apiClient.get('/api/admin/teams')
        ]);
        setDepartments(deptRes.data.map((d: any) => ({ label: d.name, value: d.id })));
        setTeams(teamRes.data.map((t: any) => ({ label: t.name, value: t.id })));
      } catch (err) {
        console.error("Failed to fetch dropdowns:", err);
      }
    };
    fetchDropdowns();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (errors[e.target.name]) {
      setErrors(prev => ({ ...prev, [e.target.name]: '' }));
    }
  };

  const handleSelectChange = (name: string, value: string) => {
    setFormData({ ...formData, [name]: value });
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: Record<string, string> = {};
    if (!formData.name) newErrors.name = 'Full Name is required';
    if (!dateOfBirth) newErrors.dateOfBirth = 'Date of Birth is required';
    if (!formData.employeeCode) newErrors.employeeCode = 'Employee Code is required';
    if (!formData.department) newErrors.department = 'Department is required';
    if (!formData.team) newErrors.team = 'Team is required';
    if (!formData.designation) newErrors.designation = 'Designation is required';
    if (!joiningDate) newErrors.joiningDate = 'Joining Date is required';
    if (!formData.email) newErrors.email = 'Email Address is required';
    if (!formData.phone) newErrors.phone = 'Phone Number is required';
    if (!formData.password) newErrors.password = 'System Password is required';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // Construct payload with required field mappings
    const payload = {
      ...formData,
      departmentId: formData.department,
      teamId: formData.team,
      dateOfBirth: dateOfBirth ? dateOfBirth.toISOString().split('T')[0] : null,
      joiningDate: joiningDate ? joiningDate.toISOString().split('T')[0] : null,
    };

    const toastId = notify.loading(`Creating employee "${payload.name}"...`);
    try {
      const response = await apiClient.post('/api/admin/employees', payload);

      if (response.status === 200 || response.status === 201) {
        notify.update(toastId, {
          render: `Employee "${payload.name}" created successfully! 🎉`,
          type: 'success',
          autoClose: 3000
        });
        navigate('/employees/view');
      }
    } catch (error: any) {
      console.error("Error creating employee:", error);
      const errorData = error.response?.data;
      const errorMsg = typeof errorData === 'object' ? (errorData.message || JSON.stringify(errorData)) : errorData || error.message;
      notify.update(toastId, {
        render: `Failed to create employee: ${errorMsg}`,
        type: 'error',
        autoClose: 4000
      });
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 pb-12">
      
      {/* Premium Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <Breadcrumb items={[
            { label: 'Employees', href: '/employees/view' },
            { label: 'Add Employee' }
          ]} />
          <h2 className="text-3xl font-bold text-slate-800 dark:text-white flex items-center gap-3">
            <UserPlus className="h-8 w-8 text-blue-500" /> Add New Employee
          </h2>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">Fill out the information below to register a new team member.</p>
        </div>
      </div>

      <form className="space-y-6" onSubmit={handleSubmit} noValidate>
        
        {/* Section 1: Personal Information */}
        <div className="bg-white dark:bg-[#1E293B] rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700/80 relative z-30 transition-colors">
          <div className="border-b border-slate-100 dark:border-slate-700/80 bg-slate-50/70 dark:bg-slate-800/60 px-6 py-4 rounded-t-2xl flex items-center gap-3">
            <div className="bg-orange-100 dark:bg-orange-500/20 p-2 rounded-lg text-[#F96D3E] dark:text-orange-400">
              <User className="h-5 w-5" />
            </div>
            <h3 className="text-lg font-bold text-slate-800 dark:text-slate-100">Personal Information</h3>
          </div>
          
          <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
            
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Full Name <span className="text-red-500">*</span></label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 dark:text-slate-500">
                  <User className="h-4 w-4" />
                </div>
                <input 
                  type="text" 
                  name="name" 
                  required 
                  placeholder="John Doe" 
                  onChange={handleChange} 
                  className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.name ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-[#F96D3E] focus:border-transparent'}`} 
                />
              </div>
              {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Date of Birth <span className="text-red-500">*</span></label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 dark:text-slate-500 z-10">
                  <CalendarIcon className="h-4 w-4" />
                </div>
                <DatePicker 
                  selected={dateOfBirth} 
                  onChange={(date: Date | null) => {
                    setDateOfBirth(date);
                    if (errors.dateOfBirth) setErrors(prev => ({ ...prev, dateOfBirth: '' }));
                  }} 
                  dateFormat="dd MMM yyyy"
                  placeholderText="Select Date"
                  className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.dateOfBirth ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-[#F96D3E] focus:border-transparent'}`}
                  wrapperClassName="w-full"
                  showMonthDropdown
                  showYearDropdown
                  dropdownMode="scroll"
                  scrollableYearDropdown
                  yearDropdownItemNumber={100}
                />
              </div>
              {errors.dateOfBirth && <p className="text-red-500 text-xs mt-1">{errors.dateOfBirth}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Blood Group <span className="text-slate-400 dark:text-slate-500 font-normal normal-case">(Optional)</span></label>
              <CustomSelect 
                value={formData.bloodGroup}
                onChange={(val) => handleSelectChange('bloodGroup', val)}
                placeholder="Select Blood Group"
                icon={<Activity className="h-4 w-4" />}
                themeColor="orange"
                size="lg"
                options={[
                  { label: 'A+', value: 'A+' }, { label: 'A-', value: 'A-' },
                  { label: 'B+', value: 'B+' }, { label: 'B-', value: 'B-' },
                  { label: 'O+', value: 'O+' }, { label: 'O-', value: 'O-' },
                  { label: 'AB+', value: 'AB+' }, { label: 'AB-', value: 'AB-' }
                ]}
              />
            </div>

          </div>
        </div>

        {/* Section 2: Professional Details */}
        <div className="bg-white dark:bg-[#1E293B] rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700/80 relative z-20 transition-colors">
          <div className="border-b border-slate-100 dark:border-slate-700/80 bg-slate-50/70 dark:bg-slate-800/60 px-6 py-4 rounded-t-2xl flex items-center gap-3">
            <div className="bg-blue-100 dark:bg-blue-500/20 p-2 rounded-lg text-blue-600 dark:text-blue-400">
              <Briefcase className="h-5 w-5" />
            </div>
            <h3 className="text-lg font-bold text-slate-800 dark:text-slate-100">Professional Details</h3>
          </div>
          
          <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
            
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Employee Code <span className="text-red-500">*</span></label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 dark:text-slate-500">
                  <ShieldCheck className="h-4 w-4" />
                </div>
                <input 
                  type="text" 
                  name="employeeCode" 
                  required 
                  placeholder="EMP-001" 
                  onChange={handleChange} 
                  className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.employeeCode ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-blue-500 focus:border-transparent'}`} 
                />
              </div>
              {errors.employeeCode && <p className="text-red-500 text-xs mt-1">{errors.employeeCode}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Department <span className="text-red-500">*</span></label>
              <div className={errors.department ? 'ring-2 ring-red-500 rounded-xl' : ''}>
                <CustomSelect 
                  value={formData.department}
                  onChange={(val) => handleSelectChange('department', val)}
                  placeholder="Select Department"
                  icon={<Building className="h-4 w-4" />}
                  themeColor="blue"
                  size="lg"
                  options={departments}
                />
              </div>
              {errors.department && <p className="text-red-500 text-xs mt-1">{errors.department}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Team <span className="text-red-500">*</span></label>
              <div className={errors.team ? 'ring-2 ring-red-500 rounded-xl' : ''}>
                <CustomSelect 
                  value={formData.team}
                  onChange={(val) => handleSelectChange('team', val)}
                  placeholder="Select Team"
                  icon={<Users className="h-4 w-4" />}
                  themeColor="blue"
                  size="lg"
                  options={teams}
                />
              </div>
              {errors.team && <p className="text-red-500 text-xs mt-1">{errors.team}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Designation <span className="text-red-500">*</span></label>
              <input 
                type="text" 
                name="designation" 
                required 
                placeholder="e.g. Senior Developer / Intern" 
                onChange={handleChange} 
                className={`w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.designation ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-blue-500 focus:border-transparent'}`} 
              />
              {errors.designation && <p className="text-red-500 text-xs mt-1">{errors.designation}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Joining Date <span className="text-red-500">*</span></label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 dark:text-slate-500 z-10">
                  <CalendarIcon className="h-4 w-4" />
                </div>
                <DatePicker 
                  selected={joiningDate} 
                  onChange={(date: Date | null) => {
                    setJoiningDate(date);
                    if (errors.joiningDate) setErrors(prev => ({ ...prev, joiningDate: '' }));
                  }} 
                  dateFormat="dd MMM yyyy"
                  placeholderText="Select Date"
                  className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.joiningDate ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-blue-500 focus:border-transparent'}`}
                  wrapperClassName="w-full"
                  showMonthDropdown
                  showYearDropdown
                  dropdownMode="scroll"
                  scrollableYearDropdown
                  yearDropdownItemNumber={100}
                />
              </div>
              {errors.joiningDate && <p className="text-red-500 text-xs mt-1">{errors.joiningDate}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Account Status</label>
              <CustomSelect 
                value={formData.status}
                onChange={(val) => handleSelectChange('status', val)}
                placeholder="Select Status"
                icon={<div className={`h-2 w-2 rounded-full ${formData.status === 'Active' ? 'bg-green-500' : 'bg-slate-400'}`}></div>}
                themeColor="blue"
                size="lg"
                options={[
                  { label: 'Active Employee', value: 'Active' },
                  { label: 'Inactive', value: 'Inactive' }
                ]}
              />
            </div>

          </div>
        </div>

        {/* Section 3: Contact & Security */}
        <div className="bg-white dark:bg-[#1E293B] rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700/80 relative z-10 transition-colors">
          <div className="border-b border-slate-100 dark:border-slate-700/80 bg-slate-50/70 dark:bg-slate-800/60 px-6 py-4 rounded-t-2xl flex items-center gap-3">
            <div className="bg-purple-100 dark:bg-purple-500/20 p-2 rounded-lg text-purple-600 dark:text-purple-400">
              <Mail className="h-5 w-5" />
            </div>
            <h3 className="text-lg font-bold text-slate-800 dark:text-slate-100">Contact & Security</h3>
          </div>
          
          <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
            
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Email Address <span className="text-red-500">*</span></label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 dark:text-slate-500">
                  <Mail className="h-4 w-4" />
                </div>
                <input 
                  type="email" 
                  name="email" 
                  required 
                  placeholder="john@company.com" 
                  onChange={handleChange} 
                  className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.email ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-purple-500 focus:border-transparent'}`} 
                />
              </div>
              {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Phone Number <span className="text-red-500">*</span></label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 dark:text-slate-500">
                  <Phone className="h-4 w-4" />
                </div>
                <input 
                  type="text" 
                  name="phone" 
                  required 
                  placeholder="+1 (555) 000-0000" 
                  onChange={handleChange} 
                  className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.phone ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-purple-500 focus:border-transparent'}`} 
                />
              </div>
              {errors.phone && <p className="text-red-500 text-xs mt-1">{errors.phone}</p>}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">System Password <span className="text-red-500">*</span></label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 dark:text-slate-500">
                  <Lock className="h-4 w-4" />
                </div>
                <input 
                  type="password" 
                  name="password" 
                  required 
                  placeholder="••••••••" 
                  onChange={handleChange} 
                  className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border rounded-xl focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-900 transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 ${errors.password ? 'border-red-500 focus:ring-red-500' : 'border-slate-200 dark:border-slate-700 focus:ring-purple-500 focus:border-transparent'}`} 
                />
              </div>
              {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password}</p>}
            </div>

            <div className="space-y-1.5 lg:col-span-3">
              <label className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Addresses <span className="text-slate-400 dark:text-slate-500 font-normal normal-case">(Optional)</span></label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 pt-3 pointer-events-none text-slate-400 dark:text-slate-500">
                    <MapPin className="h-4 w-4" />
                  </div>
                  <textarea 
                    name="currentAddress" 
                    rows={2} 
                    placeholder="Current Address" 
                    onChange={handleChange} 
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white dark:focus:bg-slate-900 focus:border-transparent transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 resize-none"
                  ></textarea>
                </div>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 pt-3 pointer-events-none text-slate-400 dark:text-slate-500">
                    <MapPin className="h-4 w-4" />
                  </div>
                  <textarea 
                    name="permanentAddress" 
                    rows={2} 
                    placeholder="Permanent Address" 
                    onChange={handleChange} 
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white dark:focus:bg-slate-900 focus:border-transparent transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 resize-none"
                  ></textarea>
                </div>
              </div>
            </div>

            {/* Emergency Contact */}
            <div className="space-y-1.5 lg:col-span-3 pt-4 mt-4 border-t border-slate-100 dark:border-slate-700/80">
              <h4 className="text-sm font-bold text-slate-700 dark:text-slate-200 mb-4">Emergency Contact <span className="text-slate-400 dark:text-slate-500 font-normal normal-case text-xs">(Optional)</span></h4>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <input 
                  type="text" 
                  name="altPhoneOwner" 
                  placeholder="Contact Name" 
                  onChange={handleChange} 
                  className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white dark:focus:bg-slate-900 focus:border-transparent transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500" 
                />
                <input 
                  type="text" 
                  name="relationType" 
                  placeholder="Relationship (e.g. Spouse)" 
                  onChange={handleChange} 
                  className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white dark:focus:bg-slate-900 focus:border-transparent transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500" 
                />
                <input 
                  type="text" 
                  name="alternativePhone" 
                  placeholder="Emergency Phone" 
                  onChange={handleChange} 
                  className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white dark:focus:bg-slate-900 focus:border-transparent transition-all text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500" 
                />
              </div>
            </div>

          </div>
        </div>

        {/* Action Buttons at Bottom */}
        <div className="flex justify-end gap-3 pt-4">
          <button 
            type="button" 
            onClick={() => navigate('/employees/view')} 
            className="flex items-center gap-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 px-6 py-2.5 rounded-xl text-sm font-bold hover:bg-slate-50 dark:hover:bg-slate-700 transition shadow-xs cursor-pointer"
          >
            <X className="h-4 w-4" /> Cancel
          </button>
          <button 
            type="submit" 
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-xl text-sm font-bold transition shadow-sm cursor-pointer"
          >
            <Save className="h-4 w-4" /> Save Profile
          </button>
        </div>

      </form>
    </div>
  );
}
