package com.github.mongobee.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author colsson11
 * @since 13.01.15
 */
public class LockDaoTest extends AbstractDaoTest {

  private static final String LOCK_COLLECTION_NAME = "mongobeelock";

  @Test
  public void shouldGetLockWhenNotPreviouslyHeld() throws Exception {

    // given
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(mongoDatabase);

    // when
    boolean hasLock = dao.acquireLock(mongoDatabase);

    // then
    assertTrue(hasLock);
  }

  @Test
  public void shouldNotGetLockWhenPreviouslyHeld() throws Exception {

    // given
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(mongoDatabase);

    // when
    dao.acquireLock(mongoDatabase);
    boolean hasLock = dao.acquireLock(mongoDatabase);
    // then
    assertFalse(hasLock);
  }

  @Test
  public void shouldGetLockWhenPreviouslyHeldAndReleased() throws Exception {

    // given
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(mongoDatabase);

    // when
    dao.acquireLock(mongoDatabase);
    dao.releaseLock(mongoDatabase);
    boolean hasLock = dao.acquireLock(mongoDatabase);
    // then
    assertTrue(hasLock);
  }

  @Test
  public void releaseLockShouldBeIdempotent() {
    // given
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);

    dao.intitializeLock(mongoDatabase);

    // when
    dao.releaseLock(mongoDatabase);
    dao.releaseLock(mongoDatabase);
    boolean hasLock = dao.acquireLock(mongoDatabase);
    // then
    assertTrue(hasLock);
  }

  @Test
  public void whenLockNotHeldCheckReturnsFalse() {
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(mongoDatabase);

    assertFalse(dao.isLockHeld(mongoDatabase));
  }

  @Test
  public void whenLockHeldCheckReturnsTrue() {

    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(mongoDatabase);

    dao.acquireLock(mongoDatabase);

    assertTrue(dao.isLockHeld(mongoDatabase));
  }
}
