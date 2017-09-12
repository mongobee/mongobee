package org.github.mongobee.spring;

import com.github.mongobee.core.exception.MongobeeChangeSetException;
import com.github.mongobee.core.utils.ChangeLogComparator;
import com.github.mongobee.core.utils.ChangeSetComparator;
import com.github.mongobee.core.utils.ChangeService;
import com.github.mongobee.core.utils.ClassUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author j-coll
 */
public class SpringChangeService extends ChangeService {
  private static final String DEFAULT_PROFILE = "default";

  private final List<String> activeProfiles;

  public SpringChangeService(String changeLogsBasePackage) {
    this(changeLogsBasePackage, null);
  }

  @Override
  public List<Class<?>> fetchChangeLogs() {
    List<Class<?>> changeLogs = super.fetchChangeLogs();
    List<Class<?>> filteredChangeLogs = filterByActiveProfiles(changeLogs);
    Collections.sort(filteredChangeLogs, new ChangeLogComparator());

    return filteredChangeLogs;
  }

  @Override
  public List<Method> fetchChangeSets(Class<?> type) throws MongobeeChangeSetException {
    final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));

    final List<Method> filteredChangeSets = (List<Method>) filterByActiveProfiles(changeSets);
    Collections.sort(filteredChangeSets, new ChangeSetComparator());

    return filteredChangeSets;
  }

  public SpringChangeService(String changeLogsBasePackage, Environment environment) {
    super(changeLogsBasePackage);

    if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length> 0) {
      this.activeProfiles = asList(environment.getActiveProfiles());
    } else {
      this.activeProfiles = asList(DEFAULT_PROFILE);
    }
  }

  private <T extends AnnotatedElement> List<T> filterByActiveProfiles(Collection<T> annotated) {
    List<T> filtered = new ArrayList<>();
    for (T element : annotated) {
      if (matchesActiveSpringProfile(element)){
        filtered.add(element);
      }
    }
    return filtered;
  }

  private boolean matchesActiveSpringProfile(AnnotatedElement element) {
    if (!element.isAnnotationPresent(Profile.class)) {
      return true; // no-profiled changeset always matches
    }
    List<String> profiles = asList(element.getAnnotation(Profile.class).value());
    for (String profile : profiles) {
      if (profile != null && profile.length() > 0 && profile.charAt(0) == '!') {
        if (!activeProfiles.contains(profile.substring(1))) {
          return true;
        }
      } else if (activeProfiles.contains(profile)) {
        return true;
      }
    }
    return false;
  }
}
