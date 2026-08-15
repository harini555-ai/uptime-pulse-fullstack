/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    './index.html',
    './src/**/*.{js,jsx}'
  ],
  theme: {
    extend: {
      colors: {
        base: {
          50: '#f5f7fa',
          100: '#e9edf3',
          200: '#d3dae5',
          300: '#a9b6c9',
          400: '#7c8ba3',
          500: '#5a6b85',
          600: '#44536b',
          700: '#333f52',
          800: '#1d2534',
          850: '#161c28',
          900: '#10141d',
          950: '#0a0d13'
        },
        status: {
          up: '#22c55e',
          down: '#ef4444',
          pending: '#f59e0b'
        },
        accent: {
          500: '#6366f1',
          600: '#4f46e5'
        }
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif']
      },
      boxShadow: {
        card: '0 1px 3px 0 rgba(0, 0, 0, 0.3), 0 1px 2px -1px rgba(0, 0, 0, 0.3)',
        glow: '0 0 0 1px rgba(99, 102, 241, 0.2), 0 0 20px rgba(99, 102, 241, 0.15)'
      },
      animation: {
        'pulse-slow': 'pulse 2.5s cubic-bezier(0.4, 0, 0.6, 1) infinite'
      }
    }
  },
  plugins: []
}
