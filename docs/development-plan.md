# 校园万能帮跑腿服务平台开发计划

## 1. 目标与交付物

本计划面向毕业设计开发与答辩演示，目标是在可控时间内完成一条稳定主链路：

`注册登录 -> 跑腿员认证 -> 发布订单 -> 支付模拟 -> 任务大厅 -> 抢单 -> 履约状态更新 -> 确认完成 -> 评价 -> 收益/统计 -> 后台审核与处理`

当前已具备的设计交付物：

- 数据库脚本：`database/schema.sql`
- 接口文档：`api/openapi.yaml`
- 需求文档：`campus_errand_requirements_dev_doc_v5_final_aligned.docx`

最终代码交付物建议包括：

- 后端 Spring Boot 3.2+ 项目
- 前端 Vue 3 + Vite + Element Plus 项目
- MySQL 初始化脚本与演示数据
- OpenAPI/Swagger 接口文档
- 测试用例与答辩演示脚本

## 2. 技术栈与工程结构

后端：

- JDK 17
- Spring Boot 3.2+
- Spring Security 6 + JWT
- MyBatis-Plus
- MySQL 8.0
- Redis
- SpringDoc OpenAPI
- JUnit 5 + MockMvc

前端：

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router 4
- Axios
- ECharts

推荐仓库结构：

```text
campus_running/
  backend/
    src/main/java/...
    src/main/resources/
  frontend/
    src/
  database/
    schema.sql
  api/
    openapi.yaml
  docs/
    development-plan.md
```

## 3. 开发阶段

### 阶段 0：工程初始化

目标：搭好可运行的前后端空工程，保证数据库能初始化。

后端任务：

- 创建 Spring Boot 3.2+ 项目。
- 接入 MyBatis-Plus、Spring Security、JWT、Redis、SpringDoc。
- 配置 MySQL 数据源。
- 执行 `database/schema.sql`，确认 18 张表和初始化数据成功创建。
- 建立统一响应结构 `ApiResponse<T>`。
- 建立全局异常处理。
- 建立基础包结构：`controller`、`service`、`mapper`、`entity`、`dto`、`vo`、`security`、`job`、`common`。

前端任务：

- 创建 Vue 3 + Vite 项目。
- 接入 Element Plus、Pinia、Vue Router、Axios。
- 建立基础布局：前台布局、后台布局、登录页。
- 建立 Axios 拦截器，统一处理 Token 和错误码。

验收标准：

- 后端启动成功。
- 前端启动成功。
- 数据库初始化成功。
- Swagger/OpenAPI 页面可访问。

## 4. 后端开发计划

### 阶段 1：认证与权限

优先级：最高。

