package com.flexlease.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.order.proof-policy")
public class ProofPolicyProperties {

    private int shipmentPhotoRequired = 3;
    private int shipmentVideoRequired = 1;

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
}
