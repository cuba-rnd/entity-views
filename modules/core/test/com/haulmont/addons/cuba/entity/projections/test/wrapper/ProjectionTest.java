package com.haulmont.addons.cuba.entity.projections.test.wrapper;

import com.haulmont.addons.cuba.entity.projections.factory.EntityProjectionWrapper;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.ExtendedUser;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.projections.test.app.views.sample.SampleMinimalView;
import com.haulmont.addons.cuba.entity.projections.test.app.views.sample.SampleViewWithDelegate;
import com.haulmont.addons.cuba.entity.projections.test.app.views.sample.SampleWithParentView;
import com.haulmont.addons.cuba.entity.projections.test.app.views.user.SampleMinimalWithUserView;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EntityStates;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import com.haulmont.cuba.security.entity.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ProjectionTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(ProjectionTest.class);

    private MetadataTools metadataTools;
    private Persistence persistence;

    private DataManager dataManager;

    private SampleEntity data1, data2;
    private User user;
    private EntityStates entityStates;


    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        Metadata metadata = cont.metadata();
        persistence = cont.persistence();
        metadataTools = AppBeans.get(MetadataTools.class);
        dataManager = AppBeans.get(DataManager.class);
        entityStates = AppBeans.get(EntityStates.class);

        user = dataManager.load(ExtendedUser.class).one();

        data1 = metadata.create(SampleEntity.class);
        data1.setName("Data1");
        data1.setUser(user);

        data2 = metadata.create(SampleEntity.class);
        data2.setName("Data2");
        data2.setParent(data1);
        data2.setUser(user);

        persistence.runInTransaction((em) -> {
            em.persist(data1);
            em.persist(data2);
        });
    }

    @After
    public void tearDown() throws Exception {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());
        runner.update("delete from PLAYGROUND_SAMPLE_ENTITY");
    }

    @Test
    public void testLoadMinimalView() {
        SampleMinimalView swu = dataManager.load(SampleMinimalView.class).id(data1.getId()).one();
        Assert.assertEquals(swu.getName(), data1.getName());
    }

    @Test
    public void testSetterMinimalView() {
        SampleMinimalView swu = dataManager.load(SampleMinimalView.class).id(data1.getId()).one();
        swu.setName("New name");
        dataManager.commit(swu);
        swu = dataManager.load(SampleMinimalView.class).id(data1.getId()).one();
        Assert.assertEquals(swu.getName(), "New name");
    }


    @Test
    public void testLoadList() {
        List<SampleMinimalWithUserView> swuList = dataManager.load(SampleMinimalWithUserView.class).list();
        Assert.assertTrue(swuList.size() > 0);
    }

    @Test
    public void testLoadOne() {
        SampleMinimalWithUserView swu = dataManager.load(SampleMinimalWithUserView.class).id(data1.getId()).one();
        Assert.assertEquals(swu.getName(), data1.getName());
    }


    @Test
    public void testLoadOneById() {
        SampleMinimalWithUserView swu = dataManager.load(Id.of(data1.getId(), SampleMinimalWithUserView.class)).one();
        Assert.assertEquals(swu.getName(), data1.getName());
    }

    @Test
    public void testEmbeddedView() {
        SampleMinimalWithUserView swu = dataManager.load(SampleMinimalWithUserView.class).id(data1.getId()).one();

        assertEquals(data1.getName(), swu.getName());
        assertEquals(data1.getUser().getName(), swu.getUser().getName());

        Assert.assertEquals(data1.getClass(), swu.getOrigin().getClass());
        assertEquals(data1.getUser().getClass(), swu.getUser().getOrigin().getClass());
    }

    @Test
    public void testDefaultMethod() {

        SampleMinimalView sampleMinimal = dataManager.load(SampleMinimalView.class).id(data1.getId()).one();

        assertEquals(data1.getName(), sampleMinimal.getName());
        assertEquals(data1.getName().toLowerCase(), sampleMinimal.getNameLowercase());
    }

    @Test
    public void testDelegateMethod() {

        SampleViewWithDelegate sampleMinimal = dataManager.load(SampleViewWithDelegate.class).id(data1.getId()).one();

        assertEquals(data1.getName(), sampleMinimal.getName());
        assertEquals(data1.getName().toUpperCase(Locale.getDefault()), sampleMinimal.getNameUppercase());
    }

    @Test
    public void testDefaultMethodEmbedded() {

        SampleWithParentView sampleMinimal = dataManager.load(SampleWithParentView.class).id(data2.getId()).one();

        assertEquals(data2.getName(), sampleMinimal.getName());
        assertEquals(data2.getParent().getName().toLowerCase(), sampleMinimal.getParent().getNameLowercase());
    }

    @Test
    public void testEntityViewIsEntity() {

        SampleMinimalView sampleMinimalView = dataManager.load(SampleMinimalView.class).id(data2.getId()).one();

        assertEquals(data2.getId(), sampleMinimalView.getId());
        assertEquals(metadataTools.getInstanceName(data2), metadataTools.getInstanceName(sampleMinimalView));
        assertSame(data2.getMetaClass(), sampleMinimalView.getMetaClass());
        assertEquals(data2.getName(), sampleMinimalView.getValue("name"));

        sampleMinimalView.addPropertyChangeListener((e) -> assertEquals(sampleMinimalView.getValue(e.getProperty()), e.getValue()));
        String newName = String.format("Name%d", System.currentTimeMillis());
        sampleMinimalView.setValue("name", newName);
        assertNotEquals(data2.getName(), sampleMinimalView.getValue("name"));
    }

    @Test
    public void testCreateNewEntity() {
        SampleMinimalView sample = dataManager.create(SampleMinimalView.class);
        assertNull(sample.getName());
        assertNull(sample.getValue("name"));
        assertTrue(entityStates.isNew(sample));
    }

    @Test
    public void testSaveNewEntity() {
        SampleMinimalView sample = dataManager.create(SampleMinimalView.class);
        sample.setName("TestName");
        assertTrue(entityStates.isNew(sample));
        sample = dataManager.commit(sample);
        assertNotNull(sample.getId());
        assertEquals("TestName", sample.getName());
        assertEquals("TestName", sample.getValue("name"));
        assertTrue(entityStates.isDetached(sample));
    }


    @Test
    public void testSaveNewEntityAndRewrap() {
        SampleMinimalView sample = dataManager.create(SampleMinimalView.class);
        sample.setName("TestName");
        SampleMinimalView sampleWithUser = dataManager.commit(sample);
        assertNotNull(sample.getId());
        assertEquals("TestName", sampleWithUser.getName());
        assertEquals("TestName", sampleWithUser.getValue("name"));
        assertTrue(entityStates.isDetached(sampleWithUser));
    }

    @Test
    public void testSetViewEntity() {
        SampleMinimalWithUserView sampleWithUser = dataManager.create(SampleMinimalWithUserView.class);
        sampleWithUser.setName("TestName");
        sampleWithUser = dataManager.commit(sampleWithUser);
        assertNull(sampleWithUser.getUser());
        sampleWithUser.setUser(EntityProjectionWrapper.wrap(user, SampleMinimalWithUserView.UserMinimalView.class));
        sampleWithUser = dataManager.commit(sampleWithUser);
        assertNotNull(sampleWithUser.getUser());
        assertEquals(user.getId(), sampleWithUser.getUser().getId());
        assertEquals(user.getName(), sampleWithUser.getUser().getName());
    }
}
