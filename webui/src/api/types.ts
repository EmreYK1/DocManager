// Datenformen des Backends (siehe die DTOs im document-service)

export type DocumentStatus = 'UPLOADED' | 'OCR_PENDING' | 'OCR_DONE' | 'SUMMARY_DONE' | 'FAILED'

/** Eintrag in Listen und Suchtreffern, bewusst ohne Text. */
export interface DocumentSummary {
  id: string
  filename: string
  contentType: string
  sizeBytes: number
  uploadedAt: string
  status: DocumentStatus
  folderId: string | null
  objectKey: string | null
}

export interface DocumentDetails extends DocumentSummary {
  ocrText: string | null
  summary: string | null
}

export interface DocumentChanges {
  filename?: string
  folderId?: string
  clearFolder?: boolean
}

export interface FolderInput {
  name: string
  parentId: string | null
}

export interface Folder extends FolderInput {
  id: string
}

export interface CommentInput {
  author: string
  content: string
}

export interface Comment extends CommentInput {
  id: string
  createdAt: string
  documentId: string
}
