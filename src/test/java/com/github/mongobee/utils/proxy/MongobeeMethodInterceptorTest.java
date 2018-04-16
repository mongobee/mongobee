package com.github.mongobee.utils.proxy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dieppa
 * @since 04/04/2018
 */
public class MongobeeMethodInterceptorTest {

  private PreInterceptor lockCheckerInterceptorMock;

  @Before
  public void setUp() {
    lockCheckerInterceptorMock = mock(PreInterceptor.class);
  }

  @Test
  public void shouldCallChecker() throws Throwable {
    final DummyClass dummyInstance = new DummyClass("value1");
    MongobeeMethodInterceptor interceptor = new MongobeeMethodInterceptor(
        dummyInstance,
        null,
        lockCheckerInterceptorMock,
        null,
        null
    );
    interceptor.intercept(
        dummyInstance,
        DummyClass.class.getMethod("getValue"),
        new Object[0],
        null

    );
    verify(lockCheckerInterceptorMock, new Times(1)).before();
  }

  @Test
  public void shouldNotCallCheckerWhenUncheckedMethod() throws Throwable {
    final DummyClass dummyInstance = new DummyClass("value1");
    MongobeeMethodInterceptor interceptor = new MongobeeMethodInterceptor(
        dummyInstance,
        null,
        lockCheckerInterceptorMock,
        null,
        Collections.singleton("getValue")
    );
    interceptor.intercept(
        dummyInstance,
        DummyClass.class.getMethod("getValue"),
        new Object[0],
        null

    );
    verify(lockCheckerInterceptorMock, new Times(0)).before();
  }

  @Test
  public void shouldNotCallCheckerAndReturnAProxyWhenUncheckedMethodAndProxyCreator() throws Throwable {
    final DummyClass dummyInstance = new DummyClass("value1");

    ProxyFactory proxyFactory = mock(ProxyFactory.class);
    when(proxyFactory.createProxyFromOriginal(dummyInstance.getValue())).thenReturn("ProxiedObject");
    MongobeeMethodInterceptor interceptor = new MongobeeMethodInterceptor(
        dummyInstance,
        proxyFactory,
        lockCheckerInterceptorMock,
        Collections.singleton("getValue"),
        Collections.singleton("getValue")
    );
    Object result = interceptor.intercept(
        dummyInstance,
        DummyClass.class.getMethod("getValue"),
        new Object[0],
        null

    );
    verify(lockCheckerInterceptorMock, new Times(0)).before();
    verify(proxyFactory, new Times(1)).createProxyFromOriginal(dummyInstance.getValue());
    assertEquals("ProxiedObject", result);
  }

}
