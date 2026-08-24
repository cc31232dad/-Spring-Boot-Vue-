package com.agromall.seckill.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SeckillTimeoutJob {
    private final SeckillService service;

    public SeckillTimeoutJob(SeckillService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "60000")
    public void cancelExpiredOrders() {
        service.cancelExpired();
    }
}
