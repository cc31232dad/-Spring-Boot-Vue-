import type { Category, ProductSummary } from '../api/products'

export const mockCategories: Category[] = [
  { id: 1, name: '新鲜水果' },
  { id: 2, name: '时令蔬菜' },
  { id: 3, name: '粮油米面' },
  { id: 4, name: '茶叶蜂蜜' },
  { id: 5, name: '地方特产' },
  { id: 6, name: '肉禽蛋品' }
]

const images = {
  apples: 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=800&q=85',
  blueberries: 'https://images.unsplash.com/photo-1498557850523-fd3d118b962e?auto=format&fit=crop&w=800&q=85',
  mango: 'https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=800&q=85',
  orange: 'https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=800&q=85',
  tomato: 'https://images.unsplash.com/photo-1546470427-e5ac89cd0b8b?auto=format&fit=crop&w=800&q=85',
  vegetables: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=800&q=85',
  rice: 'https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=800&q=85',
  tea: 'https://images.unsplash.com/photo-1594631252845-29fc4cc8cde9?auto=format&fit=crop&w=800&q=85',
  honey: 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&w=800&q=85',
  eggs: 'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?auto=format&fit=crop&w=800&q=85',
  chicken: 'https://images.unsplash.com/photo-1604503468506-a8da13d82791?auto=format&fit=crop&w=800&q=85',
  mushroom: 'https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=800&q=85'
}

export const mockProducts: ProductSummary[] = [
  { id: 1001, categoryId: 1, categoryName: '新鲜水果', name: '陕西洛川高原红富士苹果 5 斤装', price: 39.9, stock: 88, originPlace: '陕西延安', imageUrl: images.apples },
  { id: 1002, categoryId: 1, categoryName: '新鲜水果', name: '云南高原蓝莓 125g × 4 盒', price: 49.9, stock: 65, originPlace: '云南玉溪', imageUrl: images.blueberries },
  { id: 1003, categoryId: 1, categoryName: '新鲜水果', name: '海南金煌芒果 新鲜现摘 5 斤', price: 45.8, stock: 72, originPlace: '海南三亚', imageUrl: images.mango },
  { id: 1004, categoryId: 1, categoryName: '新鲜水果', name: '江西赣南脐橙 橙香多汁 10 斤', price: 59.9, stock: 51, originPlace: '江西赣州', imageUrl: images.orange },
  { id: 1005, categoryId: 2, categoryName: '时令蔬菜', name: '农家自然熟普罗旺斯番茄 2.5kg', price: 29.9, stock: 42, originPlace: '山东潍坊', imageUrl: images.tomato },
  { id: 1006, categoryId: 2, categoryName: '时令蔬菜', name: '高山有机时令蔬菜礼盒 8 袋', price: 69.0, stock: 30, originPlace: '云南昆明', imageUrl: images.vegetables },
  { id: 1007, categoryId: 2, categoryName: '时令蔬菜', name: '东北秋木耳 小朵肉厚 250g', price: 35.9, stock: 94, originPlace: '黑龙江牡丹江', imageUrl: images.mushroom },
  { id: 1008, categoryId: 3, categoryName: '粮油米面', name: '五常生态稻花香大米 5kg', price: 89.0, stock: 38, originPlace: '黑龙江五常', imageUrl: images.rice },
  { id: 1009, categoryId: 3, categoryName: '粮油米面', name: '农家现磨玉米面 2.5kg', price: 24.9, stock: 57, originPlace: '吉林松原', imageUrl: images.rice },
  { id: 1010, categoryId: 4, categoryName: '茶叶蜂蜜', name: '安溪高山铁观音 清香型 250g', price: 79.0, stock: 46, originPlace: '福建安溪', imageUrl: images.tea },
  { id: 1011, categoryId: 4, categoryName: '茶叶蜂蜜', name: '云南百花成熟蜂蜜 500g', price: 58.0, stock: 64, originPlace: '云南大理', imageUrl: images.honey },
  { id: 1012, categoryId: 4, categoryName: '茶叶蜂蜜', name: '安徽黄山毛峰 明前春茶 100g', price: 99.0, stock: 26, originPlace: '安徽黄山', imageUrl: images.tea },
  { id: 1013, categoryId: 5, categoryName: '地方特产', name: '贵州刺梨原浆 10 瓶装', price: 68.8, stock: 33, originPlace: '贵州六盘水', imageUrl: images.orange },
  { id: 1014, categoryId: 5, categoryName: '地方特产', name: '新疆阿克苏冰糖心红枣 500g', price: 42.0, stock: 82, originPlace: '新疆阿克苏', imageUrl: images.apples },
  { id: 1015, categoryId: 6, categoryName: '肉禽蛋品', name: '农家散养土鸡蛋 30 枚', price: 36.9, stock: 49, originPlace: '湖北荆门', imageUrl: images.eggs },
  { id: 1016, categoryId: 6, categoryName: '肉禽蛋品', name: '草原风干牛肉干 250g', price: 75.0, stock: 28, originPlace: '内蒙古锡林郭勒', imageUrl: images.chicken }
]
