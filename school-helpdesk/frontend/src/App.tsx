import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import { FullScreenLoader } from './components/Spinner';
import { useAuth } from './store/AuthContext';
import CategoriesPage from './pages/CategoriesPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import ProfilePage from './pages/ProfilePage';
import TicketDetailPage from './pages/TicketDetailPage';
import TicketFormPage from './pages/TicketFormPage';
import TicketsPage from './pages/TicketsPage';
import UsersPage from './pages/UsersPage';

export default function App() {
  const { loading, user } = useAuth();

  if (loading) {
    return <FullScreenLoader text="Загрузка School HelpDesk…" />;
  }

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />

      {/* все маршруты ниже требуют входа */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="tickets" element={<TicketsPage />} />
          <Route path="tickets/new" element={<TicketFormPage />} />
          <Route path="tickets/:id" element={<TicketDetailPage />} />
          <Route path="tickets/:id/edit" element={<TicketFormPage />} />
          <Route path="profile" element={<ProfilePage />} />

          {/* разделы администратора */}
          <Route
            path="users"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <UsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="categories"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <CategoriesPage />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}


