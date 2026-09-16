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
} from 'lucide-react'
import { cn } from '@/lib/utils'

interface NavItem {
  label: string
  to: string
  icon: React.ComponentType<{ className?: string }>
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard },
  { label: 'Immobilisations', to: '/immobilisations', icon: Boxes },
  { label: 'Étiquetage', to: '/etiquetage', icon: Tag },
  { label: 'Inventaires', to: '/inventaires', icon: ClipboardList },
  { label: 'Mouvements', to: '/mouvements', icon: ArrowLeftRight },
  { label: 'Anomalies', to: '/anomalies', icon: AlertTriangle },
  { label: 'Sites & Localisations', to: '/sites', icon: MapPin },
  { label: 'Utilisateurs', to: '/utilisateurs', icon: Users },
  { label: 'Rapports', to: '/rapports', icon: FileBarChart },
  { label: 'Administration', to: '/administration', icon: Settings },
]

export function AppLayout() {
  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      <aside className="flex w-64 shrink-0 flex-col bg-sidebar text-sidebar-foreground">
        <div className="flex items-center gap-2 px-5 py-5">
          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-sidebar-active text-sm font-bold text-white">
            V
          </div>
          <div>
            <p className="text-sm font-semibold leading-none">VECOPHARM</p>
            <p className="text-xs text-sidebar-muted">VECO Assets</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-2">
          {NAV_ITEMS.map(({ label, to, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
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
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-border bg-card px-6">
          <div className="flex w-full max-w-md items-center gap-2 rounded-md border border-input px-3 py-1.5 text-sm text-muted-foreground">
            <Search className="h-4 w-4" />
            <span>Rechercher une immobilisation, un site, un utilisateur...</span>
          </div>
          <div className="flex items-center gap-4">
            <Bell className="h-5 w-5 text-muted-foreground" />
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                SB
              </div>
              <div className="text-sm leading-tight">
                <p className="font-medium">Sami Benreskallah</p>
                <p className="text-xs text-muted-foreground">Administrateur</p>
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
