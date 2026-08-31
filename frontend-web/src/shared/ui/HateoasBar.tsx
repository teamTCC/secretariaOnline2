import { useActions } from '../api/hateoas'

type Props = {
  links?: Record<string, string>
  onAction?: (rel: string, href: string) => void
}

export function HateoasBar({ links, onAction }: Props) {
  const { actionRels, href } = useActions(links)
  if (actionRels.length === 0) return null
  return (
    <div className="row">
      {actionRels.map((rel) => (
        <button
          key={rel}
          type="button"
          onClick={() => {
            const h = href(rel)
            if (h) onAction?.(rel, h)
          }}
        >
          {rel}
        </button>
      ))}
    </div>
  )
}
