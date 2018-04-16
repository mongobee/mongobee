package com.github.mongobee.lock;

import com.github.fakemongo.Fongo;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Date;

import static com.github.mongobee.lock.LockStatus.LOCK_HELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
  private LockRepository repository;

  @Before
  public void setUp() {
    db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    repository = new LockRepository(LOCK_COLLECTION_NAME);
    repository.intitialize(db);
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
    final LockEntry result = repository.findByKey(LOCK_KEY);

    //then
    assertNotNull(result);
  }

  @Test
  public void insertUpdateShouldInsertWhenEmpty() throws LockPersistenceException {

    // when
    Date expiresAtExpected = new Date(System.currentTimeMillis() - 60000);
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));
  }

  @Test
  public void insertUpdateShouldUpdateWhenExpiresAtIsGraterThanSaved() throws LockPersistenceException {
    //given
    final long currentMillis = System.currentTimeMillis();
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis - 1000)));

    //when
    Date expiresAtExpected = new Date();
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));

  }

  @Test
  public void insertUpdateShouldUpdateWhenSameOwner() throws LockPersistenceException {
    //given
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() + 60 * 60 * 1000)));

    //when
    Date expiresAtExpected = new Date();
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));
  }

  @Test(expected = LockPersistenceException.class)
  public void insertUpdateShouldThrowExceptionWhenLockIsInDBWIthDifferentOwnerAndNotExpired() throws LockPersistenceException {
    //given
    final long currentMillis = System.currentTimeMillis();
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis + 60 * 60 * 1000)));

    //when
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", new Date(currentMillis + 90 * 60 * 1000)));
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
    repository.removeByKeyAndOwner(LOCK_KEY, "process1");

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
    repository.removeByKeyAndOwner(LOCK_KEY, "process2");

    //then
    assertNotNull(db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY)).first());
  }

  @Test(expected = LockPersistenceException.class)
  public void updateIfSameOwnerShouldNotInsertWhenEmpty() throws LockPersistenceException {
    //when
    repository.updateIfSameOwner(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() - 600000)));
  }

  @Test(expected = LockPersistenceException.class)
  public void updateIfSameOwnerShouldNotUpdateWhenExpiresAtIsGraterThanSavedButOtherOwner() throws LockPersistenceException {
    //given
    final long currentMillis = System.currentTimeMillis();
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis - 1000)));

    //when
    repository.updateIfSameOwner(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", new Date(currentMillis)));

  }

  @Test
  public void updateIfSameOwnerShouldUpdateWhenSameOwner() throws LockPersistenceException {
    //given
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(System.currentTimeMillis() + 60 * 60 * 1000)));

    //when
    Date expiresAtExpected = new Date(System.currentTimeMillis());
    repository.updateIfSameOwner(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", expiresAtExpected));

    //then
    FindIterable<Document> result = db.getCollection(LOCK_COLLECTION_NAME).find(new Document().append("key", LOCK_KEY));
    assertNotNull(result.first());
    assertEquals(expiresAtExpected, result.first().get("expiresAt"));
  }

  @Test(expected = LockPersistenceException.class)
  public void updateIfSameOwnerShouldNotUpdateWhenDifferentOwnerAndExpiresAtIsNotGrater() throws LockPersistenceException {
    // given
    final long currentMillis = System.currentTimeMillis();
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process1", new Date(currentMillis + 60 * 60 * 1000)));

    // when
    repository.insertUpdate(new LockEntry(LOCK_KEY, LOCK_HELD.name(), "process2", new Date(currentMillis)));
  }


  @Test
  public void setCollectionNameTest() throws NoSuchFieldException, IllegalAccessException {
    repository.setLockCollectionName("COLLECTION_NAME");

    Field f = LockRepository.class.getDeclaredField("lockCollectionName");
    try {
      f.setAccessible(true);
      assertEquals("COLLECTION_NAME", f.get(repository));

    } finally {
      f.setAccessible(false);
    }
  }

}
