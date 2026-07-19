import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as api from '../api/products'
import { useProductStore } from './products'

vi.mock('../api/products', () => ({
  listCategories: vi.fn(),
  listProducts: vi.fn(),
  getProduct: vi.fn(),
  createProduct: vi.fn()
}))

describe('product store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads categories and products', async () => {
    vi.mocked(api.listCategories).mockResolvedValue([{ id: 1, name: '水果' }])
    vi.mocked(api.listProducts).mockResolvedValue([{
      id: 1,
      name: '洛川苹果',
      price: 29.9,
      stock: 100,
      originPlace: '陕西洛川',
      imageUrl: 'https://example.com/apple.jpg',
      categoryId: 1,
      categoryName: '水果'
    }])

    const store = useProductStore()
    await store.loadCatalog()

    expect(store.categories).toHaveLength(1)
    expect(store.products[0].name).toBe('洛川苹果')
  })

  it('clears both keyword and category filters before reloading products', async () => {
    vi.mocked(api.listProducts).mockResolvedValue([])
    const store = useProductStore()

    await store.search('apples')
    await store.selectCategory(1)
    await store.clearFilters()

    expect(store.keyword).toBe('')
    expect(store.categoryId).toBeUndefined()
    expect(api.listProducts).toHaveBeenLastCalledWith({ keyword: undefined, categoryId: undefined })
  })
})
