package com.haulmont.addons.cuba.entity.views.test.wrapper;

import com.haulmont.addons.cuba.entity.views.factory.WrappingList;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleMinimalView;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WrappingListTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(WrappingListTest.class);

    private Metadata metadata;
    private List<SampleEntity> testData;


    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        metadata = cont.metadata();

        testData = new ArrayList<>(10);
        for (int i = 0; i < 10; i++){
            SampleEntity entity = metadata.create(SampleEntity.class);
            entity.setName(String.format("Data%d", i));
            testData.add(entity);
        }

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToArray(){
        List<SampleEntity> testList = testData.subList(0, 2);
        WrappingList<SampleEntity, SampleMinimalView> wrappingList = new WrappingList<>(testList, SampleMinimalView.class);

        assertEquals("Data0", wrappingList.get(0).getName());
        assertEquals("Data1", wrappingList.get(1).getName());


        Object[] objectArray = wrappingList.toArray();
        assertEquals(2, objectArray.length);
        assertEquals("Data0", ((SampleMinimalView)objectArray[0]).getName());
        assertEquals("Data1", ((SampleMinimalView)objectArray[1]).getName());

        SampleMinimalView[] viewsArray = wrappingList.toArray(new SampleMinimalView[0]);
        assertEquals(2, viewsArray.length);
        assertEquals("Data0", viewsArray[0].getName());
        assertEquals("Data1", viewsArray[1].getName());

        viewsArray = wrappingList.toArray(new SampleMinimalView[4]);
        assertEquals(4, viewsArray.length);
        assertEquals("Data0", viewsArray[0].getName());
        assertEquals("Data1", viewsArray[1].getName());
        assertNull(viewsArray[2]);
        assertNull(viewsArray[3]);
    }


    @Test
    public void testIterator(){
        WrappingList<SampleEntity, SampleMinimalView> wrappingList = new WrappingList<>(testData, SampleMinimalView.class);
        ListIterator<SampleMinimalView> iterator = wrappingList.listIterator();

        assertEquals("Data0", iterator.next().getName());
        iterator.next();
        assertEquals(2, iterator.nextIndex());
        iterator.next();
        assertEquals("Data3", iterator.next().getName());
        iterator.remove();
        assertNull(wrappingList.stream().filter(e -> e.getName().equals("Data3")).findFirst().orElse(null));
        assertEquals("Data4", iterator.next().getName());
        iterator.previous();
        assertEquals("Data2", iterator.previous().getName());
        assertTrue(iterator.hasPrevious());
        iterator.previous();
        iterator.previous();
        assertFalse(iterator.hasPrevious());
        assertTrue(iterator.hasNext());
    }

    @Test
    public void testRetainAll(){
        WrappingList<SampleEntity, SampleMinimalView> data1 = new WrappingList<>((new ArrayList<>(testData)).subList(0, 6), SampleMinimalView.class);
        data1.sort(Comparator.comparing(SampleMinimalView::getName));//To fill wrapper cache
        WrappingList<SampleEntity, SampleMinimalView> data2 = new WrappingList<>((new ArrayList<>(testData)).subList(4, 10), SampleMinimalView.class);
        if (data1.retainAll(data2)){
            assertEquals(2, data1.size());
            assertEquals("Data4", data1.get(0).getName());
            assertEquals("Data5", data1.get(1).getName());
            assertNull(data1.stream().filter(e -> "Data3".equals(e.getName())).findFirst().orElse(null));
        } else {
            fail();
        }
    }

    @Test
    public void testRemoveAll(){
        WrappingList<SampleEntity, SampleMinimalView> data1 = new WrappingList<>((new ArrayList<>(testData)).subList(0, 6), SampleMinimalView.class);
        data1.sort(Comparator.comparing(SampleMinimalView::getName));//To fill wrapper cache
        WrappingList<SampleEntity, SampleMinimalView> data2 = new WrappingList<>((new ArrayList<>(testData)).subList(4, 10), SampleMinimalView.class);
        if (data1.removeAll(data2)){
            assertEquals(4, data1.size());
            assertEquals("Data0", data1.get(0).getName());
            assertEquals("Data1", data1.get(1).getName());
            assertEquals("Data2", data1.get(2).getName());
            assertEquals("Data3", data1.get(3).getName());
            assertNull(data1.stream().filter(e -> "Data4".equals(e.getName())).findFirst().orElse(null));
        } else {
            fail();
        }
    }


}
