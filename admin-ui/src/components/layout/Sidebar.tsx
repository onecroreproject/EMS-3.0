import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  Home, Users, MonitorPlay, Globe, 
  LayoutGrid, BarChart2, CheckSquare, 
  PieChart, Settings, HeadphonesIcon, ChevronDown, ChevronRight,
  Building2
} from 'lucide-react';

export default function Sidebar({ isOpen, setIsOpen }: { isOpen: boolean, setIsOpen: (val: boolean) => void }) {
  const [expandedMenu, setExpandedMenu] = useState<string | null>('Tracking & Monitoring');

  const toggleMenu = (name: string) => {
    setExpandedMenu(expandedMenu === name ? null : name);
  };

  const menuItems = [
    { name: 'Dashboard', icon: Home, path: '/' },
    { 
      name: 'Employee Management', 
      icon: Users, 
      subItems: [
        { name: 'Add Employee', path: '/employees/add' },
        { name: 'View Employee', path: '/employees/view' },
      ]
    },
    { 
      name: 'Organization Management', 
      icon: Building2, 
      subItems: [
        { name: 'Department', isLabel: true },
        { name: 'Add Department', path: '/departments/add' },
        { name: 'View Department', path: '/departments/view' },
        { name: 'Teams', isLabel: true },
        { name: 'Add Teams', path: '/teams/add' },
        { name: 'View Teams', path: '/teams/view' },
      ]
    },
    { 
      name: 'Tracking & Monitoring', 
      icon: MonitorPlay, 
      subItems: [
        { name: 'Time Tracking Summary', path: '/time-tracking' },
        { name: 'Employee Timesheet', path: '/tracking/timesheet' },
        { name: 'Attendance', path: '/tracking/attendance' },
      ]
    },
    { name: 'Application Usage', icon: LayoutGrid, path: '/applications' },
    { 
      name: 'Task & Projects', 
      icon: CheckSquare, 
      subItems: [
        { name: 'Assign Task', path: '/tasks/assign' },
        { name: 'Task List', path: '/tasks/track' },
      ]
    },
    { name: 'Settings', icon: Settings, path: '/settings' },
  ];

  return (
    <>
      {/* Mobile Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-40 md:hidden" 
          onClick={() => setIsOpen(false)} 
        />
      )}

      {/* Sidebar Container */}
      <aside className={`
        fixed md:static inset-y-0 left-0 z-50
        w-64 bg-[#1E2B3C] text-slate-300 flex flex-col h-full shrink-0 
        transition-transform duration-300 ease-in-out
        ${isOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
      `}>
        
        {/* Mobile Header (Hidden on Desktop) */}
        <div className="h-16 flex items-center justify-center border-b border-slate-700 md:hidden bg-[#F96D3E] text-white">
           <h1 className="text-xl font-bold">EmpMonitor</h1>
        </div>

        <nav className="flex-1 py-4 overflow-y-auto custom-scrollbar">
          <ul className="space-y-1 px-3">
            {menuItems.map((item, index) => {
              const hasSubmenu = !!item.subItems;
              const isExpanded = expandedMenu === item.name;

              return (
                <li key={index}>
                  {hasSubmenu ? (
                    <div>
                      <button
                        onClick={() => toggleMenu(item.name)}
                        className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg transition-colors text-sm font-medium hover:bg-slate-700/50 hover:text-white ${isExpanded ? 'bg-slate-800 text-white' : ''}`}
                      >
                        <div className="flex items-center gap-3 truncate">
                          <item.icon className="h-4 w-4 shrink-0" />
                          <span className="truncate">{item.name}</span>
                        </div>
                        {isExpanded ? <ChevronDown className="h-4 w-4 opacity-50" /> : <ChevronRight className="h-4 w-4 opacity-50" />}
                      </button>
                      
                      {/* Submenu Items with Smooth Animation */}
                      <div className={`grid transition-all duration-300 ease-in-out ${isExpanded ? 'grid-rows-[1fr] opacity-100 mt-1' : 'grid-rows-[0fr] opacity-0'}`}>
                        <ul className="overflow-hidden space-y-1 pl-11">
                          {item.subItems.map((subItem: any, subIdx) => (
                            <li key={subIdx}>
                              {subItem.isLabel ? (
                                <div className="px-3 pt-2 pb-1 text-[10px] uppercase tracking-wider font-bold text-[#F96D3E]/60">
                                  {subItem.name}
                                </div>
                              ) : (
                                <NavLink
                                  to={subItem.path}
                                  onClick={() => setIsOpen(false)}
                                  className={({ isActive }) =>
                                    `block px-3 py-2 rounded-md text-sm transition-all duration-200 ${
                                      isActive 
                                        ? 'text-[#F96D3E] font-semibold bg-[#F96D3E]/10 translate-x-1' 
                                        : 'text-slate-400 hover:text-[#F96D3E] hover:bg-[#F96D3E]/10 hover:translate-x-1'
                                    }`
                                  }
                                >
                                  {subItem.name}
                                </NavLink>
                              )}
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  ) : (
                    <NavLink
                      to={item.path!}
                      onClick={() => setIsOpen(false)}
                      className={({ isActive }) =>
                        `flex items-center justify-between px-3 py-2.5 rounded-lg transition-colors text-sm font-medium ${
                          isActive
                            ? 'bg-[#F96D3E] text-white'
                            : 'hover:bg-slate-700/50 hover:text-white'
                        }`
                      }
                    >
                        <div className="flex items-center gap-3 truncate">
                          <item.icon className="h-4 w-4 shrink-0" />
                          <span className="truncate">{item.name}</span>
                        </div>
                    </NavLink>
                  )}
                </li>
              );
            })}
          </ul>
        </nav>

        {/* Help Box at Bottom */}
        <div className="p-4 mt-auto">
          <div className="bg-slate-800/50 p-4 rounded-xl border border-slate-700">
            <div className="flex items-center gap-2 text-white mb-2">
              <HeadphonesIcon className="h-5 w-5" />
              <h4 className="font-semibold text-sm">Need Help?</h4>
            </div>
            <p className="text-xs text-slate-400 mb-4">
              Contact our support team for assistance.
            </p>
            <button className="w-full py-2 bg-transparent border border-slate-600 hover:bg-slate-700 text-white text-xs font-medium rounded-lg transition">
              Contact Support
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}
