import { describe, expect, it } from 'vitest'
import source from './RegisterView.vue?raw'

describe('registration rules', () => {
  it('shows the account requirements beside the registration fields', () => {
    expect(source).toContain('4～32 位，只能使用字母、数字和下划线')
    expect(source).toContain('请输入 11 位数字')
    expect(source).toContain('8～64 位，必须同时包含字母和数字')
    expect(source).toContain('注册信息不符合规则，请检查用户名、手机号和密码。')
  })
})
