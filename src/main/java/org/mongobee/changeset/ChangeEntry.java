package org.mongobee.changeset;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.Date;

/**
 * Entry in the changes collection log {@link ChangeEntry#CHANGELOG_COLLECTION} <br/>
 * Type: entity class.
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntry {
  public static final String CHANGELOG_COLLECTION = "dbchangelog";

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

  public DBObject buildFullDBObject(){
    BasicDBObject entry = new BasicDBObject();
    
    entry.append("changeId", changeId)
            .append("author", author)
            .append("timestamp", timestamp)
            .append("changelogClass", changelogClass)
            .append("changesetMethod", changesetMethodName);
    
    return entry;
  }
  
  public DBObject buildSearchQueryDBObject(){
    return new BasicDBObject()
                  .append("changeId", changeId)
                  .append("author", author);
  }

  @Override
  public String toString() {
    return "[Changeset: id=" + changeId +
            ", author=" + author +
            ", changelogClass=" + changelogClass +
            ", changesetMethod=" + changesetMethodName + "]";
  }

  public String getChangeId() {
    return changeId;
  }

  public String getAuthor() {
    return author;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public String getChangelogClass() {
    return changelogClass;
  }

  public String getChangesetMethodName() {
    return changesetMethodName;
  }
}
