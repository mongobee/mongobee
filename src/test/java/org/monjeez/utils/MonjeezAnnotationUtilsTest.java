package org.monjeez.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.monjeez.changeset.ChangeEntry;
import org.monjeez.test.changelogs.MonjeezUtilsTestResource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.monjeez.utils.MonjeezAnnotationUtils.fetchChangelogsAt;
import static org.monjeez.utils.MonjeezAnnotationUtils.fetchChangesetsAt;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class MonjeezAnnotationUtilsTest {
  
  @Test
  public void shouldFindChangelogClasses(){
    // given
    String scanPackage = org.monjeez.test.changelogs.Package.class.getPackage().getName();
    // when
    Set<Class<?>> foundClasses = fetchChangelogsAt(scanPackage);
    // then
    Assert.assertTrue(foundClasses != null && foundClasses.size() > 0);
    
  }
  
  @Test
  public void shouldFindChangesetMethods(){

    // when
    List<Method> foundMethods = fetchChangesetsAt(MonjeezUtilsTestResource.class);
    
    // then
    Assert.assertTrue(foundMethods != null && foundMethods.size() == 2);
    
    
  }
  
  @Test
  public void shouldCreateEntry(){
    
    // given
    List<Method> foundMethods = fetchChangesetsAt(MonjeezUtilsTestResource.class);

    for (Method foundMethod : foundMethods) {
    
      // when
      ChangeEntry entry = MonjeezAnnotationUtils.createChangeEntryFor(foundMethod);
      
      // then
      Assert.assertEquals("lstolowski", entry.getAuthor());
      Assert.assertEquals(MonjeezUtilsTestResource.class.getName(), entry.getChangelogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangesetMethodName());
    }
  }
  
  
}
