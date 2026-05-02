interface Props {
  src?: string | null
  name?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  className?: string
}

const SIZES = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-16 h-16 text-xl',
  xl: 'w-24 h-24 text-2xl',
}

function initials(name?: string) {
  if (!name) return '?'
  return name
    .split(' ')
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('')
}

export default function Avatar({ src, name, size = 'md', className = '' }: Props) {
  return src ? (
    <img
      src={src}
      alt={name ?? 'avatar'}
      className={`${SIZES[size]} rounded-full object-cover shrink-0 ${className}`}
    />
  ) : (
    <div
      className={`${SIZES[size]} rounded-full bg-purple-100 text-purple-700 font-semibold flex items-center justify-center shrink-0 ${className}`}
    >
      {initials(name)}
    </div>
  )
}
