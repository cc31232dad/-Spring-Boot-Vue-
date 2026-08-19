import { isAxiosError } from 'axios'
import http from './http'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface LoginPayload {
  username: string
  password: string
  role?: 'USER' | 'FARMER' | 'ADMIN'
}

export interface RegisterPayload extends LoginPayload {
  phone: string
}

export interface TokenResponse {
  tokenType: 'Bearer'
  accessToken: string
}

export interface CurrentUser {
  userId: number
  username: string
  roles: string[]
}

export interface FarmerApplicationPayload {
  phone: string; password: string; realName: string; idCard: string; province: string; city: string; district: string
  detailAddress: string; category: string; licenseNo?: string
}

export interface FarmerApplicationView extends FarmerApplicationPayload {
  id: number; userId: number; status: 'PENDING' | 'APPROVED' | 'REJECTED'; rejectReason?: string
}

function unwrap<T>(response: ApiEnvelope<T>): T {
  if (response.code !== 0) {
    throw new Error(response.message || '请求未成功，请稍后重试。')
  }

  return response.data
}

export async function login(payload: LoginPayload): Promise<TokenResponse> {
  const { data } = await http.post<ApiEnvelope<TokenResponse>>('/auth/login', payload)
  return unwrap(data)
}

export async function register(payload: RegisterPayload): Promise<CurrentUser> {
  const { data } = await http.post<ApiEnvelope<CurrentUser>>('/auth/register', payload)
  return unwrap(data)
}

export async function applyFarmer(payload: FarmerApplicationPayload): Promise<FarmerApplicationView> {
  const { data } = await http.post<ApiEnvelope<FarmerApplicationView>>('/auth/farmer/apply', payload)
  return unwrap(data)
}

export async function me(): Promise<CurrentUser> {
  const { data } = await http.get<ApiEnvelope<CurrentUser>>('/auth/me')
  return unwrap(data)
}

export function errorMessage(error: unknown): string {
  if (isAxiosError<ApiEnvelope<unknown>>(error)) {
    const code = error.response?.data?.code
    const messages: Record<number, string> = {
      1001: '该用户名或手机号已被使用。',
      1002: '账号或密码不正确。',
      1004: '当前账号不属于所选登录身份。',
      1025: '您的入驻申请正在审核中，请耐心等待。',
      1026: '您的入驻申请未通过，可在农户注册页修改资料后重新提交。',
    }
    return (code && messages[code]) || error.response?.data?.message || '网络连接异常，请稍后重试。'
  }

  return error instanceof Error ? error.message : '操作未完成，请稍后重试。'
}
