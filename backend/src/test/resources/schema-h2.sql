-- H2数据库测试脚本
-- 适配MySQL语法到H2

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL COMMENT '主键',
  username VARCHAR(32) NOT NULL COMMENT '登录账号',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  real_name VARCHAR(32) NOT NULL COMMENT '真实姓名',
  nick_name VARCHAR(32) NULL COMMENT '昵称',
  phone VARCHAR(20) NOT NULL COMMENT '手机号',
  avatar_url VARCHAR(255) NULL COMMENT '头像地址',
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  user_status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 2禁用',
  last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE (username),
  UNIQUE (phone)
);

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL COMMENT '主键',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(32) NOT NULL COMMENT '角色名称',
  role_status TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT NOT NULL COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户编号',
  role_id BIGINT NOT NULL COMMENT '角色编号',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码冗余',
  grant_source TINYINT NOT NULL COMMENT '1注册默认 2认证通过 3管理员授予',
  grant_time TIMESTAMP NOT NULL COMMENT '授权时间',
  expire_time TIMESTAMP NULL COMMENT '过期时间',
  role_status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 2停用 3过期',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS errand_category (
  id BIGINT NOT NULL COMMENT '主键',
  category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
  category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
  base_fee DECIMAL(10,2) NOT NULL COMMENT '基础费用',
  distance_fee_rule CLOB NOT NULL COMMENT '距离收费规则JSON',
  urgent_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '加急附加费',
  weight_fee_rule CLOB NULL COMMENT '重量收费规则JSON',
  time_fee_rule CLOB NULL COMMENT '时段收费规则JSON',
  fee_rule_version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '收费规则版本',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  category_status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (category_name),
  UNIQUE (category_code)
);

CREATE TABLE IF NOT EXISTS runner_auth (
  id BIGINT NOT NULL COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '关联 sys_user.id',
  auth_batch_no VARCHAR(32) NOT NULL COMMENT '认证批次号',
  student_no VARCHAR(32) NOT NULL COMMENT '学号',
  school_name VARCHAR(64) NOT NULL COMMENT '学校名称',
  campus_name VARCHAR(64) NULL COMMENT '校区名称',
  cert_type TINYINT NOT NULL COMMENT '1学生证 2校园卡 3身份证',
  cert_no VARCHAR(64) NULL COMMENT '证件号码',
  cert_front_url VARCHAR(255) NOT NULL COMMENT '证件正面图片',
  cert_back_url VARCHAR(255) NULL COMMENT '证件背面图片',
  auth_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回 3失效',
  reject_reason VARCHAR(255) NULL COMMENT '驳回原因',
  review_admin_id BIGINT NULL COMMENT '审核管理员',
  review_time TIMESTAMP NULL COMMENT '审核时间',
  expire_time TIMESTAMP NULL COMMENT '认证失效时间',
  current_flag TINYINT NOT NULL DEFAULT 1 COMMENT '1当前提交记录 0历史记录',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (auth_batch_no)
);

