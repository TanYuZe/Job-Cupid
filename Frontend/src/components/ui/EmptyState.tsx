import type { ReactNode } from 'react'

interface Props {
  icon?: string
  heading: string
  subtext?: string
  action?: ReactNode
  className?: string
}

export default function EmptyState({ icon, heading, subtext, action, className = '' }: Props) {
  return (
    <div className={`text-center py-20 ${className}`}>
      {icon && <p className="text-4xl mb-3">{icon}</p>}
      <p className="text-lg font-medium text-gray-700 dark:text-gray-300 mb-1">{heading}</p>
      {subtext && <p className="text-sm text-gray-400 dark:text-gray-500 mb-6">{subtext}</p>}
      {action && <div className="flex justify-center">{action}</div>}
    </div>
  )
}
