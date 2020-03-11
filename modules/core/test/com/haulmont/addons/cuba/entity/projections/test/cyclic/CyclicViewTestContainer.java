package com.haulmont.addons.cuba.entity.projections.test.cyclic;

import com.haulmont.cuba.testsupport.TestContainer;

import java.util.ArrayList;
import java.util.Arrays;

public class CyclicViewTestContainer extends TestContainer {

    public CyclicViewTestContainer() {
        super();
        appComponents = new ArrayList<>(Arrays.asList(
                "com.haulmont.cuba"
        ));
        appPropertiesFiles = Arrays.asList(
                "com/haulmont/addons/cuba/entity/projections/test/cyclic/test-app.properties",
                "com/haulmont/cuba/testsupport/test-app.properties");
        springConfig = "com/haulmont/addons/cuba/entity/projections/test/cyclic/spring-cyclic-test.xml";
        autoConfigureDataSource();
    }
}
