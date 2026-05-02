import { useState } from 'react'
import type { CandidateProfileResponse } from '../../types/api'
import { updateProfile } from '../../api/users'
import Input from '../ui/Input'
import Select from '../ui/Select'
import Button from '../ui/Button'
import ErrorBanner from '../ui/ErrorBanner'
import SkillTagInput from './SkillTagInput'
import PhotoUpload from './PhotoUpload'

interface Props {
  profile: CandidateProfileResponse
  onSaved: (updated: CandidateProfileResponse) => void
}

const YOE_OPTIONS = Array.from({ length: 21 }, (_, i) => ({
  value: String(i),
  label: i === 0 ? 'Less than 1 year' : `${i} year${i > 1 ? 's' : ''}`,
}))

export default function CandidateProfileForm({ profile, onSaved }: Props) {
  const [headline, setHeadline] = useState(profile.headline ?? '')
  const [skills, setSkills] = useState<string[]>(profile.skills ?? [])
  const [yoe, setYoe] = useState(String(profile.yearsOfExperience ?? 0))
  const [salaryMin, setSalaryMin] = useState(String(profile.desiredSalaryMin ?? ''))
  const [salaryMax, setSalaryMax] = useState(String(profile.desiredSalaryMax ?? ''))
  const [preferredRemote, setPreferredRemote] = useState(profile.preferredRemote)
  const [preferredLocation, setPreferredLocation] = useState(profile.preferredLocation ?? '')
  const [isOpenToWork, setIsOpenToWork] = useState(profile.isOpenToWork)
  const [photoUrl, setPhotoUrl] = useState(profile.photoUrl)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)

  const validate = () => {
    if (salaryMin && salaryMax && Number(salaryMin) > Number(salaryMax)) {
      setError('Desired salary min cannot exceed max.')
      return false
    }
    return true
  }

  const handleSubmit = async (e: { preventDefault(): void }) => {
    e.preventDefault()
    if (!validate()) return
    setError('')
    setSaving(true)
    try {
      const { data } = await updateProfile({
        headline: headline || undefined,
        skills,
        yearsOfExperience: Number(yoe),
        desiredSalaryMin: salaryMin ? Number(salaryMin) : undefined,
        desiredSalaryMax: salaryMax ? Number(salaryMax) : undefined,
        preferredRemote,
        preferredLocation: preferredLocation || undefined,
        isOpenToWork,
      })
      onSaved(data as CandidateProfileResponse)
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
        name={profile.email}
        onUploaded={(url) => setPhotoUrl(url)}
      />

      {error && <ErrorBanner message={error} onDismiss={() => setError('')} />}
      {saved && (
        <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg p-3">
          Profile updated successfully.
        </div>
      )}

      <Input
        label="Headline"
        value={headline}
        onChange={(e) => setHeadline(e.target.value)}
        placeholder="e.g. Senior Full-Stack Engineer"
      />

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Skills</label>
        <SkillTagInput value={skills} onChange={setSkills} />
        <p className="mt-1 text-xs text-gray-400">Press Enter or Tab to add a skill</p>
      </div>

      <Select
        label="Years of experience"
        value={yoe}
        onChange={(e) => setYoe(e.target.value)}
        options={YOE_OPTIONS}
      />

      <div className="grid grid-cols-2 gap-4">
        <Input
          label="Desired salary min (annual)"
          type="number"
          value={salaryMin}
          onChange={(e) => setSalaryMin(e.target.value)}
          placeholder="60000"
        />
        <Input
          label="Desired salary max (annual)"
          type="number"
          value={salaryMax}
          onChange={(e) => setSalaryMax(e.target.value)}
          placeholder="100000"
        />
      </div>

      <Input
        label="Preferred location"
        value={preferredLocation}
        onChange={(e) => setPreferredLocation(e.target.value)}
        placeholder="e.g. Singapore, Remote"
      />

      <div className="flex flex-col gap-3">
        <label className="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            checked={preferredRemote}
            onChange={(e) => setPreferredRemote(e.target.checked)}
            className="w-4 h-4 accent-purple-600"
          />
          <span className="text-sm text-gray-700">Open to remote work</span>
        </label>
        <label className="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            checked={isOpenToWork}
            onChange={(e) => setIsOpenToWork(e.target.checked)}
            className="w-4 h-4 accent-purple-600"
          />
          <span className="text-sm text-gray-700">Open to work — show badge on my profile</span>
        </label>
      </div>

      <Button type="submit" loading={saving} className="w-full">
        Save profile
      </Button>
    </form>
  )
}
