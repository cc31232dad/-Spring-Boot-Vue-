import { describe, expect, it } from 'vitest'
import { validateProductImage } from './productImage'

describe('product image selection', () => {
  it('accepts supported image types within 5 MB', () => {
    const file = new File(['png'], 'apple.png', { type: 'image/png' })
    expect(validateProductImage(file)).toBeNull()
  })

  it('rejects unsupported types and oversized files', () => {
    expect(validateProductImage(new File(['text'], 'notes.txt', { type: 'text/plain' }))).toBe('只支持 JPG、PNG 或 WEBP 图片。')
    const large = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' })
    expect(validateProductImage(large)).toBe('图片大小不能超过 5MB。')
  })
})
