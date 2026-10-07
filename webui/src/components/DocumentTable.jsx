const dateFormatter = new Intl.DateTimeFormat('de-DE', {
  dateStyle: 'short',
  timeStyle: 'short',
})

export default function DocumentTable({ documents, folderNames = new Map() }) {
  if (documents.length === 0) {
    return <p>Keine Dokumente vorhanden.</p>
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Dateiname</th>
          <th>Status</th>
          <th>Ordner</th>
          <th>Upload-Datum</th>
        </tr>
      </thead>
      <tbody>
        {documents.map((document) => (
          <tr key={document.id}>
            <td>{document.filename}</td>
            <td>{document.status}</td>
            <td>{folderNames.get(document.folderId) ?? '–'}</td>
            <td>{dateFormatter.format(new Date(document.uploadedAt))}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
