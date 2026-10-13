import { useState, useEffect } from 'react';
import { Users, Edit2, Trash2, Search, Plus, X, Save, Building2 } from 'lucide-react';
import Breadcrumb from '../components/ui/Breadcrumb';
import { Link } from 'react-router-dom';
import apiClient from '../lib/axios';
import Modal from '../components/ui/Modal';

interface Team {
  id: string;
  name: string;
  departmentId: string;
}

interface Department {
  id: string;
  name: string;
}

export default function ViewTeams() {
  const [teams, setTeams] = useState<Team[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Edit Modal State
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingTeam, setEditingTeam] = useState<Team | null>(null);
  const [editLoading, setEditLoading] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [teamsRes, deptsRes] = await Promise.all([
          apiClient.get('/api/admin/teams'),
          apiClient.get('/api/admin/departments')
        ]);
        setTeams(teamsRes.data);
        setDepartments(deptsRes.data);
        setError(null);
      } catch (err: any) {
        console.error(err);
        setError('Failed to fetch teams. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleDelete = async (id: string, name: string) => {
    if (window.confirm(`Are you sure you want to delete the team "${name}"?`)) {
      try {
        await apiClient.delete(`/api/admin/teams/${id}`);
        setTeams(prev => prev.filter(team => team.id !== id));
      } catch (err: any) {
        console.error(err);
        alert('Failed to delete team. It might be in use.');
      }
    }
  };

  const handleEditClick = (team: Team) => {
    setEditingTeam({ ...team });
    setIsEditModalOpen(true);
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    if (editingTeam) {
      setEditingTeam(prev => ({ ...prev!, [name]: value }));
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingTeam) return;
    
    setEditLoading(true);
    try {
      const res = await apiClient.put(`/api/admin/teams/${editingTeam.id}`, editingTeam);
      if (res.status === 200) {
        setTeams(prev => prev.map(t => t.id === editingTeam.id ? res.data : t));
        setIsEditModalOpen(false);
      }
    } catch (err: any) {
      console.error('Error updating team:', err);
      alert('Failed to update team: ' + (err.response?.data?.message || err.message));
    } finally {
      setEditLoading(false);
    }
  };

  const getDepartmentName = (deptId: string) => {
    const dept = departments.find(d => String(d.id) === String(deptId));
    return dept ? dept.name : 'Unknown Department';
  };

  const filteredTeams = teams.filter(team => {
    const search = searchTerm.toLowerCase();
    const nameMatch = (team.name || '').toLowerCase().includes(search);
    const deptMatch = (getDepartmentName(team.departmentId) || '').toLowerCase().includes(search);
    return nameMatch || deptMatch;
  });

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full mx-auto max-w-7xl">
      {/* Header */}
      <div className="-mb-2">
         <Breadcrumb items={[
          { label: 'Organization Management', href: '#' },
          { label: 'Teams' }
        ]} />
      </div>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 mt-4 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-3">
            <div className="p-2 bg-blue-100 text-blue-600 rounded-lg">
              <Users className="w-6 h-6" />
            </div>
            Teams
          </h1>
          <p className="text-slate-500 mt-2 text-sm">
            Manage your organization's teams and their department associations.
          </p>
        </div>
        <Link
          to="/teams/add"
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium shadow-sm whitespace-nowrap"
        >
          <Plus className="w-4 h-4" />
          Add New Team
        </Link>
      </div>

      {/* Controls */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 mb-6 flex flex-col sm:flex-row gap-4 justify-between items-center">
        <div className="relative w-full sm:max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
          <input
            type="text"
            placeholder="Search teams or departments..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-shadow text-sm"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Team Name</th>
                <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Department</th>
                <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {loading ? (
                <tr>
                  <td colSpan={3} className="px-6 py-12 text-center text-slate-500">
                    <div className="flex justify-center items-center gap-2">
                      <div className="w-5 h-5 border-2 border-slate-300 border-t-blue-600 rounded-full animate-spin" />
                      Loading teams...
                    </div>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={3} className="px-6 py-12 text-center text-red-500">
                    {error}
                  </td>
                </tr>
              ) : filteredTeams.length === 0 ? (
                <tr>
                  <td colSpan={3} className="px-6 py-12 text-center text-slate-500">
                    No teams found matching your search.
                  </td>
                </tr>
              ) : (
                filteredTeams.map((team) => (
                  <tr key={team.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold text-sm shrink-0">
                          {team.name.substring(0, 2).toUpperCase()}
                        </div>
                        <span className="font-medium text-slate-900">{team.name}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2 text-slate-600">
                        <Building2 className="w-4 h-4 text-slate-400" />
                        <span className="text-sm font-medium">{getDepartmentName(team.departmentId)}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                      <button 
                        onClick={() => handleEditClick(team)}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors inline-flex items-center justify-center"
                        title="Edit Team"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button 
                        onClick={() => handleDelete(team.id, team.name)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors inline-flex items-center justify-center"
                        title="Delete Team"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Modal */}
      <Modal isOpen={isEditModalOpen} onClose={() => setIsEditModalOpen(false)} title="Edit Team">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-slate-800">Edit Team</h2>
            <button onClick={() => setIsEditModalOpen(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
              <X className="w-6 h-6" />
            </button>
          </div>

          {editingTeam && (
            <form onSubmit={handleEditSubmit} className="space-y-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="block text-sm font-medium text-slate-700">Team Name</label>
                  <input
                    type="text"
                    name="name"
                    required
                    value={editingTeam.name}
                    onChange={handleEditChange}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-sm"
                  />
                </div>

                <div className="space-y-2">
                  <label className="block text-sm font-medium text-slate-700">Department</label>
                  <select
                    name="departmentId"
                    required
                    value={editingTeam.departmentId}
                    onChange={handleEditChange}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none appearance-none bg-white text-sm"
                  >
                    {departments.map(dept => (
                      <option key={dept.id} value={dept.id}>{dept.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={editLoading}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors flex items-center gap-2"
                >
                  {editLoading ? 'Saving...' : (
                    <>
                      <Save className="w-4 h-4" />
                      Save Changes
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </div>
      </Modal>
    </div>
  );
}