CREATE TABLE IF NOT EXISTS errand_order (
  id BIGINT NOT NULL COMMENT '主键',
  order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
  publisher_id BIGINT NOT NULL COMMENT '发布人',
  runner_id BIGINT NULL COMMENT '接单跑腿员',
  category_id BIGINT NOT NULL COMMENT '任务分类',
  title VARCHAR(100) NOT NULL COMMENT '任务标题',
  order_desc VARCHAR(500) NOT NULL COMMENT '任务描述',
  pickup_address VARCHAR(255) NOT NULL COMMENT '取件地址',
  delivery_address VARCHAR(255) NOT NULL COMMENT '送达地址',
  pickup_lng DECIMAL(10,6) NULL COMMENT '取件点经度',
  pickup_lat DECIMAL(10,6) NULL COMMENT '取件点纬度',
  delivery_lng DECIMAL(10,6) NULL COMMENT '送达点经度',
  delivery_lat DECIMAL(10,6) NULL COMMENT '送达点纬度',
  distance_km DECIMAL(8,2) NULL COMMENT '预估距离',
  base_fee DECIMAL(10,2) NOT NULL COMMENT '基础费用',
  distance_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '距离费用',
  weight_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '重量费用',
  time_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '时段费用',
  tip_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '小费',
  order_amount DECIMAL(10,2) NOT NULL COMMENT '订单总额',
  platform_commission DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平台佣金',
  estimated_runner_income DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预估跑腿员收入',
  order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态',
  settlement_status TINYINT NOT NULL DEFAULT 0 COMMENT '结算状态',
  deadline_time TIMESTAMP NULL COMMENT '截止时间',
  accept_time TIMESTAMP NULL COMMENT '接单时间',
  contact_time TIMESTAMP NULL COMMENT '联系时间',
  pickup_time TIMESTAMP NULL COMMENT '取件时间',
  deliver_time TIMESTAMP NULL COMMENT '送达时间',
  complete_time TIMESTAMP NULL COMMENT '完成时间',
  cancel_time TIMESTAMP NULL COMMENT '取消时间',
  cancel_reason VARCHAR(255) NULL COMMENT '取消原因',
  cancel_user_id BIGINT NULL COMMENT '取消人',
  cancel_role VARCHAR(32) NULL COMMENT '取消角色',
  appeal_flag TINYINT NOT NULL DEFAULT 0 COMMENT '申诉标记',
  fee_rule_version VARCHAR(16) NULL COMMENT '收费规则版本快照',
  fee_detail CLOB NULL COMMENT '费用明细JSON',
  attachment_urls VARCHAR(1000) NULL COMMENT '附件URL',
  contact_name VARCHAR(32) NULL COMMENT '联系人',
  contact_phone VARCHAR(20) NULL COMMENT '联系电话',
  straight_distance_km DECIMAL(8,2) NULL COMMENT '直线距离',
  route_distance_km DECIMAL(8,2) NULL COMMENT '路线距离',
  route_duration_sec INT NULL COMMENT '路线时长',
  distance_source TINYINT NULL COMMENT '距离来源',
  distance_calc_status TINYINT NULL COMMENT '距离计算状态',
  map_provider TINYINT NULL COMMENT '地图提供商',
  route_strategy VARCHAR(32) NULL COMMENT '路线策略',
  distance_calc_time TIMESTAMP NULL COMMENT '距离计算时间',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (order_no)
);

