// 订单操作触发动作 → 中文标签
export const actionMap = {
  PUBLISH_ORDER: '发布订单',
  ACCEPT_ORDER: '接单',
  CONTACT_ORDER: '联系发布人',
  PICKUP_ORDER: '确认取件',
  DELIVER_ORDER: '确认送达',
  COMPLETE_ORDER: '确认完成',
  CANCEL_ORDER: '取消订单',
  PAY_ORDER: '支付订单'
}

export function actionLabel(triggerAction) {
  return actionMap[triggerAction] || triggerAction || '状态变更'
}

// 订单状态 → 中文标签（与 StatusTag.vue statusMap.order_status 保持同步）
export const orderStatusLabels = {
  0: '待支付', 1: '待接单', 2: '已接单', 3: '已联系用户',
  4: '已取件', 5: '配送中', 6: '已送达', 7: '已完成', 8: '已取消'
}
