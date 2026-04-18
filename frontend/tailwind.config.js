/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: ['"DM Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      colors: {
        ink: {
          50: '#f0f0f5',
          100: '#e0e0ec',
          200: '#c0c0d8',
          300: '#9090b8',
          400: '#606090',
          500: '#404068',
          600: '#303055',
          700: '#202040',
          800: '#14142e',
          900: '#0a0a1e',
          950: '#050510',
        },
        lime: {
          400: '#c8f547',
          500: '#b5e030',
          600: '#96c520',
        },
        coral: {
          400: '#ff6b6b',
          500: '#ff4f4f',
        },
        amber: {
          400: '#ffd166',
          500: '#ffbb33',
        }
      },
      animation: {
        'fade-up': 'fadeUp 0.5s ease forwards',
        'fade-in': 'fadeIn 0.4s ease forwards',
        'slide-in': 'slideIn 0.35s ease forwards',
        'pulse-slow': 'pulse 3s ease-in-out infinite',
      },
      keyframes: {
        fadeUp: {
          from: { opacity: 0, transform: 'translateY(16px)' },
          to: { opacity: 1, transform: 'translateY(0)' },
        },
        fadeIn: {
          from: { opacity: 0 },
          to: { opacity: 1 },
        },
        slideIn: {
          from: { opacity: 0, transform: 'translateX(-12px)' },
          to: { opacity: 1, transform: 'translateX(0)' },
        },
      },
    },
  },
  plugins: [],
}
