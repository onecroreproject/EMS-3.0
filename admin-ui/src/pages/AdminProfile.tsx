import { useState } from 'react';
import { 
  User, Mail, Phone, MapPin, Briefcase, Calendar, 
  Clock, Shield, Bell, Settings, Lock, Smartphone, 
  Camera, Edit, ChevronRight, Activity
} from 'lucide-react';
import Breadcrumb from '../components/ui/Breadcrumb';

export default function AdminProfile() {
  const [activeTab, setActiveTab] = useState('Profile Information');

  const tabs = [
    { name: 'Profile Information', icon: User },
    { name: 'Security Settings', icon: Shield },
    { name: 'Two-Factor Authentication', icon: Lock },
    { name: 'Change Password', icon: KeyRoundIcon },
    { name: 'Notification Preferences', icon: Bell },
    { name: 'System Preferences', icon: Settings },
  ];

  return (
    <div className="max-w-[1600px] mx-auto space-y-4 pb-10 animate-fade-in">
      {/* Header */}
      <div>
        <div className="flex justify-between items-center">
          <h2 className="text-2xl font-bold text-slate-800">Admin Profile</h2>
          <Breadcrumb items={[{ label: 'Dashboard', href: '/' }, { label: 'Admin Profile' }]} />
        </div>
        <p className="text-slate-500 text-sm mt-1">View and manage your profile, security and account settings</p>
      </div>

      {/* Top Profile Card */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div className="flex items-center gap-5">
          <div className="relative">
            <div className="w-24 h-24 rounded-full bg-slate-200 overflow-hidden border-4 border-white shadow-md">
              <img src="https://ui-avatars.com/api/?name=Admin&background=0D8ABC&color=fff&size=200" alt="Profile" className="w-full h-full object-cover" />
            </div>
            <button className="absolute bottom-0 right-0 bg-blue-600 text-white p-1.5 rounded-full shadow-md hover:bg-blue-700 transition">
              <Camera className="w-3.5 h-3.5" />
            </button>
          </div>
          <div>
            <div className="flex items-center gap-3">
              <h3 className="text-xl font-bold text-slate-800">Admin</h3>
              <span className="px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">Administrator</span>
            </div>
            <p className="text-sm text-slate-500 mt-0.5 font-medium">26@DLK001</p>
            <div className="flex flex-col sm:flex-row gap-2 sm:gap-4 mt-2">
              <div className="flex items-center gap-1.5 text-sm text-slate-600">
                <Mail className="w-4 h-4 text-slate-400" /> admin@gmail.com
              </div>
              <div className="flex items-center gap-1.5 text-sm text-slate-600">
                <Phone className="w-4 h-4 text-slate-400" /> +91 98765 43210
              </div>
            </div>
            <div className="flex items-center gap-1.5 mt-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              <span className="text-xs font-medium text-slate-600">Online</span>
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-6 md:gap-10 border-t md:border-t-0 md:border-l border-slate-200 pt-4 md:pt-0 md:pl-8 w-full md:w-auto">
          <div className="space-y-3 flex-1">
            <div className="grid grid-cols-[100px_10px_1fr] text-sm">
              <span className="text-slate-500 flex items-center gap-2"><Briefcase className="w-3.5 h-3.5" /> Department</span><span>:</span><span className="font-medium text-slate-700">IT Operations</span>
            </div>
            <div className="grid grid-cols-[100px_10px_1fr] text-sm">
              <span className="text-slate-500 flex items-center gap-2"><MapPin className="w-3.5 h-3.5" /> Location</span><span>:</span><span className="font-medium text-slate-700">Chennai, India</span>
            </div>
            <div className="grid grid-cols-[100px_10px_1fr] text-sm">
              <span className="text-slate-500 flex items-center gap-2"><Calendar className="w-3.5 h-3.5" /> Joined On</span><span>:</span><span className="font-medium text-slate-700">01 Sep 2026</span>
            </div>
            <div className="grid grid-cols-[100px_10px_1fr] text-sm">
              <span className="text-slate-500 flex items-center gap-2"><Clock className="w-3.5 h-3.5" /> Last Login</span><span>:</span><span className="font-medium text-slate-700">04 Oct 2026 10:45 AM</span>
            </div>
            <div className="grid grid-cols-[100px_10px_1fr] text-sm">
              <span className="text-slate-500 flex items-center gap-2"><Activity className="w-3.5 h-3.5" /> Account Status</span><span>:</span>
              <span className="font-medium text-emerald-600 flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-emerald-500"></span> Active</span>
            </div>
          </div>
          <div>
            <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md font-medium text-sm flex items-center gap-2 shadow-sm transition-colors">
              <Edit className="w-4 h-4" /> Edit Profile
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 w-full overflow-hidden">
        <div className="flex items-center w-full px-1">
          {tabs.map((tab) => {
            const isActive = activeTab === tab.name;
            const Icon = tab.icon;
            return (
              <button
                key={tab.name}
                onClick={() => setActiveTab(tab.name)}
                className={`flex-1 flex items-center justify-center gap-1.5 px-1 py-4 text-[10px] md:text-xs lg:text-sm font-semibold transition-colors border-b-2 text-center leading-tight ${
                  isActive 
                    ? 'border-blue-600 text-blue-600 bg-blue-50/50' 
                    : 'border-transparent text-slate-600 hover:text-slate-800 hover:bg-slate-50'
                }`}
              >
                <Icon className="w-3.5 h-3.5 md:w-4 md:h-4 shrink-0" />
                <span className="hidden sm:inline-block">{tab.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Tab Content - Profile Information */}
      {activeTab === 'Profile Information' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          
          {/* Left Column - Details */}
          <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-slate-200 p-5">
            <h3 className="text-base font-bold text-slate-800 flex items-center gap-2 mb-5">
              <User className="w-5 h-5 text-blue-500" /> Profile Information
            </h3>
            
            <div className="space-y-4 max-w-2xl">
              <div className="grid grid-cols-[140px_1fr] items-center text-sm">
                <span className="font-semibold text-slate-700">Full Name <span className="text-red-500">*</span></span>
                <input type="text" readOnly value="Admin" className="border border-slate-200 rounded-md px-3 py-2 bg-slate-50 text-slate-800 font-medium outline-none w-full" />
              </div>
              <div className="grid grid-cols-[140px_1fr] items-center text-sm">
                <span className="font-semibold text-slate-700">Employee Code <span className="text-red-500">*</span></span>
                <input type="text" readOnly value="26@DLK001" className="border border-slate-200 rounded-md px-3 py-2 bg-slate-50 text-slate-800 font-medium outline-none w-full" />
              </div>
              <div className="grid grid-cols-[140px_1fr] items-center text-sm">
                <span className="font-semibold text-slate-700">Email Address <span className="text-red-500">*</span></span>
                <input type="text" readOnly value="admin@gmail.com" className="border border-slate-200 rounded-md px-3 py-2 bg-slate-50 text-slate-800 font-medium outline-none w-full" />
              </div>
              <div className="grid grid-cols-[140px_1fr] items-center text-sm">
                <span className="font-semibold text-slate-700">Phone Number <span className="text-red-500">*</span></span>
                <input type="text" readOnly value="+91 98765 43210" className="border border-slate-200 rounded-md px-3 py-2 bg-slate-50 text-slate-800 font-medium outline-none w-full" />
              </div>
              <div className="grid grid-cols-[140px_1fr] items-center text-sm">
                <span className="font-semibold text-slate-700">Role</span>
                <input type="text" readOnly value="Administrator" className="border border-slate-200 rounded-md px-3 py-2 bg-slate-50 text-slate-800 font-medium outline-none w-full" />
              </div>
              <div className="grid grid-cols-[140px_1fr] items-center text-sm">
                <span className="font-semibold text-slate-700">Department</span>
                <input type="text" readOnly value="IT Operations" className="border border-slate-200 rounded-md px-3 py-2 bg-slate-50 text-slate-800 font-medium outline-none w-full" />
              </div>
              <div className="grid grid-cols-[140px_1fr] items-center text-sm">
                <span className="font-semibold text-slate-700">Location</span>
                <input type="text" readOnly value="Chennai, India" className="border border-slate-200 rounded-md px-3 py-2 bg-slate-50 text-slate-800 font-medium outline-none w-full" />
              </div>
            </div>
          </div>

          {/* Right Column - Misc info & Pic */}
          <div className="space-y-4">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
               <h3 className="text-base font-bold text-slate-800 flex items-center gap-2 mb-4">
                <Briefcase className="w-5 h-5 text-emerald-500" /> Account Information
              </h3>
              <div className="space-y-3">
                <div className="grid grid-cols-[100px_1fr] text-sm">
                  <span className="text-slate-600 flex items-center gap-2"><User className="w-4 h-4" /> Username</span><span className="font-medium text-slate-800">admin</span>
                </div>
                <div className="grid grid-cols-[100px_1fr] text-sm">
                  <span className="text-slate-600 flex items-center gap-2"><Calendar className="w-4 h-4" /> Joined On</span><span className="font-medium text-slate-800">01 Sep 2026</span>
                </div>
                <div className="grid grid-cols-[100px_1fr] text-sm">
                  <span className="text-slate-600 flex items-center gap-2"><Lock className="w-4 h-4" /> Last Login</span><span className="font-medium text-slate-800">04 Oct 2026 10:45 AM</span>
                </div>
                <div className="grid grid-cols-[100px_1fr] text-sm">
                  <span className="text-slate-600 flex items-center gap-2"><Shield className="w-4 h-4" /> Account Status</span>
                  <span className="font-medium text-emerald-600 flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-emerald-500"></span> Active</span>
                </div>
              </div>
            </div>
            
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
               <h3 className="text-base font-bold text-slate-800 flex items-center gap-2 mb-4">
                <Camera className="w-5 h-5 text-blue-500" /> Profile Picture
              </h3>
              <div className="flex items-center gap-4">
                <img src="https://ui-avatars.com/api/?name=Admin&background=0D8ABC&color=fff&size=150" alt="Avatar" className="w-16 h-16 rounded-full shadow-sm" />
                <div>
                  <button className="border border-slate-300 text-blue-600 hover:bg-blue-50 font-medium px-3 py-1.5 rounded-md text-xs flex items-center gap-2 transition-colors">
                    <Camera className="w-3.5 h-3.5" /> Change Photo
                  </button>
                  <p className="text-[10px] text-slate-500 mt-1.5">JPG, PNG (Max 2MB)</p>
                </div>
              </div>
            </div>
          </div>
          
          {/* Bottom Row - Settings Summaries */}
          <div className="lg:col-span-3 grid grid-cols-1 md:grid-cols-3 gap-4">
            
            {/* Security Settings */}
            <div className="bg-orange-50/30 rounded-xl shadow-sm border border-orange-100 p-5">
              <h3 className="text-base font-bold text-slate-800 flex items-center gap-2 mb-4">
                <Shield className="w-5 h-5 text-orange-500" /> Security Settings
              </h3>
              <div className="space-y-3">
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Lock className="w-4 h-4 text-slate-500" /> Two-Factor Authentication</span>
                  <div className="flex items-center gap-2">
                    <span className="bg-emerald-100 text-emerald-700 text-[10px] px-2 py-0.5 rounded font-bold">Enabled</span>
                    <ChevronRight className="w-4 h-4 text-slate-400" />
                  </div>
                </div>
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><KeyRoundIcon className="w-4 h-4 text-slate-500" /> Change Password</span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Clock className="w-4 h-4 text-slate-500" /> Login Activity</span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Smartphone className="w-4 h-4 text-slate-500" /> Manage Active Sessions</span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              </div>
            </div>

            {/* Notification Preferences */}
            <div className="bg-purple-50/30 rounded-xl shadow-sm border border-purple-100 p-5">
              <h3 className="text-base font-bold text-slate-800 flex items-center gap-2 mb-4">
                <Bell className="w-5 h-5 text-purple-500" /> Notification Preferences
              </h3>
              <div className="space-y-3">
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Mail className="w-4 h-4 text-slate-500" /> Email Notifications</span>
                  <div className="flex items-center gap-2">
                    <span className="bg-emerald-100 text-emerald-700 text-[10px] px-2 py-0.5 rounded font-bold">Enabled</span>
                    <ChevronRight className="w-4 h-4 text-slate-400" />
                  </div>
                </div>
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Lock className="w-4 h-4 text-slate-500" /> Login Alerts</span>
                  <div className="flex items-center gap-2">
                    <span className="bg-emerald-100 text-emerald-700 text-[10px] px-2 py-0.5 rounded font-bold">Enabled</span>
                    <ChevronRight className="w-4 h-4 text-slate-400" />
                  </div>
                </div>
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Settings className="w-4 h-4 text-slate-500" /> System Updates</span>
                  <div className="flex items-center gap-2">
                    <span className="bg-emerald-100 text-emerald-700 text-[10px] px-2 py-0.5 rounded font-bold">Enabled</span>
                    <ChevronRight className="w-4 h-4 text-slate-400" />
                  </div>
                </div>
                <div className="flex justify-between items-center text-sm cursor-pointer hover:bg-white p-2 rounded-md transition-colors -mx-2">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Shield className="w-4 h-4 text-slate-500" /> Security Alerts</span>
                  <div className="flex items-center gap-2">
                    <span className="bg-emerald-100 text-emerald-700 text-[10px] px-2 py-0.5 rounded font-bold">Enabled</span>
                    <ChevronRight className="w-4 h-4 text-slate-400" />
                  </div>
                </div>
              </div>
            </div>

            {/* System Preferences */}
            <div className="bg-blue-50/30 rounded-xl shadow-sm border border-blue-100 p-5">
              <h3 className="text-base font-bold text-slate-800 flex items-center gap-2 mb-4">
                <Settings className="w-5 h-5 text-blue-500" /> System Preferences
              </h3>
              <div className="space-y-3">
                <div className="grid grid-cols-[120px_1fr] items-center text-sm p-1">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><div className="w-4 h-4 rounded-full border-2 border-slate-400"></div> Theme</span>
                  <select className="border border-slate-200 rounded bg-white px-2 py-1 outline-none text-slate-600">
                    <option>Light</option>
                    <option>Dark</option>
                  </select>
                </div>
                <div className="grid grid-cols-[120px_1fr] items-center text-sm p-1">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><GlobeIcon className="w-4 h-4 text-slate-500" /> Language</span>
                  <select className="border border-slate-200 rounded bg-white px-2 py-1 outline-none text-slate-600">
                    <option>English</option>
                    <option>Spanish</option>
                  </select>
                </div>
                <div className="grid grid-cols-[120px_1fr] items-center text-sm p-1">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Calendar className="w-4 h-4 text-slate-500" /> Date Format</span>
                  <select className="border border-slate-200 rounded bg-white px-2 py-1 outline-none text-slate-600">
                    <option>dd MMM yyyy</option>
                    <option>MM/DD/YYYY</option>
                  </select>
                </div>
                <div className="grid grid-cols-[120px_1fr] items-center text-sm p-1">
                  <span className="text-slate-700 flex items-center gap-2 font-medium"><Clock className="w-4 h-4 text-slate-500" /> Time Zone</span>
                  <select className="border border-slate-200 rounded bg-white px-2 py-1 outline-none text-slate-600 text-xs">
                    <option>(GMT+05:30) Chennai</option>
                  </select>
                </div>
              </div>
            </div>

          </div>
        </div>
      )}

      {activeTab !== 'Profile Information' && (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-10 flex flex-col items-center justify-center text-slate-500">
          <Settings className="w-12 h-12 text-slate-300 mb-3" />
          <h3 className="text-lg font-bold text-slate-700">{activeTab}</h3>
          <p className="text-sm mt-1">This settings panel is coming soon.</p>
        </div>
      )}
    </div>
  );
}

// Missing icons mock
const KeyRoundIcon = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 18v3c0 .6.4 1 1 1h4v-3h3v-3h2l1.4-1.4a6.5 6.5 0 1 0-4-4Z"/><circle cx="16.5" cy="7.5" r=".5" fill="currentColor"/></svg>
);

const GlobeIcon = ({ className }: { className?: string }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/><path d="M2 12h20"/></svg>
);
