package com.flexlease.order.service;

import com.flexlease.order.config.ProofPolicyProperties;
import com.flexlease.order.dto.ProofPolicySummary;
import com.flexlease.order.dto.ProofPolicySummary.ProofStagePolicy;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProofPolicyService {

    private final ProofPolicyProperties properties;

    public ProofPolicyService(ProofPolicyProperties properties) {
        this.properties = properties;
    }

    public ProofPolicySummary getPolicy() {
        return new ProofPolicySummary(
                new ProofStagePolicy(
                        properties.getShipmentPhotoRequired(),
                        properties.getShipmentVideoRequired(),
                        List.of(
                                "外观全景：展示包装完整度与封条状态",
                                "序列号/铭牌特写：便于核对设备身份",
                                "通电演示或功能视频：证明设备可正常运行"
                        ),
                        "WATERMARK: 订单号 + 发货时间"
                ),
                new ProofStagePolicy(
                        properties.getReceivePhotoRequired(),
                        properties.getReceiveVideoRequired(),
                        List.of(
                                "拆箱过程视频：保证运输过程未被替换",
                                "签收页面或物流单据照片：标注签收人和时间"
                        ),
                        "WATERMARK: 订单号 + 签收人"
                ),
                new ProofStagePolicy(
                        properties.getReturnPhotoRequired(),
                        properties.getReturnVideoRequired(),
                        List.of(
                                "设备外观与配件清单合照",
                                "快递面单与称重照片",
                                "如有损坏请在视频中逐一说明"
                        ),
                        "WATERMARK: 订单号 + 退租日期"
                )
        );
    }
}
