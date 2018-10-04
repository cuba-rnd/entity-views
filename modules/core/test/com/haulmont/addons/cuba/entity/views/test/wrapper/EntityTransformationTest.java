package com.haulmont.addons.cuba.entity.views.test.wrapper;

import com.haulmont.addons.cuba.entity.views.global.ViewSupportDataManager;
import com.haulmont.addons.cuba.entity.views.test.app.entity.ExtendedUser;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleMinimalView;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleWithParameters;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleWithParentView;
import com.haulmont.addons.cuba.entity.views.test.app.views.user.SampleMinimalWithUserView;
import com.haulmont.addons.cuba.entity.views.test.app.views.user.SampleWithUserView;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import com.haulmont.cuba.security.entity.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@SuppressWarnings("Duplicates")
public class EntityTransformationTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(BaseEntityViewTest.class);

    private Metadata metadata;
    private Persistence persistence;
    private ViewSupportDataManager dataManager;
    private SampleEntity data1, data2;

    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        cont.getSpringAppContext().publishEvent(new AppContextStartedEvent(cont.getSpringAppContext()));
        metadata = cont.metadata();
        persistence = cont.persistence();
        dataManager = AppBeans.get(ViewSupportDataManager.class);

        User user = dataManager.load(ExtendedUser.class).one();

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
    public void testDoubleLazyTransform() {
        SampleMinimalView sampleMinimalView = dataManager.loadWithView(SampleMinimalView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

        SampleWithParameters sampleWithParameters = sampleMinimalView.reload(SampleWithParameters.class);
        SampleWithParentView sampleWithParent = sampleWithParameters.reload(SampleWithParentView.class);

        //Reloading only final transformed version here
        assertEquals("Data2", sampleWithParent.getName());

        assertSame(sampleMinimalView.getOrigin(), sampleWithParameters.getOrigin());
        assertNotSame(sampleMinimalView.getOrigin(), sampleWithParent.getOrigin());
        assertNotSame(sampleWithParameters.getOrigin(), sampleWithParent.getOrigin());

        //Reloading intermediate view here
        assertEquals("Data2", sampleWithParameters.getName());
        assertNotSame(sampleMinimalView.getOrigin(), sampleWithParameters.getOrigin());
        assertNotSame(sampleMinimalView.getOrigin(), sampleWithParent.getOrigin());
        assertNotSame(sampleWithParameters.getOrigin(), sampleWithParent.getOrigin());
    }

    @Test
    public void testTransformToParentView() {
        //SampleMinimalWithUserView is a child of SampleMinimalView

        SampleMinimalWithUserView sampleMinimalWithUser = dataManager.loadWithView(SampleMinimalWithUserView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

        SampleMinimalView sampleMinimal = sampleMinimalWithUser.reload(SampleMinimalView.class);

        assertEquals(sampleMinimalWithUser.getName(), sampleMinimal.getName());
        assertEquals(sampleMinimalWithUser.getNameLowercase(), sampleMinimal.getNameLowercase());
        //In case of transforming to parent view we should not reload entity object
        assertSame(sampleMinimalWithUser.getOrigin(), sampleMinimal.getOrigin());
    }

    @Test
    public void testTransformToViewWithSameAttributes() {
        //SampleMinimalWithUserView contains subset of SampleWithUserView attributes
        SampleWithUserView sampleWithUser = dataManager.loadWithView(SampleWithUserView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

        SampleMinimalWithUserView sampleMinimal = sampleWithUser.reload(SampleMinimalWithUserView.class);

        //In case of transforming to view with the subset of attributes we should not reload entity object
        assertSame(sampleWithUser.getOrigin(), sampleMinimal.getOrigin());

        assertEquals(sampleWithUser.getName(), sampleMinimal.getName());
        assertEquals(sampleWithUser.getUser().getName(), sampleMinimal.getUser().getName());
    }

    @Test
    public void testAddingSettersByTransform() {
        //SampleWithUserView has only getters, but SampleMinimalWithUserView has setters too
        SampleWithUserView sampleWithUser = dataManager.loadWithView(SampleWithUserView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

        SampleMinimalWithUserView sampleMinimal = sampleWithUser.reload(SampleMinimalWithUserView.class);

        //In case of transforming to view with the subset of attributes we should not reload entity object
        assertSame(sampleWithUser.getOrigin(), sampleMinimal.getOrigin());

        String entityName = "New name" + System.currentTimeMillis();
        sampleMinimal.setName(entityName);
        assertEquals(sampleWithUser.getName(), sampleMinimal.getName());

        String userName = "New user name " + System.currentTimeMillis();
        sampleMinimal.getUser().setName(userName);
        assertEquals(sampleWithUser.getUser().getName(), sampleMinimal.getUser().getName());
    }

    @Test
    public void testTransformWithEntityReload() {

        SampleMinimalView sampleMinimalView = dataManager.loadWithView(SampleMinimalView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

        SampleWithParentView sampleWithParent = sampleMinimalView.reload(SampleWithParentView.class);
        Assert.assertEquals(sampleWithParent.getId(), sampleMinimalView.getOrigin().getId());
        assertEquals(sampleMinimalView.getName(), sampleWithParent.getName());
        assertNotNull(sampleWithParent.getParent());
        assertEquals(sampleWithParent.getParent().getName(), data1.getName());
        assertEquals(sampleWithParent.getParent().getNameLowercase(), data1.getName().toLowerCase());
        assertNotSame(sampleMinimalView.getOrigin(), sampleWithParent.getOrigin());
    }
}
