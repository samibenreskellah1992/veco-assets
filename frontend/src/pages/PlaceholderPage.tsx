interface PlaceholderPageProps {
  title: string
  phase: string
}

/**
 * Honest "not built yet" placeholder — never a page dressed up to look
 * functional. Each real module replaces its placeholder in the phase
 * listed in docs/ROADMAP.md.
 */
export function PlaceholderPage({ title, phase }: PlaceholderPageProps) {
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
        Module prévu en <span className="font-medium text-foreground">{phase}</span> (voir docs/ROADMAP.md).
        <br />
        Pas encore implémenté — cette page n'appelle aucune API tant que le module n'est pas construit.
      </div>
    </div>
  )
}
