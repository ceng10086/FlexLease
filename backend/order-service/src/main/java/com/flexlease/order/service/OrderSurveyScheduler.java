package com.flexlease.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderSurveyScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(OrderSurveyScheduler.class);

    private final OrderSurveyService orderSurveyService;

    public OrderSurveyScheduler(OrderSurveyService orderSurveyService) {
        this.orderSurveyService = orderSurveyService;
    }

    @Scheduled(fixedDelayString = "${flexlease.order.survey.scan-interval-ms:600000}")
    public void activateSurveys() {
        try {
            orderSurveyService.activatePendingSurveys();
        } catch (Exception ex) {
            LOG.warn("激活满意度调查失败: {}", ex.getMessage());
        }
    }
}
