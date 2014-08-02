package com.github.mongobee.changeset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class containing particular changesets (@{@link com.github.mongobee.changeset.Changeset})
 * @author lstolowski
 * @since 27/07/2014
 * @see com.github.mongobee.changeset.Changeset
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Changelog {
  /**
   * Sequence that provide an order for changelog classes.
   * If not set, then canonical name of the class is taken and sorted alphabetically, ascending.
   */
  String order() default "";
}
