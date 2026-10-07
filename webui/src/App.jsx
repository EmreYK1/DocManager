import { Route, Routes } from 'react-router-dom'
import DashboardPage from './pages/DashboardPage.jsx'
import DocumentDetailPage from './pages/DocumentDetailPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/documents/:id" element={<DocumentDetailPage />} />
    </Routes>
  )
}
