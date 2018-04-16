package com.github.mongobee.lock;

import com.github.fakemongo.Fongo;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.github.mongobee.lock.LockStatus.LOCK_HELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author dieppa
 * @since 04/04/2018
 */
public class LockRepositoryITest {

  private static final String TEST_SERVER = "testServer";
  private static final String DB_NAME = "mongobeetest";
  private static final String LOCK_COLLECTION_NAME = "mongobeelock";
  private static final String LOCK_KEY = "LOCK_KEY";

  private MongoDatabase db;
  private LockRepository dao;

  @Before
  public void setUp() {
    db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    dao = new LockRepository(LOCK_COLLECTION_NAME);
    dao.intitialize(db);
  }

  @Test
  public void ensureKeyUniqueness() {
    //inserting lock with key1: fine
    db.getCollection(LOCK_COLLECTION_NAME)
        .insertOne(new LockEntry("KEY1", "STATUS1", "process1", new Date(System.currentTimeMillis() - 60000)).buildFullDBObject());
    //inserting lock with key2: fine
    db.getCollection(LOCK_COLLECTION_NAME)
        .insertOne(new LockEntry("KEY2", "STATUS1", "process1", new Date(System.currentTimeMillis() - 60000)).buildFullDBObject());

    try {
      //inserting lock with key1 again: Exception
      db.getCollection(LOCK_COLLECTION_NAME)
          .insertOne(new LockEntry("KEY1", "STATUS2", "process2", new Date(System.currentTimeMillis() - 60000)).buildFullDBObject());

    } catch (MongoWriteException ex) {
      assertEquals(ErrorCategory.DUPLICATE_KEY, ex.getError().getCategory());
    }
  }

  @Test
  public void findByKeyShouldReturnLockWhenThereIsOne() throws LockPersistenceException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() - 60000)).buildFullDBObject()),
        new UpdateOptions().upsert(true));

    //when
    final LockEntry result = dao.findByKey(LOCK_KEY);

    //then
    assertNotNull(result);
  }

  @Test
  public void insertUpdateShouldInsertWhenEmpty() throws LockPersistenceException {

    // when
    Date expiresAtExpected = new Date(System.currentTimeMillis() - 60000);
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));
  }

  @Test
  public void insertUpdateShouldUpdateWhenExpiresAtIsGraterThanSaved() throws LockPersistenceException {
    //given
    final long currentMillis = System.currentTimeMillis();
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis - 1000)));

    //when
    Date expiresAtExpected = new Date();
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));

  }

  @Test
  public void insertUpdateShouldUpdateWhenSameOwner() throws LockPersistenceException {
    //given
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() + 60 * 60 * 1000)));

    //when
    Date expiresAtExpected = new Date();
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));
  }

  @Test(expected = LockPersistenceException.class)
  public void insertUpdateShouldThrowExceptionWhenLockIsInDBWIthDifferentOwnerAndNotExpired() throws LockPersistenceException {
    //given
    final long currentMillis = System.currentTimeMillis();
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis + 60 * 60 * 1000)));

    //when
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", new Date(currentMillis + 90 * 60 * 1000)));
  }

  @Test
  public void removeShouldRemoveWhenSameOwner() throws LockPersistenceException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() - 600000)).buildFullDBObject()),
        new UpdateOptions().upsert(true));
    assertNotNull("Precondition: Lock should be in db",
        db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY)).first());

    //when
    dao.removeByKeyAndOwner(LOCK_KEY, "process1");

    //then
    assertNull(db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY)).first());
  }

  @Test
  public void removeShouldNotRemoveWhenDifferentOwner() throws LockPersistenceException {
    //given
    db.getCollection(LOCK_COLLECTION_NAME).updateMany(
        new Document(),
        new Document().append("$set", new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() - 600000)).buildFullDBObject()),
        new UpdateOptions().upsert(true));
    assertNotNull("Precondition: Lock should be in db",
        db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY)).first());

    //when
    dao.removeByKeyAndOwner(LOCK_KEY, "process2");

    //then
    assertNotNull(db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY)).first());
  }

  @Test(expected = LockPersistenceException.class)
  public void updateIfSameOwnerShouldNotInsertWhenEmpty() throws LockPersistenceException {
    //when
    dao.updateIfSameOwner(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() - 600000)));
  }

  @Test(expected = LockPersistenceException.class)
  public void updateIfSameOwnerShouldNotUpdateWhenExpiresAtIsGraterThanSavedButOtherOwner() throws LockPersistenceException {
    //given
    final long currentMillis = System.currentTimeMillis();
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis - 1000)));

    //when
    dao.updateIfSameOwner(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", new Date(currentMillis)));

  }

  @Test
  public void updateIfSameOwnerShouldUpdateWhenSameOwner() throws LockPersistenceException {
    //given
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() + 60 * 60 * 1000)));

    //when
    Date expiresAtExpected = new Date(System.currentTimeMillis());
    dao.updateIfSameOwner(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));
  }

  @Test(expected = LockPersistenceException.class)
  public void updateIfSameOwnerShouldNotUpdateWhenDifferentOwnerAndExpiresAtIsNotGrater() throws LockPersistenceException {
    // given
    final long currentMillis = System.currentTimeMillis();
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis + 60 * 60 * 1000)));

    // when
    dao.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", new Date(currentMillis)));
  }
}
