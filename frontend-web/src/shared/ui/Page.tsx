import type { ReactNode } from 'react'

type Props = { title: string; children: ReactNode }

export function Page({ title, children }: Props) {
  return (
    <main>
      <h1>{title}</h1>
      {children}
    </main>
  )
}
