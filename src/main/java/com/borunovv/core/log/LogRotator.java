package com.borunovv.core.log;

import com.borunovv.core.log.db.service.AutoLogCleaner;
import org.springframework.stereotype.Service;

@Service
public class LogRotator extends AutoLogCleaner {

    @Override
    protected void onCreate() {
        start();
    }
}