package com.github.mongobee.utils;

import com.github.mongobee.ChangeLogsSupplier;
import com.github.mongobee.PackageScanningChangeLogsSupplier;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.test.SpringConfigWithProxying;
import com.github.mongobee.test.changelogs.AnotherMongobeeTestResource;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeServiceTest {

  @Test
  public void shouldFindChangeLogObjs() throws Exception {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(new PackageScanningChangeLogsSupplier(scanPackage));
    // when
    List<Object> found = service.fetchChangeLogs();
    // then
    assertTrue(found != null && found.size() > 0);
  }
  
  @Test
  public void shouldFindChangeSetMethods() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(new PackageScanningChangeLogsSupplier(scanPackage));

    // when
    List<Method> foundMethods = service.fetchChangeSets(MongobeeTestResource.class);
    
    // then
    assertTrue(foundMethods != null && foundMethods.size() == 5);
  }

  @Test
  public void shouldFindChangeSetsOnProxiedClass() throws MongobeeChangeSetException {

    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfigWithProxying.class);
    ChangeLogsSupplier changeLogsSupplier = context.getBean(ChangeLogsSupplier.class);
    ChangeService changeService = new ChangeService(changeLogsSupplier);

    List<Method> foundMethods = changeService.fetchChangeSets(MongobeeTestResource.class);
    assertTrue(foundMethods.size() == 5);
  }

  @Test
  public void shouldFindIsRunAlwaysMethod() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(new PackageScanningChangeLogsSupplier(scanPackage));

    // when
    List<Method> foundMethods = service.fetchChangeSets(AnotherMongobeeTestResource.class);
    // then
    for (Method foundMethod : foundMethods) {
      if (foundMethod.getName().equals("testChangeSetWithAlways")){
        assertTrue(service.isRunAlwaysChangeSet(foundMethod));
      } else {
        assertFalse(service.isRunAlwaysChangeSet(foundMethod));
      }
    }
    assertTrue(foundMethods != null && foundMethods.size() == 6);
  }

  @Test
  public void shouldCreateEntry() throws MongobeeChangeSetException {
    
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(new PackageScanningChangeLogsSupplier(scanPackage));
    List<Method> foundMethods = service.fetchChangeSets(MongobeeTestResource.class);

    for (Method foundMethod : foundMethods) {
    
      // when
      ChangeEntry entry = service.createChangeEntry(foundMethod);
      
      // then
      Assert.assertEquals("testuser", entry.getAuthor());
      Assert.assertEquals(MongobeeTestResource.class.getName(), entry.getChangeLogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangeSetMethodName());
    }
  }

  @Test(expected = MongobeeChangeSetException.class)
  public void shouldFailOnDuplicatedChangeSets() throws MongobeeChangeSetException {
    String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
    ChangeService service = new ChangeService(new PackageScanningChangeLogsSupplier(scanPackage));
    service.fetchChangeSets(ChangeLogWithDuplicate.class);
  }

}
