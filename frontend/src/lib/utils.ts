import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Merge Tailwind class lists safely (shadcn/ui convention).
 * Used by every UI primitive added via `npx shadcn add <component>`.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
