export default function FolderSelect({ label, value, onChange, folders, emptyLabel, excludeIds = new Set() }) {
  return (
    <label className="field">
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">{emptyLabel}</option>
        {folders
          .filter((folder) => !excludeIds.has(folder.id))
          .map((folder) => (
            <option key={folder.id} value={folder.id}>
              {folder.name}
            </option>
          ))}
      </select>
    </label>
  )
}
