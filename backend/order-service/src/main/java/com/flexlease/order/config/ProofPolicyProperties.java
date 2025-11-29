package com.flexlease.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.order.proof-policy")
public class ProofPolicyProperties {

    private int shipmentPhotoRequired = 3;
    private int shipmentVideoRequired = 1;
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
}
