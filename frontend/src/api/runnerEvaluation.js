import request from '@/utils/request'

// 跑腿员评价列表
export function getRunnerEvaluations(params) {
  return request.get('/api/runner/evaluations', { params })
}

// 跑腿员评价汇总
export function getRunnerEvaluationSummary() {
  return request.get('/api/runner/evaluations/summary')
}
