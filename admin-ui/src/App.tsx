import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
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

// ProtectedRoute checks if adminToken exists in localStorage or sessionStorage
const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const token = localStorage.getItem('adminToken') || sessionStorage.getItem('adminToken');
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

function App() {
  return (
    <BrowserRouter>
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
          <Route path="employees/add" element={<AddEmployee />} />
          <Route path="employees/view" element={<ViewEmployee />} />
          <Route path="tasks/assign" element={<AssignTask />} />
          <Route path="tasks/track" element={<TaskList />} />
          <Route path="departments/add" element={<AddDepartment />} />
          <Route path="departments/view" element={<ViewDepartments />} />
          <Route path="teams/add" element={<AddTeam />} />
          <Route path="teams/view" element={<ViewTeams />} />
          <Route path="admin/profile" element={<AdminProfile />} />
          <Route path="settings" element={<AdminProfile />} />
          {/* We will add more routes here later */}
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
