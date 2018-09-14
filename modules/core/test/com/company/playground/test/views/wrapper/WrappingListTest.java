package com.company.playground.test.views.wrapper;

import com.company.playground.entity.SampleEntity;
import com.company.playground.test.AppTestContainer;
import com.company.playground.views.sample.SampleMinimalView;
import com.google.common.collect.Lists;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import com.haulmont.cuba.core.views.factory.WrappingList;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WrappingListTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(WrappingListTest.class);

    private Metadata metadata;
    private SampleEntity data1, data2;


    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        metadata = cont.metadata();

        data1 = metadata.create(SampleEntity.class);
        data1.setName("Data1");

        data2 = metadata.create(SampleEntity.class);
        data2.setName("Data2");

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToArray(){
        List<SampleEntity> testList = Lists.newArrayList(data1, data2);
        testList.sort(Comparator.comparing(SampleEntity::getName));
        WrappingList<SampleEntity, SampleMinimalView> wrappingList = new WrappingList<>(testList, SampleMinimalView.class);

        assertEquals("Data1", wrappingList.get(0).getName());
        assertEquals("Data2", wrappingList.get(1).getName());


        Object[] objectArray = wrappingList.toArray();
        assertEquals(2, objectArray.length);
        assertEquals("Data1", ((SampleMinimalView)objectArray[0]).getName());
        assertEquals("Data2", ((SampleMinimalView)objectArray[1]).getName());

        SampleMinimalView[] viewsArray = wrappingList.toArray(new SampleMinimalView[0]);
        assertEquals(2, viewsArray.length);
        assertEquals("Data1", viewsArray[0].getName());
        assertEquals("Data2", viewsArray[1].getName());

        viewsArray = wrappingList.toArray(new SampleMinimalView[4]);
        assertEquals(4, viewsArray.length);
        assertEquals("Data1", viewsArray[0].getName());
        assertEquals("Data2", viewsArray[1].getName());
        assertNull(viewsArray[2]);
        assertNull(viewsArray[3]);
    }

}
