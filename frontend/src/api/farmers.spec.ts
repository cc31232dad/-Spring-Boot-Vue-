import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { approveFarmer, listFarmerApplications, rejectFarmer } from './farmers'
vi.mock('./http', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
describe('farmer review api', () => { beforeEach(() => vi.clearAllMocks())
  it('loads applications and sends review decisions', async () => { vi.mocked(http.get).mockResolvedValue({ data: { data: [] } }); vi.mocked(http.post).mockResolvedValue({ data: { data: { id: 1 } } }); await listFarmerApplications(); await approveFarmer(1); await rejectFarmer(2,'资料不完整'); expect(http.get).toHaveBeenCalledWith('/admin/farmers'); expect(http.post).toHaveBeenNthCalledWith(1,'/admin/farmers/1/approve'); expect(http.post).toHaveBeenNthCalledWith(2,'/admin/farmers/2/reject',{reason:'资料不完整'}) })
})
