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

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="time-tracking" element={<TimeTrackingSummary />} />
          <Route path="employees/add" element={<AddEmployee />} />
          <Route path="employees/view" element={<ViewEmployee />} />
          <Route path="tracking/timesheet" element={<EmployeeTimesheet />} />
          <Route path="tasks/assign" element={<AssignTask />} />
          <Route path="tasks/track" element={<TaskList />} />
          <Route path="departments/add" element={<AddDepartment />} />
          <Route path="departments/view" element={<ViewDepartments />} />
          <Route path="admin/profile" element={<AdminProfile />} />
          <Route path="settings" element={<AdminProfile />} />
          {/* We will add more routes here later */}
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
