import { useState, useEffect } from 'react';
import type { ReactElement } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer, Slide } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Layout from './components/layout/Layout';
import Dashboard from './pages/Dashboard';
import TimeTrackingSummary from './pages/TimeTrackingSummary';
import AddEmployee from './pages/AddEmployee';
import ViewEmployee from './pages/ViewEmployee';
import Login from './pages/Login';
import AssignTask from './pages/AssignTask';
import TaskList from './pages/TaskList';
import EmployeeTimesheet from './pages/EmployeeTimesheet';
import AdminProfile from './pages/AdminProfile';
import AddDepartment from './pages/AddDepartment';
import ViewDepartments from './pages/ViewDepartments';
import AddTeam from './pages/AddTeam';
import ViewTeams from './pages/ViewTeams';
import Attendance from './pages/Attendance';
import ScreenMonitoringSettings from './pages/ScreenMonitoringSettings';
import ScreenMonitoringView from './pages/ScreenMonitoringView';

// ProtectedRoute checks if adminToken exists in localStorage or sessionStorage
const ProtectedRoute = ({ children }: { children: ReactElement }) => {
  const token = localStorage.getItem('adminToken') || sessionStorage.getItem('adminToken');
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

function App() {
  const [isDark, setIsDark] = useState(() => {
    if (typeof window !== 'undefined') {
      return document.documentElement.classList.contains('dark');
    }
    return false;
  });

  useEffect(() => {
    const handleThemeChange = () => {
      setIsDark(document.documentElement.classList.contains('dark'));
    };
    window.addEventListener('theme-change', handleThemeChange);
    return () => window.removeEventListener('theme-change', handleThemeChange);
  }, []);

  return (
    <BrowserRouter>
      <ToastContainer
        position="bottom-right"
        autoClose={3500}
        hideProgressBar={false}
        newestOnTop={true}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme={isDark ? "dark" : "light"}
        transition={Slide}
      />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }>
          <Route index element={<Dashboard />} />
          <Route path="time-tracking" element={<TimeTrackingSummary />} />
          <Route path="tracking/timesheet" element={<EmployeeTimesheet />} />
          <Route path="tracking/attendance" element={<Attendance />} />
          <Route path="tracking/screen-monitoring" element={<ScreenMonitoringView />} />
          <Route path="tracking/screenshots" element={<ScreenMonitoringView />} />
          <Route path="screen-monitoring" element={<ScreenMonitoringView />} />
          <Route path="screenshots" element={<ScreenMonitoringView />} />
          <Route path="employees/add" element={<AddEmployee />} />
          <Route path="employees/view" element={<ViewEmployee />} />
          <Route path="tasks/assign" element={<AssignTask />} />
          <Route path="tasks/track" element={<TaskList />} />
          <Route path="departments/add" element={<AddDepartment />} />
          <Route path="departments/view" element={<ViewDepartments />} />
          <Route path="teams/add" element={<AddTeam />} />
          <Route path="teams/view" element={<ViewTeams />} />
          <Route path="admin/profile" element={<AdminProfile />} />
          <Route path="profile" element={<AdminProfile />} />
          <Route path="settings" element={<AdminProfile />} />
          <Route path="settings/general" element={<AdminProfile />} />
          <Route path="settings/screen-monitoring" element={<ScreenMonitoringSettings />} />
          <Route path="settings/attendance" element={<ScreenMonitoringSettings />} />
          <Route path="settings/notifications" element={<AdminProfile />} />
          <Route path="settings/roles" element={<AdminProfile />} />
          <Route path="settings/system" element={<AdminProfile />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
