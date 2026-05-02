import { useState, type KeyboardEvent } from 'react'

interface Props {
  value: string[]
  onChange: (skills: string[]) => void
  placeholder?: string
}

export default function SkillTagInput({ value, onChange, placeholder = 'Add a skill…' }: Props) {
  const [input, setInput] = useState('')

  const add = () => {
    const trimmed = input.trim()
    if (trimmed && !value.includes(trimmed)) {
      onChange([...value, trimmed])
    }
    setInput('')
  }

  const remove = (skill: string) => onChange(value.filter((s) => s !== skill))

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') { e.preventDefault(); add() }
    if (e.key === 'Backspace' && !input && value.length) remove(value[value.length - 1])
  }

  return (
    <div className="border border-gray-300 rounded-lg px-3 py-2 flex flex-wrap gap-1.5 focus-within:ring-2 focus-within:ring-purple-500 focus-within:border-transparent min-h-[42px]">
      {value.map((skill) => (
        <span
          key={skill}
          className="inline-flex items-center gap-1 text-xs bg-purple-50 text-purple-700 border border-purple-200 px-2 py-0.5 rounded"
        >
          {skill}
          <button
            type="button"
            onClick={() => remove(skill)}
            className="text-purple-400 hover:text-purple-700 leading-none"
            aria-label={`Remove ${skill}`}
          >
            ✕
          </button>
        </span>
      ))}
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={handleKey}
        onBlur={add}
        placeholder={value.length === 0 ? placeholder : ''}
        className="flex-1 min-w-[120px] text-sm outline-none bg-transparent"
      />
    </div>
  )
}
