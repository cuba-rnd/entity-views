package com.company.playground;

import com.company.playground.entity.SampleEntity;
import com.company.playground.views.sample.SampleMinimalView;
import com.company.playground.views.sample.SampleMinimalWithUserView;
import com.company.playground.views.sample.SampleWithParentView;
import com.haulmont.cuba.client.testsupport.CubaClientTestCase;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.sys.serialization.SerializationSupport;
import com.haulmont.cuba.core.views.factory.EntityViewWrapper;
import com.haulmont.cuba.core.views.scan.ViewsConfiguration;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class ViewInterfaceTest extends CubaClientTestCase {

    @Mocked
    ViewsConfiguration viewsConfiguration;

    @Before
    public void setUp() {
        addEntityPackage("com.haulmont.cuba");
        addEntityPackage("com.company.playground.entity");
        setupInfrastructure();

        new NonStrictExpectations(){{
            AppBeans.get(ViewsConfiguration.class); result = viewsConfiguration;
        }};


    }

    @Test
    public void testSerialization() {

        new NonStrictExpectations(){{
            viewsConfiguration.getEffectiveView(SampleWithParentView.class); result = SampleWithParentView.class;
            viewsConfiguration.getEffectiveView(SampleMinimalView.class); result = SampleMinimalWithUserView.class;
            viewsConfiguration.getEffectiveView(SampleMinimalWithUserView.UserMinimalView.class); result = SampleMinimalWithUserView.UserMinimalView.class;
        }};

        SampleEntity data1 = metadata.create(SampleEntity.class);
        data1.setName("Data1");

        SampleEntity data2 = metadata.create(SampleEntity.class);
        data2.setName("Data2");
        data2.setParent(data1);

        SampleWithParentView sampleWithParent = EntityViewWrapper.wrap(data2, SampleWithParentView.class);

        byte[] data = SerializationSupport.serialize(sampleWithParent);
        SampleWithParentView deserialized = (SampleWithParentView) SerializationSupport.deserialize(data);

        assertEquals(sampleWithParent.getName(), deserialized.getName());
        assertEquals(sampleWithParent.getParent().getName(), deserialized.getParent().getName());
        assertEquals(sampleWithParent.getParent().getNameLowercase(), sampleWithParent.getParent().getNameLowercase());
    }
}
