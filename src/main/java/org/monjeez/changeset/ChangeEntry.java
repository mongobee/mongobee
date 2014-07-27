package org.monjeez.changeset;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.Date;

/**
 * Entry in the changes collection log {@link org.monjeez.Monjeez#MONJEEZ_CHANGELOG_COLLECTION}
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntry {
  
  private String changeId;
  private String author;
  private Date timestamp;
  private String changelogClass;
  private String changesetMethodName;

  public ChangeEntry(String changeId, String author, Date timestamp, String changelogClass, String changesetMethodName) {
    this.changeId = changeId;
    this.author = author;
    this.timestamp = timestamp;
    this.changelogClass = changelogClass;
    this.changesetMethodName = changesetMethodName;
  }
  
  public DBObject buildDBObject(){
    BasicDBObject entry = new BasicDBObject();
    
    entry.append("changeId", changeId)
            .append("author", author)
            .append("timestamp", timestamp)
            .append("changelogClass", changelogClass)
            .append("changesetMethod", changesetMethodName);
    
    return entry;
  }
  
}
