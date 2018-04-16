package com.github.mongobee.lock;

import com.github.fakemongo.Fongo;
import com.github.mongobee.exception.MongobeeLockException;
import com.github.mongobee.utils.TimeUtils;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.github.mongobee.lock.LockStatus.LOCK_HELD;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author dieppa
 * @since 04/04/2018
 */
public class LockCheckerITest {
  private static final String TEST_SERVER = "testServer";
  private static final String DB_NAME = "mongobeetest";
  private static final String LOCK_COLLECTION_NAME = "mongobeelock";
  private static final long lockActiveMillis = 5 * 60 * 1000;
  private static final long maxWaitMillis = 5 * 60 * 1000;
  private static final int lockMaxTries = 3;

  private MongoDatabase db;
  private LockChecker checker;

  @Before
  public void setUp() {
    db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    LockRepository dao = new LockRepository(LOCK_COLLECTION_NAME);
    TimeUtils timeUtils = new TimeUtils();
    checker = new LockChecker(dao, timeUtils)
        .setLockAcquiredForMillis(lockActiveMillis)
        .setLockMaxTries(lockMaxTries)
        .setLockMaxWaitMillis(maxWaitMillis);
    checker.initialize(db);
  }

  @Test
  public void shouldAcquireLockWhenFirstTime() throws MongobeeLockException {
    checker.acquireLockDefault();
  }

  @Test
  public void shouldAcquireLockWhenLockHeldBySameOwner() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody(checker.getOwner(), currentTimePlusHours(24))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.acquireLockDefault();
  }

  @Test
  public void shouldAcquireLockWhenLockHeldByOtherAndExpired() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody("otherOwner", currentTimePlusHours(-1))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.acquireLockDefault();
  }

  @Test
  public void shouldAcquireLockWhenLockHeldByOtherAndExpiresAtLtMaxWaitTime() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document()
            .append("$set", getLockDbBody("otherOwner", System.currentTimeMillis() + 100)),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.acquireLockDefault();
  }

  @Test(expected = MongobeeLockException.class)
  public void shouldNotAcquireLockWhenLockHeldByOtherAndExpiresAtGtMaxWaitTime() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document()
            .append("$set", getLockDbBody("otherOwner", currentTimePlusMinutes(millisToMinutes(maxWaitMillis) + 1))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.acquireLockDefault();
  }

  @Test(expected = MongobeeLockException.class)
  public void shouldNotEnsureWhenFirstTime() throws MongobeeLockException {
    //when
    checker.ensureLockDefault();
  }

  @Test
  public void shouldEnsureWhenHeldBySameOwnerAndNotExpiredInDB() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody(checker.getOwner(), currentTimePlusMinutes(1))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.ensureLockDefault();
  }

  @Test
  public void shouldEnsureWhenHeldBySameOwnerAndExpiredInDB() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody(checker.getOwner(), currentTimePlusMinutes(-10))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.ensureLockDefault();
  }

  @Test
  public void shouldEnsureWhenAcquiredPreviouslyBySameOwner() throws MongobeeLockException {
    //given
    checker.acquireLockDefault();

    //when
    checker.ensureLockDefault();
  }

  @Test(expected = MongobeeLockException.class)
  public void shouldNotEnsureWhenHeldByOtherOwnerAndExpiredInDB() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody("other", currentTimePlusMinutes(-10))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.ensureLockDefault();
  }

  @Test(expected = MongobeeLockException.class)
  public void shouldNotEnsureWhenHeldByOtherOwnerAndNotExpiredInDB() throws MongobeeLockException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody("other", currentTimePlusMinutes(10))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.ensureLockDefault();
  }

  @Test
  public void shouldReleaseLockWhenHeldBySameOwner() {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody(checker.getOwner(), currentTimePlusMinutes(10))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.releaseLockDefault();

    //then
    FindIterable<Document> resultAfter = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNull("Lock should be removed from DB", resultAfter.first());
  }

  @Test
  public void shouldNotReleaseLockWhenHeldByOtherOwner() {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody("otherOwner", currentTimePlusMinutes(10))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.releaseLockDefault();

    //then
    FindIterable<Document> resultAfter = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Lock should be removed from DB", resultAfter.first());
  }

  @Test
  public void releaseLockShouldBeIdempotentWhenHeldBySameOwner() {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody(checker.getOwner(), currentTimePlusMinutes(10))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.releaseLockDefault();
    checker.releaseLockDefault();

    //then
    FindIterable<Document> resultAfter = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNull("Lock should be removed from DB", resultAfter.first());
  }

  @Test
  public void releaseLockShouldBeIdempotentWhenHeldByOtherOwner() {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", getLockDbBody("otherOwner", currentTimePlusMinutes(10))),
        new UpdateOptions().upsert(true));
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Precondition: Lock should be in database", resultBefore.first());

    //when
    checker.releaseLockDefault();
    checker.releaseLockDefault();

    //then
    FindIterable<Document> resultAfter = db.getCollection(LOCK_COLLECTION_NAME)
        .find(new Document().append("key", LockChecker.getDefaultKey()));
    assertNotNull("Lock should be removed from DB", resultAfter.first());
  }

  @Test
  public void releaseLockShouldNotThrowAnyExceptionWhenNoLockPresent() {
    //given
    FindIterable<Document> resultBefore = db.getCollection(LOCK_COLLECTION_NAME).find();
    assertNull("Precondition: Lock should not be in database", resultBefore.first());

    //when
    checker.releaseLockDefault();
    checker.releaseLockDefault();

    //then
    FindIterable<Document> resultAfter = db.getCollection(LOCK_COLLECTION_NAME).find();
    assertNull("Lock should be removed from DB", resultAfter.first());
  }

  private Document getLockDbBody(String owner, long expiresAt) {
    return new LockEntry(
        LockChecker.getDefaultKey(),
        LOCK_HELD.name(),
        owner,
        new Date(expiresAt)
    ).buildFullDBObject();
  }

  private long currentTimePlusHours(int hours) {
    return currentTimePlusMinutes(hours * 60);
  }

  private long currentTimePlusMinutes(int minutes) {
    long millis = minutes * 60 * 1000;
    return System.currentTimeMillis() + millis;
  }

  private int millisToMinutes(long millis) {
    return (int) (millis / (1000 * 60));
  }
}
