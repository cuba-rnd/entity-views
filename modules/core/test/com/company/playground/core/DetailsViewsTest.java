package com.company.playground.core;

import com.company.playground.AppTestContainer;
import com.company.playground.entity.EntityParameter;
import com.company.playground.entity.SampleEntity;
import com.company.playground.views.sample.ParameterNameOnly;
import com.company.playground.views.sample.SampleWithParameters;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.ViewSupportDataManager;
import com.haulmont.cuba.core.global.ViewsSupportEntityStates;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DetailsViewsTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(ViewInterfacesTest.class);

    private Metadata metadata;
    private Persistence persistence;
    private ViewSupportDataManager dataManager;
    private SampleEntity data1;
    private EntityParameter param1, param2;
    private ViewsSupportEntityStates entityStates;


    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        metadata = cont.metadata();
        persistence = cont.persistence();
        dataManager = AppBeans.get(ViewSupportDataManager.class);
        entityStates = AppBeans.get(ViewsSupportEntityStates.class);

        data1 = metadata.create(SampleEntity.class);
        data1.setName("Data1");

        param1 = metadata.create(EntityParameter.class);
        param1.setName("Param1");
        param1.setSampleEntity(data1);

        param2 = metadata.create(EntityParameter.class);
        param2.setName("Param2");
        param2.setSampleEntity(data1);

        persistence.runInTransaction((em) -> {
            em.persist(data1);
            em.persist(param1);
            em.persist(param2);
        });
    }

    @After
    public void tearDown() throws Exception {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());
        runner.update("delete from PLAYGROUND_ENTITY_PARAMETER");
        runner.update("delete from PLAYGROUND_SAMPLE_ENTITY");
    }

    @Test
    public void testMasterDetails(){
        SampleWithParameters sampleWithParameters = dataManager.loadWithView(SampleWithParameters.class).list().get(0);
        List<ParameterNameOnly> params = sampleWithParameters.getParams();
        assertTrue(params.get(0).getName().equals("Param1") || params.get(0).getName().equals("Param2"));
        assertEquals(2, params.size());
    }

}
