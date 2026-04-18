import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#14142e',
            color: '#e0e0ec',
            border: '1px solid rgba(144,144,184,0.15)',
            borderRadius: '12px',
            fontFamily: 'DM Sans, sans-serif',
            fontSize: '14px',
          },
          success: { iconTheme: { primary: '#c8f547', secondary: '#0a0a1e' } },
          error: { iconTheme: { primary: '#ff6b6b', secondary: '#0a0a1e' } },
        }}
      />
    </BrowserRouter>
  </React.StrictMode>
)
