package com.haulmont.addons.cuba.entity.views.test.cyclic;

import com.haulmont.addons.cuba.entity.views.scan.exception.ViewInitializationException;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import com.haulmont.cuba.testsupport.TestContainer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyclicViewsTest {

    @ClassRule
    public static final TestContainer cont = CyclicViewTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(CyclicViewsTest.class);

    @Before
    public void setUp()  {
        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));
    }

    @Test(expected = ViewInitializationException.class)
    public void testCyclicDependencyFailure(){
        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        Assert.fail("The test should fail on Initialization due to a cyclic view presence");
    }

}
