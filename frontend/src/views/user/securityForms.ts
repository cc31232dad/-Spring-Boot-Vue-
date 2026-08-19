const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/
export function validatePasswordChange(currentPassword: string, newPassword: string, confirmPassword: string) {
  if (!currentPassword) return '请输入当前密码'
  if (!PASSWORD_PATTERN.test(newPassword)) return '新密码需为 8-64 位，且同时包含字母和数字'
  if (newPassword !== confirmPassword) return '两次输入的新密码不一致'
  return ''
}
export function validatePhoneChange(currentPassword: string, phone: string) {
  if (!currentPassword) return '请输入当前密码'
  if (!/^1[3-9]\d{9}$/.test(phone)) return '请输入正确的 11 位手机号码'
  return ''
}
