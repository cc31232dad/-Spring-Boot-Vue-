import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/products'
import { mockCategories, mockProducts } from '../data/mockProducts'

export const useProductStore = defineStore('products', () => {
  const categories = ref<api.Category[]>([])
  const products = ref<api.ProductSummary[]>([])
  const currentProduct = ref<api.ProductDetail | null>(null)
  const keyword = ref('')
  const categoryId = ref<number | undefined>()
  const isUsingMockData = ref(false)

  function applyCatalog(categories: api.Category[], products: api.ProductSummary[]) {
    isUsingMockData.value = products.length === 0
    if (isUsingMockData.value) {
      return { categories: mockCategories, products: mockProducts }
    }
    return { categories, products }
  }

  async function loadCatalog() {
    const categoriesResult = await api.listCategories()
    const productsResult = await api.listProducts({ keyword: keyword.value || undefined, categoryId: categoryId.value })
    const catalog = applyCatalog(categoriesResult, productsResult)
    categories.value = catalog.categories
    products.value = catalog.products
  }

  async function selectCategory(id?: number) {
    categoryId.value = id
    const productsResult = await api.listProducts({ keyword: keyword.value || undefined, categoryId: id })
    products.value = productsResult.length ? productsResult : mockProducts.filter((product) => id === undefined || product.categoryId === id)
    isUsingMockData.value = productsResult.length === 0
  }

  async function search(value: string) {
    keyword.value = value
    const productsResult = await api.listProducts({ keyword: value || undefined, categoryId: categoryId.value })
    const query = value.trim().toLowerCase()
    products.value = productsResult.length ? productsResult : mockProducts.filter((product) => {
      return (!query || `${product.name} ${product.originPlace} ${product.categoryName}`.toLowerCase().includes(query)) &&
        (categoryId.value === undefined || product.categoryId === categoryId.value)
    })
    isUsingMockData.value = productsResult.length === 0
  }

  async function clearFilters() {
    keyword.value = ''
    categoryId.value = undefined
    const productsResult = await api.listProducts({ keyword: undefined, categoryId: undefined })
    products.value = productsResult.length ? productsResult : mockProducts
    isUsingMockData.value = productsResult.length === 0
  }

  async function loadProduct(id: number) {
    currentProduct.value = await api.getProduct(id)
  }

  return { categories, products, currentProduct, keyword, categoryId, isUsingMockData, loadCatalog, selectCategory, search, clearFilters, loadProduct }
})
