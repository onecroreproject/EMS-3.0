import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// Force Light Mode by default so header is orange
if (typeof window !== 'undefined') {
  document.documentElement.classList.remove('dark');
  localStorage.setItem('theme', 'light');
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

