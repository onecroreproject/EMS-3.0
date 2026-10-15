import React, { useState, useEffect } from 'react';
import { 
  Monitor, Camera, Database, HardDrive, 
  CloudUpload, Shield, Settings, Clock, 
  AlertTriangle, History, Info, RotateCcw, 
  Save, Check, Search, Calendar, Trash2, X, ChevronRight,
  Server
} from 'lucide-react';
import { toast } from 'react-toastify';
import apiClient from '../lib/axios';

interface ScreenMonitoringConfig {
  id?: string;
  configurationVersion: number;
  versionString: string;
  status: string;
  updatedAt?: string;
  updatedBy: string;
  createdAt?: string;

  // 1. Screenshot Capture Settings
  captureEnabled: boolean;
  captureInterval: number;
  captureIntervalUnit: string;
  imageQuality: number;
  imageFormat: string;
  captureDuringWorking: boolean;
  captureDuringIdle: boolean;
  captureDuringBreak: boolean;
  captureWhenLoggedOut: boolean;

  // 2. Retention and Automatic Cleanup
  retentionPeriod: number;
  retentionPeriodUnit: string;
  automaticCleanup: boolean;
  cleanupScheduleTime: string;
  cleanupTimeZone: string;
  cleanupBatchSize: number;
  storageUsageWarning: number;

  // 3. Storage and Performance
  maxUploadSizeMb: number;
  thumbnailGeneration: boolean;
  thumbnailDimensions: string;
  thumbnailQuality: number;

  // 4. Offline Upload and Synchronization
  enableOfflineCapture: boolean;
  localStorageLimit: number;
  localStorageLimitUnit: string;
  retryInterval: number;
  retryIntervalUnit: string;
  maxRetryAttempts: number;
  autoSyncOnStartup: boolean;

  // 5. Access Control
  allowScreenshotDownload: boolean;
  restrictToAdminUsersOnly: boolean;
  auditConfigurationChanges: boolean;

  // 7. Attendance & Idle Policy
  dailyRequiredWorkingHours: number;
  dailyRequiredWorkingHoursUnit: string;
  idleDetectionThreshold: number;
  idleDetectionThresholdUnit: string;
  markOfflineAfterContinuousIdle: number;
  markOfflineAfterContinuousIdleUnit: string;
  enableAutomaticOfflineStatus: boolean;
  applyPolicyPerEmployee: boolean;
}

interface StorageMetrics {
  totalScreenshots: number;
  totalSizeBytes: number;
  totalSizeMb: number;
  totalSizeGb: number;
  oldestScreenshotDate: string | null;
  newestScreenshotDate: string | null;
}

interface HistoryItem {
  id: string;
  configurationVersion: number;
  versionString: string;
  changedBy: string;
  changedAt: string;
  changeSummary: string;
}

