import { LevelPanel, type LevelConfig } from '@/features/referentiel/LevelPanel'
import { floorsApi, zonesApi } from '@/services/referentiel-service'
import type { ZoneDto, ZoneRequest } from '@/types/referentiel'

const config: LevelConfig<ZoneDto, ZoneRequest> = {
  itemLabel: 'zone',
  itemLabelPlural: 'zones',
  parentLabel: 'Étage',
  queryKey: ['zones'],
  parentOptionsQueryKey: ['floors'],
  fetchParentOptions: async () => floorsApi.list(),
  getParentId: (item) => item.floorId,
  getParentName: (item) => item.floorName,
  buildRequest: ({ code, name, parentId }) => ({ code, name, floorId: parentId }),
  api: zonesApi,
}

export function ZonesPanel() {
  return <LevelPanel config={config} />
}
