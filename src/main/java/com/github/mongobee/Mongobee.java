package com.github.mongobee;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.utils.ChangeService;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClientURI;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.List;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.springframework.util.StringUtils.hasText;

/**
 * Mongobee runner
 * 
 * @author lstolowski
 * @since 26/07/2014
 */
public class Mongobee implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(Mongobee.class);

  private ChangeEntryDao dao;

  private boolean enabled = true;
  private String changeLogsScanPackage;
  private MongoClientURI mongoClientURI;
  private Mongo mongo;
  private String dbName;
  private Environment springEnvironment;

  /**
   * <p>Simple constructor with default configuration of host (localhost) and port (27017). Although
   * <b>the database name need to be provided</b> using {@link Mongobee#setDbName(String)} setter.</p>
   * <p>It is recommended to use constructors with MongoURI</p>
   */
  public Mongobee() {
    this(new MongoClientURI("mongodb://" + defaultHost() + ":" + defaultPort() + "/"));
  }

  /**
   * <p>Constructor takes db.mongodb.MongoClientURI object as a parameter.
   * </p><p>For more details about MongoClientURI please see com.mongodb.MongoClientURI docs
   * </p>
   * @param mongoClientURI uri to your db
   * @see MongoClientURI
   */
  public Mongobee(MongoClientURI mongoClientURI) {
    this.mongoClientURI = mongoClientURI;
    this.setDbName(mongoClientURI.getDatabase());
    this.dao = new ChangeEntryDao();
  }

  /**
   * <p>Constructor takes db.mongodb.Mongo object as a parameter.
   * </p><p>For more details about <tt>Mongo</tt> please see com.mongodb.Mongo docs
   * </p>
   * @param mongo database connection
   * @see Mongo
   */
  public Mongobee(Mongo mongo) {
    this.mongo = mongo;
    this.dao = new ChangeEntryDao();
  }

  /**
   * <p>Mongobee runner. Correct MongoDB URI should be provided.</p>
   * <p>The format of the URI is:
   * <pre>
   *   mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
   * </pre>
   * <ul>
   * <li>{@code mongodb://} Required prefix</li>
   * <li>{@code username:password@} are optional.  If given, the driver will attempt to login to a database after
   * connecting to a database server. For some authentication mechanisms, only the username is specified and the password is not,
   * in which case the ":" after the username is left off as well.</li>
   * <li>{@code host1} Required.  It identifies a server address to connect to. More than one host can be provided.</li>
   * <li>{@code :portX} is optional and defaults to :27017 if not provided.</li>
   * <li>{@code /database} the name of the database to login to and thus is only relevant if the
   * {@code username:password@} syntax is used. If not specified the "admin" database will be used by default.
   * <b>Mongobee will operate on the database provided here or on the database overriden by setter setDbName(String).</b>
   * </li>
   *
   * <li>{@code ?options} are connection options. For list of options please see com.mongodb.MongoClientURI docs</li>
   * </ul>
   *
   * <p>For details, please see com.mongodb.MongoClientURI
   * @param mongoURI with correct format
   * @see com.mongodb.MongoClientURI
   */

  public Mongobee(String mongoURI) {
    this(new MongoClientURI(mongoURI));
  }

  /**
   * For Spring users: executing mongobee after bean is created in the Spring context
   * @throws Exception exception
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    execute();
  }

  /**
   * Executing migration
   * 
   * @throws UnknownHostException exception
   * @throws NoSuchMethodException exception
   * @throws IllegalAccessException exception
   * @throws InvocationTargetException exception
   * @throws InstantiationException exception
   */
  public void execute() throws UnknownHostException, NoSuchMethodException, IllegalAccessException, 
                        InvocationTargetException, InstantiationException {
    if(!isEnabled()){
      logger.info("Mongobee is disabled. Exiting.");
      return;
    }

    validateConfig();

    logger.info("Mongobee has started the data migration sequence..");
    
    if (this.mongo != null) {
    	dao.connectMongoDb(this.mongo, dbName);	
    } else {
    	dao.connectMongoDb(this.mongoClientURI, dbName);	
    }
    

    ChangeService service = new ChangeService(changeLogsScanPackage, springEnvironment);

    for (Class<?> changelogClass : service.fetchChangeLogs()) {
      
      Object changelogInstance = changelogClass.getConstructor().newInstance();

      List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());

      for (Method changesetMethod : changesetMethods) {
        ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);

        try {
          if (dao.isNewChange(changeEntry)) {
            executeChangeSetMethod(changesetMethod, changelogInstance, dao.getDb());
            dao.save(changeEntry);
            logger.info(changeEntry + " applied");
          }
          else if (service.isRunAlwaysChangeSet(changesetMethod)){
            executeChangeSetMethod(changesetMethod, changelogInstance, dao.getDb());
            logger.info(changeEntry + " reapplied");
          }
          else {
            logger.info(changeEntry + " passed over");
          }
        } catch (MongobeeChangeSetException e) {
          logger.error(e.getMessage());
        }
      }

    }
    logger.info("Mongobee has finished his job.");
  }

  private Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance, DB db)
                          throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
    if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(DB.class)) {
      logger.debug("method with DB argument");

      return changeSetMethod.invoke(changeLogInstance, db);
    }
    else if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(Jongo.class)) {
      logger.debug("method with Jongo argument");

      return changeSetMethod.invoke(changeLogInstance, new Jongo(db));
    }
    else if (changeSetMethod.getParameterTypes().length == 0) {
      logger.debug("method with no params");

      return changeSetMethod.invoke(changeLogInstance);
    }
    else {
      throw new MongobeeChangeSetException("ChangeSet method " + changeSetMethod.getName() +
                                            " has wrong arguments list. Please see docs for more info!");
    }
  }

  private void validateConfig() {
    if (!hasText(dbName)) {
      throw new MongobeeConfigurationException(
        "DB name is not set. It should be defined in MongoDB URI or via setter");
    }
    if (!hasText(changeLogsScanPackage)) {
      throw new MongobeeConfigurationException(
        "Scan package for changelogs is not set: use appropriate setter");
    }
  }

  /**
   * Used DB name should be set here or via MongoDB URI (in a constructor)
   * @param dbName database name
   * @return Mongobee object for fluent interface
   */
  public Mongobee setDbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  /**
   * Sets uri to MongoDB
   * @param mongoClientURI object with defined mongo uri
   * @return Mongobee object for fluent interface
   */
  public Mongobee setMongoClientURI(MongoClientURI mongoClientURI) {
    this.mongoClientURI = mongoClientURI;
    return this;
  }

  /**
   * Package name where @ChangeLog-annotated classes are kept.
   * @param changeLogsScanPackage package where your changelogs are
   * @return Mongobee object for fluent interface
   */
  public Mongobee setChangeLogsScanPackage(String changeLogsScanPackage) {
    this.changeLogsScanPackage = changeLogsScanPackage;
    return this;
  }

  /**
   * @return true if Mongobee runner is enabled and able to run, otherwise false
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Feature which enables/disables Mongobee runner execution
   * @param enabled MOngobee will run only if this option is set to true
   * @return Mongobee object for fluent interface
   */
  public Mongobee setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Set Environment object for Spring Profiles (@Profile) integration
   * @param environment org.springframework.core.env.Environment object to inject
   * @return Mongobee object for fluent interface
   */
  public Mongobee setSpringEnvironment(Environment environment) {
    this.springEnvironment = environment;
    return this;
  }
}
