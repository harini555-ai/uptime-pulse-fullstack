import React, { useState } from 'react'
import { X, Loader2, Globe2, Bell, AlertCircle } from 'lucide-react'

const DEFAULT_FORM = {
  name: '',
  url: '',
  tenantId: 'default',
  checkIntervalSeconds: 30,
  expectedStatusCode: 200,
  discordWebhookUrl: '',
  alertsEnabled: false
}

export default function AddMonitorModal({ isOpen, onClose, onSubmit }) {
  const [form, setForm] = useState(DEFAULT_FORM)
  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')

  if (!isOpen) return null

  const handleChange = (field) => (e) => {
    const value =
      e.target.type === 'checkbox'
        ? e.target.checked
        : e.target.type === 'number'
          ? Number(e.target.value)
          : e.target.value
    setForm((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  const validate = () => {
    const nextErrors = {}
    if (!form.name || form.name.trim().length === 0) {
      nextErrors.name = 'Monitor name is required.'
    }
    if (!form.url || form.url.trim().length === 0) {
      nextErrors.url = 'URL is required.'
    } else if (!/^https?:\/\/[^\s/$.?#].[^\s]*$/i.test(form.url.trim())) {
      nextErrors.url = 'Enter a valid http:// or https:// URL.'
    }
    if (form.alertsEnabled && (!form.discordWebhookUrl || form.discordWebhookUrl.trim().length === 0)) {
      nextErrors.discordWebhookUrl = 'A Discord webhook URL is required when alerts are enabled.'
    }
    if (form.checkIntervalSeconds < 10) {
      nextErrors.checkIntervalSeconds = 'Interval must be at least 10 seconds.'
    }
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitError('')
    if (!validate()) return

    setIsSubmitting(true)
    try {
      await onSubmit({
        name: form.name.trim(),
        url: form.url.trim(),
        tenantId: form.tenantId.trim() || 'default',
        checkIntervalSeconds: Number(form.checkIntervalSeconds),
        expectedStatusCode: Number(form.expectedStatusCode),
        discordWebhookUrl: form.discordWebhookUrl.trim() || null,
        alertsEnabled: Boolean(form.alertsEnabled),
        isActive: true
      })
      setForm(DEFAULT_FORM)
      onClose()
    } catch (err) {
      const message =
        err?.response?.data?.message ||
        err?.message ||
        'Failed to create monitor. Please check your input and try again.'
      setSubmitError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleClose = () => {
    if (isSubmitting) return
    setForm(DEFAULT_FORM)
    setErrors({})
    setSubmitError('')
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/70 px-4 py-8 backdrop-blur-sm">
      <div className="w-full max-w-lg card-surface max-h-[90vh] overflow-y-auto p-6">
        <div className="mb-5 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-600/15 text-accent-500">
              <Globe2 size={18} />
            </div>
            <div>
              <h2 className="text-base font-semibold text-base-50">Add New Monitor</h2>
              <p className="text-xs text-base-400">Register an endpoint for automated uptime checks</p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="rounded-lg p-1.5 text-base-400 transition-colors hover:bg-base-800 hover:text-base-100"
          >
            <X size={18} />
          </button>
        </div>

        {submitError && (
          <div className="mb-4 flex items-start gap-2 rounded-lg border border-status-down/30 bg-status-down/10 p-3 text-sm text-status-down">
            <AlertCircle size={16} className="mt-0.5 shrink-0" />
            <span>{submitError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label-text" htmlFor="monitor-name">Monitor Name</label>
            <input
              id="monitor-name"
              type="text"
              className="input-field"
              placeholder="Production API"
              value={form.name}
              onChange={handleChange('name')}
              disabled={isSubmitting}
            />
            {errors.name && <p className="mt-1 text-xs text-status-down">{errors.name}</p>}
          </div>

          <div>
            <label className="label-text" htmlFor="monitor-url">Endpoint URL</label>
            <input
              id="monitor-url"
              type="text"
              className="input-field"
              placeholder="https://api.example.com/health"
              value={form.url}
              onChange={handleChange('url')}
              disabled={isSubmitting}
            />
            {errors.url && <p className="mt-1 text-xs text-status-down">{errors.url}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label-text" htmlFor="tenant-id">Tenant ID</label>
              <input
                id="tenant-id"
                type="text"
                className="input-field"
                placeholder="default"
                value={form.tenantId}
                onChange={handleChange('tenantId')}
                disabled={isSubmitting}
              />
            </div>
            <div>
              <label className="label-text" htmlFor="expected-status">Expected Status Code</label>
              <input
                id="expected-status"
                type="number"
                className="input-field"
                min={100}
                max={599}
                value={form.expectedStatusCode}
                onChange={handleChange('expectedStatusCode')}
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div>
            <label className="label-text" htmlFor="check-interval">Check Interval (seconds)</label>
            <input
              id="check-interval"
              type="number"
              className="input-field"
              min={10}
              step={5}
              value={form.checkIntervalSeconds}
              onChange={handleChange('checkIntervalSeconds')}
              disabled={isSubmitting}
            />
            {errors.checkIntervalSeconds && (
              <p className="mt-1 text-xs text-status-down">{errors.checkIntervalSeconds}</p>
            )}
            <p className="mt-1 text-xs text-base-500">
              Note: the background worker sweeps all monitors on a shared cycle (default 30s).
            </p>
          </div>

          <div className="rounded-lg border border-base-800 bg-base-850 p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Bell size={16} className="text-accent-500" />
                <span className="text-sm font-medium text-base-100">Discord Alerts</span>
              </div>
              <label className="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  className="peer sr-only"
                  checked={form.alertsEnabled}
                  onChange={handleChange('alertsEnabled')}
                  disabled={isSubmitting}
                />
                <div className="peer h-5 w-9 rounded-full bg-base-700 after:absolute after:left-[2px] after:top-[2px] after:h-4 after:w-4 after:rounded-full after:bg-white after:transition-all after:content-[''] peer-checked:bg-accent-600 peer-checked:after:translate-x-4 peer-focus:outline-none" />
              </label>
            </div>

            {form.alertsEnabled && (
              <div className="mt-3">
                <label className="label-text" htmlFor="webhook-url">Discord Webhook URL</label>
                <input
                  id="webhook-url"
                  type="text"
                  className="input-field"
                  placeholder="https://discord.com/api/webhooks/..."
                  value={form.discordWebhookUrl}
                  onChange={handleChange('discordWebhookUrl')}
                  disabled={isSubmitting}
                />
                {errors.discordWebhookUrl && (
                  <p className="mt-1 text-xs text-status-down">{errors.discordWebhookUrl}</p>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              className="btn-secondary"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  Creating...
                </>
              ) : (
                'Create Monitor'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
