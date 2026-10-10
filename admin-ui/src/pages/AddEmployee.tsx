import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../lib/axios';
import { Save, X, User, Users, UserPlus, Mail, Phone, Lock, Briefcase, Calendar as CalendarIcon, Building, MapPin, Activity, ShieldCheck, ChevronRight } from 'lucide-react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import CustomSelect from '../components/ui/CustomSelect';
import Breadcrumb from '../components/ui/Breadcrumb';

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
  };

  const handleSelectChange = (name: string, value: string) => {
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Construct payload with required field mappings
    const payload = {
      ...formData,
      departmentId: formData.department,
      teamId: formData.team,
      dateOfBirth: dateOfBirth ? dateOfBirth.toISOString().split('T')[0] : null,
      joiningDate: joiningDate ? joiningDate.toISOString().split('T')[0] : null,
    };

    try {
      const response = await apiClient.post('/api/admin/employees', payload);

      if (response.status === 200 || response.status === 201) {
        navigate('/employees/view');
      }
    } catch (error: any) {
      console.error("Error creating employee:", error);
      alert(`Failed to create employee: ${error.response?.data || error.message}`);
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
          <h2 className="text-3xl font-bold text-slate-800 flex items-center gap-3">
            <UserPlus className="h-8 w-8 text-blue-500" /> Add New Employee
          </h2>
          <p className="text-slate-500 text-sm mt-1">Fill out the information below to register a new team member.</p>
        </div>
      </div>

      <form className="space-y-6" onSubmit={handleSubmit}>
        
        {/* Section 1: Personal Information */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative z-30">
          <div className="border-b border-slate-100 bg-slate-50/50 px-6 py-4 rounded-t-2xl flex items-center gap-3">
            <div className="bg-orange-100 p-2 rounded-lg text-[#F96D3E]">
              <User className="h-5 w-5" />
            </div>
            <h3 className="text-lg font-bold text-slate-800">Personal Information</h3>
          </div>
          
          <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
            
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Full Name</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <User className="h-4 w-4" />
                </div>
                <input type="text" name="name" placeholder="John Doe" onChange={handleChange} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#F96D3E] focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Date of Birth</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 z-10">
                  <CalendarIcon className="h-4 w-4" />
                </div>
                <DatePicker 
                  selected={dateOfBirth} 
                  onChange={(date) => setDateOfBirth(date)} 
                  dateFormat="dd MMM yyyy"
                  placeholderText="Select Date"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#F96D3E] focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700"
                  wrapperClassName="w-full"
                  showMonthDropdown
                  showYearDropdown
                  dropdownMode="scroll"
                  scrollableYearDropdown
                  yearDropdownItemNumber={100}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Blood Group</label>
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
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative z-20">
          <div className="border-b border-slate-100 bg-slate-50/50 px-6 py-4 rounded-t-2xl flex items-center gap-3">
            <div className="bg-blue-100 p-2 rounded-lg text-blue-600">
              <Briefcase className="h-5 w-5" />
            </div>
            <h3 className="text-lg font-bold text-slate-800">Professional Details</h3>
          </div>
          
          <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
            
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Employee Code</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <ShieldCheck className="h-4 w-4" />
                </div>
                <input type="text" name="employeeCode" placeholder="EMP-001" onChange={handleChange} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Department</label>
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

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Team</label>
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

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Designation</label>
              <input type="text" name="designation" placeholder="e.g. Senior Developer" onChange={handleChange} className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Joining Date</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 z-10">
                  <CalendarIcon className="h-4 w-4" />
                </div>
                <DatePicker 
                  selected={joiningDate} 
                  onChange={(date) => setJoiningDate(date)} 
                  dateFormat="dd MMM yyyy"
                  placeholderText="Select Date"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700"
                  wrapperClassName="w-full"
                  showMonthDropdown
                  showYearDropdown
                  dropdownMode="scroll"
                  scrollableYearDropdown
                  yearDropdownItemNumber={100}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Account Status</label>
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
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 relative z-10">
          <div className="border-b border-slate-100 bg-slate-50/50 px-6 py-4 rounded-t-2xl flex items-center gap-3">
            <div className="bg-purple-100 p-2 rounded-lg text-purple-600">
              <Mail className="h-5 w-5" />
            </div>
            <h3 className="text-lg font-bold text-slate-800">Contact & Security</h3>
          </div>
          
          <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
            
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Email Address</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Mail className="h-4 w-4" />
                </div>
                <input type="email" name="email" placeholder="john@company.com" onChange={handleChange} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Phone Number</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Phone className="h-4 w-4" />
                </div>
                <input type="text" name="phone" placeholder="+1 (555) 000-0000" onChange={handleChange} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">System Password</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="h-4 w-4" />
                </div>
                <input type="password" name="password" placeholder="••••••••" onChange={handleChange} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
              </div>
            </div>

            <div className="space-y-1.5 lg:col-span-3">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Addresses</label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 pt-3 pointer-events-none text-slate-400">
                    <MapPin className="h-4 w-4" />
                  </div>
                  <textarea name="currentAddress" rows={2} placeholder="Current Address" onChange={handleChange} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700 resize-none"></textarea>
                </div>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 pt-3 pointer-events-none text-slate-400">
                    <MapPin className="h-4 w-4" />
                  </div>
                  <textarea name="permanentAddress" rows={2} placeholder="Permanent Address" onChange={handleChange} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700 resize-none"></textarea>
                </div>
              </div>
            </div>

            {/* Emergency Contact */}
            <div className="space-y-1.5 lg:col-span-3 pt-4 mt-4 border-t border-slate-100">
              <h4 className="text-sm font-bold text-slate-700 mb-4">Emergency Contact</h4>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <input type="text" name="altPhoneOwner" placeholder="Contact Name" onChange={handleChange} className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
                <input type="text" name="relationType" placeholder="Relationship (e.g. Spouse)" onChange={handleChange} className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
                <input type="text" name="alternativePhone" placeholder="Emergency Phone" onChange={handleChange} className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white focus:border-transparent transition-all text-sm font-medium text-slate-700" />
              </div>
            </div>

          </div>
        </div>

        {/* Action Buttons at Bottom */}
        <div className="flex justify-end gap-3 pt-4">
          <button type="button" onClick={() => navigate('/employees/view')} className="flex items-center gap-2 bg-white border border-slate-200 text-slate-600 px-6 py-2.5 rounded-xl text-sm font-bold hover:bg-slate-50 transition shadow-sm">
            <X className="h-4 w-4" /> Cancel
          </button>
          <button type="submit" className="flex items-center gap-2 bg-blue-600 text-white px-6 py-2.5 rounded-xl text-sm font-bold hover:bg-blue-700 transition shadow-sm">
            <Save className="h-4 w-4" /> Save Profile
          </button>
        </div>

      </form>
    </div>
  );
}
