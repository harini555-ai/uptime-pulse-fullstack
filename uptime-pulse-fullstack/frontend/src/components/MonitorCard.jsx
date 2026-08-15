import React from 'react'
import {
  CheckCircle2,
  XCircle,
  Clock,
  Globe,
  Trash2,
  RefreshCw,
  Bell,
  BellOff,
  ChevronRight
} from 'lucide-react'

const STATUS_CONFIG = {
  UP: {
    label: 'Up',
    badgeClass: 'badge-up',
    dotClass: 'bg-status-up',
    Icon: CheckCircle2
  },
  DOWN: {
    label: 'Down',
    badgeClass: 'badge-down',
    dotClass: 'bg-status-down',
    Icon: XCircle
  },
  PENDING: {
    label: 'Pending',
    badgeClass: 'badge-pending',
    dotClass: 'bg-status-pending',
    Icon: Clock
  }
}

function timeAgo(dateString) {
  if (!dateString) return 'never'
  const now = new Date()
  const then = new Date(dateString)
  const diffMs = now - then
  const diffSec = Math.floor(diffMs / 1000)

  if (diffSec < 5) return 'just now'
  if (diffSec < 60) return `${diffSec}s ago`
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin}m ago`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}h ago`
  const diffDay = Math.floor(diffHour / 24)
  return `${diffDay}d ago`
}

function latencyColorClass(latencyMs) {
  if (latencyMs == null) return 'text-base-400'
  if (latencyMs < 300) return 'text-status-up'
  if (latencyMs < 1000) return 'text-status-pending'
  return 'text-status-down'
}

export default function MonitorCard({
  monitor,
  isSelected,
  onSelect,
  onDelete,
  onCheckNow,
  isChecking
}) {
  const config = STATUS_CONFIG[monitor.status] || STATUS_CONFIG.PENDING
  const StatusIcon = config.Icon

  const handleDelete = (e) => {
    e.stopPropagation()
    if (window.confirm(`Delete monitor "${monitor.name}"? This will remove all of its history.`)) {
      onDelete(monitor.id)
    }
  }

  const handleCheckNow = (e) => {
    e.stopPropagation()
    onCheckNow(monitor.id)
  }

  return (
    <button
      type="button"
      onClick={() => onSelect(monitor.id)}
      className={`card-surface group relative flex w-full flex-col gap-4 p-5 text-left transition-all hover:border-base-600 ${
        isSelected ? 'border-accent-500 ring-1 ring-accent-500 shadow-glow' : ''
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <span className="relative flex h-3 w-3 shrink-0">
            {monitor.status === 'UP' && (
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-status-up opacity-75" />
            )}
            <span className={`relative inline-flex h-3 w-3 rounded-full ${config.dotClass}`} />
          </span>
          <div className="min-w-0">
            <h3 className="truncate text-sm font-semibold text-base-50">{monitor.name}</h3>
            <p className="mt-0.5 flex items-center gap-1 truncate text-xs text-base-400">
              <Globe size={12} className="shrink-0" />
              <span className="truncate">{monitor.url}</span>
            </p>
          </div>
        </div>
        <span className={`badge ${config.badgeClass} shrink-0`}>
          <StatusIcon size={12} />
          {config.label}
        </span>
      </div>

      <div className="grid grid-cols-3 gap-3 rounded-lg bg-base-850 p-3">
        <div>
          <p className="text-[10px] font-medium uppercase tracking-wide text-base-500">Latency</p>
          <p className={`mt-0.5 text-sm font-semibold ${latencyColorClass(monitor.lastLatencyMs)}`}>
            {monitor.lastLatencyMs != null ? `${monitor.lastLatencyMs} ms` : '—'}
          </p>
        </div>
        <div>
          <p className="text-[10px] font-medium uppercase tracking-wide text-base-500">HTTP Code</p>
          <p className="mt-0.5 text-sm font-semibold text-base-100">
            {monitor.lastStatusCode ?? '—'}
          </p>
        </div>
        <div>
          <p className="text-[10px] font-medium uppercase tracking-wide text-base-500">Checked</p>
          <p className="mt-0.5 text-sm font-semibold text-base-100">
            {timeAgo(monitor.lastCheckedAt)}
          </p>
        </div>
      </div>

      <div className="flex items-center justify-between border-t border-base-800 pt-3">
        <div className="flex items-center gap-2 text-xs text-base-400">
          {monitor.alertsEnabled ? (
            <span className="flex items-center gap-1 text-accent-500">
              <Bell size={13} /> Alerts on
            </span>
          ) : (
            <span className="flex items-center gap-1">
              <BellOff size={13} /> Alerts off
            </span>
          )}
          <span className="text-base-700">•</span>
          <span>every {monitor.checkIntervalSeconds}s</span>
        </div>

        <div className="flex items-center gap-1">
          <span
            role="button"
            tabIndex={0}
            title="Check now"
            onClick={handleCheckNow}
            className="rounded-md p-1.5 text-base-400 transition-colors hover:bg-base-800 hover:text-accent-500"
          >
            <RefreshCw size={14} className={isChecking ? 'animate-spin' : ''} />
          </span>
          <span
            role="button"
            tabIndex={0}
            title="Delete monitor"
            onClick={handleDelete}
            className="rounded-md p-1.5 text-base-400 transition-colors hover:bg-status-down/10 hover:text-status-down"
          >
            <Trash2 size={14} />
          </span>
          <ChevronRight
            size={16}
            className="ml-1 text-base-600 transition-transform group-hover:translate-x-0.5"
          />
        </div>
      </div>
    </button>
  )
}
