-- Campus Errand Service Platform
-- MySQL 8.0 schema and seed data generated from
-- campus_errand_requirements_dev_doc_v5_final_aligned.docx

CREATE DATABASE IF NOT EXISTS campus_errand
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE campus_errand;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS system_notice;
DROP TABLE IF EXISTS order_status_log;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS station_message;
DROP TABLE IF EXISTS appeal_record;
DROP TABLE IF EXISTS order_evaluation;
DROP TABLE IF EXISTS runner_income_record;
DROP TABLE IF EXISTS refund_record;
DROP TABLE IF EXISTS payment_order;
DROP TABLE IF EXISTS errand_order;
DROP TABLE IF EXISTS errand_category;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS runner_auth;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  username VARCHAR(32) NOT NULL COMMENT '登录账号',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  real_name VARCHAR(32) NOT NULL COMMENT '真实姓名',
  nick_name VARCHAR(32) NULL COMMENT '昵称',
  phone VARCHAR(20) NOT NULL COMMENT '手机号',
  avatar_url VARCHAR(255) NULL COMMENT '头像地址',
  gender TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  user_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常 2禁用',
  last_login_time DATETIME NULL COMMENT '最后登录时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一用户表';

CREATE TABLE runner_auth (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  auth_batch_no VARCHAR(32) NOT NULL COMMENT '认证批次号',
  student_no VARCHAR(32) NOT NULL COMMENT '学号',
  school_name VARCHAR(64) NOT NULL COMMENT '学校名称',
  campus_name VARCHAR(64) NULL COMMENT '校区名称',
  cert_type TINYINT UNSIGNED NOT NULL COMMENT '1学生证 2校园卡 3身份证(扩展)',
  cert_no VARCHAR(64) NULL COMMENT '证件号码，敏感字段需加密',
  cert_front_url VARCHAR(255) NOT NULL COMMENT '证件正面图片',
  cert_back_url VARCHAR(255) NULL COMMENT '证件背面图片',
  auth_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回 3失效',
  reject_reason VARCHAR(255) NULL COMMENT '驳回原因',
  review_admin_id BIGINT UNSIGNED NULL COMMENT '审核管理员',
  review_time DATETIME NULL COMMENT '审核时间',
  expire_time DATETIME NULL COMMENT '认证失效时间',
  current_flag TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1当前提交记录 0历史记录',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_batch_no (auth_batch_no),
  KEY idx_user_id (user_id),
  KEY idx_student_no (student_no),
  KEY idx_current_user (user_id, current_flag),
  KEY idx_auth_status (auth_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿员认证表';

CREATE TABLE user_address (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
  contact_name VARCHAR(32) NOT NULL COMMENT '联系人',
  contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
  campus_name VARCHAR(64) NOT NULL COMMENT '校区',
  building_name VARCHAR(64) NOT NULL COMMENT '楼栋/地点',
  detail_address VARCHAR(255) NOT NULL COMMENT '详细地址',
  longitude DECIMAL(10,6) NULL COMMENT '经度',
  latitude DECIMAL(10,6) NULL COMMENT '纬度',
  is_default TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认地址',
  address_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  active_default_user_id BIGINT UNSIGNED GENERATED ALWAYS AS (
    CASE WHEN is_deleted = 0 AND address_status = 1 AND is_default = 1 THEN user_id ELSE NULL END
  ) STORED COMMENT '同一用户只能有一个当前默认地址',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_user_default (user_id, is_default),
  UNIQUE KEY uk_active_default_user (active_default_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户地址表';

CREATE TABLE errand_category (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
  category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
  base_fee DECIMAL(10,2) NOT NULL COMMENT '基础费用',
  distance_fee_rule JSON NOT NULL COMMENT '距离收费规则，[min,max)，max=null 表示无上限',
  urgent_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '加急附加费',
  weight_fee_rule JSON NULL COMMENT '重量收费规则，[min,max)，max=null 表示无上限',
  time_fee_rule JSON NULL COMMENT '时段收费规则',
  fee_rule_version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '收费规则版本',
  sort_no INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
  category_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_name (category_name),
  UNIQUE KEY uk_category_code (category_code),
  KEY idx_category_status (category_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务分类与收费配置表';

CREATE TABLE errand_order (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
  publisher_id BIGINT UNSIGNED NOT NULL COMMENT '发布人',
  runner_id BIGINT UNSIGNED NULL COMMENT '接单跑腿员',
  category_id BIGINT UNSIGNED NOT NULL COMMENT '任务分类',
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
  order_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  platform_commission DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平台抽成',
  estimated_runner_income DECIMAL(10,2) NOT NULL COMMENT '预估跑腿员收益',
  order_status TINYINT UNSIGNED NOT NULL COMMENT '0待支付 1待接单 2已接单 3已联系用户 4已取件 5配送中 6已送达 7已完成 8已取消 9已关闭 10申诉中',
  pay_status TINYINT UNSIGNED NOT NULL COMMENT '0未支付 1支付中 2支付成功 3退款中 4已退款 5支付关闭 6部分退款',
  settlement_status TINYINT UNSIGNED NOT NULL COMMENT '结算摘要状态，事实来源为 runner_income_record',
  deadline_time DATETIME NOT NULL COMMENT '期望完成时间',
  accept_time DATETIME NULL COMMENT '接单时间',
  contact_time DATETIME NULL COMMENT '联系用户时间',
  pickup_time DATETIME NULL COMMENT '取件时间',
  deliver_time DATETIME NULL COMMENT '送达时间',
  complete_time DATETIME NULL COMMENT '完成时间',
  cancel_time DATETIME NULL COMMENT '取消时间',
  cancel_reason VARCHAR(255) NULL COMMENT '取消原因',
  appeal_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否申诉中',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_publisher_id (publisher_id),
  KEY idx_runner_id (runner_id),
  KEY idx_category_id (category_id),
  KEY idx_order_status (order_status),
  KEY idx_pay_status (pay_status),
  KEY idx_settlement_status (settlement_status),
  KEY idx_create_time (create_time),
  KEY idx_order_hall (order_status, category_id, deadline_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿订单主表';

CREATE TABLE payment_order (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '关联订单',
  order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
  pay_no VARCHAR(64) NOT NULL COMMENT '支付单号',
  pay_channel VARCHAR(32) NOT NULL COMMENT '支付渠道',
  pay_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  pay_status TINYINT UNSIGNED NOT NULL COMMENT '支付状态',
  trade_no VARCHAR(64) NULL COMMENT '第三方交易号',
  callback_content TEXT NULL COMMENT '支付回调原文',
  callback_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未回调 1已回调 2回调异常',
  callback_time DATETIME NULL COMMENT '回调时间',
  refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款金额',
  pay_time DATETIME NULL COMMENT '支付完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_id (order_id),
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';

CREATE TABLE refund_record (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '关联订单',
  refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
  apply_user_id BIGINT UNSIGNED NOT NULL COMMENT '申请人',
  refund_type TINYINT UNSIGNED NOT NULL COMMENT '1全额 2部分 3异常逆向',
  refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  refund_reason VARCHAR(255) NOT NULL COMMENT '退款原因',
  refund_status TINYINT UNSIGNED NOT NULL COMMENT '0待处理 1处理中 2成功 3失败',
  third_refund_no VARCHAR(64) NULL COMMENT '第三方退款流水号',
  request_id VARCHAR(64) NOT NULL COMMENT '退款请求幂等号',
  callback_content TEXT NULL COMMENT '最近一次退款回调或查询返回报文',
  retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '退款补偿重试次数',
  last_retry_time DATETIME NULL COMMENT '最近一次补偿重试时间',
  approve_admin_id BIGINT UNSIGNED NULL COMMENT '处理管理员',
  approve_result VARCHAR(255) NULL COMMENT '处理说明',
  refund_time DATETIME NULL COMMENT '退款完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refund_no (refund_no),
  UNIQUE KEY uk_request_id (request_id),
  KEY idx_order_id (order_id),
  KEY idx_refund_status (refund_status),
  KEY idx_third_refund_no (third_refund_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录表';

CREATE TABLE runner_income_record (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '关联订单',
  runner_id BIGINT UNSIGNED NOT NULL COMMENT '跑腿员',
  income_amount DECIMAL(10,2) NOT NULL COMMENT '收益金额',
  commission_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平台抽成',
  settlement_status TINYINT UNSIGNED NOT NULL COMMENT '0待结算 1结算中 2已结算 3已回滚',
  settlement_time DATETIME NULL COMMENT '结算完成时间',
  rollback_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '回滚金额',
  rollback_reason VARCHAR(255) NULL COMMENT '回滚原因',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_id_runner (order_id),
  KEY idx_runner_id (runner_id),
  KEY idx_settlement_status (settlement_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿员收益记录表';

CREATE TABLE order_evaluation (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '关联订单',
  publisher_id BIGINT UNSIGNED NOT NULL COMMENT '评价人',
  runner_id BIGINT UNSIGNED NOT NULL COMMENT '被评价跑腿员',
  star_score TINYINT UNSIGNED NOT NULL COMMENT '1~5 星',
  content VARCHAR(500) NULL COMMENT '评价内容',
  is_anonymous TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否匿名',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_id (order_id),
  KEY idx_runner_id (runner_id),
  CHECK (star_score BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单评价表';

CREATE TABLE appeal_record (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '关联订单',
  appeal_no VARCHAR(64) NOT NULL COMMENT '申诉单号',
  apply_user_id BIGINT UNSIGNED NOT NULL COMMENT '申诉人',
  apply_role VARCHAR(16) NOT NULL COMMENT 'STUDENT/RUNNER',
  appeal_type TINYINT UNSIGNED NOT NULL COMMENT '1取消争议 2履约争议 3退款争议',
  appeal_content VARCHAR(1000) NOT NULL COMMENT '申诉内容',
  evidence_urls VARCHAR(1000) NULL COMMENT '证据地址，多值逗号分隔或 JSON',
  appeal_status TINYINT UNSIGNED NOT NULL COMMENT '0待处理 1处理中 2已成立 3已驳回 4已关闭',
  before_order_status TINYINT UNSIGNED NOT NULL COMMENT '进入申诉前的订单状态',
  result_order_status TINYINT UNSIGNED NULL COMMENT '申诉结案后的目标状态',
  responsibility_type TINYINT UNSIGNED NULL COMMENT '1用户 2跑腿员 3双方 4平台/系统',
  refund_decision TINYINT UNSIGNED NULL COMMENT '0无退款 1全额退款 2部分退款',
  handle_admin_id BIGINT UNSIGNED NULL COMMENT '处理管理员',
  handle_result VARCHAR(500) NULL COMMENT '处理结果',
  handle_time DATETIME NULL COMMENT '处理时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_appeal_no (appeal_no),
  KEY idx_order_id (order_id),
  KEY idx_appeal_status (appeal_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申诉记录表';

CREATE TABLE station_message (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  receiver_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收人',
  biz_type VARCHAR(32) NOT NULL COMMENT 'ORDER/AUTH/NOTICE/APPEAL 等',
  biz_id BIGINT UNSIGNED NULL COMMENT '业务主键',
  title VARCHAR(100) NOT NULL COMMENT '消息标题',
  content VARCHAR(500) NOT NULL COMMENT '消息内容',
  message_level TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1普通 2重要',
  is_read TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已读',
  read_time DATETIME NULL COMMENT '已读时间',
  send_time DATETIME NOT NULL COMMENT '发送时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_receiver_user_id (receiver_user_id),
  KEY idx_is_read (is_read),
  KEY idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息表';

CREATE TABLE audit_log (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
  biz_type VARCHAR(32) NULL COMMENT '业务类型',
  biz_id VARCHAR(64) NULL COMMENT '业务标识',
  operator_id BIGINT UNSIGNED NOT NULL COMMENT '操作人',
  operator_role VARCHAR(16) NOT NULL COMMENT '操作人角色',
  action_type VARCHAR(32) NOT NULL COMMENT '操作类型',
  request_path VARCHAR(255) NULL COMMENT '请求路径',
  request_method VARCHAR(10) NULL COMMENT 'HTTP 方法',
  request_param TEXT NULL COMMENT '请求参数摘要',
  result_code VARCHAR(32) NOT NULL COMMENT '处理结果码',
  result_msg VARCHAR(255) NULL COMMENT '处理结果说明',
  ip_address VARCHAR(64) NULL COMMENT 'IP 地址',
  trace_id VARCHAR(64) NULL COMMENT '链路追踪号',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_trace_id (trace_id),
  KEY idx_operator_id (operator_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台审计日志表';

CREATE TABLE sys_role (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  role_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色定义表';

CREATE TABLE sys_dict_type (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  dict_type VARCHAR(64) NOT NULL COMMENT '字典类型编码',
  dict_name VARCHAR(64) NOT NULL COMMENT '字典类型名称',
  dict_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';

CREATE TABLE sys_dict_data (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  dict_type VARCHAR(64) NOT NULL COMMENT '字典类型编码',
  dict_value VARCHAR(32) NOT NULL COMMENT '字典值',
  dict_label VARCHAR(64) NOT NULL COMMENT '字典标签',
  sort_no INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
  css_class VARCHAR(64) NULL COMMENT '前端样式类',
  data_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_value (dict_type, dict_value),
  KEY idx_dict_type (dict_type),
  KEY idx_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';

CREATE TABLE sys_user_role (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色编号',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码冗余',
  grant_source TINYINT UNSIGNED NOT NULL COMMENT '1注册默认 2认证通过 3管理员授予',
  grant_time DATETIME NOT NULL COMMENT '授权时间',
  expire_time DATETIME NULL COMMENT '过期时间，NULL 表示长期有效',
  role_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1有效 2停用 3过期',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '固定为 0，撤销角色通过 role_status 控制',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user_id (user_id),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

CREATE TABLE order_status_log (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '订单编号',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号冗余',
  before_status TINYINT UNSIGNED NULL COMMENT '变更前订单状态',
  after_status TINYINT UNSIGNED NOT NULL COMMENT '变更后订单状态',
  trigger_action VARCHAR(64) NOT NULL COMMENT '触发动作',
  operator_user_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作人，系统任务为 0',
  operator_role VARCHAR(32) NOT NULL COMMENT 'STUDENT/RUNNER/ADMIN/SYSTEM',
  remark VARCHAR(500) NULL COMMENT '状态变更说明',
  request_id VARCHAR(64) NULL COMMENT '请求号或回调编号',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_order_id (order_id),
  KEY idx_order_no (order_no),
  KEY idx_before_after_status (before_status, after_status),
  KEY idx_operator_user_id (operator_user_id),
  KEY idx_request_id (request_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单状态流转日志表';

CREATE TABLE system_notice (
  id BIGINT UNSIGNED NOT NULL COMMENT '主键',
  notice_title VARCHAR(100) NOT NULL COMMENT '公告标题',
  notice_content TEXT NOT NULL COMMENT '公告内容',
  notice_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1普通 2重要 3维护',
  notice_status TINYINT UNSIGNED NOT NULL COMMENT '0草稿 1已发布 2已下架',
  publish_time DATETIME NULL COMMENT '发布时间',
  offline_time DATETIME NULL COMMENT '下架时间',
  publisher_id BIGINT UNSIGNED NOT NULL COMMENT '发布人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
  update_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_notice_status (notice_status),
  KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告表';

-- Seed data
-- Password is a placeholder BCrypt hash for demonstration only. Replace before production use.
INSERT INTO sys_user (
  id, username, password, real_name, nick_name, phone, gender, user_status,
  create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (1000000000000000001, 'admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiI3e4KcW1xg6QbgJzzc9EoRjQ7xS6G', '系统管理员', '管理员', '18800000000', 0, 1, NOW(), NOW(), 0, 0, 0);

INSERT INTO sys_role (
  id, role_code, role_name, role_status, remark, create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (1, 'STUDENT', '学生用户', 1, '普通学生用户，默认可发单', NOW(), NOW(), 0, 0, 0),
  (2, 'RUNNER', '校园跑腿员', 1, '认证通过后可接单', NOW(), NOW(), 0, 0, 0),
  (3, 'ADMIN', '系统管理员', 1, '后台运营与审核管理员', NOW(), NOW(), 0, 0, 0);

INSERT INTO sys_dict_type (
  id, dict_type, dict_name, dict_status, remark, create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (2001, 'order_status', '订单状态', 1, '跑腿订单生命周期状态', NOW(), NOW(), 0, 0, 0),
  (2002, 'pay_status', '支付状态', 1, '订单支付与退款状态', NOW(), NOW(), 0, 0, 0),
  (2003, 'settlement_status', '结算状态', 1, '跑腿员收益结算状态', NOW(), NOW(), 0, 0, 0),
  (2004, 'auth_status', '认证状态', 1, '跑腿员认证审核状态', NOW(), NOW(), 0, 0, 0),
  (2005, 'appeal_status', '申诉状态', 1, '申诉处理状态', NOW(), NOW(), 0, 0, 0),
  (2006, 'refund_status', '退款状态', 1, '退款记录处理状态', NOW(), NOW(), 0, 0, 0),
  (2007, 'notice_status', '公告状态', 1, '系统公告发布状态', NOW(), NOW(), 0, 0, 0),
  (2008, 'role_code', '角色编码', 1, '系统角色编码', NOW(), NOW(), 0, 0, 0);

INSERT INTO sys_dict_data (
  id, dict_type, dict_value, dict_label, sort_no, css_class, data_status, remark,
  create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (2101, 'order_status', '0', '待支付', 0, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2102, 'order_status', '1', '待接单', 1, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2103, 'order_status', '2', '已接单', 2, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2104, 'order_status', '3', '已联系用户', 3, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2105, 'order_status', '4', '已取件', 4, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2106, 'order_status', '5', '配送中', 5, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2107, 'order_status', '6', '已送达', 6, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2108, 'order_status', '7', '已完成', 7, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2109, 'order_status', '8', '已取消', 8, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2110, 'order_status', '9', '已关闭', 9, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2111, 'order_status', '10', '申诉中', 10, 'danger', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2201, 'pay_status', '0', '未支付', 0, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2202, 'pay_status', '1', '支付中', 1, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2203, 'pay_status', '2', '支付成功', 2, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2204, 'pay_status', '3', '退款中', 3, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2205, 'pay_status', '4', '已退款', 4, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2206, 'pay_status', '5', '支付关闭', 5, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2207, 'pay_status', '6', '部分退款', 6, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2301, 'settlement_status', '0', '待结算', 0, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2302, 'settlement_status', '1', '结算中', 1, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2303, 'settlement_status', '2', '已结算', 2, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2304, 'settlement_status', '3', '已回滚', 3, 'danger', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2401, 'auth_status', '0', '待审核', 0, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2402, 'auth_status', '1', '审核通过', 1, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2403, 'auth_status', '2', '审核驳回', 2, 'danger', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2404, 'auth_status', '3', '已失效', 3, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2501, 'appeal_status', '0', '待处理', 0, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2502, 'appeal_status', '1', '处理中', 1, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2503, 'appeal_status', '2', '已成立', 2, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2504, 'appeal_status', '3', '已驳回', 3, 'danger', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2505, 'appeal_status', '4', '已关闭', 4, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2601, 'refund_status', '0', '待处理', 0, 'warning', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2602, 'refund_status', '1', '处理中', 1, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2603, 'refund_status', '2', '成功', 2, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2604, 'refund_status', '3', '失败', 3, 'danger', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2701, 'notice_status', '0', '草稿', 0, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2702, 'notice_status', '1', '已发布', 1, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2703, 'notice_status', '2', '已下架', 2, 'info', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2801, 'role_code', 'STUDENT', '学生用户', 1, 'primary', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2802, 'role_code', 'RUNNER', '校园跑腿员', 2, 'success', 1, NULL, NOW(), NOW(), 0, 0, 0),
  (2803, 'role_code', 'ADMIN', '系统管理员', 3, 'danger', 1, NULL, NOW(), NOW(), 0, 0, 0);

INSERT INTO sys_user_role (
  id, user_id, role_id, role_code, grant_source, grant_time, expire_time, role_status,
  create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (1000000000000000101, 1000000000000000001, 3, 'ADMIN', 3, NOW(), NULL, 1, NOW(), NOW(), 0, 0, 0);

INSERT INTO errand_category (
  id, category_name, category_code, base_fee, distance_fee_rule, urgent_fee,
  weight_fee_rule, time_fee_rule, fee_rule_version, sort_no, category_status,
  create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (
    1001, '代取快递', 'EXPRESS_PICKUP', 3.00,
    '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},{"min":3,"max":null,"fee":4}]',
    2.00,
    '[{"min":0,"max":5,"fee":0},{"min":5,"max":10,"fee":3},{"min":10,"max":null,"fee":6}]',
    '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":2}]',
    'v1', 10, 1, NOW(), NOW(), 0, 0, 0
  ),
  (
    1002, '代买商品', 'SHOPPING', 4.00,
    '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},{"min":3,"max":null,"fee":5}]',
    2.00,
    '[{"min":0,"max":5,"fee":0},{"min":5,"max":10,"fee":3},{"min":10,"max":null,"fee":6}]',
    '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":2}]',
    'v1', 20, 1, NOW(), NOW(), 0, 0, 0
  ),
  (
    1003, '代送资料', 'DOCUMENT_DELIVERY', 3.00,
    '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":2},{"min":3,"max":null,"fee":4}]',
    2.00,
    '[{"min":0,"max":5,"fee":0},{"min":5,"max":null,"fee":2}]',
    '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":2}]',
    'v1', 30, 1, NOW(), NOW(), 0, 0, 0
  ),
  (
    1004, '临时帮办', 'TEMP_HELP', 5.00,
    '[{"min":0,"max":1,"fee":0},{"min":1,"max":3,"fee":3},{"min":3,"max":null,"fee":6}]',
    3.00,
    '[{"min":0,"max":5,"fee":0},{"min":5,"max":null,"fee":5}]',
    '[{"code":"NORMAL","fee":0},{"code":"NIGHT","start":"22:00","end":"07:00","fee":3}]',
    'v1', 40, 1, NOW(), NOW(), 0, 0, 0
  );

INSERT INTO system_notice (
  id, notice_title, notice_content, notice_type, notice_status, publish_time, offline_time,
  publisher_id, create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (
    1000000000000000201,
    '校园万能帮平台试运行公告',
    '平台当前处于毕业设计演示与试运行阶段，支付、退款和结算均使用沙箱或模拟流程。',
    1,
    1,
    NOW(),
    NULL,
    1000000000000000001,
    NOW(),
    NOW(),
    1000000000000000001,
    1000000000000000001,
    0
  );
