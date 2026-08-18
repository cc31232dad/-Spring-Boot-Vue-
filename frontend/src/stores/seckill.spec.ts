import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as api from '../api/seckill'
import type { SeckillActivity } from '../api/seckill'
import { useSeckillStore } from './seckill'

vi.mock('../api/seckill', () => ({ listActivities: vi.fn(), rush: vi.fn(), cancel: vi.fn() }))

describe('seckill store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads published activities', async () => {
    const activities: SeckillActivity[] = [{ id: 1, productId: 2, farmerId: 3, status: 'PUBLISHED', seckillPrice: 5, totalStock: 10, limitPerUser: 1, startAt: '2026-08-18T10:00:00', endAt: '2026-08-18T12:00:00' }]
    vi.mocked(api.listActivities).mockResolvedValue(activities)
    const store = useSeckillStore()

    await store.loadActivities()

    expect(store.activities).toEqual(activities)
    expect(store.error).toBe('')
  })

  it('delegates rush with receiver information', async () => {
    vi.mocked(api.rush).mockResolvedValue({ id: 9 })
    const store = useSeckillStore()
    const payload = { receiverName: 'Li Lei', receiverPhone: '13800000000', receiverAddress: 'Xi an' }

    await store.rush(1, payload)

    expect(api.rush).toHaveBeenCalledWith(1, payload)
  })
})
