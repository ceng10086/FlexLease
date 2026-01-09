package com.flexlease.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 取证策略配置：每个阶段要求的最少照片/视频数量。
 * <p>
 * 约束点：发货、确认收货、退租完成等关键动作会据此校验取证是否足够。
 */
@ConfigurationProperties(prefix = "flexlease.order.proof-policy")
public class ProofPolicyProperties {

    private int shipmentPhotoRequired = 3;
    private int shipmentVideoRequired = 1;
    private int receivePhotoRequired = 2;
    private int receiveVideoRequired = 1;
    private int returnPhotoRequired = 2;
    private int returnVideoRequired = 1;

    public int getShipmentPhotoRequired() {
        return shipmentPhotoRequired;
    }

    public void setShipmentPhotoRequired(int shipmentPhotoRequired) {
        this.shipmentPhotoRequired = shipmentPhotoRequired;
    }

    public int getShipmentVideoRequired() {
        return shipmentVideoRequired;
    }

    public void setShipmentVideoRequired(int shipmentVideoRequired) {
        this.shipmentVideoRequired = shipmentVideoRequired;
    }

    public int getReturnPhotoRequired() {
        return returnPhotoRequired;
    }

    public void setReturnPhotoRequired(int returnPhotoRequired) {
        this.returnPhotoRequired = returnPhotoRequired;
    }

    public int getReturnVideoRequired() {
        return returnVideoRequired;
    }

    public void setReturnVideoRequired(int returnVideoRequired) {
        this.returnVideoRequired = returnVideoRequired;
    }

    public int getReceivePhotoRequired() {
        return receivePhotoRequired;
    }

    public void setReceivePhotoRequired(int receivePhotoRequired) {
        this.receivePhotoRequired = receivePhotoRequired;
    }

    public int getReceiveVideoRequired() {
        return receiveVideoRequired;
    }

    public void setReceiveVideoRequired(int receiveVideoRequired) {
        this.receiveVideoRequired = receiveVideoRequired;
    }
}
