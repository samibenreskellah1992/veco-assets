import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { SitesPanel } from '@/features/referentiel/SitesPanel'
import { BuildingsPanel } from '@/features/referentiel/BuildingsPanel'
import { FloorsPanel } from '@/features/referentiel/FloorsPanel'
import { ZonesPanel } from '@/features/referentiel/ZonesPanel'
import { LocationsPanel } from '@/features/referentiel/LocationsPanel'
import { CategoriesPanel } from '@/features/referentiel/CategoriesPanel'

/**
 * Référentiel (prompt maitre Phase 4) : hiérarchie de localisation complète
 * (Site > Bâtiment > Étage > Zone > Localisation) et catégories
 * d'immobilisation - toutes les valeurs viennent de l'API, aucune n'est
 * codée en dur ici (voir docs/ROADMAP.md, critère de vérification Phase 4).
 */
export function ReferentielPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Sites & Localisations</h1>
        <p className="text-sm text-muted-foreground">
          Référentiel administrable : sites, bâtiments, étages, zones, localisations et catégories d'immobilisation.
        </p>
      </div>

      <Tabs defaultValue="sites">
        <TabsList>
          <TabsTrigger value="sites">Sites</TabsTrigger>
          <TabsTrigger value="buildings">Bâtiments</TabsTrigger>
          <TabsTrigger value="floors">Étages</TabsTrigger>
          <TabsTrigger value="zones">Zones</TabsTrigger>
          <TabsTrigger value="locations">Localisations</TabsTrigger>
          <TabsTrigger value="categories">Catégories</TabsTrigger>
        </TabsList>
        <TabsContent value="sites">
          <SitesPanel />
        </TabsContent>
        <TabsContent value="buildings">
          <BuildingsPanel />
        </TabsContent>
        <TabsContent value="floors">
          <FloorsPanel />
        </TabsContent>
        <TabsContent value="zones">
          <ZonesPanel />
        </TabsContent>
        <TabsContent value="locations">
          <LocationsPanel />
        </TabsContent>
        <TabsContent value="categories">
          <CategoriesPanel />
        </TabsContent>
      </Tabs>
    </div>
  )
}
