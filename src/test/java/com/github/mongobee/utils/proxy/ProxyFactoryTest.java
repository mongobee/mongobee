package com.github.mongobee.utils.proxy;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author dieppa
 * @since 04/04/2018
 */
public class ProxyFactoryTest {

  private PreInterceptor lockCheckerInterceptorMock;

  @Before
  public void setUp() {
    lockCheckerInterceptorMock = mock(PreInterceptor.class);
  }

  @Test
  public void shouldOnlyCallConstructorOnce() {

    final DummyClass original = new DummyClassConstructorVerifier("value1");
    ProxyFactory proxyFactory = new ProxyFactory(
        lockCheckerInterceptorMock,
        Collections.<String>emptySet(),
        Collections.<String>emptySet());
    proxyFactory.createProxyFromOriginal(original);

    assertEquals(1, constructorCalls);
  }

  @Test
  public void shouldReturnProxy() {

    final DummyClass original = new DummyClass("value1");
    ProxyFactory proxyFactory = new ProxyFactory(
        lockCheckerInterceptorMock,
        Collections.<String>emptySet(),
        Collections.<String>emptySet());

    DummyClass proxiedObject = proxyFactory.createProxyFromOriginal(original);
    final String proxyClass = proxiedObject.getClass().getSimpleName();
    assertTrue(proxyClass.startsWith("DummyClass$$EnhancerByMongobee"));
  }

  public static int constructorCalls = 0;

  public class DummyClassConstructorVerifier extends DummyClass {

    DummyClassConstructorVerifier(String value) {
      super(value);
      constructorCalls++;
    }

  }
}
