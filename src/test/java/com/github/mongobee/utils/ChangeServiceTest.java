package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.test.changelogs.AnotherMongobeeTestResource;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import junit.framework.Assert;
import org.junit.Test;

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
  public void shouldFindChangelogClasses(){
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    // when
    List<Class<?>> foundClasses = service.fetchChangelogs();
    // then
    assertTrue(foundClasses != null && foundClasses.size() > 0);
  }
  
  @Test
  public void shouldFindChangesetMethods(){
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangesets(MongobeeTestResource.class);
    
    // then
    assertTrue(foundMethods != null && foundMethods.size() == 4);
  }

  @Test
  public void shouldFindIsRunAlwaysMethod(){
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangesets(AnotherMongobeeTestResource.class);
    // then
    for (Method foundMethod : foundMethods) {
      if (foundMethod.getName().equals("testChangesetAlways")){
        assertTrue(service.isRunAlwaysChangeset(foundMethod));
      } else {
        assertFalse(service.isRunAlwaysChangeset(foundMethod));
      }
    }
    assertTrue(foundMethods != null && foundMethods.size() == 5);
  }

  @Test
  public void shouldCreateEntry(){
    
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    List<Method> foundMethods = service.fetchChangesets(MongobeeTestResource.class);

    for (Method foundMethod : foundMethods) {
    
      // when
      ChangeEntry entry = service.createChangeEntry(foundMethod);
      
      // then
      Assert.assertEquals("testuser", entry.getAuthor());
      Assert.assertEquals(MongobeeTestResource.class.getName(), entry.getChangelogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangesetMethodName());
    }
  }

}
