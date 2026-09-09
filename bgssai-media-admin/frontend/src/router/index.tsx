import { Navigate, Route, Routes } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import PrivateRoute from '../components/PrivateRoute';
import Login from '../pages/Login';
import DramaList from '../pages/DramaList';
import DramaForm from '../pages/DramaForm';
import DramaDetail from '../pages/DramaDetail';
import IngestLogs from '../pages/IngestLogs';

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <AdminLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<Navigate to="/dramas" replace />} />
        <Route path="dramas" element={<DramaList />} />
        <Route path="dramas/create" element={<DramaForm />} />
        <Route path="dramas/:id" element={<DramaDetail />} />
        <Route path="dramas/:id/edit" element={<DramaForm />} />
        <Route path="ingest-logs" element={<IngestLogs />} />
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
