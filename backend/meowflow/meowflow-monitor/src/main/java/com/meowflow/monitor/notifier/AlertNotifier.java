package com.meowflow.monitor.notifier;

import com.meowflow.monitor.entity.AlertRecord;

public interface AlertNotifier {

    String getChannel();

    void send(AlertRecord alert);

    boolean supports(String channel);
}