export default function ScreenMonitoringSettings() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [initialConfig, setInitialConfig] = useState<ScreenMonitoringConfig | null>(null);
  const [config, setConfig] = useState<ScreenMonitoringConfig>({
    configurationVersion: 1,
    versionString: 'v2.1.0',
    status: 'ACTIVE',
    updatedBy: 'Admin',
    captureEnabled: true,
    captureInterval: 5,
    captureIntervalUnit: 'Minutes',
    imageQuality: 75,
    imageFormat: 'JPEG',
    captureDuringWorking: true,
    captureDuringIdle: true,
    captureDuringBreak: true,
    captureWhenLoggedOut: false,
    retentionPeriod: 30,
    retentionPeriodUnit: 'Days',
    automaticCleanup: true,
    cleanupScheduleTime: '02:00 AM',
    cleanupTimeZone: 'Asia/Kolkata (IST)',
    cleanupBatchSize: 1000,
    storageUsageWarning: 80,
    maxUploadSizeMb: 5,
    thumbnailGeneration: true,
    thumbnailDimensions: '320 x 180',
    thumbnailQuality: 70,
    enableOfflineCapture: true,
    localStorageLimit: 2,
    localStorageLimitUnit: 'GB',
    retryInterval: 5,
    retryIntervalUnit: 'Minutes',
    maxRetryAttempts: 10,
    autoSyncOnStartup: true,
    allowScreenshotDownload: true,
    restrictToAdminUsersOnly: true,
    auditConfigurationChanges: true,
    dailyRequiredWorkingHours: 9,
    dailyRequiredWorkingHoursUnit: 'Hours',
    idleDetectionThreshold: 2,
    idleDetectionThresholdUnit: 'Minutes',
    markOfflineAfterContinuousIdle: 30,
    markOfflineAfterContinuousIdleUnit: 'Minutes',
    enableAutomaticOfflineStatus: true,
    applyPolicyPerEmployee: false,
  });

  const [storageMetrics, setStorageMetrics] = useState<StorageMetrics>({
    totalScreenshots: 0,
    totalSizeBytes: 0,
    totalSizeMb: 0,
    totalSizeGb: 0,
    oldestScreenshotDate: null,
    newestScreenshotDate: null,
  });

  // Modals state
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [historyList, setHistoryList] = useState<HistoryItem[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  const [showCleanupModal, setShowCleanupModal] = useState(false);
  const [cleanupPreview, setCleanupPreview] = useState<{ countToDelete: number; retentionDays: number; cutoffDate: string } | null>(null);
  const [executingCleanup, setExecutingCleanup] = useState(false);

  const [showStorageModal, setShowStorageModal] = useState(false);

  useEffect(() => {
    fetchConfiguration();
    fetchStorageMetrics();
  }, []);

  const fetchConfiguration = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/api/admin/screen-monitoring/config');
      if (res.data) {
        setConfig(res.data);
        setInitialConfig(JSON.parse(JSON.stringify(res.data)));
      }
    } catch (err: any) {
      console.error('Error fetching screen monitoring config:', err);
      toast.error('Failed to load screen monitoring settings from server');
    } finally {
      setLoading(false);
    }
  };

  const fetchStorageMetrics = async () => {
    try {
      const res = await apiClient.get('/api/admin/screen-monitoring/storage');
      if (res.data) {
        setStorageMetrics(res.data);
      }
    } catch (err) {
      console.warn('Could not fetch storage metrics', err);
    }
  };

  const handleSave = async () => {
    // Basic validation
    if (config.captureInterval < 1 || config.captureInterval > 60) {
      toast.warning('Capture Interval must be between 1 and 60 minutes.');
      return;
    }
    if (config.idleDetectionThreshold < 1 || config.idleDetectionThreshold > 60) {
      toast.warning('Idle Detection Threshold must be between 1 and 60 minutes.');
      return;
    }
    if (config.markOfflineAfterContinuousIdle < 5 || config.markOfflineAfterContinuousIdle > 180) {
      toast.warning('Mark Offline After Continuous Idle must be between 5 and 180 minutes.');
      return;
    }

    try {
      setSaving(true);
      const res = await apiClient.put('/api/admin/screen-monitoring/config', config);
      if (res.data) {
        setConfig(res.data);
        setInitialConfig(JSON.parse(JSON.stringify(res.data)));
        toast.success(`Screen Monitoring configuration ${res.data.versionString || ''} saved successfully! Agent timing updated dynamically.`);
      }
    } catch (err: any) {
      console.error('Error saving config:', err);
      toast.error(err.response?.data?.message || 'Failed to save configuration.');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    if (!window.confirm('Reset all screen monitoring settings to default factory values?')) {
      return;
    }
    try {
      const res = await apiClient.get('/api/admin/screen-monitoring/config/default');
      if (res.data) {
        setConfig(res.data);
        toast.info('Settings reset to default values. Click "Save Changes" to apply.');
      }
    } catch (err) {
      toast.error('Failed to load default settings');
    }
  };

  const handleCancel = () => {
    if (initialConfig) {
      setConfig(JSON.parse(JSON.stringify(initialConfig)));
      toast.info('Changes reverted.');
    }
  };

  const openHistory = async () => {
    setShowHistoryModal(true);
    setLoadingHistory(true);
    try {
      const res = await apiClient.get('/api/admin/screen-monitoring/config/history');
      if (res.data) {
        setHistoryList(res.data);
      }
    } catch (err) {
      toast.error('Failed to load version history');
    } finally {
      setLoadingHistory(false);
    }
  };

  const openCleanupPreview = async () => {
    try {
      const res = await apiClient.post(`/api/admin/screen-monitoring/cleanup/preview?days=${config.retentionPeriod}`);
      if (res.data) {
        setCleanupPreview(res.data);
        setShowCleanupModal(true);
      }
    } catch (err) {
      toast.error('Failed to generate cleanup preview');
    }
  };

  const executeCleanup = async () => {
    try {
      setExecutingCleanup(true);
      const res = await apiClient.post(`/api/admin/screen-monitoring/cleanup/execute?days=${config.retentionPeriod}&batchSize=${config.cleanupBatchSize}`);
      toast.success(res.data?.message || 'Automatic cleanup completed successfully!');
      setShowCleanupModal(false);
      fetchStorageMetrics();
    } catch (err: any) {
      toast.error('Failed to execute cleanup');
    } finally {
      setExecutingCleanup(false);
    }
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '14/10/2026 10:30 AM';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-GB') + ' ' + d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateStr;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-medium text-slate-600 dark:text-slate-300">Loading Screen Monitoring Settings...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8FAFC] dark:bg-[#0B1120] text-slate-800 dark:text-slate-200 pb-16">
      <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 pt-6">
        
        {/* Top Breadcrumb */}
        <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400 mb-2">
          <span>Settings</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="font-semibold text-slate-800 dark:text-slate-200">Screen Monitoring</span>
        </div>

        {/* Header Section */}
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 mb-6">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/60 border border-blue-200 dark:border-blue-800/80 flex items-center justify-center text-blue-600 dark:text-blue-400 shadow-sm">
              <Monitor className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
                Screen Monitoring Settings
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-0.5">
                Configure screenshot capture, retention, storage and security settings for employee monitoring.
              </p>
            </div>
          </div>

          {/* Current Version Card */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl px-4 py-3 shadow-sm flex items-center gap-4">
            <div className="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-950/40 flex items-center justify-center text-blue-600 dark:text-blue-400">
              <Server className="w-5 h-5" />
            </div>
            <div className="pr-2 border-r border-slate-200 dark:border-slate-800">
              <div className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Current Configuration Version</div>
              <div className="flex items-center gap-2 mt-0.5">
                <span className="text-lg font-bold text-slate-900 dark:text-white">{config.versionString || 'v2.1.0'}</span>
                <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400">
                  {config.status || 'Active'}
                </span>
              </div>
              <div className="text-[10px] text-slate-400 mt-0.5">
                Last updated on {formatDate(config.updatedAt)} by {config.updatedBy || 'Admin'}
              </div>
            </div>
            <button 
              onClick={openHistory}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 text-xs font-medium transition shadow-xs"
            >
              <History className="w-3.5 h-3.5 text-slate-500" />
              <span>View History</span>
            </button>
          </div>
        </div>

        {/* 2-Column Grid of Settings Cards */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          
          {/* ========================================================= */}
          {/* CARD 1: Screenshot Capture Settings */}
          {/* ========================================================= */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center gap-2.5 pb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
                <Camera className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-slate-900 dark:text-white">1. Screenshot Capture Settings</h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">Configure how and when screenshots should be captured by the employee agent.</p>
              </div>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-slate-800/60 text-xs">
              
              {/* Row 1: Enable Capture */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Screenshot Capture</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Enable or disable screenshot capture.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.captureEnabled} 
                    onChange={e => setConfig({ ...config, captureEnabled: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.captureEnabled ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

              {/* Row 2: Capture Interval */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Capture Interval</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Time interval between screenshots.</div>
                  <div className="text-[10px] text-slate-400 mt-0.5">Min: 1 minute | Max: 60 minutes</div>
                </div>
                <div className="flex items-center gap-2">
                  <input 
                    type="number" 
                    min="1" 
                    max="60" 
                    value={config.captureInterval} 
                    onChange={e => setConfig({ ...config, captureInterval: Math.max(1, Math.min(60, parseInt(e.target.value) || 1)) })}
                    className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                  <select 
                    value={config.captureIntervalUnit} 
                    onChange={e => setConfig({ ...config, captureIntervalUnit: e.target.value })}
                    className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="Minutes">Minutes</option>
                  </select>
                </div>
              </div>

              {/* Row 3: Image Quality */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Image Quality</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">JPEG quality (affects file size).</div>
                </div>
                <div className="flex items-center gap-3 w-full sm:w-64">
                  <input 
                    type="range" 
                    min="10" 
                    max="100" 
                    value={config.imageQuality} 
                    onChange={e => setConfig({ ...config, imageQuality: parseInt(e.target.value) || 75 })}
                    className="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-600"
                  />
                  <span className="text-xs font-bold text-slate-700 dark:text-slate-200 min-w-[38px] text-right">
                    {config.imageQuality}%
                  </span>
                </div>
              </div>

              {/* Row 4: Image Format */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Image Format</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Screenshot image format.</div>
                </div>
                <select 
                  value={config.imageFormat} 
                  onChange={e => setConfig({ ...config, imageFormat: e.target.value })}
                  className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 font-medium focus:outline-none focus:ring-1 focus:ring-blue-500"
                >
                  <option value="JPEG">JPEG (Recommended)</option>
                  <option value="PNG">PNG</option>
                </select>
              </div>

              {/* Row 5: Capture During States */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Capture During States</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Select when screenshots should be captured.</div>
                </div>
                <div className="flex items-center gap-4">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input 
                      type="checkbox" 
                      checked={config.captureDuringWorking} 
                      onChange={e => setConfig({ ...config, captureDuringWorking: e.target.checked })}
                      className="w-4 h-4 text-blue-600 rounded border-slate-300 dark:border-slate-700 focus:ring-blue-500 accent-blue-600"
                    />
                    <span className="text-xs font-medium text-slate-700 dark:text-slate-200">Working</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input 
                      type="checkbox" 
                      checked={config.captureDuringIdle} 
                      onChange={e => setConfig({ ...config, captureDuringIdle: e.target.checked })}
                      className="w-4 h-4 text-blue-600 rounded border-slate-300 dark:border-slate-700 focus:ring-blue-500 accent-blue-600"
                    />
                    <span className="text-xs font-medium text-slate-700 dark:text-slate-200">Idle</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input 
                      type="checkbox" 
                      checked={config.captureDuringBreak} 
                      onChange={e => setConfig({ ...config, captureDuringBreak: e.target.checked })}
                      className="w-4 h-4 text-blue-600 rounded border-slate-300 dark:border-slate-700 focus:ring-blue-500 accent-blue-600"
                    />
                    <span className="text-xs font-medium text-slate-700 dark:text-slate-200">Break</span>
                  </label>
                </div>
              </div>

              {/* Row 6: Capture When Logged Out */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Capture When Logged Out</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Allow capturing when employee is logged out.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.captureWhenLoggedOut} 
                    onChange={e => setConfig({ ...config, captureWhenLoggedOut: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.captureWhenLoggedOut ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

            </div>
          </div>


          {/* ========================================================= */}
          {/* CARD 2: Retention and Automatic Cleanup */}
          {/* ========================================================= */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2.5 pb-4 border-b border-slate-100 dark:border-slate-800">
                <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
                  <Database className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="text-sm font-bold text-slate-900 dark:text-white">2. Retention and Automatic Cleanup</h2>
                  <p className="text-xs text-slate-500 dark:text-slate-400">Configure how long screenshots should be retained and cleanup settings.</p>
                </div>
              </div>

              <div className="divide-y divide-slate-100 dark:divide-slate-800/60 text-xs">
                
                {/* Row 1: Retention Period */}
                <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-slate-800 dark:text-slate-100">Screenshot Retention Period</div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400">Automatically delete screenshots older than this period.</div>
                  </div>
                  <div className="flex items-center gap-2">
                    <input 
                      type="number" 
                      min="1" 
                      max="365" 
                      value={config.retentionPeriod} 
                      onChange={e => setConfig({ ...config, retentionPeriod: Math.max(1, parseInt(e.target.value) || 30) })}
                      className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                    />
                    <select 
                      value={config.retentionPeriodUnit} 
                      onChange={e => setConfig({ ...config, retentionPeriodUnit: e.target.value })}
                      className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                    >
                      <option value="Days">Days</option>
                      <option value="Months">Months</option>
                    </select>
                  </div>
                </div>

                {/* Row 2: Automatic Cleanup Toggle */}
                <div className="py-3.5 flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-slate-800 dark:text-slate-100">Automatic Cleanup</div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400">Enable scheduled cleanup of old screenshots.</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input 
                      type="checkbox" 
                      checked={config.automaticCleanup} 
                      onChange={e => setConfig({ ...config, automaticCleanup: e.target.checked })} 
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                    <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                      {config.automaticCleanup ? 'Enabled' : 'Disabled'}
                    </span>
                  </label>
                </div>

                {/* Row 3: Cleanup Schedule */}
                <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-slate-800 dark:text-slate-100">Cleanup Schedule</div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400">Run cleanup automatically at specified time.</div>
                  </div>
                  <div className="flex items-center gap-2">
                    <select 
                      value={config.cleanupScheduleTime} 
                      onChange={e => setConfig({ ...config, cleanupScheduleTime: e.target.value })}
                      className="px-2.5 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500 font-medium"
                    >
                      <option value="01:00 AM">01:00 AM</option>
                      <option value="02:00 AM">02:00 AM</option>
                      <option value="03:00 AM">03:00 AM</option>
                      <option value="04:00 AM">04:00 AM</option>
                      <option value="12:00 AM">12:00 AM (Midnight)</option>
                    </select>
                    <select 
                      value={config.cleanupTimeZone} 
                      onChange={e => setConfig({ ...config, cleanupTimeZone: e.target.value })}
                      className="px-2.5 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                    >
                      <option value="Asia/Kolkata (IST)">Asia/Kolkata (IST)</option>
                      <option value="UTC">UTC</option>
                      <option value="America/New_York (EST)">America/New_York (EST)</option>
                    </select>
                  </div>
                </div>

                {/* Row 4: Cleanup Batch Size */}
                <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-slate-800 dark:text-slate-100">Cleanup Batch Size</div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400">Number of screenshots to delete per batch.</div>
                  </div>
                  <input 
                    type="number" 
                    min="100" 
                    max="5000" 
                    step="100"
                    value={config.cleanupBatchSize} 
                    onChange={e => setConfig({ ...config, cleanupBatchSize: parseInt(e.target.value) || 1000 })}
                    className="w-28 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                  />
                </div>

                {/* Row 5: Storage Usage Warning */}
                <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold text-slate-800 dark:text-slate-100">Storage Usage Warning</div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400">Get notified when storage usage exceeds this limit.</div>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <input 
                      type="number" 
                      min="50" 
                      max="95" 
                      value={config.storageUsageWarning} 
                      onChange={e => setConfig({ ...config, storageUsageWarning: parseInt(e.target.value) || 80 })}
                      className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                    />
                    <span className="text-xs text-slate-500 font-medium">%</span>
                  </div>
                </div>

              </div>
            </div>

            {/* Amber Warning Box & Preview Cleanup */}
            <div className="mt-4 p-3.5 bg-amber-50/80 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/50 rounded-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <div className="flex items-start gap-2.5">
                <Trash2 className="w-5 h-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
                <div>
                  <div className="text-xs font-bold text-amber-900 dark:text-amber-200">
                    This will automatically delete screenshots older than {config.retentionPeriod} days.
                  </div>
                  <div className="text-[11px] text-amber-700/80 dark:text-amber-300/70">
                    A preview can be generated below to see how many screenshots will be deleted.
                  </div>
                </div>
              </div>
              <button 
                type="button"
                onClick={openCleanupPreview}
                className="shrink-0 px-3 py-1.5 rounded-lg border border-blue-600 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-950/50 bg-white dark:bg-[#151E2E] text-xs font-semibold transition flex items-center gap-1.5 shadow-xs"
              >
                <Search className="w-3.5 h-3.5" />
                <span>Preview Cleanup</span>
              </button>
            </div>
          </div>


          {/* ========================================================= */}
          {/* CARD 3: Storage and Performance */}
          {/* ========================================================= */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center gap-2.5 pb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
                <HardDrive className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-slate-900 dark:text-white">3. Storage and Performance</h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">Configure storage limits, thumbnail settings and performance options.</p>
              </div>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-slate-800/60 text-xs">
              
              {/* Row 1: Maximum Upload Size */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Maximum Upload Size</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Maximum size per screenshot image.</div>
                </div>
                <div className="flex items-center gap-1.5">
                  <input 
                    type="number" 
                    min="1" 
                    max="25" 
                    value={config.maxUploadSizeMb} 
                    onChange={e => setConfig({ ...config, maxUploadSizeMb: parseInt(e.target.value) || 5 })}
                    className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                  />
                  <span className="text-xs text-slate-500 font-medium">MB</span>
                </div>
              </div>

              {/* Row 2: Thumbnail Generation */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Thumbnail Generation</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Generate thumbnails for faster loading.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.thumbnailGeneration} 
                    onChange={e => setConfig({ ...config, thumbnailGeneration: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.thumbnailGeneration ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

              {/* Row 3: Thumbnail Dimensions */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Thumbnail Dimensions</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Thumbnail image size (width x height).</div>
                </div>
                <select 
                  value={config.thumbnailDimensions} 
                  onChange={e => setConfig({ ...config, thumbnailDimensions: e.target.value })}
                  className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 font-medium focus:outline-none focus:ring-1 focus:ring-blue-500"
                >
                  <option value="320 x 180">320 x 180 (Recommended)</option>
                  <option value="480 x 270">480 x 270</option>
                  <option value="640 x 360">640 x 360</option>
                </select>
              </div>

              {/* Row 4: Thumbnail Image Quality */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Thumbnail Image Quality</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">JPEG quality for thumbnails.</div>
                </div>
                <div className="flex items-center gap-3 w-full sm:w-64">
                  <input 
                    type="range" 
                    min="30" 
                    max="90" 
                    value={config.thumbnailQuality} 
                    onChange={e => setConfig({ ...config, thumbnailQuality: parseInt(e.target.value) || 70 })}
                    className="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-600"
                  />
                  <span className="text-xs font-bold text-slate-700 dark:text-slate-200 min-w-[38px] text-right">
                    {config.thumbnailQuality}%
                  </span>
                </div>
              </div>

              {/* Row 5: Storage Metrics Widget */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Storage Metrics</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Current screenshot storage usage.</div>
                </div>
                <div className="flex items-center gap-3 px-3.5 py-2 rounded-xl bg-slate-50 dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700/80">
                  <div className="p-1.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
                    <Database className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-slate-900 dark:text-white">
                      {storageMetrics.totalSizeGb > 0 ? `${storageMetrics.totalSizeGb} GB` : `${storageMetrics.totalSizeMb} MB`}
                    </div>
                    <div className="text-[10px] text-slate-500 dark:text-slate-400">Total Storage Used</div>
                  </div>
                  <button 
                    type="button"
                    onClick={() => setShowStorageModal(true)}
                    className="ml-2 text-xs font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400 underline underline-offset-2"
                  >
                    View Details
                  </button>
                </div>
              </div>

            </div>
          </div>


          {/* ========================================================= */}
          {/* CARD 4: Offline Upload and Synchronization */}
          {/* ========================================================= */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center gap-2.5 pb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
                <CloudUpload className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-slate-900 dark:text-white">4. Offline Upload and Synchronization</h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">Configure offline screenshot storage and upload behavior in the employee agent.</p>
              </div>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-slate-800/60 text-xs">
              
              {/* Row 1: Enable Offline Capture */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Enable Offline Capture</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Allow capturing screenshots when server is unavailable.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.enableOfflineCapture} 
                    onChange={e => setConfig({ ...config, enableOfflineCapture: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.enableOfflineCapture ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

              {/* Row 2: Local Storage Limit */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Local Storage Limit</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Maximum local storage for pending uploads.</div>
                </div>
                <div className="flex items-center gap-2">
                  <input 
                    type="number" 
                    min="1" 
                    max="10" 
                    value={config.localStorageLimit} 
                    onChange={e => setConfig({ ...config, localStorageLimit: parseInt(e.target.value) || 2 })}
                    className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                  />
                  <select 
                    value={config.localStorageLimitUnit} 
                    onChange={e => setConfig({ ...config, localStorageLimitUnit: e.target.value })}
                    className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="GB">GB</option>
                  </select>
                </div>
              </div>

              {/* Row 3: Retry Interval */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Retry Interval</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Time between upload retry attempts.</div>
                </div>
                <div className="flex items-center gap-2">
                  <input 
                    type="number" 
                    min="1" 
                    max="60" 
                    value={config.retryInterval} 
                    onChange={e => setConfig({ ...config, retryInterval: parseInt(e.target.value) || 5 })}
                    className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                  />
                  <select 
                    value={config.retryIntervalUnit} 
                    onChange={e => setConfig({ ...config, retryIntervalUnit: e.target.value })}
                    className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="Minutes">Minutes</option>
                  </select>
                </div>
              </div>

              {/* Row 4: Max Retry Attempts */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Max Retry Attempts</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Maximum retry attempts for failed uploads.</div>
                </div>
                <input 
                  type="number" 
                  min="1" 
                  max="50" 
                  value={config.maxRetryAttempts} 
                  onChange={e => setConfig({ ...config, maxRetryAttempts: parseInt(e.target.value) || 10 })}
                  className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                />
              </div>

              {/* Row 5: Auto Sync on Startup */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Auto Sync on Startup</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Automatically upload pending screenshots on agent startup.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.autoSyncOnStartup} 
                    onChange={e => setConfig({ ...config, autoSyncOnStartup: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.autoSyncOnStartup ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

            </div>
          </div>


          {/* ========================================================= */}
          {/* CARD 5: Access Control */}
          {/* ========================================================= */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center gap-2.5 pb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
                <Shield className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-slate-900 dark:text-white">5. Access Control</h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">Configure who can view and manage screenshots.</p>
              </div>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-slate-800/60 text-xs">
              
              {/* Row 1: Allow Screenshot Download */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Allow Screenshot Download</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Allow administrators to download full-size screenshots.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.allowScreenshotDownload} 
                    onChange={e => setConfig({ ...config, allowScreenshotDownload: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.allowScreenshotDownload ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

              {/* Row 2: Restrict to Admin Users Only */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Restrict to Admin Users Only</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Only users with admin role can view screenshots.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.restrictToAdminUsersOnly} 
                    onChange={e => setConfig({ ...config, restrictToAdminUsersOnly: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.restrictToAdminUsersOnly ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

              {/* Row 3: Audit Configuration Changes */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Audit Configuration Changes</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Track and log all configuration changes.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.auditConfigurationChanges} 
                    onChange={e => setConfig({ ...config, auditConfigurationChanges: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.auditConfigurationChanges ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

            </div>
          </div>


          {/* ========================================================= */}
          {/* CARD 6: Configuration Actions */}
          {/* ========================================================= */}
          <div className="bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
                    <Settings className="w-5 h-5" />
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-slate-900 dark:text-white">6. Configuration Actions</h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Save changes and manage configuration.</p>
                  </div>
                </div>

                <div className="flex items-center gap-2 px-3 py-1 bg-blue-50 dark:bg-blue-950/40 border border-blue-200/80 dark:border-blue-800/80 rounded-lg text-xs">
                  <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-blue-600 text-white">Last Updated</span>
                  <div className="text-[11px] text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>{formatDate(config.updatedAt)}</span>
                    <span className="text-slate-400">Updated by {config.updatedBy || 'Admin'}</span>
                  </div>
                </div>
              </div>

              <div className="py-4 text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                Changes made here immediately synchronize to the Employee Agent without requiring application reinstall or system restart.
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-wrap items-center justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
              <button 
                type="button"
                onClick={handleReset}
                className="px-4 py-2 rounded-lg border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 text-xs font-semibold flex items-center gap-1.5 transition shadow-xs"
              >
                <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
                <span>Reset to Default</span>
              </button>

              <button 
                type="button"
                onClick={handleCancel}
                className="px-4 py-2 rounded-lg border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 text-xs font-semibold transition shadow-xs"
              >
                Cancel
              </button>

              <button 
                type="button"
                disabled={saving}
                onClick={handleSave}
                className="px-5 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold flex items-center gap-2 transition shadow-sm disabled:opacity-50"
              >
                {saving ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Saving...</span>
                  </>
                ) : (
                  <>
                    <Save className="w-3.5 h-3.5" />
                    <span>Save Changes</span>
                  </>
                )}
              </button>
            </div>
          </div>

        </div>


        {/* ========================================================= */}
        {/* CARD 7: Attendance & Idle Policy (Full Width) */}
        {/* ========================================================= */}
        <div className="mt-6 bg-white dark:bg-[#151E2E] border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm hover:shadow-md transition">
          <div className="flex items-center gap-2.5 pb-4 border-b border-slate-100 dark:border-slate-800">
            <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-900 dark:text-white">7. Attendance & Idle Policy</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">Configure working hours and idle detection policy used for employee monitoring and attendance calculations.</p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-x-12 divide-y lg:divide-y-0 divide-slate-100 dark:divide-slate-800/60 text-xs pt-2">
            
            {/* Left Column */}
            <div className="divide-y divide-slate-100 dark:divide-slate-800/60">
              
              {/* Daily Required Working Hours */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Daily Required Working Hours</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Total working hours expected per day.</div>
                </div>
                <div className="flex items-center gap-2">
                  <input 
                    type="number" 
                    min="1" 
                    max="24" 
                    value={config.dailyRequiredWorkingHours} 
                    onChange={e => setConfig({ ...config, dailyRequiredWorkingHours: Math.max(1, parseInt(e.target.value) || 9) })}
                    className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                  />
                  <select 
                    value={config.dailyRequiredWorkingHoursUnit} 
                    onChange={e => setConfig({ ...config, dailyRequiredWorkingHoursUnit: e.target.value })}
                    className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="Hours">Hours</option>
                  </select>
                </div>
              </div>

              {/* Idle Detection Threshold */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Idle Detection Threshold</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Idle begins after no user activity for this period.</div>
                </div>
                <div className="flex items-center gap-2">
                  <input 
                    type="number" 
                    min="1" 
                    max="60" 
                    value={config.idleDetectionThreshold} 
                    onChange={e => setConfig({ ...config, idleDetectionThreshold: Math.max(1, parseInt(e.target.value) || 2) })}
                    className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                  />
                  <select 
                    value={config.idleDetectionThresholdUnit} 
                    onChange={e => setConfig({ ...config, idleDetectionThresholdUnit: e.target.value })}
                    className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="Minutes">Minutes</option>
                  </select>
                </div>
              </div>

            </div>

            {/* Right Column */}
            <div className="divide-y divide-slate-100 dark:divide-slate-800/60">
              
              {/* Mark Offline After Continuous Idle */}
              <div className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Mark Offline After Continuous Idle</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">If continuous idle exceeds this time, employee/device status automatically changes to Offline.</div>
                </div>
                <div className="flex items-center gap-2">
                  <input 
                    type="number" 
                    min="5" 
                    max="180" 
                    value={config.markOfflineAfterContinuousIdle} 
                    onChange={e => setConfig({ ...config, markOfflineAfterContinuousIdle: Math.max(5, parseInt(e.target.value) || 30) })}
                    className="w-20 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-semibold text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-right"
                  />
                  <select 
                    value={config.markOfflineAfterContinuousIdleUnit} 
                    onChange={e => setConfig({ ...config, markOfflineAfterContinuousIdleUnit: e.target.value })}
                    className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="Minutes">Minutes</option>
                  </select>
                </div>
              </div>

              {/* Enable Automatic Offline Status */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Enable Automatic Offline Status</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Automatically mark employee/device as Offline after continuous idle.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.enableAutomaticOfflineStatus} 
                    onChange={e => setConfig({ ...config, enableAutomaticOfflineStatus: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.enableAutomaticOfflineStatus ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

              {/* Apply Policy Per Employee/Department */}
              <div className="py-3.5 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-800 dark:text-slate-100">Apply Policy Per Employee/Department</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Use different policies for specific employees or departments (optional).</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={config.applyPolicyPerEmployee} 
                    onChange={e => setConfig({ ...config, applyPolicyPerEmployee: e.target.checked })} 
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  <span className="ml-2.5 text-xs font-medium text-slate-600 dark:text-slate-300 w-16">
                    {config.applyPolicyPerEmployee ? 'Enabled' : 'Disabled'}
                  </span>
                </label>
              </div>

            </div>

          </div>

          {/* Bottom Alert Banner */}
          <div className="mt-4 p-3 bg-amber-50/80 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/50 rounded-xl flex items-center gap-2.5 text-xs text-amber-900 dark:text-amber-200">
            <Info className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0" />
            <span>
              Offline status is applied after <strong>{config.markOfflineAfterContinuousIdle} minutes</strong> of continuous idle. Attendance events and original timestamps are preserved.
            </span>
          </div>

        </div>

      </div>

      {/* ========================================================= */}
      {/* MODAL 1: History Modal */}
      {/* ========================================================= */}
      {showHistoryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
          <div className="bg-white dark:bg-[#151E2E] rounded-2xl max-w-2xl w-full border border-slate-200 dark:border-slate-800 shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <History className="w-5 h-5 text-blue-600" />
                <h3 className="font-bold text-slate-900 dark:text-white">Configuration Version History</h3>
              </div>
              <button 
                onClick={() => setShowHistoryModal(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 max-h-[60vh] overflow-y-auto">
              {loadingHistory ? (
                <div className="py-8 text-center text-xs text-slate-500">Loading audit history...</div>
              ) : historyList.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-500">
                  Current version is {config.versionString || 'v2.1.0'}. No previous snapshots recorded yet.
                </div>
              ) : (
                <div className="space-y-3">
                  {historyList.map(h => (
                    <div key={h.id} className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 text-xs">
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-bold text-blue-600 dark:text-blue-400">{h.versionString || `v2.${h.configurationVersion}.0`}</span>
                        <span className="text-slate-400">{formatDate(h.changedAt)}</span>
                      </div>
                      <div className="text-slate-600 dark:text-slate-300">
                        {h.changeSummary || 'Configuration modified'}
                      </div>
                      <div className="text-[10px] text-slate-400 mt-1">
                        Updated by: {h.changedBy || 'Admin'}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="px-6 py-3 border-t border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50 flex justify-end">
              <button 
                onClick={() => setShowHistoryModal(false)}
                className="px-4 py-2 rounded-lg bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-200 text-xs font-semibold hover:bg-slate-300 transition"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL 2: Cleanup Preview Modal */}
      {/* ========================================================= */}
      {showCleanupModal && cleanupPreview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
          <div className="bg-white dark:bg-[#151E2E] rounded-2xl max-w-md w-full border border-slate-200 dark:border-slate-800 shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Trash2 className="w-5 h-5 text-amber-600" />
                <h3 className="font-bold text-slate-900 dark:text-white">Screenshot Cleanup Preview</h3>
              </div>
              <button 
                onClick={() => setShowCleanupModal(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-4 text-xs">
              <div className="p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900/50 rounded-xl text-amber-900 dark:text-amber-200">
                <div className="font-bold text-sm mb-1">
                  {cleanupPreview.countToDelete} screenshot(s) match cleanup policy
                </div>
                <p className="text-[11px] leading-relaxed text-amber-800 dark:text-amber-300/80">
                  Screenshots older than {cleanupPreview.retentionDays} days (before {formatDate(cleanupPreview.cutoffDate)}) will be permanently deleted.
                </p>
              </div>

              <div className="space-y-2 text-slate-600 dark:text-slate-400 text-xs">
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                  <span>Retention Threshold:</span>
                  <span className="font-semibold text-slate-900 dark:text-white">{cleanupPreview.retentionDays} Days</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                  <span>Target Deletion Batch Size:</span>
                  <span className="font-semibold text-slate-900 dark:text-white">{config.cleanupBatchSize}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                  <span>Cutoff Date:</span>
                  <span className="font-semibold text-slate-900 dark:text-white">{formatDate(cleanupPreview.cutoffDate)}</span>
                </div>
              </div>
            </div>

            <div className="px-6 py-3 border-t border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50 flex items-center justify-end gap-2">
              <button 
                onClick={() => setShowCleanupModal(false)}
                className="px-4 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold hover:bg-slate-100 dark:hover:bg-slate-700 transition"
              >
                Close
              </button>
              {cleanupPreview.countToDelete > 0 && (
                <button 
                  disabled={executingCleanup}
                  onClick={executeCleanup}
                  className="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-xs font-semibold flex items-center gap-1.5 transition disabled:opacity-50"
                >
                  {executingCleanup ? 'Deleting...' : 'Delete Now'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL 3: Storage Details Modal */}
      {/* ========================================================= */}
      {showStorageModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
          <div className="bg-white dark:bg-[#151E2E] rounded-2xl max-w-md w-full border border-slate-200 dark:border-slate-800 shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Database className="w-5 h-5 text-emerald-600" />
                <h3 className="font-bold text-slate-900 dark:text-white">Screenshot Storage Details</h3>
              </div>
              <button 
                onClick={() => setShowStorageModal(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-3 text-xs">
              <div className="flex justify-between py-2 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500">Total Screenshots Stored:</span>
                <span className="font-bold text-slate-900 dark:text-white">{storageMetrics.totalScreenshots}</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500">Total Space Used (MB):</span>
                <span className="font-bold text-slate-900 dark:text-white">{storageMetrics.totalSizeMb} MB</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500">Total Space Used (GB):</span>
                <span className="font-bold text-emerald-600 dark:text-emerald-400">{storageMetrics.totalSizeGb} GB</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500">Oldest Screenshot:</span>
                <span className="font-medium text-slate-700 dark:text-slate-300">
                  {storageMetrics.oldestScreenshotDate ? formatDate(storageMetrics.oldestScreenshotDate) : 'No screenshots stored yet'}
                </span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-100 dark:border-slate-800">
                <span className="text-slate-500">Newest Screenshot:</span>
                <span className="font-medium text-slate-700 dark:text-slate-300">
                  {storageMetrics.newestScreenshotDate ? formatDate(storageMetrics.newestScreenshotDate) : 'No screenshots stored yet'}
                </span>
              </div>
            </div>

            <div className="px-6 py-3 border-t border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50 flex justify-end">
              <button 
                onClick={() => setShowStorageModal(false)}
                className="px-4 py-2 rounded-lg bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-200 text-xs font-semibold hover:bg-slate-300 transition"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
