package com.flexlease.order.domain;

/**
 * 订单时间线/取证/纠纷等动作的“业务角色”。
 * <p>
 * 与鉴权角色（JWT roles）相近，但更偏向事件记录与审计用途。
 */
public enum OrderActorRole {
    USER,
    VENDOR,
    ADMIN,
    ARBITRATOR,
    REVIEW_PANEL,
    INTERNAL
}
