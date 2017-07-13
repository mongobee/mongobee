package com.github.mongobee;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.test.SpringConfig;
import com.github.mongobee.test.SpringConfigWithProxying;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.github.mongobee.test.changelogs.NotAChangeLog;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collection;

public class SpringContextChangeLogsSupplierTest {

  @Test
  public void testOnlyChangeLogObjectsIncluded() throws Exception {

    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
    ChangeLogsSupplier changeLogsSupplier = context.getBean(ChangeLogsSupplier.class);

    Assert.assertFalse(NotAChangeLog.class.isAnnotationPresent(ChangeLog.class));
    Assert.assertEquals(NotAChangeLog.class.getPackage().getName(), MongobeeTestResource.class.getPackage().getName());

    Collection<?> objs = changeLogsSupplier.get();
    Assert.assertFalse(objs.isEmpty());
    for (Object o : objs) {
      Assert.assertTrue(o.getClass().isAnnotationPresent(ChangeLog.class));
    }
  }

  @Test
  public void testProxiedChangeLogs() throws Exception {

    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfigWithProxying.class);
    ChangeLogsSupplier changeLogsSupplier = context.getBean(ChangeLogsSupplier.class);

    Assert.assertFalse(NotAChangeLog.class.isAnnotationPresent(ChangeLog.class));
    Assert.assertEquals(NotAChangeLog.class.getPackage().getName(), MongobeeTestResource.class.getPackage().getName());

    Collection<?> objs = changeLogsSupplier.get();
    Assert.assertFalse(objs.isEmpty());
    for (Object o : objs) {
      Assert.assertTrue(o instanceof Advised);
      Assert.assertTrue(o.getClass().isAnnotationPresent(ChangeLog.class));
    }
  }
}
