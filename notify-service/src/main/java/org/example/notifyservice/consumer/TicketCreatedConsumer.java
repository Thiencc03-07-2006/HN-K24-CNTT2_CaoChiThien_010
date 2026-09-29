package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

public class TicketCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketCreatedConsumer.class);

    private final EmailService emailService;

    public TicketCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${notification.kafka.order-created-topic}")
    public void consume(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("Bỏ qua sự kiện ticket có email rỗng");
            return;
        }
        log.info("Nhận sự kiện tạo ticket thành công cho email: {}", email);
        emailService.sendOrderCreatedEmail(email);
        log.info("Đã gửi email xác nhận đặt tới: {}", email);
    }
}
