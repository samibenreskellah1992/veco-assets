import { LevelPanel, type LevelConfig } from '@/features/referentiel/LevelPanel'
import { locationsApi, zonesApi } from '@/services/referentiel-service'
import type { LocationDto, LocationRequest } from '@/types/referentiel'

const config: LevelConfig<LocationDto, LocationRequest> = {
  itemLabel: 'localisation',
  itemLabelPlural: 'localisations',
  parentLabel: 'Zone',
  queryKey: ['locations'],
  parentOptionsQueryKey: ['zones'],
  fetchParentOptions: async () => zonesApi.list(),
  getParentId: (item) => item.zoneId,
  getParentName: (item) => item.zoneName,
  buildRequest: ({ code, name, parentId }) => ({ code, name, zoneId: parentId }),
  api: locationsApi,
}

export function LocationsPanel() {
  return <LevelPanel config={config} />
}
