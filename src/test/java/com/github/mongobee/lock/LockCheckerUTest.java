package com.github.mongobee.lock;

import com.github.mongobee.exception.MongobeeLockException;
import com.github.mongobee.utils.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

import java.util.Date;

import static com.github.mongobee.lock.LockStatus.LOCK_HELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dieppa
 * @since 04/04/2018
 */
public class LockCheckerUTest {

  private static final long lockActiveMillis = 5 * 60 * 1000;
  private static final long maxWaitMillis = 60 * 1000;
  private static final int lockMaxTries = 3;

  private LockRepository dao;
  private TimeUtils timeUtils;

  private LockChecker checker;

  @Before
  public void setUp() {
    dao = Mockito.mock(LockRepository.class);
    timeUtils = Mockito.mock(TimeUtils.class);

    checker = new LockChecker(dao, timeUtils)
        .setLockAcquiredForMillis(lockActiveMillis)
        .setLockMaxTries(lockMaxTries)
        .setLockMaxWaitMillis(maxWaitMillis);
  }

  @Test
  public void acquireLockShouldCallDaoFirstTime() throws LockPersistenceException, MongobeeLockException {
    //given
    final Date expirationAt = new Date(1000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(expirationAt);

    // when
    checker.acquireLockDefault();

    //then
    assertDaoInsertUpdateCalledWithRightParameters(expirationAt, 1);
  }

  @Test
  public void acquireLockShouldCallDaoSecondTimeWhenTimeHasAlreadyExpired() throws LockPersistenceException, MongobeeLockException {
    //given
    final Date expirationAt = new Date(1000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(expirationAt);
    when(timeUtils.currentTime()).thenReturn(new Date(40000L));// Exactly the expiration time(minus margin)
    checker.acquireLockDefault();

    //when
    checker.acquireLockDefault();

    //then
    assertDaoInsertUpdateCalledWithRightParameters(expirationAt, 2);
  }

  @Test
  public void acquireShouldCallDaoSecondTimeEvenWhenTimeHasNotExpiredYet() throws LockPersistenceException, MongobeeLockException {
    //given
    final Date expirationAt = new Date(1000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(expirationAt);
    when(timeUtils.currentTime()).thenReturn(new Date(39999L));// 1ms less than the expiration time(minus margin)
    checker.acquireLockDefault();

    // when
    checker.acquireLockDefault();

    //then
    assertDaoInsertUpdateCalledWithRightParameters(expirationAt, 2);
  }

  @Test
  public void acquireLockShouldWaitUntilExpirationTimeWhenDaoThrowsExceptionAndLockHeldByOther() throws LockPersistenceException, MongobeeLockException {
    //given
    long expiresAt = 3000L;
    long waitingTime = 0L;
    doThrow(new LockPersistenceException("Faked"))
        .doNothing()
        .when(dao).insertUpdate(any(LockEntry.class));

    when(dao.findByKey(anyString())).thenReturn(createFakeLockWithOtherOwner(expiresAt));
    final Date newExpirationAt = new Date(1000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(newExpirationAt);
    when(timeUtils.currentTime())
        .thenReturn(new Date(expiresAt - waitingTime));

    //when
    final long timeBeforeCall = System.currentTimeMillis();
    checker.acquireLockDefault();
    final long timeSpent = System.currentTimeMillis() - timeBeforeCall;

    //then
    assertTrue("Checker should wait at least " + waitingTime + "ms", timeSpent >= waitingTime);
    assertDaoInsertUpdateCalledWithRightParameters(newExpirationAt, 2);
  }

  @Test
  public void acquireLockShouldNotWaitWhenWaitForLockIsFalse() throws LockPersistenceException, MongobeeLockException {
    //given
    checker.setLockMaxTries(1);
    long expiresAt = 3000L;
    long waitingTime = 1000L;
    doThrow(new LockPersistenceException("Faked"))
        .doNothing()
        .when(dao).insertUpdate(any(LockEntry.class));

    when(dao.findByKey(anyString())).thenReturn(createFakeLockWithOtherOwner(expiresAt));
    final Date newExpirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(newExpirationAt);
    when(timeUtils.currentTime()).thenReturn(new Date(expiresAt - waitingTime));

    //when
    final long timeBeforeCall = System.currentTimeMillis();
    boolean exceptionThrown = false;
    try {
      checker.acquireLockDefault();
    } catch (MongobeeLockException ex) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);
    final long timeSpent = System.currentTimeMillis() - timeBeforeCall;

    //then
    assertTrue("Checker should not wait at all " + waitingTime + "ms", timeSpent <= maxWaitMillis);
    assertDaoInsertUpdateCalledWithRightParameters(newExpirationAt, 1);
  }

  @Test
  public void acquireLockShouldNotWaitWhenDaoThrowsExceptionButLockHeldByTheSameOwner() throws LockPersistenceException, MongobeeLockException {
    //given
    long expiresAt = 3000L;
    long waitingTime = 1000L;
    doThrow(new LockPersistenceException("Faked"))
        .doNothing()
        .when(dao).insertUpdate(any(LockEntry.class));

    when(dao.findByKey(anyString())).thenReturn(new LockEntry(
        LockChecker.getDefaultKey(),
        LockStatus.LOCK_HELD.name(),
        checker.getOwner(),
        new Date(expiresAt)
    ));
    final Date newExpirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(newExpirationAt);
    when(timeUtils.currentTime())
        .thenReturn(new Date(40000L))
        .thenReturn(new Date(expiresAt - waitingTime));

    //when
    final long timeBeforeCall = System.currentTimeMillis();
    checker.acquireLockDefault();
    final long timeSpent = System.currentTimeMillis() - timeBeforeCall;

    //then
    assertTrue("Checker should wait that long", timeSpent < waitingTime);
    assertDaoInsertUpdateCalledWithRightParameters(newExpirationAt, 2);
  }

  @Test
  public void acquireLockShouldNotWaitButThrowExceptionWhenDaoThrowsExceptionAndLockIsHeldByOtherAndWaitingTimeIsGTMaxWaitMillis()
      throws LockPersistenceException {
    //given
    long expiresAt = 3000L;
    long waitingTime = maxWaitMillis + 1;
    doThrow(new LockPersistenceException("Faked")).when(dao).insertUpdate(any(LockEntry.class));

    when(dao.findByKey(anyString())).thenReturn(createFakeLockWithOtherOwner(expiresAt));
    final Date newExpirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(newExpirationAt);
    when(timeUtils.currentTime())
        .thenReturn(new Date(expiresAt - waitingTime));

    //when
    final long timeBeforeCall = System.currentTimeMillis();
    boolean exceptionThrown = false;
    try {
      checker.acquireLockDefault();
    } catch (MongobeeLockException ex) {
      exceptionThrown = true;
    }

    //then
    assertTrue("MongobeeLockException should be thrown", exceptionThrown);
    final long timeSpent = System.currentTimeMillis() - timeBeforeCall;

    //then
    assertTrue(timeSpent < waitingTime);
    assertDaoInsertUpdateCalledWithRightParameters(newExpirationAt, 1);
  }

  @Test
  public void acquireLockShouldNotTryMoreThenMaxWhenDaoThrowsExceptionAndLockIsHeldByOther() throws LockPersistenceException {
    //given
    long expiresAt = 3000L;
    long waitingTime = 1;
    doThrow(new LockPersistenceException("Faked")).when(dao).insertUpdate(any(LockEntry.class));

    when(dao.findByKey(anyString())).thenReturn(createFakeLockWithOtherOwner(expiresAt));
    final Date newExpirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(newExpirationAt);
    when(timeUtils.currentTime())
        .thenReturn(new Date(expiresAt - waitingTime));

    // when
    boolean exceptionThrown = false;
    try {
      checker.acquireLockDefault();
    } catch (MongobeeLockException ex) {
      exceptionThrown = true;
    }

    //then
    assertTrue("MongobeeLockException should be thrown", exceptionThrown);
    assertDaoInsertUpdateCalledWithRightParameters(newExpirationAt, 3);
  }

  @Test
  public void ensureLockShouldCallDaoFirstTime() throws LockPersistenceException, MongobeeLockException {
    //given
    final Date expirationAt = new Date(1000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(expirationAt);

    // when
    checker.ensureLockDefault();

    //then
    assertDaoUpdateIfSameOwnerCalledWithRightParameters(expirationAt, 1);
  }

  @Test
  public void ensureLockShouldCallDaoSecondTimeWhenTimeHasAlreadyExpired() throws LockPersistenceException, MongobeeLockException {
    //given
    final Date expirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(expirationAt);
    when(timeUtils.currentTime()).thenReturn(new Date(40000L));// Exactly the expiration time(minus margin)
    checker.acquireLockDefault();

    // when
    checker.ensureLockDefault();

    //then
    assertDaoUpdateIfSameOwnerCalledWithRightParameters(expirationAt, 1);
  }

  @Test
  public void ensureLockShouldNotCallDaoSecondTimeWhenTimeHasNotExpiredYet() throws LockPersistenceException, MongobeeLockException {
    //given
    final Date expirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(expirationAt);
    when(timeUtils.currentTime()).thenReturn(new Date(39999L));// 1ms less than the expiration time(minus margin)
    checker.acquireLockDefault();

    // when
    checker.ensureLockDefault();

    //then
    assertDaoUpdateIfSameOwnerCalledWithRightParameters(expirationAt, 0);
  }

  @Test(expected = MongobeeLockException.class)
  public void ensureLockShouldThrowExceptionWhenDaoThrowsExceptionAndLockHeldByOther() throws LockPersistenceException, MongobeeLockException {
    //given
    long expiresAt = 3000L;
    long waitingTime = 1000L;

    doNothing().when(dao).insertUpdate(any(LockEntry.class));
    doThrow(new LockPersistenceException("Faked")).doNothing().when(dao).updateIfSameOwner(any(LockEntry.class));

    when(dao.findByKey(anyString())).thenReturn(createFakeLockWithOtherOwner(expiresAt));
    final Date newExpirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong()))
        .thenReturn(newExpirationAt)
        .thenReturn(newExpirationAt);
    when(timeUtils.currentTime())
        .thenReturn(new Date(40001L))
        .thenReturn(new Date(expiresAt - waitingTime));
    checker.acquireLockDefault();

    // when
    checker.ensureLockDefault();

  }

  @Test
  public void ensureLockShouldTryAgainWhenDaoThrowsExceptionButLockHeldByTheSameOwner() throws LockPersistenceException, MongobeeLockException {
    //given
    long expiresAt = 3000L;
    long waitingTime = 1000L;
    doNothing().when(dao).insertUpdate(any(LockEntry.class));
    doThrow(new LockPersistenceException("Faked")).doNothing()
        .when(dao).updateIfSameOwner(any(LockEntry.class));

    when(dao.findByKey(anyString())).thenReturn(new LockEntry(
        LockChecker.getDefaultKey(),
        LockStatus.LOCK_HELD.name(),
        checker.getOwner(),
        new Date(expiresAt)
    ));
    final Date newExpirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(newExpirationAt);
    when(timeUtils.currentTime())
        .thenReturn(new Date(40000L))
        .thenReturn(new Date(expiresAt - waitingTime));
    checker.acquireLockDefault();

    // when
    final long timeBeforeCall = System.currentTimeMillis();
    checker.ensureLockDefault();
    final long timeSpent = System.currentTimeMillis() - timeBeforeCall;

    //then
    assertTrue("Checker should wait that long", timeSpent < waitingTime);
    assertDaoUpdateIfSameOwnerCalledWithRightParameters(newExpirationAt, 1);
  }

  @Test
  public void ensureLockShouldNotTryMoreThanMaxWhenDaoThrowsException() throws LockPersistenceException {
    //given
    long expiresAt = 3000L;
    long waitingTime = 1;
    doThrow(new LockPersistenceException("Faked")).when(dao).updateIfSameOwner(any(LockEntry.class));
    when(dao.findByKey(anyString())).thenReturn(createFakeLockWithSameOwner(expiresAt));
    final Date newExpirationAt = new Date(100000L);
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(newExpirationAt);
    when(timeUtils.currentTime())
        .thenReturn(new Date(expiresAt - waitingTime));

    // when
    boolean exceptionThrown = false;
    try {
      checker.ensureLockDefault();
    } catch (MongobeeLockException ex) {
      exceptionThrown = true;
    }

    //then
    assertTrue("MongobeeLockException should be thrown", exceptionThrown);
    assertDaoUpdateIfSameOwnerCalledWithRightParameters(newExpirationAt, 3);
  }

  @Test
  public void releaseLockCallDaoAlways() {
    //when
    checker.releaseLockDefault();

    //then
    verify(dao).removeByKeyAndOwner(LockChecker.getDefaultKey(), checker.getOwner());
  }

  @Test
  public void shouldHitTheDBAfterReleaseWhenAcquiringLock() throws LockPersistenceException, MongobeeLockException {
    //given
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(new Date(1000000L));
    when(timeUtils.currentTime()).thenReturn(new Date(0L));

    //when
    checker.acquireLockDefault();
    checker.releaseLockDefault();
    checker.acquireLockDefault();

    //then
    verify(dao).removeByKeyAndOwner(LockChecker.getDefaultKey(), checker.getOwner());
    verify(dao, new Times(2)).insertUpdate(any(LockEntry.class));
  }

  @Test
  public void shouldHitTheDBAfterReleaseWhenEnsuringLock() throws LockPersistenceException, MongobeeLockException {
    //given
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(new Date(1000000L));
    when(timeUtils.currentTime()).thenReturn(new Date(0L));

    //when
    checker.acquireLockDefault();
    checker.releaseLockDefault();
    checker.ensureLockDefault();

    //then
    verify(dao).removeByKeyAndOwner(LockChecker.getDefaultKey(), checker.getOwner());
    verify(dao, new Times(1)).updateIfSameOwner(any(LockEntry.class));
  }

  @Test
  public void getLockMaxTriesShouldReturnRight() {
    //given
    checker.setLockMaxTries(3);

    //when
    int lockMaxTries = checker.getLockMaxTries();

    //then
    assertEquals(3, lockMaxTries);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalExceptionWhenLockMaxWaitMillisLtOne() {
    checker.setLockMaxWaitMillis(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalExceptionWhenLockMaxTriesLtOne() {
    checker.setLockMaxTries(0);
  }



  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalExceptionWhenAcquiredForLessThan2Minutes() {
    checker.setLockAcquiredForMillis(new TimeUtils().minutesToMillis(1) + 59);
  }

  @Test
  public void isLockHeldShouldReturnFalseWhenIsNotStarted() {
    //when
    boolean lockHeld = checker.isLockHeld();

    //then
    assertFalse(lockHeld);
  }

  @Test
  public void isLockHeldShouldReturnTrueWhenIsStarted() throws MongobeeLockException {
    //given
    when(timeUtils.currentTimePlusMillis(anyLong())).thenReturn(new Date(1000000L));
    when(timeUtils.currentTime()).thenReturn(new Date(0L));
    checker.acquireLockDefault();

    //when
    boolean lockHeld = checker.isLockHeld();

    //then
    assertTrue(lockHeld);
  }

  private void assertDaoInsertUpdateCalledWithRightParameters(Date expirationAt, int invocationTimes)
      throws LockPersistenceException {
    assertDao(expirationAt, invocationTimes, false);
  }

  private void assertDaoUpdateIfSameOwnerCalledWithRightParameters(Date expirationAt, int invocationTimes)
      throws LockPersistenceException {
    assertDao(expirationAt, invocationTimes, true);
  }

  private void assertDao(Date expirationAt, int invocationTimes, boolean onlyIfSameOwner) throws LockPersistenceException {
    ArgumentCaptor<LockEntry> captor = ArgumentCaptor.forClass(LockEntry.class);
    if (onlyIfSameOwner) {
      verify(dao, new Times(invocationTimes)).updateIfSameOwner(captor.capture());

    } else {
      verify(dao, new Times(invocationTimes)).insertUpdate(captor.capture());
    }
    if (invocationTimes > 0) {
      LockEntry saved = captor.getValue();
      assertEquals("Lock was saved with the wrong key", LockChecker.getDefaultKey(), saved.getKey());
      assertEquals("Lock was saved with the wrong status", LOCK_HELD.name(), saved.getStatus());
      assertEquals("lock was saved with the wrong owner", checker.getOwner(), saved.getOwner());
      assertEquals("Lock was saved with the wrong expires time", expirationAt, saved.getExpiresAt());
    }

  }

  private LockEntry createFakeLockWithOtherOwner(long expiresAt) {
    return createFakeLock(expiresAt, "otherOwner");
  }

  private LockEntry createFakeLockWithSameOwner(long expiresAt) {
    return createFakeLock(expiresAt, checker.getOwner());
  }

  private LockEntry createFakeLock(long expiresAt, String owner) {
    return new LockEntry(
        LockChecker.getDefaultKey(),
        LockStatus.LOCK_HELD.name(),
        owner,
        new Date(expiresAt)
    );
  }

}
