const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

export function validateProductImage(file: File) {
  if (!IMAGE_TYPES.has(file.type)) return '只支持 JPG、PNG 或 WEBP 图片。'
  if (file.size > MAX_IMAGE_BYTES) return '图片大小不能超过 5MB。'
  return null
}
