import { describe, expect, it } from 'vitest'
import router from './index'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

describe('role workspace route contracts', () => {
  it('keeps farmer and admin routes nested under role-protected roots', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/router/index.ts'), 'utf8')
    expect(source).toContain("path: '/farmer', component: FarmerLayout, meta: { requiresAuth: true, requiredRole: 'FARMER' }")
    expect(source).toContain("path: '/admin', component: AdminLayout, meta: { requiresAuth: true, requiredRole: 'ADMIN' }")
    for (const name of ['farmer-dashboard', 'farmer-products', 'farmer-product-new', 'farmer-product-edit', 'farmer-orders', 'admin-dashboard', 'admin-products', 'admin-farmers', 'admin-orders']) {
      expect(source).toContain(`name: '${name}'`)
    }
    expect(router.getRoutes().some((route) => route.name === 'farmer-dashboard')).toBe(true)
  })
})
