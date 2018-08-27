package com.company.playground.core;

import com.company.playground.AppTestContainer;
import com.company.playground.entity.SampleEntity;
import com.company.playground.views.factory.EntityViewWrapper;
import com.company.playground.views.sample.SampleMinimalView;
import com.company.playground.views.sample.SampleMinimalWithUserView;
import com.company.playground.views.sample.SampleWithParentView;
import com.company.playground.views.sample.SampleWithUserView;
import com.company.playground.views.scan.ViewsConfiguration;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.entity.User;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class SampleIntegrationTest {

    @ClassRule
    public static AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(SampleIntegrationTest.class);

    private Metadata metadata;
    private Persistence persistence;
    private DataManager dataManager;
    private ViewsConfiguration conf;
    private SampleEntity data1, data2;


    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        metadata = cont.metadata();
        persistence = cont.persistence();
        dataManager = AppBeans.get(DataManager.class);
        conf = AppBeans.get(ViewsConfiguration.class);


        User user;
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            TypedQuery<User> query = em.createQuery(
                    "select u from sec$User u where u.login = :userLogin", User.class);
            query.setParameter("userLogin", "admin");
            user = query.getResultList().get(0);
            tx.commit();
        }

        data1 = metadata.create(SampleEntity.class);
        data1.setName("Data1");
        data1.setUser(user);

        data2 = metadata.create(SampleEntity.class);
        data2.setName("Data2");
        data2.setParent(data1);
        data2.setUser(user);

        persistence.runInTransaction( (em) -> {
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
    public void testLoadUser() {
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            TypedQuery<User> query = em.createQuery(
                    "select u from sec$User u where u.login = :userLogin", User.class);
            query.setParameter("userLogin", "admin");
            List<User> users = query.getResultList();
            tx.commit();
            assertEquals(1, users.size());
        }
    }

    @Test
    public void testWrap() {
        User user = dataManager.load(User.class).list().get(0);
        SampleMinimalWithUserView.UserMinimalView userMinimal = EntityViewWrapper.wrap(user, SampleMinimalWithUserView.UserMinimalView.class);
        assertEquals(user.getLogin(), userMinimal.getLogin());
        assertEquals(user.getName(), userMinimal.getName());
    }

    @Test
    public void testEmbeddedView() {
        SampleEntity se = dataManager.load(SampleEntity.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data1")
                .view(conf.getViewByInterface(SampleMinimalWithUserView.class))
                .list().get(0);
        SampleMinimalWithUserView swu = EntityViewWrapper.wrap(se, SampleMinimalWithUserView.class);

        assertEquals(data1.getName(), swu.getName());
        assertEquals(data1.getUser().getName(), swu.getUser().getName());

        assertEquals(data1.getClass(), swu.getOrigin().getClass());
        assertEquals(data1.getUser().getClass(), swu.getUser().getOrigin().getClass());
    }

    @Test
    public void testDefaultMethod() {
        SampleMinimalView sampleMinimal = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                        .query("select e from playground$SampleEntity e where e.name = :name")
                        .parameter("name", "Data1")
                        .view(conf.getViewByInterface(SampleMinimalView.class))
                        .list()
                        .get(0)
                , SampleMinimalView.class);

        assertEquals(data1.getName(), sampleMinimal.getName());
        assertEquals(data1.getName().toLowerCase(), sampleMinimal.getNameLowercase());
    }


    @Test
    public void testDefaultMethodEmbedded() {
        SampleWithParentView sampleMinimal = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                        .query("select e from playground$SampleEntity e where e.name = :name")
                        .parameter("name", "Data2")
                        .view(conf.getViewByInterface(SampleWithParentView.class))
                        .list()
                        .get(0)
                , SampleWithParentView.class);

        assertEquals(data2.getName(), sampleMinimal.getName());
        assertEquals(data2.getParent().getName().toLowerCase(), sampleMinimal.getParent().getNameLowercase());
    }


    @Test
    public void testTransformToParentView() {
        //SampleMinimalWithUserView is a child of SampleMinimalView
        SampleMinimalWithUserView sampleMinimalWithUser = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                        .query("select e from playground$SampleEntity e where e.name = :name")
                        .parameter("name", "Data2")
                        .view(conf.getViewByInterface(SampleMinimalWithUserView.class))
                        .list()
                        .get(0)
                , SampleMinimalWithUserView.class);

        SampleMinimalView sampleMinimal = sampleMinimalWithUser.transform(SampleMinimalView.class);

        assertEquals(sampleMinimalWithUser.getName(), sampleMinimal.getName());
        assertEquals(sampleMinimalWithUser.getNameLowercase(), sampleMinimal.getNameLowercase());
        //In case of transforming to parent view we should not reload entity object
        assertSame(sampleMinimalWithUser.getOrigin(), sampleMinimal.getOrigin());
    }

    @Test
    public void testTransformToViewWithSameAttributes() {
        //SampleMinimalWithUserView contains subset of SampleWithUserView attributes
        SampleWithUserView sampleWithUser = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                        .query("select e from playground$SampleEntity e where e.name = :name")
                        .parameter("name", "Data2")
                        .view(conf.getViewByInterface(SampleWithUserView.class))
                        .list()
                        .get(0)
                , SampleWithUserView.class);

        SampleMinimalWithUserView sampleMinimal = sampleWithUser.transform(SampleMinimalWithUserView.class);

        //In case of transforming to view with the subset of attributes we should not reload entity object
        assertSame(sampleWithUser.getOrigin(), sampleMinimal.getOrigin());

        assertEquals(sampleWithUser.getName(), sampleMinimal.getName());
        assertEquals(sampleWithUser.getUser().getName(), sampleMinimal.getUser().getName());
    }

    @Test
    public void testAddingSettersByTransform() {
        //SampleWithUserView has only getters, but SampleMinimalWithUserView has setters too
        SampleWithUserView sampleWithUser = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                        .query("select e from playground$SampleEntity e where e.name = :name")
                        .parameter("name", "Data2")
                        .view(conf.getViewByInterface(SampleWithUserView.class))
                        .list()
                        .get(0)
                , SampleWithUserView.class);

        SampleMinimalWithUserView sampleMinimal = sampleWithUser.transform(SampleMinimalWithUserView.class);

        //In case of transforming to view with the subset of attributes we should not reload entity object
        assertSame(sampleWithUser.getOrigin(), sampleMinimal.getOrigin());

        String entityName = "New name" + System.currentTimeMillis();
        sampleMinimal.setName(entityName);
        assertEquals(sampleWithUser.getName(), sampleMinimal.getName());

        String userName = "New user name "+ System.currentTimeMillis();
        sampleMinimal.getUser().setName(userName);
        assertEquals(sampleWithUser.getUser().getName(), sampleMinimal.getUser().getName());
    }

    @Test
    public void testTransformWithEntityReload() {

        SampleMinimalView sampleMinimalView = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                        .query("select e from playground$SampleEntity e where e.name = :name")
                        .parameter("name", "Data2")
                        .view(conf.getViewByInterface(SampleMinimalView.class))
                        .list()
                        .get(0)
                , SampleMinimalView.class);

        SampleWithParentView sampleWithParent = sampleMinimalView.transform(SampleWithParentView.class);

        assertNotSame(sampleMinimalView.getOrigin(), sampleWithParent.getOrigin());

        assertEquals(sampleWithParent.getId(), sampleMinimalView.getOrigin().getId());

        assertEquals(sampleMinimalView.getName(), sampleWithParent.getName());

        assertNotNull(sampleWithParent.getParent());

        assertEquals(sampleWithParent.getParent().getName(), data1.getName());

        assertEquals(sampleWithParent.getParent().getNameLowercase(), data1.getName().toLowerCase());

    }


}