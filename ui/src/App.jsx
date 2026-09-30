import { Routes, Route } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import DocumentDetail from './pages/DocumentDetail'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard/>} />
      <Route path="/documents/:id" element={<DocumentDetail />} /> 
   </Routes>
  )
}

export default App