import { useState } from 'react'

interface Props {
  message: string
  onDismiss?: () => void
  className?: string
}

export default function ErrorBanner({ message, onDismiss, className = '' }: Props) {
  const [visible, setVisible] = useState(true)

  if (!visible || !message) return null

  const dismiss = () => {
    setVisible(false)
    onDismiss?.()
  }

  return (
    <div className={`flex items-start gap-3 text-sm text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3 ${className}`}>
      <span className="shrink-0 mt-0.5">⚠️</span>
      <span className="flex-1">{message}</span>
      <button onClick={dismiss} className="shrink-0 text-red-400 hover:text-red-600" aria-label="Dismiss">
        ✕
      </button>
    </div>
  )
}
