package com.flexlease.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 业务回放日志写入器（消息/事件级）。
 *
 * <p>用于把关键业务事件的收发记录落库到 {@code audit.business_replay_log}，便于排查跨服务事件链路、
 * 以及基于同一聚合（如订单）做收发时间线回放。为保持主流程稳定，写入失败会被吞掉并仅输出 debug 日志。</p>
 */
@Component
public class BusinessReplayLogWriter {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessReplayLogWriter.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceName;

    public BusinessReplayLogWriter(JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${spring.application.name:unknown}") String serviceName) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }

    public void writeOutgoing(String topic,
                              String routingKey,
                              String eventType,
                              String aggregateType,
                              UUID aggregateId,
                              Object payload,
                              OffsetDateTime occurredAt) {
        write("OUT", topic, routingKey, eventType, aggregateType, aggregateId, payload, occurredAt);
    }

    public void writeIncoming(String topic,
                              String routingKey,
                              String eventType,
                              String aggregateType,
                              UUID aggregateId,
                              Object payload,
                              OffsetDateTime occurredAt) {
        write("IN", topic, routingKey, eventType, aggregateType, aggregateId, payload, occurredAt);
    }

    private void write(String direction,
                       String topic,
                       String routingKey,
                       String eventType,
                       String aggregateType,
                       UUID aggregateId,
                       Object payload,
                       OffsetDateTime occurredAt) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            json = String.valueOf(payload);
        }
        try {
            jdbcTemplate.update("""
                            INSERT INTO audit.business_replay_log (
                              direction,
                              service_name,
                              topic,
                              routing_key,
                              event_type,
                              aggregate_type,
                              aggregate_id,
                              payload,
                              occurred_at
                            ) VALUES (?,?,?,?,?,?,?,?,?)
                            """,
                    truncate(direction, 10),
                    truncate(serviceName, 50),
                    truncate(topic, 100),
                    truncate(routingKey, 200),
                    truncate(eventType, 100),
                    truncate(aggregateType, 100),
                    aggregateId,
                    truncate(json, 20000),
                    occurredAt
            );
        } catch (Exception ex) {
            LOG.debug("Skip persisting business replay log due to error: {}", ex.getMessage());
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
