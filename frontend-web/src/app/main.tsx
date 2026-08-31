import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { api } from '../shared/api/client'
import '../index.css'
import { Providers } from './providers'
import { router } from './router'

async function boot() {
  try {
    await api('/auth/csrf')
  } catch {
    /* backend off — smoke page still usable */
  }

  const root = document.getElementById('root')
  if (!root) throw new Error('#root ausente')

  createRoot(root).render(
    <StrictMode>
      <Providers>
        <RouterProvider router={router} />
      </Providers>
    </StrictMode>,
  )
}

void boot()
