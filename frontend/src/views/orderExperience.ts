import type { OrderStatus } from '../api/orders'

export interface OrderExperience {
  payment: boolean
  review: boolean
}

export function getOrderExperience(status: OrderStatus): OrderExperience {
  return {
    payment: status === 'PENDING_PAYMENT',
    review: status === 'COMPLETED',
  }
}

export function getOrderExperienceNotice(status: OrderStatus, action: unknown): string {
  const experience = getOrderExperience(status)
  if (action === 'payment' && experience.payment) {
    return '支付功能尚未接入，当前不会扣款或改变订单状态。'
  }
  if (action === 'review' && experience.review) {
    return '评价功能正在建设中，暂时无法提交评价。'
  }
  return ''
}
