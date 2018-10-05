package com.haulmont.addons.cuba.entity.views.test.wrapper;

import com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper;
import com.haulmont.addons.cuba.entity.views.global.ViewSupportDataManager;
import com.haulmont.addons.cuba.entity.views.test.app.entity.EntityParameter;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.ParameterNameOnly;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleWithParameters;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DetailsViewsTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(DetailsViewsTest.class);

    private Metadata metadata;
    private Persistence persistence;
    private ViewSupportDataManager dataManager;
    private SampleEntity data1, data2, data3;
    private EntityParameter param1, param2, param3, param4;


    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        metadata = cont.metadata();
        persistence = cont.persistence();
        dataManager = AppBeans.get(ViewSupportDataManager.class);

        data1 = metadata.create(SampleEntity.class);
        data1.setName("Data1");

        param1 = metadata.create(EntityParameter.class);
        param1.setName("Param1");
        param1.setSampleEntity(data1);

        param2 = metadata.create(EntityParameter.class);
        param2.setName("Param2");
        param2.setSampleEntity(data1);

        data2 = metadata.create(SampleEntity.class);
        data2.setName("Data2");

        data3 = metadata.create(SampleEntity.class);
        data3.setName("Data3");

        param3 = metadata.create(EntityParameter.class);
        param3.setName("Param3");
        param3.setCompEntity(data3);

        param4 = metadata.create(EntityParameter.class);
        param4.setName("Param4");
        param4.setCompEntity(data3);

        persistence.runInTransaction((em) -> {
            em.persist(data1);
            em.persist(param1);
            em.persist(param2);
            em.persist(data2);
            em.persist(data3);
            em.persist(param3);
            em.persist(param4);
        });
    }

    @After
    public void tearDown() throws Exception {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());
        runner.update("delete from PLAYGROUND_ENTITY_PARAMETER");
        runner.update("delete from PLAYGROUND_SAMPLE_ENTITY");
    }

    @Test
    public void testMasterDetailsComposition(){
        SampleWithParameters sampleWithParameters = dataManager.loadWithView(SampleWithParameters.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data1")
                .list().get(0);
        List<ParameterNameOnly> params = sampleWithParameters.getParams();
        params.sort(Comparator.comparing(ParameterNameOnly::getName));
        assertEquals(2, params.size());
        assertTrue(params.get(0).getName().equals("Param1") && params.get(1).getName().equals("Param2"));
    }

    @Test
    public void testEmptyDetails(){
        SampleWithParameters sampleWithParameters = dataManager.loadWithView(SampleWithParameters.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list().get(0);
        List<ParameterNameOnly> params = sampleWithParameters.getParams();
        assertEquals(0, params.size());
    }

    @Test
    public void testAddChild() {
        SampleWithParameters parent = dataManager
                .loadWithView(SampleWithParameters.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data1")
                .list().get(0);
        assertEquals(2, parent.getParams().size());

        EntityParameter child = metadata.create(EntityParameter.class);
        child.setName("Param3");
        child.setSampleEntity(parent.getOrigin());
        parent.getParams().add(EntityViewWrapper.wrap(child, ParameterNameOnly.class));
        assertEquals(3, parent.getParams().size());

        dataManager.commit(child);

        parent = dataManager
                .loadWithView(SampleWithParameters.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data1")
                .list().get(0);
        List<ParameterNameOnly> children = parent.getParams();
        children.sort(Comparator.comparing(ParameterNameOnly::getName));
        assertEquals(3, children.size());
        assertEquals("Param3", children.get(2).getName());
    }

    @Test
    public void testComposition(){
        SampleWithParameters sampleWithParameters = dataManager.loadWithView(SampleWithParameters.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data3")
                .list().get(0);
        List<ParameterNameOnly> params = sampleWithParameters.getCompParams();
        assertEquals(2, params.size());
        assertTrue(params.get(0).getName().equals("Param3") || params.get(1).getName().equals("Param3"));
    }

    @Test
    public void testDeleteComposition(){
        SampleWithParameters sampleWithParameters = dataManager.loadWithView(SampleWithParameters.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data3")
                .list().get(0);
        dataManager.remove(sampleWithParameters);

        List<SampleWithParameters> testList = dataManager.loadWithView(SampleWithParameters.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data3")
                .list();
        assertEquals(0, testList.size());
    }

}
