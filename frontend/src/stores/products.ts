import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/products'

export const useProductStore = defineStore('products', () => {
  const categories = ref<api.Category[]>([])
  const products = ref<api.ProductSummary[]>([])
  const currentProduct = ref<api.ProductDetail | null>(null)
  const keyword = ref('')
  const categoryId = ref<number | undefined>()

  async function loadCatalog() {
    categories.value = await api.listCategories()
    products.value = await api.listProducts({ keyword: keyword.value || undefined, categoryId: categoryId.value })
  }

  async function selectCategory(id?: number) {
    categoryId.value = id
    products.value = await api.listProducts({ keyword: keyword.value || undefined, categoryId: id })
  }

  async function search(value: string) {
    keyword.value = value
    products.value = await api.listProducts({ keyword: value || undefined, categoryId: categoryId.value })
  }

  async function loadProduct(id: number) {
    currentProduct.value = await api.getProduct(id)
  }

  return { categories, products, currentProduct, keyword, categoryId, loadCatalog, selectCategory, search, loadProduct }
})
