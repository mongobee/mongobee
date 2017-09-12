package org.github.mongobee.jongo;

import com.github.mongobee.core.Mongobee;
import com.github.mongobee.core.exception.MongobeeChangeSetException;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Jongo Mongobee runner
 *
 * @author j-coll
 */
public class JongoMongobee extends Mongobee {
  private static final Logger logger = LoggerFactory.getLogger(JongoMongobee.class);

  private Jongo jongo;

  public JongoMongobee() {
  }

  public JongoMongobee(MongoClientURI mongoClientURI) {
    super(mongoClientURI);
  }

  public JongoMongobee(MongoClient mongoClient) {
    super(mongoClient);
  }

  public JongoMongobee(String mongoURI) {
    super(mongoURI);
  }

  @Override
  protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance, DB db, MongoDatabase mongoDatabase) throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
    if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(Jongo.class)) {
      logger.debug("method with Jongo argument");

      return changeSetMethod.invoke(changeLogInstance, jongo != null ? jongo : new Jongo(db));
    } else {
      return super.executeChangeSetMethod(changeSetMethod, changeLogInstance, db, mongoDatabase);
    }
  }

  /**
   * Sets pre-configured {@link Jongo} instance to use by the Mongobee
   *
   * @param jongo {@link Jongo} instance
   * @return Mongobee object for fluent interface
   */
  public Mongobee setJongo(Jongo jongo) {
    this.jongo = jongo;
    return this;
  }
}
