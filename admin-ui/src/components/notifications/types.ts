export type NotificationCategory = 'Security' | 'Employee' | 'Tasks' | 'System';

export interface NotificationItem {
  id: string;
  title: string;
  description: string;
  category: NotificationCategory;
  timestamp: string;
  timeAgo: string;
  isRead: boolean;
  iconType: 'login' | 'offline' | 'task' | 'usage' | 'idle' | 'user' | 'system';
  link?: string;
  details?: string;
  createdAt?: string;
}
