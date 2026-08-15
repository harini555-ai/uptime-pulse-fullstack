import React, { useMemo } from 'react'
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ReferenceLine
} from 'recharts'
import { Activity } from 'lucide-react'

function formatTimeLabel(isoString) {
  const date = new Date(isoString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload || payload.length === 0) return null
  const point = payload[0].payload
  const isDown = point.result === 'DOWN'

  return (
    <div className="rounded-lg border border-base-700 bg-base-900 px-3.5 py-2.5 text-xs shadow-card">
      <p className="mb-1 font-medium text-base-300">{formatTimeLabel(label)}</p>
      <p className="flex items-center gap-1.5">
        <span className={`h-2 w-2 rounded-full ${isDown ? 'bg-status-down' : 'bg-status-up'}`} />
        <span className="font-semibold text-base-50">{point.latencyMs} ms</span>
      </p>
      {point.statusCode != null && (
        <p className="mt-0.5 text-base-400">HTTP {point.statusCode}</p>
      )}
    </div>
  )
}

export default function LatencyChart({ history, monitorName }) {
  const chartData = useMemo(() => {
    return [...history]
      .sort((a, b) => new Date(a.checkedAt) - new Date(b.checkedAt))
      .map((entry) => ({
        checkedAt: entry.checkedAt,
        latencyMs: entry.latencyMs ?? 0,
        statusCode: entry.statusCode,
        result: entry.result
      }))
  }, [history])

  const averageLatency = useMemo(() => {
    if (chartData.length === 0) return 0
    const sum = chartData.reduce((acc, d) => acc + (d.latencyMs || 0), 0)
    return Math.round(sum / chartData.length)
  }, [chartData])

  if (chartData.length === 0) {
    return (
      <div className="card-surface flex h-72 flex-col items-center justify-center gap-2 p-6 text-center">
        <Activity size={28} className="text-base-600" />
        <p className="text-sm text-base-400">
          No latency history yet for <span className="font-medium text-base-200">{monitorName}</span>.
        </p>
        <p className="text-xs text-base-500">Data will appear after the next automated check.</p>
      </div>
    )
  }

  return (
    <div className="card-surface p-5">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold text-base-50">Latency History</h3>
          <p className="text-xs text-base-400">{monitorName}</p>
        </div>
        <div className="text-right">
          <p className="text-[10px] font-medium uppercase tracking-wide text-base-500">Average</p>
          <p className="text-sm font-semibold text-accent-500">{averageLatency} ms</p>
        </div>
      </div>

      <div className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#1d2534" vertical={false} />
            <XAxis
              dataKey="checkedAt"
              tickFormatter={formatTimeLabel}
              stroke="#5a6b85"
              tick={{ fontSize: 11 }}
              minTickGap={40}
            />
            <YAxis
              stroke="#5a6b85"
              tick={{ fontSize: 11 }}
              width={50}
              tickFormatter={(v) => `${v}ms`}
            />
            <Tooltip content={<CustomTooltip />} />
            {averageLatency > 0 && (
              <ReferenceLine
                y={averageLatency}
                stroke="#7c8ba3"
                strokeDasharray="4 4"
                strokeOpacity={0.6}
              />
            )}
            <Area
              type="monotone"
              dataKey="latencyMs"
              stroke="#6366f1"
              strokeWidth={2}
              fill="url(#latencyGradient)"
              isAnimationActive={true}
              animationDuration={400}
              dot={false}
              activeDot={{ r: 4, fill: '#6366f1', stroke: '#0a0d13', strokeWidth: 2 }}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
