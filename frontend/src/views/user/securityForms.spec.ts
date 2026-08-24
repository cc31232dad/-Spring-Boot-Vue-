import { describe, expect, it } from 'vitest'
import { validatePasswordChange, validatePhoneChange } from './securityForms'
describe('security form validation', () => {
  it('requires matching strong new passwords', () => {
    expect(validatePasswordChange('Oldpass1', 'short', 'short')).toBeTruthy()
    expect(validatePasswordChange('Oldpass1', 'Newpass1', 'Different1')).toBe('两次输入的新密码不一致')
    expect(validatePasswordChange('Oldpass1', 'Newpass1', 'Newpass1')).toBe('')
  })
  it('requires current password and a mainland mobile number', () => {
    expect(validatePhoneChange('', '13900000000')).toBe('请输入当前密码')
    expect(validatePhoneChange('Passw0rd!', '123')).toBe('请输入正确的 11 位手机号码')
    expect(validatePhoneChange('Passw0rd!', '10000000000')).toBe('请输入正确的 11 位手机号码')
    expect(validatePhoneChange('Passw0rd!', '13900000000')).toBe('')
  })
})
