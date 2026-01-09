package com.flexlease.user.domain;

/**
 * 厂商入驻申请状态。
 */
public enum VendorApplicationStatus {
    /** 草稿：可反复编辑但未提交审核。 */
    DRAFT,
    /** 已提交：等待管理员审核。 */
    SUBMITTED,
    /** 已通过：厂商资料生效并绑定认证账号的 vendorId。 */
    APPROVED,
    /** 已驳回：可在原记录上修改后重新提交。 */
    REJECTED,
    /** 已暂停：平台暂停该申请或厂商资格。 */
    SUSPENDED
}
