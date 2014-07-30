package org.mongobee.changeset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set of changes to be added to the DB. Many changesets are included in a changelog
 * @author lstolowski
 * @since 27/07/2014
 * @see org.mongobee.changeset.Changelog
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Changeset {
  /**
   * Author of the changeset. Obligatory
   * @return
   */
  public String author();  // must be set

  /**
   * Unique ID of the changeset. Obligatory
   * @return
   */
  public String id();      // must be set

  /**
   * Executes the change set on every mongobee's execution, even if it has been run before
   * @return
   */
  public boolean runAlways() default false;

  /**
   * Executes the change the first time it is seen and each time the change set has been changed 
   * @return
   */
  public boolean runOnChange() default false;
}
