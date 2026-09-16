import { LevelPanel, type LevelConfig } from '@/features/referentiel/LevelPanel'
import { buildingsApi, sitesApi } from '@/services/referentiel-service'
import type { BuildingDto, BuildingRequest } from '@/types/referentiel'

const config: LevelConfig<BuildingDto, BuildingRequest> = {
  itemLabel: 'bâtiment',
  itemLabelPlural: 'bâtiments',
  parentLabel: 'Site',
  queryKey: ['buildings'],
  parentOptionsQueryKey: ['sites'],
  fetchParentOptions: async () => sitesApi.list(),
  getParentId: (item) => item.siteId,
  getParentName: (item) => item.siteName,
  buildRequest: ({ code, name, parentId }) => ({ code, name, siteId: parentId }),
  api: buildingsApi,
}

export function BuildingsPanel() {
  return <LevelPanel config={config} />
}
