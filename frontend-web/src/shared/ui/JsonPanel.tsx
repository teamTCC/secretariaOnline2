type Props = { data: unknown }

export function JsonPanel({ data }: Props) {
  if (data === undefined) return null
  return <pre className="json">{JSON.stringify(data, null, 2)}</pre>
}