CREATE TABLE IF NOT EXISTS order_status_log (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单编号',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号冗余',
  before_status TINYINT NULL COMMENT '变更前订单状态',
  after_status TINYINT NOT NULL COMMENT '变更后订单状态',
  trigger_action VARCHAR(64) NOT NULL COMMENT '触发动作',
  operator_user_id BIGINT NOT NULL DEFAULT 0 COMMENT '操作人',
  operator_role VARCHAR(32) NOT NULL COMMENT 'STUDENT/RUNNER/ADMIN/SYSTEM',
  remark VARCHAR(500) NULL COMMENT '状态变更说明',
  request_id VARCHAR(64) NULL COMMENT '请求号',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS errand_order_address (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号',
  address_role TINYINT NOT NULL COMMENT '1起点 2终点',
  address_source TINYINT NULL COMMENT '地址来源',
  map_provider TINYINT NULL COMMENT '地图提供商',
  map_poi_id VARCHAR(64) NULL COMMENT 'POI ID',
  source_ref_id BIGINT NULL COMMENT '来源ID',
  contact_name VARCHAR(32) NULL COMMENT '联系人',
  contact_phone VARCHAR(20) NULL COMMENT '联系电话',
  campus_name VARCHAR(64) NULL COMMENT '校区',
  building_name VARCHAR(64) NULL COMMENT '楼栋',
  detail_address VARCHAR(255) NOT NULL COMMENT '详细地址',
  formatted_address VARCHAR(255) NULL COMMENT '格式化地址',
  province_name VARCHAR(32) NULL COMMENT '省',
  city_name VARCHAR(32) NULL COMMENT '市',
  district_name VARCHAR(32) NULL COMMENT '区',
  adcode VARCHAR(16) NULL COMMENT '行政区划代码',
  longitude DECIMAL(10,6) NULL COMMENT '经度',
  latitude DECIMAL(10,6) NULL COMMENT '纬度',
  coord_type VARCHAR(16) NULL COMMENT '坐标类型',
  geocode_status TINYINT NULL COMMENT '地理编码状态',
  geocode_time TIMESTAMP NULL COMMENT '地理编码时间',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS errand_order_detail (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号',
  category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
  express_company VARCHAR(32) NULL COMMENT '快递公司',
  express_station VARCHAR(64) NULL COMMENT '快递站点',
  express_no VARCHAR(64) NULL COMMENT '快递单号',
  express_pickup_code VARCHAR(32) NULL COMMENT '取件码',
  express_phone_suffix VARCHAR(8) NULL COMMENT '手机尾号',
  package_count INT NULL DEFAULT 1 COMMENT '包裹数量',
  package_weight_kg DECIMAL(8,2) NULL COMMENT '包裹重量',
  package_size VARCHAR(32) NULL COMMENT '包裹尺寸',
  takeaway_platform VARCHAR(32) NULL COMMENT '外卖平台',
  takeaway_order_no VARCHAR(64) NULL COMMENT '外卖订单号',
  takeaway_pickup_code VARCHAR(32) NULL COMMENT '外卖取餐码',
  takeaway_phone_suffix VARCHAR(8) NULL COMMENT '手机尾号',
  merchant_name VARCHAR(64) NULL COMMENT '商家名称',
  merchant_phone VARCHAR(20) NULL COMMENT '商家电话',
  food_item_count INT NULL COMMENT '餐品数量',
  expected_pickup_time TIMESTAMP NULL COMMENT '期望取餐时间',
  need_insulation TINYINT NULL DEFAULT 0 COMMENT '需要保温',
  shopping_items CLOB NULL COMMENT '代买商品JSON',
  shopping_budget DECIMAL(10,2) NULL COMMENT '代买预算',
  allow_price_adjust TINYINT NULL DEFAULT 0 COMMENT '允许调价',
  document_name VARCHAR(64) NULL COMMENT '资料名称',
  document_count INT NULL COMMENT '资料数量',
  document_remark VARCHAR(255) NULL COMMENT '资料备注',
  help_type VARCHAR(32) NULL COMMENT '帮办类型',
  help_content VARCHAR(500) NULL COMMENT '帮办内容',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS payment_order (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  pay_no VARCHAR(64) NOT NULL COMMENT '支付单号',
  trade_no VARCHAR(64) NULL COMMENT '第三方交易号',
  pay_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  pay_channel VARCHAR(32) NULL COMMENT '支付渠道',
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态',
  pay_time TIMESTAMP NULL COMMENT '支付时间',
  expire_time TIMESTAMP NULL COMMENT '过期时间',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (pay_no)
);

CREATE TABLE IF NOT EXISTS refund_record (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  request_id VARCHAR(64) NOT NULL COMMENT '退款请求号',
  refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
  pay_no VARCHAR(64) NULL COMMENT '原支付单号',
  apply_user_id BIGINT NOT NULL COMMENT '申请人',
  refund_type TINYINT NOT NULL COMMENT '退款类型',
  refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  refund_reason VARCHAR(255) NULL COMMENT '退款原因',
  refund_status TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态',
  approve_admin_id BIGINT NULL COMMENT '审批管理员',
  approve_time TIMESTAMP NULL COMMENT '审批时间',
  approve_remark VARCHAR(255) NULL COMMENT '审批备注',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS runner_income_record (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  runner_id BIGINT NOT NULL COMMENT '跑腿员ID',
  income_amount DECIMAL(10,2) NOT NULL COMMENT '收入金额',
  settlement_status TINYINT NOT NULL DEFAULT 0 COMMENT '结算状态',
  settle_time TIMESTAMP NULL COMMENT '结算时间',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS order_evaluation (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  evaluator_id BIGINT NOT NULL COMMENT '评价人',
  rating TINYINT NOT NULL COMMENT '评分1-5',
  content VARCHAR(500) NULL COMMENT '评价内容',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS appeal_record (
  id BIGINT NOT NULL COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  appeal_no VARCHAR(32) NOT NULL COMMENT '申诉编号',
  applicant_id BIGINT NOT NULL COMMENT '申请人',
  appeal_type TINYINT NOT NULL COMMENT '申诉类型',
  appeal_reason VARCHAR(500) NOT NULL COMMENT '申诉原因',
  appeal_status TINYINT NOT NULL DEFAULT 0 COMMENT '申诉状态',
  handler_id BIGINT NULL COMMENT '处理人',
  handle_time TIMESTAMP NULL COMMENT '处理时间',
  handle_result VARCHAR(500) NULL COMMENT '处理结果',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS station_message (
  id BIGINT NOT NULL COMMENT '主键',
  receiver_id BIGINT NOT NULL COMMENT '接收人',
  msg_type TINYINT NOT NULL COMMENT '消息类型',
  title VARCHAR(100) NOT NULL COMMENT '标题',
  content VARCHAR(500) NOT NULL COMMENT '内容',
  biz_type VARCHAR(32) NULL COMMENT '业务类型',
  biz_id BIGINT NULL COMMENT '业务ID',
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '读取状态',
  read_time TIMESTAMP NULL COMMENT '读取时间',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_address (
  id BIGINT NOT NULL COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  contact_name VARCHAR(32) NOT NULL COMMENT '联系人',
  contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
  campus_name VARCHAR(64) NOT NULL COMMENT '校区',
  building_name VARCHAR(64) NOT NULL COMMENT '楼栋/地点',
  detail_address VARCHAR(255) NOT NULL COMMENT '详细地址',
  longitude DECIMAL(10,6) NULL COMMENT '经度',
  latitude DECIMAL(10,6) NULL COMMENT '纬度',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址',
  address_status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS system_notice (
  id BIGINT NOT NULL COMMENT '主键',
  notice_title VARCHAR(100) NOT NULL COMMENT '公告标题',
  notice_content CLOB NOT NULL COMMENT '公告内容',
  notice_type TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2重要 3维护',
  notice_status TINYINT NOT NULL COMMENT '0草稿 1已发布 2已下架',
  publish_time TIMESTAMP NULL COMMENT '发布时间',
  offline_time TIMESTAMP NULL COMMENT '下架时间',
  publisher_id BIGINT NOT NULL COMMENT '发布人',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL COMMENT '主键',
  trace_id VARCHAR(64) NULL COMMENT '链路追踪ID',
  module VARCHAR(32) NOT NULL COMMENT '模块',
  action VARCHAR(64) NOT NULL COMMENT '操作',
  biz_type VARCHAR(32) NULL COMMENT '业务类型',
  biz_id BIGINT NULL COMMENT '业务ID',
  operator_id BIGINT NULL COMMENT '操作人ID',
  operator_role VARCHAR(32) NULL COMMENT '操作人角色',
  description VARCHAR(255) NULL COMMENT '操作描述',
  request_method VARCHAR(16) NULL COMMENT '请求方法',
  request_url VARCHAR(255) NULL COMMENT '请求URL',
  request_params CLOB NULL COMMENT '请求参数',
  response_code INT NULL COMMENT '响应码',
  success TINYINT NULL COMMENT '是否成功',
  error_message VARCHAR(500) NULL COMMENT '错误信息',
  ip_address VARCHAR(64) NULL COMMENT 'IP地址',
  user_agent VARCHAR(255) NULL COMMENT 'UserAgent',
  cost_time BIGINT NULL COMMENT '耗时ms',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS campus_location (
  id BIGINT NOT NULL COMMENT '主键',
  location_name VARCHAR(64) NOT NULL COMMENT '地点名称',
  location_type VARCHAR(32) NOT NULL COMMENT '地点类型',
  campus_name VARCHAR(64) NULL COMMENT '校区',
  longitude DECIMAL(10,6) NULL COMMENT '经度',
  latitude DECIMAL(10,6) NULL COMMENT '纬度',
  location_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT NOT NULL DEFAULT 0,
  update_by BIGINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

-- 种子数据
-- 初始账号密码：admin/admin123, student/student123, runner/runner123
INSERT INTO sys_user (id, username, password, real_name, nick_name, phone, gender, user_status, create_time, update_time, create_by, update_by, is_deleted) VALUES
  (1000000000000000001, 'admin', '$2b$10$TteXdT/eC7uLCSi/Si1TM.8P2zOEWtWLHQNOLgx1wzmnyeYF6jnUS', '系统管理员', '管理员', '18800000000', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (1000000000000000002, 'student', '$2b$10$0T4I2dKkRZ.Qlo8BQ4icKODrhLrdKm8hbvANt57FCqdlT/oc0bs3O', '测试学生', '学生小明', '13900000001', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (1000000000000000003, 'runner', '$2b$10$fPApJnM4pJdXgj30Iu4Zku64.Kb52uxdZO60ZGM0wWV2oJIyvUKR.', '测试跑腿员', '跑腿小李', '13900000002', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO sys_role (id, role_code, role_name, role_status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES
  (1, 'STUDENT', '学生用户', 1, '普通学生用户，默认可发单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (2, 'RUNNER', '校园跑腿员', 1, '认证通过后可接单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (3, 'ADMIN', '系统管理员', 1, '后台运营与审核管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO sys_user_role (id, user_id, role_id, role_code, grant_source, grant_time, expire_time, role_status, create_time, update_time, create_by, update_by, is_deleted) VALUES
  (1000000000000000101, 1000000000000000001, 3, 'ADMIN', 3, CURRENT_TIMESTAMP, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (1000000000000000102, 1000000000000000002, 1, 'STUDENT', 1, CURRENT_TIMESTAMP, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (1000000000000000103, 1000000000000000003, 2, 'RUNNER', 2, CURRENT_TIMESTAMP, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO errand_category (id, category_name, category_code, base_fee, distance_fee_rule, urgent_fee, weight_fee_rule, time_fee_rule, fee_rule_version, sort_no, category_status, create_time, update_time, create_by, update_by, is_deleted) VALUES
  (1001, '代取快递', 'EXPRESS_PICKUP', 3.00, '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},{"min":3,"max":null,"fee":4}]', 2.00, '[{"min":0,"max":5,"fee":0},{"min":5,"max":10,"fee":3},{"min":10,"max":null,"fee":6}]', '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":2}]', 'v1', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (1002, '代买商品', 'SHOPPING', 4.00, '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},{"min":3,"max":null,"fee":5}]', 2.00, '[{"min":0,"max":5,"fee":0},{"min":5,"max":10,"fee":3},{"min":10,"max":null,"fee":6}]', '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":2}]', 'v1', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (1003, '代送资料', 'DOCUMENT_DELIVERY', 3.00, '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},{"min":3,"max":null,"fee":4}]', 2.00, '[{"min":0,"max":5,"fee":0},{"min":5,"max":null,"fee":2}]', '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":2}]', 'v1', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0),
  (1004, '临时帮办', 'TEMP_HELP', 5.00, '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":3},{"min":3,"max":null,"fee":6}]', 3.00, '[{"min":0,"max":5,"fee":0},{"min":5,"max":null,"fee":5}]', '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":3}]', 'v1', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO runner_auth (id, user_id, auth_batch_no, student_no, school_name, campus_name, cert_type, cert_no, cert_front_url, cert_back_url, auth_status, reject_reason, review_admin_id, review_time, expire_time, current_flag, create_time, update_time, create_by, update_by, is_deleted) VALUES
  (1000000000000000301, 1000000000000000003, 'AUTH20240101001', '2020001', '测试大学', '主校区', 1, NULL, 'https://example.com/cert1.jpg', NULL, 1, NULL, 1000000000000000001, CURRENT_TIMESTAMP, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0);
