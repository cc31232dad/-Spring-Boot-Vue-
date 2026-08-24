import { describe, expect, it } from 'vitest'
import { resolvePostLoginRoute } from './loginNavigation'

describe('post-login navigation', () => {
  it('sends administrators to the platform backoffice', () => {
    expect(resolvePostLoginRoute(['ADMIN'])).toBe('/admin')
  })

  it('sends farmers to their workbench', () => {
    expect(resolvePostLoginRoute(['FARMER'])).toBe('/farmer')
  })

  it('keeps buyers in the mall', () => {
    expect(resolvePostLoginRoute(['USER'])).toBe('/')
  })

  it('keeps local redirects and rejects external redirects', () => {
    expect(resolvePostLoginRoute(['ADMIN'], '/admin/products')).toBe('/admin/products')
    expect(resolvePostLoginRoute(['ADMIN'], 'https://example.com')).toBe('/admin')
    expect(resolvePostLoginRoute(['FARMER'], '//example.com')).toBe('/farmer')
  })
})
