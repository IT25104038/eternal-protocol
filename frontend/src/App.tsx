// MILESTONE 1 SCOPE — the route table for the fourteen Milestone 1 screens.
// Deferred to Milestone 2: /shop/new-drops, /shop/upcoming, /account,
// /account/orders, /athlete/dashboard, /admin/commissions. Each is left as a
// comment where it belongs, so adding it back is a one-line change.
// 
// In the real repo every page named here should exist as a one-line stub
// before anyone starts feature work, so the app builds green from clone one.

import { Route, Routes, Outlet } from 'react-router-dom'
import { Navbar } from '@/components/layout/Navbar'
import { AdminNav } from '@/components/layout/AdminNav'
import { ToastViewport } from '@/components/ui/ToastViewport'

import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { ConfirmationPage } from '@/pages/ConfirmationPage'

import { ShopPage } from '@/features/shop/ShopPage'
import { ProductDetailPage } from '@/features/shop/ProductDetailPage'
import { CartPage } from '@/features/cart/CartPage'
import { CheckoutPage } from '@/features/checkout/CheckoutPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { AdminProductsPage } from '@/features/admin/AdminProductsPage'
import { AdminOrdersPage } from '@/features/admin/AdminOrdersPage'
import { AdminAthletesPage } from '@/features/admin/AdminAthletesPage'

import { RequireRole } from '@/routes/RequireRole'

export default function App() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/shop" element={<ShopPage />} />
          {/* MILESTONE 2 — reachable only from the homepage, never shown in the navbar:
              <Route path="/shop/new-drops" element={<NewDropsPage />} />
              <Route path="/shop/upcoming" element={<UpcomingPage />} /> */}
          <Route path="/shop/:categorySlug" element={<ShopPage />} />
          <Route path="/product/:id" element={<ProductDetailPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/order/:id/confirmation" element={<ConfirmationPage />} />

          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* MILESTONE 2 — customer account and athlete dashboard:
              /account            → ProfilePage          (RequireRole CUSTOMER)
              /account/orders     → OrderHistoryPage     (RequireRole CUSTOMER)
              /athlete/dashboard  → AthleteDashboardPage (RequireRole ATHLETE) */}

          <Route
            element={
              <RequireRole role="ADMIN">
                <div>
                  <AdminNav />
                  <Outlet />
                </div>
              </RequireRole>
            }
          >
            <Route path="/admin/products" element={<AdminProductsPage />} />
            <Route path="/admin/orders" element={<AdminOrdersPage />} />
            <Route path="/admin/athletes" element={<AdminAthletesPage />} />
            {/* MILESTONE 2: <Route path="/admin/commissions" element={<AdminCommissionsPage />} /> */}
          </Route>

          <Route path="/403" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      {/* MILESTONE 2: <Footer /> */}
      <ToastViewport />
    </div>
  )
}