实现接口：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/runner-auth/apply`
- `POST /api/admin/runner-auth/{id}/review`
- `GET /api/admin/runner-auth/list`

核心任务：

- 用户注册默认授予 `STUDENT` 角色。
- 登录成功返回 JWT 与角色列表。
- Spring Security 按角色保护接口。
- 跑腿员认证支持多批次提交。
- 重新提交认证时，在同一事务内将旧 `current_flag=1` 改为 `0`，再插入新批次。
- 审核通过后授予 `RUNNER` 角色。

重点测试：

- 禁用账号不能登录。
- 未认证用户不能访问任务大厅和抢单接口。
- 重复提交待审核认证返回 `409`。
- 审核通过后用户角色包含 `RUNNER`。

### 阶段 2：基础数据与用户中心

实现接口：

- `GET /api/system/dict/{dictType}`
- `GET /api/address`
- `POST /api/address`
- `PUT /api/address/{id}`
- `DELETE /api/address/{id}`
- `POST /api/address/{id}/default`
- `GET /api/notice/list`
- `GET /api/category/list`
- `GET /api/message/list`
- `POST /api/message/{id}/read`
- `POST /api/message/read-all`

核心任务：

- 字典数据从 `sys_dict_type/sys_dict_data` 查询。
- 地址最多 10 条。
- 同一用户只能有一条默认地址。
- 消息支持未读数、单条已读、全部已读。
- 公告仅展示已发布数据。
- 前台任务分类只展示启用状态分类，用于发布订单和任务大厅筛选。

重点测试：

- 第 11 条地址创建失败。
- 设置默认地址会取消其他默认地址。
- 用户不能修改或删除别人的地址。
- 消息只能由接收人读取。

### 阶段 3：订单与支付主链路

实现接口：

- `POST /api/order`
- `GET /api/order`
- `GET /api/order/{id}`
- `POST /api/pay/{orderId}`
- `GET /api/order/hall`
- `POST /api/order/{id}/accept`
- `POST /api/order/{id}/status`
- `POST /api/order/{id}/confirm`
- `POST /api/order/{id}/cancel`

核心任务：

- 创建订单时状态为 `order_status=0`、`pay_status=0`。
- 支付成功后订单进入 `待接单`。
- 任务大厅只展示已支付、待接单、未删除订单。
- 抢单必须使用条件更新防止并发抢单。
- 履约状态按迁移表流转。
- 每次状态变化写入 `order_status_log`。
- 送达后用户确认完成，生成收益记录。
- 24 小时未确认可由定时任务自动完成。

重点测试：

- 未支付订单不能进入任务大厅。
- 多个跑腿员并发抢单时只能一个成功。
- 非接单人不能更新履约状态。
- 状态不能越级流转。
- 已申诉订单不能自动完成。

### 阶段 4：退款、申诉、评价、收益

实现接口：

- `POST /api/appeal`
- `POST /api/admin/appeal/{id}/handle`
- `POST /api/admin/refund/{id}/approve`
- `POST /api/evaluation`

核心任务：

- 同一订单同一时刻只允许一个未关闭申诉。
- 申诉记录保存 `before_order_status` 和 `result_order_status`。
- 已支付未接单取消生成全额退款记录。
- 部分退款时 `pay_status=6`，并更新 `payment_order.refund_amount`。
- 同一退款单补偿重试复用 `request_id`。
- 订单完成且未评价时才允许评价。
- 一单只能评价一次。
- 收益以 `runner_income_record` 为事实来源。

重点测试：

- 重复评价返回 `409`。
- 重复未关闭申诉返回 `409`。
- 退款重试不生成重复退款。
- 部分退款后金额展示一致。
- 结算后逆向退款生成回滚记录。

### 阶段 5：后台运营与统计

实现接口：

- `GET /api/admin/order/list`
- `GET /api/admin/category`
- `POST /api/admin/category`
- `PUT /api/admin/category/{id}`
- `DELETE /api/admin/category/{id}`
- `POST /api/admin/notice`
- `PUT /api/admin/notice/{id}`
- `POST /api/admin/notice/{id}/status`
- `GET /api/admin/stat/overview`
- `GET /api/admin/audit-log/list`

核心任务：

- 后台订单支持按状态、支付状态、关键词查询。
- 任务分类支持结构化 JSON 收费规则。
- 分类收费区间必须连续、不重叠，最后必须有 `max=null` 兜底区间。
- 公告支持草稿、发布、下架。
- 统计概览展示订单量、成交额、退款额、活跃跑腿员、申诉数量、待审核数量。
- 审计日志支持按模块、操作人、trace_id 查询，敏感资料查看必须可追溯。

重点测试：

- 非管理员不能访问后台接口。
- 分类编码不能重复。
- 无效收费区间不能保存。
- 公告发布后可在前台公告列表查看。

## 5. 前端开发计划

### 阶段 1：基础框架

页面：

- 登录页
- 注册页
- 前台首页/任务大厅布局
- 用户中心布局
- 后台管理布局

任务：

- 路由守卫读取 Token。
- Pinia 保存用户信息和角色。
- Axios 统一处理 `401`、`403`、`409`。
- 菜单按角色显示。

### 阶段 2：学生用户功能

页面：

- 地址管理
- 发布订单
- 我的订单
- 订单详情
- 支付模拟结果页
- 评价页面
- 消息中心
- 公告列表

核心交互：

- 发布订单时按分类和距离展示费用明细。
- 订单详情展示状态流转。
- 已送达订单展示确认收货按钮。
- 已完成未评价订单展示评价入口。

### 阶段 3：跑腿员功能

页面：

- 跑腿员认证申请
- 任务大厅
- 接单订单
- 履约状态更新
- 收益统计
- 评价反馈

核心交互：

- 未认证用户进入任务大厅时提示先认证。
- 抢单成功后进入订单履约页。
- 履约按钮按当前状态展示下一步动作。

### 阶段 4：后台管理功能

页面：

- 后台首页统计
- 跑腿员审核
- 订单管理
- 退款处理
- 申诉处理
- 分类管理
- 公告管理
- 审计日志查询

核心交互：

- 审核通过/驳回必须给出明确反馈。
- 退款处理支持全额、部分、拒绝。
- 申诉处理展示证据、责任归属和结果状态。
- ECharts 展示订单量、成交额、分类占比、退款率、申诉率。

## 6. 数据库与数据策略

初始化：

- 执行 `database/schema.sql`。
- 确认 `sys_role` 包含 `STUDENT`、`RUNNER`、`ADMIN`。
- 确认 `sys_dict_type/sys_dict_data` 状态字典完整。
- 确认 `errand_category` 有至少 4 个任务分类。
- 确认管理员账号存在。

开发约束：

- 所有业务查询必须带 `is_deleted = 0`。
- 不使用物理外键，Service 层保证一致性。
- 金额统一使用 `BigDecimal`。
- 状态值统一使用枚举类，不在代码里散落数字。
- 订单状态变更必须写 `order_status_log`。
- 管理员关键操作必须写 `audit_log`。

## 7. 测试计划

### 单元测试

覆盖对象：

- 费用计算规则
- 订单状态迁移校验
- 抢单条件更新
- 认证重提事务
- 退款幂等
- 评分计算

### 接口测试

优先覆盖：

- 注册登录
- 跑腿员认证申请与审核
- 发布订单
- 支付模拟
- 并发抢单
- 履约状态更新
- 确认收货
- 评价
- 取消退款
- 申诉处理

### 前端测试

重点检查：

- 路由权限
- 表单校验
- 状态按钮是否正确显示
- 后台操作反馈
- 移动端和桌面端布局

### 演示测试

必须准备一组完整演示数据：

- 一个学生账号
- 一个跑腿员账号
- 一个管理员账号
- 一条待支付订单
- 一条待接单订单
- 一条履约中订单
- 一条已完成可评价订单
- 一条退款/申诉订单

## 8. 里程碑安排

建议按 4 周开发。

第 1 周：

- 完成前后端工程初始化。
- 完成数据库初始化。
- 完成注册登录、JWT、角色权限。
- 完成地址、字典、公告、消息基础查询。

第 2 周：

- 完成跑腿员认证申请与审核。
- 完成订单创建、支付模拟、任务大厅。
- 完成抢单和履约状态更新。

第 3 周：

- 完成确认收货、评价、收益记录。
- 完成取消、退款、申诉处理。
- 完成后台订单、分类、公告管理。

第 4 周：

- 完成统计看板。
- 完成接口测试和关键流程测试。
- 完成前端体验打磨。
- 准备答辩演示数据、截图和讲解稿。

## 9. 风险与应对

风险：支付宝沙箱接入耗时。

应对：先实现模拟支付成功回调，再接支付宝沙箱，保证主链路不断。

风险：并发抢单实现不严谨。

应对：必须使用条件更新，并检查影响行数。影响行数为 0 时返回“订单已被接走”。

风险：状态流转混乱。

应对：集中封装订单状态机，不允许 Controller 直接改状态。

风险：前端页面过多导致时间失控。

应对：先完成主链路页面，再补后台和统计。答辩优先展示完整流程。

风险：退款、申诉逻辑复杂。

应对：V1 只实现全额退款、部分退款、拒绝退款三种明确结果，保留日志和字段扩展。

## 10. 开发顺序建议

严格按以下顺序推进：

1. 数据库初始化
2. 后端认证与权限
3. 前端登录注册
4. 字典、地址、公告、消息
5. 跑腿员认证
6. 订单创建与支付
7. 任务大厅与抢单
8. 履约状态流转
9. 确认收货与评价
10. 退款与申诉
11. 后台运营
12. 统计看板
13. 测试与答辩演示

不要先做统计、地图美化或复杂后台筛选。主链路稳定之前，这些都不是关键路径。
