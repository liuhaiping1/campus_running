import request from '@/utils/request'

// 跑腿员收益概览
export function getIncomeOverview() {
  return request.get('/api/runner/income/overview')
}

// 跑腿员收益明细
export function getIncomeList(params) {
  return request.get('/api/runner/income/list', { params })
}
