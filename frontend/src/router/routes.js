// 全局路由表
// 布局方案：FrontLayout 用于学生端和跑腿员端，AdminLayout 用于管理端
// 登录/注册/错误页不套布局
export const routes = [
  // ==================== 公开页面 ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guest: true, layout: 'none' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', guest: true, layout: 'none' }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: { title: '无权限', layout: 'none' }
  },

  // ==================== 学生端 / 跑腿员端（FrontLayout） ====================
  {
    path: '/',
    component: () => import('@/layouts/FrontLayout.vue'),
    meta: { requiresAuth: true, layout: 'front' },
    children: [
      // --- 首页 ---
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/student/Home.vue'),
        meta: { title: '首页' }
      },
      // --- 公告 ---
      {
        path: 'notices',
        name: 'NoticeList',
        component: () => import('@/views/student/NoticeList.vue'),
        meta: { title: '公告列表' }
      },
      // --- 地址 ---
      {
        path: 'user/address',
        name: 'AddressManage',
        component: () => import('@/views/student/AddressManage.vue'),
        meta: { title: '地址管理', roles: ['STUDENT', 'RUNNER'] }
      },
      // --- 消息 ---
      {
        path: 'user/messages',
        name: 'MessageCenter',
        component: () => import('@/views/student/MessageCenter.vue'),
        meta: { title: '消息中心' }
      },
      // --- 订单 ---
      {
        path: 'order/create',
        name: 'OrderCreate',
        component: () => import('@/views/student/OrderCreate.vue'),
        meta: { title: '发布订单', roles: ['STUDENT'] }
      },
      {
        path: 'order/my',
        name: 'MyOrders',
        component: () => import('@/views/student/MyOrders.vue'),
        meta: { title: '我的订单', roles: ['STUDENT', 'RUNNER'] }
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/student/OrderDetail.vue'),
        meta: { title: '订单详情' }
      },
      // --- 评价 / 申诉 / 支付 ---
      {
        path: 'order/:id/evaluation',
        name: 'OrderEvaluation',
        component: () => import('@/views/student/OrderEvaluation.vue'),
        meta: { title: '评价', roles: ['STUDENT'] }
      },
      {
        path: 'order/:id/appeal',
        name: 'OrderAppeal',
        component: () => import('@/views/student/OrderAppeal.vue'),
        meta: { title: '申诉', roles: ['STUDENT', 'RUNNER'] }
      },
      {
        path: 'pay/result',
        name: 'PayResult',
        component: () => import('@/views/pay/PayResult.vue'),
        meta: { title: '支付结果', roles: ['STUDENT'] }
      },
      // --- 个人中心 ---
      {
        path: 'profile',
        name: 'ProfileCenter',
        component: () => import('@/views/user/ProfileCenter.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },

  // ==================== 跑腿员端（FrontLayout） ====================
  {
    path: '/runner',
    component: () => import('@/layouts/FrontLayout.vue'),
    meta: { requiresAuth: true, layout: 'front' },
    children: [
      {
        path: 'auth',
        name: 'RunnerAuth',
        component: () => import('@/views/runner/RunnerAuth.vue'),
        // 允许学生访问，因为学生要申请成为跑腿员
        meta: { title: '跑腿员认证', roles: ['STUDENT', 'RUNNER'] }
      },
      {
        path: 'hall',
        name: 'TaskHall',
        component: () => import('@/views/runner/TaskHall.vue'),
        meta: { title: '任务大厅', roles: ['RUNNER'] }
      },
      {
        path: 'orders',
        name: 'RunnerOrders',
        component: () => import('@/views/runner/RunnerOrders.vue'),
        meta: { title: '我的接单', roles: ['RUNNER'] }
      },
      {
        path: 'order/:id',
        name: 'RunnerOrderDetail',
        component: () => import('@/views/runner/RunnerOrderDetail.vue'),
        meta: { title: '履约详情', roles: ['RUNNER'] }
      },
      // --- 收益 / 评价 ---
      {
        path: 'income',
        name: 'RunnerIncome',
        component: () => import('@/views/runner/RunnerIncome.vue'),
        meta: { title: '收益统计', roles: ['RUNNER'] }
      },
      {
        path: 'evaluations',
        name: 'RunnerEvaluations',
        component: () => import('@/views/runner/RunnerEvaluations.vue'),
        meta: { title: '评价反馈', roles: ['RUNNER'] }
      }
    ]
  },

  // ==================== 管理端（AdminLayout） ====================
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'admin' },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '管理首页' }
      },
      {
        path: 'runner-auth',
        name: 'RunnerAuthAdmin',
        component: () => import('@/views/admin/RunnerAuthAdmin.vue'),
        meta: { title: '跑腿员审核' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/AdminOrders.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'refunds',
        name: 'AdminRefunds',
        component: () => import('@/views/admin/AdminRefunds.vue'),
        meta: { title: '退款处理' }
      },
      {
        path: 'appeals',
        name: 'AdminAppeals',
        component: () => import('@/views/admin/AdminAppeals.vue'),
        meta: { title: '申诉处理' }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/AdminCategories.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'notices',
        name: 'AdminNotices',
        component: () => import('@/views/admin/AdminNotices.vue'),
        meta: { title: '公告管理' }
      },
      {
        path: 'audit-logs',
        name: 'AdminAuditLogs',
        component: () => import('@/views/admin/AdminAuditLogs.vue'),
        meta: { title: '审计日志' }
      }
    ]
  },

  // ==================== 404 兜底 ====================
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在', layout: 'none' }
  }
]
