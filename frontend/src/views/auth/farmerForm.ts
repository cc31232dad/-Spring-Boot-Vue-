export interface FarmerForm {
  phone: string
  password: string
  confirmPassword: string
  realName: string
  idCard: string
  province: string
  city: string
  district: string
  detailAddress: string
  category: string
  licenseNo: string
  agreement: boolean
}

export function validateFarmerForm(form: FarmerForm): string {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) return '请输入有效的手机号。'
  if (form.password.length < 8 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) return '密码需为 8～64 位且同时包含字母和数字。'
  if (form.password !== form.confirmPassword) return '两次输入的密码不一致。'
  if (!form.realName.trim() || !form.idCard.trim()) return '请填写真实姓名和身份证号。'
  if (!form.province || !form.city || !form.district || !form.detailAddress.trim()) return '请完整填写所在地区和详细地址。'
  if (!form.category) return '请选择主营品类。'
  if (!form.agreement) return '请先同意用户协议和隐私政策。'
  return ''
}
