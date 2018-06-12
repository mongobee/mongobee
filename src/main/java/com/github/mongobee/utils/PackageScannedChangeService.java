package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.exception.MongobeeException;
import org.reflections.Reflections;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Class to deal with changelogs that are scanned from a package.
 * 
 * @author christopher.ogrady
 *
 */
public class PackageScannedChangeService extends ChangeService {
  private final String changeLogsBasePackage;
    
  public PackageScannedChangeService(String changeLogsBasePackage, Environment environment) {
    super(environment);
    this.changeLogsBasePackage = changeLogsBasePackage;
  }

  public PackageScannedChangeService(String changeLogsBasePackage) {
    this(changeLogsBasePackage, null);
  }
  
  public List<Object> fetchChangeLogs() throws MongobeeException{
    Reflections reflections = new Reflections(changeLogsBasePackage);
    Set<Class<?>> changeLogs = reflections.getTypesAnnotatedWith(ChangeLog.class); // TODO remove dependency, do own method
    List<Class<?>> filteredChangeLogs = (List<Class<?>>) filterByActiveProfiles(changeLogs);

    Collections.sort(filteredChangeLogs, new ChangeLogComparator());
    List<Object> changeLogInstances = new ArrayList<>();
    try {
      for(Class<?> changelogClass : filteredChangeLogs) {
        changeLogInstances.add(changelogClass.getConstructor().newInstance());
      }
    } catch (InstantiationException e) {
        throw new MongobeeException(e.getMessage(), e);
    } catch (IllegalAccessException e) {
        throw new MongobeeException(e.getMessage(), e);
    } catch (IllegalArgumentException e) {
        throw new MongobeeException(e.getMessage(), e);
    } catch (InvocationTargetException e) {
        throw new MongobeeException(e.getMessage(), e);
    } catch (NoSuchMethodException e) {
        throw new MongobeeException(e.getMessage(), e);
    } catch (SecurityException e) {
        throw new MongobeeException(e.getMessage(), e);
    }
    return changeLogInstances;
  }
}
