import { apiClient } from '@/services/api-client'
import type { ExportFormat, ReportFilterParams, ReportResultDto, ReportType } from '@/types/report'

export const reportsApi = {
  get: async (type: ReportType, filters: ReportFilterParams) =>
    (await apiClient.get<ReportResultDto>(`/reports/${type}`, { params: filters })).data,
  /** responseType 'blob' - même convention que labelsApi.generate (Phase 6) pour un flux binaire téléchargeable. */
  export: async (type: ReportType, format: ExportFormat, filters: ReportFilterParams) =>
    (
      await apiClient.get(`/reports/${type}/export`, {
        params: { ...filters, format },
        responseType: 'blob',
      })
    ).data as Blob,
}
