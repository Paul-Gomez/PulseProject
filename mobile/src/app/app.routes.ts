import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'tabs',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/tabs.page').then((m) => m.TabsPage),
    children: [
      { path: 'workspaces', loadComponent: () => import('./pages/workspaces.page').then((m) => m.WorkspacesPage) },
      {
        path: 'workspaces/:workspaceId',
        loadComponent: () => import('./pages/channels.page').then((m) => m.ChannelsPage),
      },
      {
        path: 'workspaces/:workspaceId/c/:channelId',
        loadComponent: () => import('./pages/channel-chat.page').then((m) => m.ChannelChatPage),
      },
      { path: 'dm', loadComponent: () => import('./pages/conversations.page').then((m) => m.ConversationsPage) },
      {
        path: 'dm/:conversationId',
        loadComponent: () => import('./pages/conversation-chat.page').then((m) => m.ConversationChatPage),
      },
      {
        path: 'notifications',
        loadComponent: () => import('./pages/notifications.page').then((m) => m.NotificationsPage),
      },
      { path: 'profile', loadComponent: () => import('./pages/profile.page').then((m) => m.ProfilePage) },
      { path: '', redirectTo: 'workspaces', pathMatch: 'full' },
    ],
  },
  { path: '', redirectTo: 'tabs/workspaces', pathMatch: 'full' },
  { path: '**', redirectTo: 'tabs/workspaces' },
];
