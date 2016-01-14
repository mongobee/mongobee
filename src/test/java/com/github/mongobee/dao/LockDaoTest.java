package com.github.mongobee.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.fakemongo.Fongo;
import com.mongodb.DB;

/**
 * @author colsson11
 * @since 13.01.15
 */
public class LockDaoTest {
	
  private static final String TEST_SERVER = "testServer";
  private static final String DB_NAME = "mongobeetest";
	  
  @Test
  public void shouldGetLockWhenNotPreviouslyHeld() throws Exception{
    
	// given
	DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
	LockDao dao = new LockDao();
	dao.intitializeLock(db);
	
	// when
	boolean hasLock = dao.acquireLock(db);
		  
	// then
	assertTrue(hasLock);	  
	  
  }
  
  @Test
  public void shouldNotGetLockWhenPreviouslyHeld() throws Exception{
    
	// given
	DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
	LockDao dao = new LockDao();
	dao.intitializeLock(db);
	
	// when
	dao.acquireLock(db);  
	boolean hasLock = dao.acquireLock(db);	  
	// then
	assertFalse(hasLock);	  
	  
  }
  
  @Test
  public void shouldGetLockWhenPreviouslyHeldAndReleased() throws Exception{
    
	// given
	DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
	LockDao dao = new LockDao();
	dao.intitializeLock(db);
	
	// when
	dao.acquireLock(db); 
	dao.releaseLock(db);
	boolean hasLock = dao.acquireLock(db);	  
	// then
	assertTrue(hasLock);	  
	  
  }
  
  @Test
  public void releaseLockShouldBeIdempotent(){
	// given
	DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
	LockDao dao = new LockDao();
	dao.intitializeLock(db);
		
	// when
	dao.releaseLock(db); 
	dao.releaseLock(db);
	boolean hasLock = dao.acquireLock(db);	  
	// then
	assertTrue(hasLock);	
	  
  }
  
  @Test
  public void whenLockNotHeldCheckReturnsFalse(){
	
	DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
    LockDao dao = new LockDao();
	dao.intitializeLock(db);
		  
	assertFalse(dao.isLockHeld(db));
	
  }
  
  @Test
  public void whenLockHeldCheckReturnsTrue(){
	
	DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
    LockDao dao = new LockDao();
	dao.intitializeLock(db);
	
	dao.acquireLock(db);
		  
	assertTrue(dao.isLockHeld(db));
	
  }
	
}
