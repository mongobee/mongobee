package org.monjeez.utils;

import org.monjeez.changeset.ChangeEntry;
import org.monjeez.changeset.Changelog;
import org.monjeez.changeset.Changeset;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class MonjeezAnnotationUtils {
  
  public static Set<Class<?>> fetchChangelogsAt(String changelogsBasePackage){
    Reflections reflections = new Reflections(changelogsBasePackage);
    Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Changelog.class);
    return annotated;
  }

  public static List<Method> fetchChangesetsAt(final Class<?> type) {
    final List<Method> methods = new ArrayList<>();
      final List<Method> allMethods = new ArrayList<>(Arrays.asList(type.getDeclaredMethods()));
      for (final Method method : allMethods) {
        if (method.isAnnotationPresent(Changeset.class)) {
          methods.add(method);
        }
      }
    return methods;
  }
  
  
  public static ChangeEntry createChangeEntryFor(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(Changeset.class)){
      Changeset annotation = changesetMethod.getAnnotation(Changeset.class);
  
      return new ChangeEntry(
                      annotation.id(), 
                      annotation.author(), 
                      new Date(), 
                      changesetMethod.getDeclaringClass().getName(), 
                      changesetMethod.getName()
                  );
      
    } else {
      return null;
    }
  }
  
}
