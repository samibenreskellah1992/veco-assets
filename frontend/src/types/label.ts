/** Mirrors the backend DTOs 1:1 (dz.vecopharm.vecoassets.dto) - see docs/ARCHITECTURE.md. */

export interface AssetLabelFormatDto {
  id: string
  code: string
  name: string
  widthMm: number
  heightMm: number
  showLogo: boolean
  showShortDesignation: boolean
  showQrCode: boolean
  showBarcode: boolean
  active: boolean
}

export interface AssetLabelFormatRequest {
  code: string
  name: string
  widthMm: number
  heightMm: number
  showLogo: boolean
  showShortDesignation: boolean
  showQrCode: boolean
  showBarcode: boolean
}

export interface AssetLabelDto {
  id: string
  formatId: string
  formatCode: string
  formatName: string
  generatedByName: string | null
  generatedAt: string
}

export interface LabelGenerationRequest {
  assetIds: string[]
  formatId: string
}
