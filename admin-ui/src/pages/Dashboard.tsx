import { useState, useEffect } from 'react';
import apiClient from '../lib/axios';
import { Users, Monitor, MonitorOff, Clock } from 'lucide-react';

export default function Dashboard() {
  const [stats, setStats] = useState({
    totalEmployees: 0,
    totalDepartments: 0,
    totalTeams: 0,
    totalTasks: 0
  });

  useEffect(() => {
    // Fetch REAL data from Spring Boot API
    apiClient.get('/api/admin/dashboard')
      .then(response => {
        setStats(response.data);
      })
      .catch(error => {
        console.error("Error fetching dashboard stats", error);
      });
  }, []);

  const summaryCards = [
    { title: 'Total Employees', count: stats.totalEmployees, trend: 'From Database', icon: Users, color: 'bg-blue-50 text-blue-600', trendColor: 'text-gray-500' },
    { title: 'Total Tasks', count: stats.totalTasks, trend: 'From Database', icon: CheckSquare, color: 'bg-green-50 text-green-600', trendColor: 'text-gray-500' },
    { title: 'Total Departments', count: stats.totalDepartments, trend: 'From Database', icon: Users, color: 'bg-orange-50 text-orange-600', trendColor: 'text-gray-500' },
    { title: 'Total Teams', count: stats.totalTeams, trend: 'From Database', icon: Users, color: 'bg-purple-50 text-purple-600', trendColor: 'text-gray-500' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-start">
        <div>
          <p className="text-gray-500 text-sm">Welcome Back,</p>
          <h2 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
            Admin <span className="text-2xl">👋</span>
          </h2>
          <p className="text-gray-500 text-sm mt-1">Here's what's happening in your organization today.</p>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {summaryCards.map((card, idx) => (
          <div key={idx} className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
            <div className={`p-4 rounded-xl ${card.color}`}>
              <card.icon className="h-7 w-7" />
            </div>
            <div>
              <p className="text-gray-500 text-xs font-medium mb-1">{card.title}</p>
              <h3 className="text-2xl font-bold text-gray-800 leading-none">{card.count}</h3>
              <p className={`text-[10px] font-medium mt-1 ${card.trendColor}`}>{card.trend}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function CheckSquare(props: any) {
  return (
    <svg {...props} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  )
}
