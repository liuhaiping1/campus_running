<template>
  <!--
    布局切换由 Vue Router 嵌套路由自动完成：
    - 登录/注册/错误页 → 无布局，直接渲染页面组件
    - 学生/跑腿员页面 → FrontLayout 包裹
    - 管理端页面 → AdminLayout 包裹
  -->
  <router-view />
</template>
