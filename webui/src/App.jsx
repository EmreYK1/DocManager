import { Route, Routes } from 'react-router-dom'
import Layout from './layout/Layout.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import DocumentDetailPage from './pages/DocumentDetailPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/documents/:id" element={<DocumentDetailPage />} />
      </Route>
    </Routes>
  )
}
