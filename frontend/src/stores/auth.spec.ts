import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as authApi from '../api/auth'
import { useAuthStore } from './auth'

describe('auth store', () => {
  beforeEach(() => {
    const values = new Map<string, string>()
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: {
        getItem: (key: string) => values.get(key) ?? null,
        setItem: (key: string, value: string) => values.set(key, value),
        removeItem: (key: string) => values.delete(key)
      }
    })
    setActivePinia(createPinia())
  })

  it('stores a successful login token', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({ accessToken: 'jwt', tokenType: 'Bearer' })
    const store = useAuthStore()

    await store.login({ username: 'alice', password: 'Passw0rd!' })

    expect(store.accessToken).toBe('jwt')
    expect(localStorage.getItem('agromall.access-token')).toBe('jwt')
  })

  it('clears the persisted token and transient identity', () => {
    localStorage.setItem('agromall.access-token', 'jwt')
    const store = useAuthStore()
    store.username = 'alice'

    store.clearSession()

    expect(store.accessToken).toBe('')
    expect(store.username).toBe('')
    expect(localStorage.getItem('agromall.access-token')).toBeNull()
  })
})
