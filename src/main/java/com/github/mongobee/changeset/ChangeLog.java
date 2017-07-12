package com.github.mongobee.changeset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class containing particular changesets (@{@link ChangeSet})
 * @author lstolowski
 * @since 27/07/2014
 * @see ChangeSet
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ChangeLog {
  /**
   * Sequence that provide an order for changelog classes.
   * If not set, then canonical name of the class is taken and sorted alphabetically, ascending.
   * @return order
   */
  String order() default "";
}
