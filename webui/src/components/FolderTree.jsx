import { Link } from 'react-router-dom'
import { buildFolderTree } from '../utils/folderTree.js'

function FolderNode({ node, selectedId, depth }) {
  return (
    <li>
      <Link
        to={`/?folder=${node.id}`}
        className={`nav-item${selectedId === node.id ? ' active' : ''}`}
        style={{ paddingLeft: `${0.75 + depth}rem` }}
      >
        <span aria-hidden="true">{node.children.length > 0 ? '📂' : '📁'}</span>
        <span className="nav-label">{node.name}</span>
      </Link>
      {node.children.length > 0 && (
        <ul>
          {node.children.map((child) => (
            <FolderNode key={child.id} node={child} selectedId={selectedId} depth={depth + 1} />
          ))}
        </ul>
      )}
    </li>
  )
}

export default function FolderTree({ folders, selectedId }) {
  const tree = buildFolderTree(folders)
  return (
    <nav className="sidebar" aria-label="Ordner">
      <ul>
        <li>
          <Link to="/" className={`nav-item${!selectedId ? ' active' : ''}`}>
            <span aria-hidden="true">🗂️</span>
            <span className="nav-label">Alle Dokumente</span>
          </Link>
        </li>
        <li>
          <Link to="/?folder=none" className={`nav-item${selectedId === 'none' ? ' active' : ''}`}>
            <span aria-hidden="true">📄</span>
            <span className="nav-label">Ohne Ordner</span>
          </Link>
        </li>
      </ul>
      <h3 className="sidebar-title">Ordner</h3>
      {tree.length === 0 ? (
        <p className="muted small">Noch keine Ordner.</p>
      ) : (
        <ul>
          {tree.map((node) => (
            <FolderNode key={node.id} node={node} selectedId={selectedId} depth={0} />
          ))}
        </ul>
      )}
    </nav>
  )
}
