package com.flexlease.payment.domain;

/**
 * 支付渠道枚举。
 *
 * <p>当前项目以 MOCK 为主用于演示；其他渠道为预留值，便于后续扩展真实通道。</p>
 */
public enum PaymentChannel {
    /** 模拟通道（不接入真实支付）。 */
    MOCK,
    /** 支付宝（预留）。 */
    ALIPAY,
    /** 微信支付（预留）。 */
    WECHAT,
    /** 银行转账（预留）。 */
    BANK_TRANSFER
}
