import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { addFavorite, removeFavorite } from './userCenter'
vi.mock('./http', () => ({ default: { post: vi.fn(), delete: vi.fn() } }))
describe('favorite api', () => {
  beforeEach(() => vi.clearAllMocks())
  it('adds a favorite and removes it by product id', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { data: { productId: 7 } } })
    await addFavorite(7)
    await removeFavorite(7)
    expect(http.post).toHaveBeenCalledWith('/favorites/7')
    expect(http.delete).toHaveBeenCalledWith('/favorites/7')
  })
})
