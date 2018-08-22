package com.company.playground;

import com.company.playground.views.scan.ViewsConfiguration;
import com.company.playground.views.scan.exception.ViewInitializationException;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.sys.events.AppContextInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Created by Aleksey Stukalov on 22/08/2018.
 */
@Component("playground_Lifecycle")
public class AppLifecycle {

    @Inject
    private ViewsConfiguration viewsConfiguration;

    @EventListener(AppContextInitializedEvent.class)
    @Order(Events.LOWEST_PLATFORM_PRECEDENCE + 100)
    public void initEntityListeners() throws ViewInitializationException {
        viewsConfiguration.scan();
    }
}
