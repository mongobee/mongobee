package com.github.mongobee;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.github.mongobee.test.changelogs.NotAChangeLog;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

public class PackageScanningChangeLogsSupplierTest {

  private final ChangeLogsSupplier changeLogsSupplier =
      new PackageScanningChangeLogsSupplier(MongobeeTestResource.class.getPackage().getName());

  @Test
  public void testOnlyChangeLogObjectsIncluded() throws Exception {

    Assert.assertFalse(NotAChangeLog.class.isAnnotationPresent(ChangeLog.class));
    Assert.assertEquals(NotAChangeLog.class.getPackage().getName(), MongobeeTestResource.class.getPackage().getName());

    Collection<?> objs = changeLogsSupplier.get();
    Assert.assertFalse(objs.isEmpty());
    for (Object o : objs) {
      Assert.assertTrue(o.getClass().isAnnotationPresent(ChangeLog.class));
    }
  }
}
