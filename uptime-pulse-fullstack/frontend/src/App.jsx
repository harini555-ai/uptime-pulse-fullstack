import React, { useCallback, useEffect, useRef, useState } from 'react'
import axios from 'axios'
import {
  Activity,
  Plus,
  RadioTower,
  RefreshCcw,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  WifiOff
} from 'lucide-react'
import MonitorCard from './components/MonitorCard.jsx'
import LatencyChart from './components/LatencyChart.jsx'
import AddMonitorModal from './components/AddMonitorModal.jsx'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const POLL_INTERVAL_MS = 10000

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
})

function StatCard({ label, value, Icon, colorClass }) {
  return (
    <div className="card-surface flex items-center gap-4 p-4">
      <div className={`flex h-10 w-10 items-center justify-center rounded-lg bg-base-850 ${colorClass}`}>
        <Icon size={18} />
      </div>
      <div>
        <p className="text-xs font-medium uppercase tracking-wide text-base-500">{label}</p>
        <p className="text-xl font-bold text-base-50">{value}</p>
      </div>
    </div>
  )
}

export default function App() {
  const [monitors, setMonitors] = useState([])
  const [selectedMonitorId, setSelectedMonitorId] = useState(null)
  const [history, setHistory] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isHistoryLoading, setIsHistoryLoading] = useState(false)
  const [error, setError] = useState('')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [checkingIds, setCheckingIds] = useState(new Set())
  const [isBackendReachable, setIsBackendReachable] = useState(true)
  const [lastSyncedAt, setLastSyncedAt] = useState(null)

  const pollTimerRef = useRef(null)
  const selectedMonitorIdRef = useRef(selectedMonitorId)
  selectedMonitorIdRef.current = selectedMonitorId

  const fetchMonitors = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setIsLoading(true)
    try {
      const response = await apiClient.get('/monitors')
      setMonitors(response.data || [])
      setIsBackendReachable(true)
      setError('')
      setLastSyncedAt(new Date())

      if (!selectedMonitorIdRef.current && response.data && response.data.length > 0) {
        setSelectedMonitorId(response.data[0].id)
      }
    } catch (err) {
      setIsBackendReachable(false)
      setError(
        err?.response?.data?.message ||
        'Unable to reach the UptimePulse backend. Confirm the Spring Boot API is running on port 8080.'
      )
    } finally {
      if (!silent) setIsLoading(false)
    }
  }, [])

  const fetchHistory = useCallback(async (monitorId, { silent = false } = {}) => {
    if (!monitorId) {
      setHistory([])
      return
    }
    if (!silent) setIsHistoryLoading(true)
    try {
      const response = await apiClient.get(`/monitors/${monitorId}/history`, {
        params: { limit: 60 }
      })
      setHistory(response.data || [])
    } catch (err) {
      if (!silent) {
        console.error('Failed to load latency history:', err)
      }
    } finally {
      if (!silent) setIsHistoryLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchMonitors()
    pollTimerRef.current = setInterval(() => {
      fetchMonitors({ silent: true })
      if (selectedMonitorIdRef.current) {
        fetchHistory(selectedMonitorIdRef.current, { silent: true })
      }
    }, POLL_INTERVAL_MS)

    return () => clearInterval(pollTimerRef.current)
  }, [fetchMonitors, fetchHistory])

  useEffect(() => {
    fetchHistory(selectedMonitorId)
  }, [selectedMonitorId, fetchHistory])

  const handleCreateMonitor = async (payload) => {
    await apiClient.post('/monitors', payload)
    await fetchMonitors({ silent: true })
  }

  const handleDeleteMonitor = async (monitorId) => {
    try {
      await apiClient.delete(`/monitors/${monitorId}`)
      if (selectedMonitorId === monitorId) {
        setSelectedMonitorId(null)
        setHistory([])
      }
      await fetchMonitors({ silent: true })
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to delete monitor.')
    }
  }

  const handleCheckNow = async (monitorId) => {
    setCheckingIds((prev) => new Set(prev).add(monitorId))
    try {
      await apiClient.post(`/monitors/${monitorId}/check-now`)
      await fetchMonitors({ silent: true })
      if (selectedMonitorIdRef.current === monitorId) {
        await fetchHistory(monitorId, { silent: true })
      }
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to trigger health check.')
    } finally {
      setCheckingIds((prev) => {
        const next = new Set(prev)
        next.delete(monitorId)
        return next
      })
    }
  }

  const selectedMonitor = monitors.find((m) => m.id === selectedMonitorId) || null

  const totalCount = monitors.length
  const upCount = monitors.filter((m) => m.status === 'UP').length
  const downCount = monitors.filter((m) => m.status === 'DOWN').length
  const pendingCount = monitors.filter((m) => m.status === 'PENDING').length

  return (
    <div className="min-h-screen bg-base-950">
      <header className="sticky top-0 z-30 border-b border-base-800 bg-base-950/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-600/15 text-accent-500">
              <RadioTower size={20} />
            </div>
            <div>
              <h1 className="text-lg font-bold tracking-tight text-base-50">UptimePulse</h1>
              <p className="text-xs text-base-400">Multi-Tenant API Monitoring &amp; Uptime Status</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {!isBackendReachable && (
              <span className="flex items-center gap-1.5 rounded-full bg-status-down/10 px-3 py-1.5 text-xs font-medium text-status-down">
                <WifiOff size={13} /> Backend unreachable
              </span>
            )}
            {lastSyncedAt && isBackendReachable && (
              <span className="hidden text-xs text-base-500 sm:inline">
                Synced {lastSyncedAt.toLocaleTimeString()}
              </span>
            )}
            <button
              type="button"
              onClick={() => fetchMonitors()}
              className="btn-secondary !px-3"
              title="Refresh now"
            >
              <RefreshCcw size={15} />
            </button>
            <button
              type="button"
              onClick={() => setIsModalOpen(true)}
              className="btn-primary"
            >
              <Plus size={16} />
              Add Monitor
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8">
        {error && (
          <div className="mb-6 flex items-start gap-3 rounded-lg border border-status-down/30 bg-status-down/10 p-4 text-sm text-status-down">
            <AlertTriangle size={18} className="mt-0.5 shrink-0" />
            <div>
              <p className="font-medium">Something went wrong</p>
              <p className="mt-0.5 text-status-down/90">{error}</p>
            </div>
          </div>
        )}

        <div className="mb-8 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <StatCard label="Total Monitors" value={totalCount} Icon={Activity} colorClass="text-accent-500" />
          <StatCard label="Up" value={upCount} Icon={CheckCircle2} colorClass="text-status-up" />
          <StatCard label="Down" value={downCount} Icon={XCircle} colorClass="text-status-down" />
          <StatCard label="Pending" value={pendingCount} Icon={Clock} colorClass="text-status-pending" />
        </div>

        {isLoading ? (
          <div className="flex h-64 items-center justify-center">
            <div className="flex flex-col items-center gap-3 text-base-400">
              <RefreshCcw size={24} className="animate-spin" />
              <p className="text-sm">Loading monitors...</p>
            </div>
          </div>
        ) : monitors.length === 0 ? (
          <div className="card-surface flex flex-col items-center justify-center gap-3 py-16 text-center">
            <RadioTower size={32} className="text-base-600" />
            <h3 className="text-base font-semibold text-base-100">No monitors yet</h3>
            <p className="max-w-sm text-sm text-base-400">
              Add your first API or website endpoint to start tracking uptime and latency in real time.
            </p>
            <button type="button" onClick={() => setIsModalOpen(true)} className="btn-primary mt-2">
              <Plus size={16} />
              Add Monitor
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:col-span-2 lg:grid-cols-1">
              {monitors.map((monitor) => (
                <MonitorCard
                  key={monitor.id}
                  monitor={monitor}
                  isSelected={monitor.id === selectedMonitorId}
                  onSelect={setSelectedMonitorId}
                  onDelete={handleDeleteMonitor}
                  onCheckNow={handleCheckNow}
                  isChecking={checkingIds.has(monitor.id)}
                />
              ))}
            </div>

            <div className="lg:col-span-3">
              {selectedMonitor ? (
                <div className="space-y-4">
                  <LatencyChart history={history} monitorName={selectedMonitor.name} />
                  {isHistoryLoading && (
                    <p className="text-center text-xs text-base-500">Refreshing history...</p>
                  )}
                </div>
              ) : (
                <div className="card-surface flex h-72 items-center justify-center text-sm text-base-400">
                  Select a monitor to view its latency history.
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      <AddMonitorModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleCreateMonitor}
      />
    </div>
  )
}
