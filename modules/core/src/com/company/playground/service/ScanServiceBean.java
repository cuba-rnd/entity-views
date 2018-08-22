package com.company.playground.service;

import com.company.playground.views.scan.ViewsConfiguration;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(ScanService.NAME)
public class ScanServiceBean implements ScanService {

    @Inject
    private ViewsConfiguration conf;

    @Override
    public void runScan() {
        try {

            conf.scan();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}