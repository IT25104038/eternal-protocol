// MILESTONE 1 SCOPE — logo, category links, cart count, auth state.
// Deferred to Milestone 2: CategoryNavDropdown and MobileCategorySection (the
// live subcategory menus), UserMenu (the avatar dropdown), and the CUSTOMER
// and ATHLETE links that need pages which do not exist yet.

import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { FaCartShopping, FaXmark } from 'react-icons/fa6'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'
import { useCartCount } from '@/features/cart/useCartCount'
import { CATEGORY_NAV } from '@/lib/categories'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `block text-sm uppercase tracking-widest transition-colors hover:text-white ${isActive ? 'text-white' : 'text-ep-muted'}`

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { role, logout } = useAuthStore()
  const clearGuestCart = useGuestCartStore((s) => s.clear)
  const cartCount = useCartCount()

  function handleLogout() {
    setMobileOpen(false)
    logout()
    clearGuestCart()
  }

  return (
    <header className="sticky top-0 z-40 border-b border-ep-border/60 bg-ep-black/45 backdrop-blur-md">
      <div className="mx-auto grid max-w-7xl grid-cols-2 items-center gap-4 px-4 py-4 sm:px-6 md:grid-cols-3 lg:px-8">
        <Link to="/" className="text-sm font-semibold uppercase tracking-widest text-ep-bone">
          ETERNAL PROTOCOL
        </Link>

        {/* MILESTONE 2 replaces these plain links with CategoryNavDropdown,
            which fetches each category's live subcategory list on hover. */}
        <nav className="hidden items-center justify-center gap-10 md:flex">
          {CATEGORY_NAV.map(({ category, slug, label }) => (
            <NavLink key={category} to={`/shop/${slug}`} className={navLinkClass}>
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center justify-end gap-5">
          {/* MILESTONE 2 replaces this with UserMenu: an avatar dropdown with
              the role-specific links and a log-out item. */}
          <div className="hidden items-center gap-5 md:flex">
            {role === 'ADMIN' && (
              <NavLink to="/admin/products" className={navLinkClass}>
                Admin
              </NavLink>
            )}
            {role ? (
              <button onClick={handleLogout} className="text-sm uppercase tracking-widest text-ep-muted transition-colors hover:text-white">
                Log out
              </button>
            ) : (
              <NavLink to="/login" className={navLinkClass}>
                Log in
              </NavLink>
            )}
          </div>
          <Link to="/cart" className="relative text-ep-muted transition-colors hover:text-white" aria-label="Cart">
            <FaCartShopping className="h-5 w-5" />
            {cartCount > 0 && (
              <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-ep-red-bright text-[10px] text-white">
                {cartCount}
              </span>
            )}
          </Link>
          <button
            className="text-ep-muted md:hidden"
            aria-label="Toggle menu"
            onClick={() => setMobileOpen((v) => !v)}
          >
            {mobileOpen ? <FaXmark className="h-5 w-5" /> : <span className="text-lg leading-none">☰</span>}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <nav className="flex flex-col gap-1 border-t border-ep-border px-4 py-4 md:hidden">
          {/* MILESTONE 2 replaces these with MobileCategorySection, an
              accordion that expands each category's subcategory list. */}
          {CATEGORY_NAV.map(({ category, slug, label }) => (
            <NavLink
              key={category}
              to={`/shop/${slug}`}
              className={navLinkClass + ' py-2'}
              onClick={() => setMobileOpen(false)}
            >
              {label}
            </NavLink>
          ))}

          <div className="mt-3 border-t border-ep-border pt-3">
            {/* MILESTONE 2 adds the CUSTOMER links (/account, /account/orders)
                and the ATHLETE link (/athlete/dashboard) here. */}
            {role === 'ADMIN' && (
              <NavLink to="/admin/products" className={navLinkClass + ' py-2'} onClick={() => setMobileOpen(false)}>
                Admin Dashboard
              </NavLink>
            )}
            {role ? (
              <button onClick={handleLogout} className="block py-2 text-left text-sm uppercase tracking-widest text-ep-muted">
                Log out
              </button>
            ) : (
              <NavLink to="/login" className={navLinkClass + ' py-2'} onClick={() => setMobileOpen(false)}>
                Log in
              </NavLink>
            )}
          </div>
        </nav>
      )}
    </header>
  )
}

// ----------------------------------------------------------------------
// MILESTONE 2 — MobileCategorySection lives here: the mobile accordion
// version of one category, tapping to expand its live subcategory list
// (GET /api/products/subcategories) instead of a hover dropdown.
// ----------------------------------------------------------------------
