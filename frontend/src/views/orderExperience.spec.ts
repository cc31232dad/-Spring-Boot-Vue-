import { describe, expect, it } from 'vitest'
import type { OrderStatus } from '../api/orders'
import { getOrderExperience, getOrderExperienceNotice } from './orderExperience'

describe('order experience actions', () => {
  it('offers payment only while an order is pending payment', () => {
    expect(getOrderExperience('PENDING_PAYMENT')).toEqual({ payment: true, review: false })
  })

  it('offers review only after an order is completed', () => {
    expect(getOrderExperience('COMPLETED')).toEqual({ payment: false, review: true })
  })

  it.each<OrderStatus>(['PENDING_SHIPMENT', 'SHIPPED', 'CANCELLED'])(
    'does not offer unsupported actions for %s',
    (status) => {
      expect(getOrderExperience(status)).toEqual({ payment: false, review: false })
    },
  )

  it('explains that the payment entry cannot charge or update the order', () => {
    expect(getOrderExperienceNotice('PENDING_PAYMENT', 'payment')).toBe(
      '支付功能尚未接入，当前不会扣款或改变订单状态。',
    )
  })

  it('explains that completed orders cannot submit reviews yet', () => {
    expect(getOrderExperienceNotice('COMPLETED', 'review')).toBe(
      '评价功能正在建设中，暂时无法提交评价。',
    )
  })

  it('ignores actions that do not match the current order status', () => {
    expect(getOrderExperienceNotice('COMPLETED', 'payment')).toBe('')
    expect(getOrderExperienceNotice('PENDING_PAYMENT', 'review')).toBe('')
    expect(getOrderExperienceNotice('SHIPPED', 'payment')).toBe('')
  })
})
