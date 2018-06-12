package com.github.mongobee.utils;

import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.test.changelogs.*;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertTrue;

/**
 * @author christopher.ogrady
 * @since 2018/06/12
 */
public class PreInstantiatedChangeServiceTest {

  @Test
  public void shouldFindChangeLogObjects() throws MongobeeException{
    // given
    List<Object> instantiatedChangeLogs = Lists.newArrayList(new AnotherMongobeeTestResource(), new EnvironmentDependentTestResource());
    ChangeService service = new PreInstantiatedChangeService(instantiatedChangeLogs, null);
    // when
    List<Object> foundObjects = service.fetchChangeLogs();
    // then
    
    assertTrue(instantiatedChangeLogs.equals(foundObjects));
  }

}
