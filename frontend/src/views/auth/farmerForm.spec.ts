import { describe, expect, it } from 'vitest'
import { validateFarmerForm, type FarmerForm } from './farmerForm'

const valid: FarmerForm = { phone: '13800000001', password: 'Passw0rd!', confirmPassword: 'Passw0rd!', realName: '张三', idCard: '610102199001010011', province: '陕西省', city: '西安市', district: '雁塔区', detailAddress: '示例村1号', category: '新鲜水果', licenseNo: '', agreement: true }

describe('farmer application validation', () => {
  it('requires complete farmer credentials and agreement', () => {
    expect(validateFarmerForm({ ...valid, agreement: false })).toContain('同意')
    expect(validateFarmerForm({ ...valid, province: '' })).toContain('地区')
  })
  it('accepts a complete application', () => { expect(validateFarmerForm(valid)).toBe('') })
})
