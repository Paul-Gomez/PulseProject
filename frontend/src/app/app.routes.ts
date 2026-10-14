import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guards';

// Every page is lazy-loaded, so the first download only contains what the login screen needs.
export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password.page').then((m) => m.ForgotPasswordPage),
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password.page').then((m) => m.ResetPasswordPage),
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./features/auth/verify-email.page').then((m) => m.VerifyEmailPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', loadComponent: () => import('./features/home/home.page').then((m) => m.HomePage) },
      {
        path: 'w/:workspaceId',
        loadComponent: () => import('./features/workspace/workspace.page').then((m) => m.WorkspacePage),
      },
      {
        path: 'w/:workspaceId/c/:channelId',
        loadComponent: () => import('./features/chat/channel-chat.page').then((m) => m.ChannelChatPage),
      },
      {
        path: 'w/:workspaceId/settings',
        loadComponent: () => import('./features/admin/workspace-settings.page').then((m) => m.WorkspaceSettingsPage),
      },
      {
        path: 'dm',
        loadComponent: () => import('./features/dm/conversations.page').then((m) => m.ConversationsPage),
      },
      {
        path: 'dm/:conversationId',
        loadComponent: () => import('./features/dm/conversation-chat.page').then((m) => m.ConversationChatPage),
      },
      {
        path: 'notifications',
        loadComponent: () => import('./features/notifications/notifications.page').then((m) => m.NotificationsPage),
      },
      { path: 'search', loadComponent: () => import('./features/search/search.page').then((m) => m.SearchPage) },
      { path: 'profile', loadComponent: () => import('./features/profile/profile.page').then((m) => m.ProfilePage) },
    ],
  },
  { path: '**', redirectTo: '' },
];
