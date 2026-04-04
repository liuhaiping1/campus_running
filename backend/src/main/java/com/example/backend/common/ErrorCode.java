package com.example.backend.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举定义
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 通用错误 1xxx
    SUCCESS(0, "success"),
    SYSTEM_ERROR(1000, "系统异常"),
    BAD_REQUEST(1001, "请求参数错误"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无权限访问"),
    NOT_FOUND(1004, "资源不存在"),
    CONFLICT(1005, "业务状态冲突"),

    // 认证错误 2xxx
    USERNAME_EXISTS(2001, "用户名已存在"),
    PHONE_EXISTS(2002, "手机号已存在"),
    USERNAME_OR_PASSWORD_ERROR(2003, "用户名或密码错误"),
    ACCOUNT_DISABLED(2004, "账号已被禁用"),
    TOKEN_INVALID(2005, "Token无效"),
    TOKEN_EXPIRED(2006, "Token已过期"),

    // 跑腿认证错误 3xxx
    AUTH_NOT_FOUND(3001, "认证记录不存在"),
    AUTH_PENDING_EXISTS(3002, "存在待审核的认证申请"),
    AUTH_ALREADY_APPROVED(3003, "认证已通过，无需重复申请"),

    // 地址错误 4xxx
    ADDRESS_LIMIT_EXCEEDED(4001, "地址数量已达上限（10条）"),
    ADDRESS_NOT_FOUND(4002, "地址不存在"),
    ADDRESS_NOT_OWNED(4003, "无权操作该地址"),
    DEFAULT_ADDRESS_EXISTS(4004, "已存在默认地址"),

    // 订单错误 5xxx
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_NOT_OWNED(5002, "无权操作该订单"),
    ORDER_STATUS_CONFLICT(5003, "订单状态不允许此操作"),
    ORDER_ALREADY_PAID(5004, "订单已支付"),
    ORDER_NOT_PAID(5005, "订单未支付"),
    ORDER_ALREADY_ACCEPTED(5006, "订单已被接单"),
    ORDER_HALL_ACCESS_DENIED(5007, "任务大厅仅对跑腿员开放"),
    ORDER_CANNOT_CANCEL(5008, "当前状态不允许取消订单"),
    ORDER_CANNOT_ACCEPT(5009, "该订单不允许接单"),
    ORDER_CANNOT_EVALUATE(5010, "该订单不允许评价"),
    ORDER_CANNOT_ACCEPT_SELF(5011, "不能接自己发布的订单"),

    // 评价错误 6xxx
    EVALUATION_ALREADY_EXISTS(6001, "该订单已评价"),

    // 申诉错误 7xxx
    APPEAL_EXISTS(7001, "该订单存在进行中的申诉"),
    APPEAL_NOT_FOUND(7002, "申诉记录不存在"),
    APPEAL_CANNOT_HANDLE(7003, "申诉状态不允许此操作"),

    // 退款错误 8xxx
    REFUND_NOT_FOUND(8001, "退款记录不存在"),
    REFUND_CANNOT_APPROVE(8002, "退款状态不允许此操作"),

    // 分类错误 9xxx
    CATEGORY_NOT_FOUND(9001, "分类不存在"),
    CATEGORY_CODE_EXISTS(9002, "分类编码已存在"),
    CATEGORY_INVALID_FEE_RULE(9003, "收费区间配置不合法"),

    // 公告错误 10xxx
    NOTICE_NOT_FOUND(10001, "公告不存在"),

    // 字典错误 11xxx
    DICT_TYPE_NOT_FOUND(11001, "字典类型不存在");

    private final Integer code;
    private final String message;
}
