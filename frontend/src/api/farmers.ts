import http from './http'
import type { FarmerApplicationView } from './auth'
export async function listFarmerApplications(): Promise<FarmerApplicationView[]> { const { data } = await http.get('/admin/farmers'); return data.data }
export async function approveFarmer(id: number): Promise<FarmerApplicationView> { const { data } = await http.post(`/admin/farmers/${id}/approve`); return data.data }
export async function rejectFarmer(id: number, reason: string): Promise<FarmerApplicationView> { const { data } = await http.post(`/admin/farmers/${id}/reject`, { reason }); return data.data }
