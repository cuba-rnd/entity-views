package com.haulmont.addons.cuba.entity.views.test.wrapper;

import com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper;
import com.haulmont.addons.cuba.entity.views.global.ViewSupportDataManager;
import com.haulmont.addons.cuba.entity.views.global.ViewsSupportEntityStates;
import com.haulmont.addons.cuba.entity.views.test.app.entity.ExtendedUser;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleMinimalView;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleWithParentView;
import com.haulmont.addons.cuba.entity.views.test.app.views.user.SampleMinimalWithUserView;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import com.haulmont.cuba.security.entity.User;
import mockit.Mocked;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BaseEntityViewTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(BaseEntityViewTest.class);

    private Metadata metadata;
    private Persistence persistence;

    @Mocked
    private ViewSupportDataManager dataManager;

    private SampleEntity data1, data2;
    private User user;
    private ViewsSupportEntityStates entityStates;


    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        metadata = cont.metadata();
        persistence = cont.persistence();
        dataManager = AppBeans.get(ViewSupportDataManager.class);
        entityStates = AppBeans.get(ViewsSupportEntityStates.class);

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
    public void testEmbeddedView() {
        SampleMinimalWithUserView swu = dataManager.loadWithView(SampleMinimalWithUserView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data1").list().get(0);

        assertEquals(data1.getName(), swu.getName());
        assertEquals(data1.getUser().getName(), swu.getUser().getName());

        Assert.assertEquals(data1.getClass(), swu.getOrigin().getClass());
        assertEquals(data1.getUser().getClass(), swu.getUser().getOrigin().getClass());
    }

    @Test
    public void testDefaultMethod() {

        SampleMinimalView sampleMinimal = dataManager.loadWithView(SampleMinimalView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data1")
                .list()
                .get(0);

        assertEquals(data1.getName(), sampleMinimal.getName());
        assertEquals(data1.getName().toLowerCase(), sampleMinimal.getNameLowercase());
    }


    @Test
    public void testDefaultMethodEmbedded() {

        SampleWithParentView sampleMinimal = dataManager.loadWithView(SampleWithParentView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

        assertEquals(data2.getName(), sampleMinimal.getName());
        assertEquals(data2.getParent().getName().toLowerCase(), sampleMinimal.getParent().getNameLowercase());
    }

    @Test
    public void testEntityViewIsEntity() {

        SampleMinimalView sampleMinimalView = dataManager.loadWithView(SampleMinimalView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

        assertEquals(data2.getId(), sampleMinimalView.getId());
        assertEquals(data2.getInstanceName(), sampleMinimalView.getInstanceName());
        assertSame(data2.getMetaClass(), sampleMinimalView.getMetaClass());
        assertEquals(data2.getName(), sampleMinimalView.getValue("name"));

        sampleMinimalView.addPropertyChangeListener((e) -> {
            assertEquals(sampleMinimalView.getValue(e.getProperty()), e.getValue());
        });
        String newName = String.format("Name%d", System.currentTimeMillis());
        sampleMinimalView.setValue("name", newName);
        assertNotEquals(data2.getName(), sampleMinimalView.getValue("name"));
    }

    @Test
    public void testCreateNewEntity() {
        SampleMinimalView sample = dataManager.createWithView(SampleMinimalView.class);
        assertNull(sample.getName());
        assertNull(sample.getValue("name"));
        assertTrue(entityStates.isNew(sample));
    }

    @Test
    public void testSaveNewEntity() {
        SampleMinimalView sample = dataManager.createWithView(SampleMinimalView.class);
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
        SampleMinimalView sample = dataManager.createWithView(SampleMinimalView.class);
        sample.setName("TestName");
        SampleMinimalWithUserView sampleWithUser = dataManager.commit(sample, SampleMinimalWithUserView.class);
        assertNotNull(sample.getId());
        assertEquals("TestName", sampleWithUser.getName());
        assertEquals("TestName", sampleWithUser.getValue("name"));
        assertNull(sampleWithUser.getUser());
        assertTrue(entityStates.isDetached(sampleWithUser));
    }

    @Test
    public void testSetViewEntity() {
        SampleMinimalWithUserView sampleWithUser = dataManager.createWithView(SampleMinimalWithUserView.class);
        sampleWithUser.setName("TestName");
        sampleWithUser = dataManager.commit(sampleWithUser, SampleMinimalWithUserView.class);
        assertNull(sampleWithUser.getUser());
        sampleWithUser.setUser(EntityViewWrapper.wrap(user, SampleMinimalWithUserView.UserMinimalView.class));
        sampleWithUser = dataManager.commit(sampleWithUser);
        assertNotNull(sampleWithUser.getUser());
        assertEquals(user.getId(), sampleWithUser.getUser().getId());
        assertEquals(user.getName(), sampleWithUser.getUser().getName());
    }
}