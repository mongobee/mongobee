package com.github.mongobee.utils;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

public class SpringProfileChangeLogLoader extends ChangeLogLoader {

  private static final String DEFAULT_PROFILE = "default";
  private final List<String> activeProfiles;

  public SpringProfileChangeLogLoader(String changeLogsBasePackage, Environment environment) {
    super(changeLogsBasePackage);
    if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length > 0) {
      this.activeProfiles = asList(environment.getActiveProfiles());
    } else {
      this.activeProfiles = asList(DEFAULT_PROFILE);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  /**
   * Filters change log classes by the active profile
   */
  public List<Class<?>> filterChangeLogClasses(Set<Class<?>> changeLogClassSet) {
    return (List<Class<?>>) filterByActiveProfiles(changeLogClassSet);
  }

  @SuppressWarnings("unchecked")
  @Override
  /**
   * Filters change set methods by the active profile
   */
  public List<Method> filterChangeSetMethods(List<Method> methods) {
    return (List<Method>) filterByActiveProfiles(methods);
  }

  private List<?> filterByActiveProfiles(Collection<? extends AnnotatedElement> annotated) {
    List<AnnotatedElement> filtered = new ArrayList<>();
    for (AnnotatedElement element : annotated) {
      if (matchesActiveSpringProfile(element)) {
        filtered.add(element);
      }
    }
    return filtered;
  }

  private boolean matchesActiveSpringProfile(AnnotatedElement element) {
    if (!ClassUtils.isPresent("org.springframework.context.annotation.Profile", null)) {
      return true;
    }
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
