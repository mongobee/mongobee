package com.github.mongobee.changeset;

import com.github.mongobee.test.changelogs.MongobeeTestResource;
import org.junit.Test;

import java.util.Set;

import static junit.framework.Assert.assertTrue;

public class ScanPackageChangeLogsProviderTest {

  @Test
  public void shouldFindChangeLogClasses() {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ScanPackageChangeLogsProvider provider = new ScanPackageChangeLogsProvider(scanPackage);
    // when
    Set<Class<?>> foundClasses = provider.load();
    // then
    assertTrue(foundClasses != null && foundClasses.size() > 0);
  }

}
