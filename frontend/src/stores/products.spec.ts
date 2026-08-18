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

  it('uses the demo catalog when the backend catalog is empty', async () => {
    vi.mocked(api.listCategories).mockResolvedValue([])
    vi.mocked(api.listProducts).mockResolvedValue([])
    const store = useProductStore()

    await store.loadCatalog()

    expect(store.isUsingMockData).toBe(true)
    expect(store.products.length).toBeGreaterThan(10)
    expect(store.products[0].originPlace).toBeTruthy()
  })

  it('keeps real catalog data ahead of demo products', async () => {
    vi.mocked(api.listCategories).mockResolvedValue([{ id: 1, name: '水果' }])
    vi.mocked(api.listProducts).mockResolvedValue([{
      id: 9,
      name: '真实商品',
      price: 9.9,
      stock: 8,
      originPlace: '本地',
      imageUrl: 'https://example.com/real.jpg',
      categoryId: 1,
      categoryName: '水果'
    }])
    const store = useProductStore()

    await store.loadCatalog()

    expect(store.isUsingMockData).toBe(false)
    expect(store.products).toHaveLength(1)
  })
})
