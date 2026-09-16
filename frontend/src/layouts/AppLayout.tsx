import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import {
  LayoutDashboard,
  Boxes,
  Tag,
  ClipboardList,
  ArrowLeftRight,
  AlertTriangle,
  MapPin,
  Users,
  FileBarChart,
  Settings,
  Search,
  Bell,
  LogOut,
  Menu,
  X,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '@/hooks/use-auth'
import { Button } from '@/components/ui/button'

interface NavItem {
  label: string
  to: string
  icon: React.ComponentType<{ className?: string }>
  /** Omit to show the item to every authenticated user - see prompt maitre section 28 (le backend reste la seule source de verite). */
  requiresPermission?: string
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard },
  { label: 'Immobilisations', to: '/immobilisations', icon: Boxes },
  { label: 'Étiquetage', to: '/etiquetage', icon: Tag },
  { label: 'Inventaires', to: '/inventaires', icon: ClipboardList },
  { label: 'Mouvements', to: '/mouvements', icon: ArrowLeftRight },
  { label: 'Anomalies', to: '/anomalies', icon: AlertTriangle },
  { label: 'Sites & Localisations', to: '/sites', icon: MapPin },
  { label: 'Utilisateurs', to: '/utilisateurs', icon: Users, requiresPermission: 'USER_MANAGE' },
  { label: 'Rapports', to: '/rapports', icon: FileBarChart },
  { label: 'Administration', to: '/administration', icon: Settings },
]

function initialsOf(fullName: string) {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

/**
 * Navigation responsive (Phase 10 - tache #89) : en dessous du breakpoint
 * `lg` (tablette portrait / mobile), la barre laterale de 256px ne peut
 * plus rester statique - elle passerait sur la moitie de l'ecran d'un
 * telephone. Elle devient donc un tiroir hors-champ (`fixed`,
 * `-translate-x-full`) ouvert via le bouton hamburger de l'en-tete, avec un
 * overlay pour la refermer. A partir de `lg`, elle redevient statique et
 * toujours visible comme avant (aucun changement de comportement desktop).
 */
export function AppLayout() {
  const { user, logout, hasPermission } = useAuth()
  const visibleNavItems = NAV_ITEMS.filter((item) => !item.requiresPermission || hasPermission(item.requiresPermission))
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      {mobileNavOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setMobileNavOpen(false)}
          aria-hidden="true"
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-64 shrink-0 flex-col bg-sidebar text-sidebar-foreground transition-transform duration-200 ease-in-out lg:static lg:translate-x-0',
          mobileNavOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center justify-between gap-2 px-5 py-5">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-md bg-sidebar-active text-sm font-bold text-white">
              V
            </div>
            <div>
              <p className="text-sm font-semibold leading-none">VECOPHARM</p>
              <p className="text-xs text-sidebar-muted">VECO Assets</p>
            </div>
          </div>
          <button
            type="button"
            className="rounded-md p-1 text-sidebar-foreground/70 hover:bg-white/5 hover:text-sidebar-foreground lg:hidden"
            onClick={() => setMobileNavOpen(false)}
            title="Fermer le menu"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-2">
          {visibleNavItems.map(({ label, to, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              onClick={() => setMobileNavOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors',
                  isActive
                    ? 'bg-sidebar-active text-white'
                    : 'text-sidebar-foreground/80 hover:bg-white/5 hover:text-sidebar-foreground',
                )
              }
            >
              <Icon className="h-4 w-4 shrink-0" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex h-16 shrink-0 items-center justify-between gap-2 border-b border-border bg-card px-3 sm:px-6">
          <div className="flex min-w-0 flex-1 items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              className="shrink-0 lg:hidden"
              title="Ouvrir le menu"
              onClick={() => setMobileNavOpen(true)}
            >
              <Menu className="h-5 w-5" />
            </Button>
            <div className="hidden min-w-0 flex-1 items-center gap-2 rounded-md border border-input px-3 py-1.5 text-sm text-muted-foreground sm:flex sm:max-w-md">
              <Search className="h-4 w-4 shrink-0" />
              <span className="truncate">Rechercher une immobilisation, un site, un utilisateur...</span>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-2 sm:gap-4">
            <Bell className="hidden h-5 w-5 text-muted-foreground sm:block" />
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                {user ? initialsOf(user.fullName) : ''}
              </div>
              <div className="hidden text-sm leading-tight md:block">
                <p className="font-medium">{user?.fullName}</p>
                <p className="text-xs text-muted-foreground">{user?.roles.join(', ')}</p>
              </div>
            </div>
            <Button variant="ghost" size="icon" title="Se deconnecter" onClick={logout}>
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
