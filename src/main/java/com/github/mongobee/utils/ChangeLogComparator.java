package com.github.mongobee.utils;

import static org.springframework.util.StringUtils.hasText;

import com.github.mongobee.changeset.ChangeLog;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Sort ChangeLogs by 'order' value or class name (if no 'order' is set)
 *
 * @author lstolowski
 * @since 2014-09-17
 */
public class ChangeLogComparator implements Comparator<Class<?>>, Serializable {
  @Override
  public int compare(Class<?> o1, Class<?> o2) {
    ChangeLog c1 = o1.getAnnotation(ChangeLog.class);
    ChangeLog c2 = o2.getAnnotation(ChangeLog.class);

    String val1 = !(hasText(c1.order())) ? o1.getCanonicalName() : c1.order();
    String val2 = !(hasText(c2.order())) ? o2.getCanonicalName() : c2.order();

    if (val1 == null && val2 == null) {
      return 0;
    } else if (val1 == null) {
      return -1;
    } else if (val2 == null) {
      return 1;
    }

    return val1.compareTo(val2);
  }
}
