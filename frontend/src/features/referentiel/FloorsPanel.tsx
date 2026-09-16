import { LevelPanel, type LevelConfig } from '@/features/referentiel/LevelPanel'
import { buildingsApi, floorsApi } from '@/services/referentiel-service'
import type { FloorDto, FloorRequest } from '@/types/referentiel'

const config: LevelConfig<FloorDto, FloorRequest> = {
  itemLabel: 'étage',
  itemLabelPlural: 'étages',
  parentLabel: 'Bâtiment',
  queryKey: ['floors'],
  parentOptionsQueryKey: ['buildings'],
  fetchParentOptions: async () => buildingsApi.list(),
  getParentId: (item) => item.buildingId,
  getParentName: (item) => item.buildingName,
  buildRequest: ({ code, name, parentId }) => ({ code, name, buildingId: parentId }),
  api: floorsApi,
}

export function FloorsPanel() {
  return <LevelPanel config={config} />
}
