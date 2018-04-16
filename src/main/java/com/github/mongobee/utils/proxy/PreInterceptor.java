package com.github.mongobee.utils.proxy;

/**
 * <p>Generic interface to be provided to ProxyFactory to wrap the desired actions before the execution of the
 * intercepted method</p>
 *
 * @author dieppa
 * @see ProxyFactory
 * @since 04/04/2018
 */
public interface PreInterceptor {

  /**
   * To be executed before the intercepted method
   */
  void before();
}
