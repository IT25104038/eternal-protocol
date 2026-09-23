// MILESTONE 1 SCOPE.
// Deferred to Milestone 2: merging the guest cart into the server cart on
// login — see the comment in the submit handler below.

import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { useAuthStore, landingRouteForRole } from '@/stores/auth'
import { getErrorMessage } from '@/api/client'
import { Button } from '@/components/ui/Button'
import { FormInput } from '@/components/ui/FormField'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useAuthStore((s) => s.setSession)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const auth = await authApi.login({ email: email.trim(), password })
      setSession(auth)
      // MILESTONE 2 folds the guest cart into the server cart here, via
      // mergeGuestCartIntoServer(). Milestone 1 leaves the two carts
      // separate — a customer's guest-cart items don't carry over on login.
      const from = (location.state as { from?: Location })?.from?.pathname
      navigate(from ?? landingRouteForRole(auth.role), { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, 'Invalid email or password.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm px-4 py-20 sm:px-6">
      <h1 className="font-display text-4xl">Log in</h1>
      <form onSubmit={handleSubmit} className="mt-8 space-y-5">
        <FormInput label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <FormInput
          label="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p className="text-sm text-red-400">{error}</p>}
        <Button type="submit" loading={submitting} className="w-full">
          Log in
        </Button>
      </form>
      <p className="mt-6 text-sm text-ep-muted">
        New here?{' '}
        <Link to="/register" className="text-ep-bone underline">
          Create an account
        </Link>
      </p>
    </div>
  )
}
