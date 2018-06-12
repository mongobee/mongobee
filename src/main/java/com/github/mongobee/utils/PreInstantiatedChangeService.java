package com.github.mongobee.utils;

import com.github.mongobee.exception.MongobeeException;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Class meant to deal with ChangeLog classes that have already been initiated.
 * 
 * @author christopher.ogrady
 *
 */
public class PreInstantiatedChangeService extends ChangeService {
    
  private List<Object> preInstantiatedChangeLogs;
    
  public PreInstantiatedChangeService(List<Object> preInstantiatedChangeLogs, Environment environment) {
    super(environment);
    this.preInstantiatedChangeLogs = preInstantiatedChangeLogs;
  }

  @Override
  public List<Object> fetchChangeLogs() throws MongobeeException {
    return this.preInstantiatedChangeLogs;
  }
}
