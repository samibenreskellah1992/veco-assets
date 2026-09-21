import { useAuth } from '@/hooks/use-auth'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { EtiquetageAssetsPanel } from '@/features/etiquetage/EtiquetageAssetsPanel'
import { EtiquetageLocauxPanel } from '@/features/etiquetage/EtiquetageLocauxPanel'

/**
 * Étiquetage (Phase 6). Checkpoint 2 de l'evolution "locaux scannables"
 * (2026-09) : ajoute un onglet "Locaux" a cote de l'onglet historique
 * "Immobilisations" - meme permission ETIQUETTE_GENERATE pour les deux
 * (generer une etiquette, que ce soit pour une immobilisation ou un local,
 * releve du meme geste metier), meme page pour eviter de multiplier les
 * entrees de navigation.
 */
export function EtiquetagePage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('ETIQUETTE_GENERATE')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          La génération d'étiquettes est réservée aux comptes disposant de la permission ETIQUETTE_GENERATE.
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Étiquetage</h1>
        <p className="text-sm text-muted-foreground">
          Génération réelle d'étiquettes QR (et code-barres selon le format) pour les immobilisations et les locaux.
        </p>
      </div>

      <Tabs defaultValue="assets">
        <TabsList>
          <TabsTrigger value="assets">Immobilisations</TabsTrigger>
          <TabsTrigger value="locations">Locaux</TabsTrigger>
        </TabsList>
        <TabsContent value="assets">
          <EtiquetageAssetsPanel />
        </TabsContent>
        <TabsContent value="locations">
          <EtiquetageLocauxPanel />
        </TabsContent>
      </Tabs>
    </div>
  )
}
