import { useState } from 'react'
import type { EmployerProfileResponse } from '../../types/api'
import { updateProfile } from '../../api/users'
import Input from '../ui/Input'
import Select from '../ui/Select'
import Textarea from '../ui/Textarea'
import Button from '../ui/Button'
import ErrorBanner from '../ui/ErrorBanner'
import PhotoUpload from './PhotoUpload'

interface Props {
  profile: EmployerProfileResponse
  onSaved: (updated: EmployerProfileResponse) => void
}

const COMPANY_SIZES = [
  { value: '', label: 'Select size…' },
  { value: '1-10', label: '1–10 employees' },
  { value: '11-50', label: '11–50 employees' },
  { value: '51-200', label: '51–200 employees' },
  { value: '201-500', label: '201–500 employees' },
  { value: '501-1000', label: '501–1000 employees' },
  { value: '1001+', label: '1001+ employees' },
]

const INDUSTRIES = [
  { value: '', label: 'Select industry…' },
  ...['Technology', 'Finance', 'Healthcare', 'Education', 'Retail', 'Manufacturing',
      'Media', 'Consulting', 'Real Estate', 'Other'].map((v) => ({ value: v, label: v })),
]

export default function EmployerProfileForm({ profile, onSaved }: Props) {
  const [companyName, setCompanyName] = useState(profile.companyName ?? '')
  const [companyDescription, setCompanyDescription] = useState(profile.companyDescription ?? '')
  const [companyWebsite, setCompanyWebsite] = useState(profile.companyWebsite ?? '')
  const [companySize, setCompanySize] = useState(profile.companySize ?? '')
  const [industry, setIndustry] = useState(profile.industry ?? '')
  const [foundedYear, setFoundedYear] = useState(String(profile.foundedYear ?? ''))
  const [photoUrl, setPhotoUrl] = useState(profile.photoUrl)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [nameError, setNameError] = useState('')

  const handleSubmit = async (e: { preventDefault(): void }) => {
    e.preventDefault()
    if (!companyName.trim()) { setNameError('Company name is required.'); return }
    setNameError('')
    setError('')
    setSaving(true)
    try {
      const { data } = await updateProfile({
        companyName: companyName.trim(),
        companyDescription: companyDescription || undefined,
        companyWebsite: companyWebsite || undefined,
        companySize: companySize || undefined,
        industry: industry || undefined,
        foundedYear: foundedYear ? Number(foundedYear) : undefined,
      })
      onSaved(data as EmployerProfileResponse)
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch {
      setError('Failed to save profile. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <PhotoUpload
        currentUrl={photoUrl}
        name={companyName || profile.email}
        onUploaded={(url) => setPhotoUrl(url)}
      />

      {error && <ErrorBanner message={error} onDismiss={() => setError('')} />}
      {saved && (
        <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg p-3">
          Profile updated successfully.
        </div>
      )}

      <Input
        label="Company name *"
        value={companyName}
        onChange={(e) => { setCompanyName(e.target.value); setNameError('') }}
        placeholder="Acme Corp"
        error={nameError}
      />

      <Textarea
        label="Company description"
        value={companyDescription}
        onChange={(e) => setCompanyDescription(e.target.value)}
        rows={4}
        placeholder="What does your company do?"
      />

      <Input
        label="Company website"
        type="url"
        value={companyWebsite}
        onChange={(e) => setCompanyWebsite(e.target.value)}
        placeholder="https://acme.com"
      />

      <div className="grid grid-cols-2 gap-4">
        <Select
          label="Company size"
          value={companySize}
          onChange={(e) => setCompanySize(e.target.value)}
          options={COMPANY_SIZES}
        />
        <Select
          label="Industry"
          value={industry}
          onChange={(e) => setIndustry(e.target.value)}
          options={INDUSTRIES}
        />
      </div>

      <Input
        label="Founded year"
        type="number"
        value={foundedYear}
        onChange={(e) => setFoundedYear(e.target.value)}
        placeholder="2015"
      />

      <Button type="submit" loading={saving} className="w-full">
        Save profile
      </Button>
    </form>
  )
}
