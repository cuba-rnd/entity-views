package com.company.playground.test.views.wrapper;

import com.company.playground.entity.SampleEntity;
import com.company.playground.test.AppTestContainer;
import com.company.playground.views.sample.SampleMinimalView;
import com.company.playground.views.sample.SampleMinimalWithUserView;
import com.company.playground.views.sample.SampleWithUserView;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewSupportDataManager;
import com.haulmont.cuba.core.views.factory.EntityViewWrapper;
import com.haulmont.cuba.core.views.scan.ViewsConfiguration;
import com.haulmont.cuba.security.entity.User;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class WrapperTest {

    @ClassRule
    public static final AppTestContainer cont = AppTestContainer.Common.INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(WrapperTest.class);

    private Metadata metadata;
    private Persistence persistence;
    private ViewSupportDataManager dataManager;
    private SampleEntity data1, data2;
    private User user;
    private ViewsConfiguration viewsConfig;

    @Before
    public void setUp() throws Exception {

        log.info("Java Version: {}", System.getProperty("java.version", "Cannot read Java version from system properties"));

        metadata = cont.metadata();
        persistence = cont.persistence();
        dataManager = AppBeans.get(ViewSupportDataManager.class);
        viewsConfig = AppBeans.get(ViewsConfiguration.class);

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
    public void testWrapUser() {
        User user = dataManager.load(User.class).list().get(0);
        SampleMinimalWithUserView.UserMinimalView userMinimal = EntityViewWrapper.wrap(user, SampleMinimalWithUserView.UserMinimalView.class);
        assertEquals(user.getName(), userMinimal.getName());
    }

    @Test
    public void testWrapSimpleEntity() {
        SampleEntity sampleEntity = dataManager.load(SampleEntity.class).list().get(0);
        SampleWithUserView userMinimal = EntityViewWrapper.wrap(sampleEntity, SampleWithUserView.class);
        assertEquals(sampleEntity.getName(), userMinimal.getName());
    }

    @Test
    public void testWrapWithSubstitution() {
        SampleEntity sampleEntity = dataManager.load(SampleEntity.class).list().get(0);
        SampleMinimalView userMinimal = EntityViewWrapper.wrap(sampleEntity, SampleMinimalView.class);
        assertEquals(sampleEntity.getName(), userMinimal.getName());
        assertEquals(SampleMinimalWithUserView.class, userMinimal.getInterfaceClass());
    }

    @Test
    public void testGetInterfaceWithSubstitution(){
        View view = viewsConfig.getViewByInterface(SampleMinimalView.class);
        View substitute = viewsConfig.getViewByInterface(SampleMinimalWithUserView.class);
         assertEquals(view.getName(), substitute.getName());
    }


}
