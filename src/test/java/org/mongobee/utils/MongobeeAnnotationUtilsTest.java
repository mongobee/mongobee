package org.mongobee.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.mongobee.changeset.ChangeEntry;
import org.mongobee.test.changelogs.MongobeeUtilsTestResource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.mongobee.utils.MongobeeAnnotationUtils.fetchChangelogsAt;
import static org.mongobee.utils.MongobeeAnnotationUtils.fetchChangesetsAt;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class MongobeeAnnotationUtilsTest {
  
  @Test
  public void shouldFindChangelogClasses(){
    // given
    String scanPackage = MongobeeUtilsTestResource.class.getPackage().getName();
    // when
    Set<Class<?>> foundClasses = fetchChangelogsAt(scanPackage);
    // then
    Assert.assertTrue(foundClasses != null && foundClasses.size() > 0);
    
  }
  
  @Test
  public void shouldFindChangesetMethods(){

    // when
    List<Method> foundMethods = fetchChangesetsAt(MongobeeUtilsTestResource.class);
    
    // then
    Assert.assertTrue(foundMethods != null && foundMethods.size() == 2);
    
  }
  
  @Test
  public void shouldCreateEntry(){
    
    // given
    List<Method> foundMethods = fetchChangesetsAt(MongobeeUtilsTestResource.class);

    for (Method foundMethod : foundMethods) {
    
      // when
      ChangeEntry entry = MongobeeAnnotationUtils.createChangeEntryFor(foundMethod);
      
      // then
      Assert.assertEquals("lstolowski", entry.getAuthor());
      Assert.assertEquals(MongobeeUtilsTestResource.class.getName(), entry.getChangelogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangesetMethodName());
    }
  }
  
}
