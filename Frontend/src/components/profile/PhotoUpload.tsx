import { useRef, useState } from 'react'
import Avatar from '../ui/Avatar'
import Spinner from '../ui/Spinner'
import { requestPhotoUpload, confirmPhotoUpload } from '../../api/users'

const MAX_BYTES = 5 * 1024 * 1024 // 5 MB

interface Props {
  currentUrl?: string | null
  name?: string
  onUploaded: (publicUrl: string) => void
}

export default function PhotoUpload({ currentUrl, name, onUploaded }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')

  const handleFile = async (file: File) => {
    setError('')
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setError('Only JPEG and PNG files are allowed.')
      return
    }
    if (file.size > MAX_BYTES) {
      setError('File must be under 5 MB.')
      return
    }
    setUploading(true)
    try {
      const { data: uploadData } = await requestPhotoUpload(
        file.type as 'image/jpeg' | 'image/png'
      )
      // PUT directly to S3 — no auth header
      await fetch(uploadData.presignedUrl, {
        method: 'PUT',
        headers: { 'Content-Type': file.type },
        body: file,
      })
      await confirmPhotoUpload(uploadData.publicUrl)
      onUploaded(uploadData.publicUrl)
    } catch {
      setError('Upload failed. Please try again.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="flex items-center gap-4">
      <div className="relative">
        <Avatar src={currentUrl} name={name} size="xl" />
        {uploading && (
          <div className="absolute inset-0 rounded-full bg-white/70 flex items-center justify-center">
            <Spinner size="sm" />
          </div>
        )}
      </div>
      <div>
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={uploading}
          className="text-sm text-purple-600 font-medium hover:underline disabled:opacity-50"
        >
          {uploading ? 'Uploading…' : 'Change photo'}
        </button>
        <p className="text-xs text-gray-400 mt-0.5">JPEG or PNG, max 5 MB</p>
        {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png"
          className="hidden"
          onChange={(e) => e.target.files?.[0] && handleFile(e.target.files[0])}
        />
      </div>
    </div>
  )
}
