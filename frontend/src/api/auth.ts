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

export async function me(): Promise<CurrentUser> {
  const { data } = await http.get<ApiEnvelope<CurrentUser>>('/auth/me')
  return unwrap(data)
}

export function errorMessage(error: unknown): string {
  if (isAxiosError<ApiEnvelope<unknown>>(error)) {
    return error.response?.data?.message || '网络连接异常，请稍后重试。'
  }

  return error instanceof Error ? error.message : '操作未完成，请稍后重试。'
}
