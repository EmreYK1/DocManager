import DocumentTable from '../components/DocumentTable.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import { getDocuments } from '../api/documentApi.js'
import { useApiResource } from '../hooks/useApiResource.js'

export default function DashboardPage() {
  const { data, loading, error } = useApiResource(getDocuments)

  return (
    <main>
      <h1>Dokumente</h1>
      {loading && <p>Lade…</p>}
      <ErrorBanner error={error} />
      {data && <DocumentTable documents={data} />}
    </main>
  )
}
