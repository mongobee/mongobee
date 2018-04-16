package com.github.mongobee.lock;

import org.bson.Document;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class LockEntryTest {


  @Test
  public void constructorAnGetters() {
    Date date = new Date();
    final LockEntry e = new LockEntry("KEY","STATUS","OWNER", date);
    assertEquals("KEY", e.getKey());
    assertEquals("STATUS", e.getStatus());
    assertEquals("OWNER", e.getOwner());
    assertEquals(date, e.getExpiresAt());
  }

  @Test
  public void toStringTest() {
    assertEquals("LockEntry{key='KEY', status='STATUS', owner='OWNER', expiresAt=Thu Jan 01 01:00:00 GMT 1970}",
        new LockEntry("KEY","STATUS","OWNER", new Date(1)).toString());
  }

  @Test
  public void buildFullDBObject() {
    Document actual = new LockEntry("KEY","STATUS","OWNER", new Date(1)).buildFullDBObject();
    Document expected = new Document()
        .append("key", "KEY")
        .append("status", "STATUS")
        .append("owner", "OWNER")
        .append("expiresAt", new Date(1));
    assertEquals(expected, actual);
  }

}
