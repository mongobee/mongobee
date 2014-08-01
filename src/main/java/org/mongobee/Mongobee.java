package org.mongobee;

import com.mongodb.*;
import org.jongo.Jongo;
import org.mongobee.changeset.ChangeEntry;
import org.mongobee.dao.ChangeEntryDao;
import org.mongobee.exception.MongobeeChangesetException;
import org.mongobee.exception.MongobeeConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.List;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mongobee.utils.MongobeeAnnotationUtils.*;

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
  private String changelogsScanPackage;
  private MongoClientURI mongoClientURI;
  private String dbName;

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
   * </p><p>For more details about MongoClientURI please see com.mongodb.MongoClientURI
   * </p>
   * @param mongoClientURI
   * @see MongoClientURI
   */
  public Mongobee(MongoClientURI mongoClientURI) {
    this.mongoClientURI = mongoClientURI;
    this.setDbName(mongoClientURI.getDatabase());
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
   * <li>{@code ?options} are connection options. For list of options please see com.mongodb.MongoClientURI</li>
   * </ul>
   * </p>
   * <p>For details, please see com.mongodb.MongoClientURI</p>
   * @param mongoURI with correct format
   * @see com.mongodb.MongoClientURI
   */

  public Mongobee(String mongoURI) {
    this(new MongoClientURI(mongoURI));
  }

  /**
   * For Spring users: executing mongobee after bean is created in the Spring context
   * @throws Exception
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    execute();
  }

  /**
   * Executing migration
   * 
   * @throws UnknownHostException
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws InstantiationException
   */
  public void execute() throws UnknownHostException, NoSuchMethodException, IllegalAccessException, 
                        InvocationTargetException, InstantiationException {
    if(!isEnabled()){
      logger.info("Mongobee is disabled. Exiting.");
      return;
    }

    validateConfig();

    logger.info("Mongobee has started the data migration sequence..");
    
    DB db = dao.connectMongoDb(mongoClientURI, dbName);

    for (Class<?> changelogClass : fetchChangelogsAt(changelogsScanPackage)) {
      
      Object changesetInstance = changelogClass.getConstructor().newInstance();

      List<Method> changesetMethods = fetchChangesetsAt(changesetInstance.getClass());

      for (Method changesetMethod : changesetMethods) {

        ChangeEntry changeEntry = createChangeEntryFor(changesetMethod);
        if (dao.isNewChange(changeEntry)) {

          if (changesetMethod.getParameterTypes().length == 1 
                      && changesetMethod.getParameterTypes()[0].equals(DB.class)) {
            logger.debug("method with DB argument");

            changesetMethod.invoke(changesetInstance, db);
            dao.save(changeEntry);

          }
          else if (changesetMethod.getParameterTypes().length == 1 
                      && changesetMethod.getParameterTypes()[0].equals(Jongo.class)) {
            logger.debug("method with Jongo argument");

            changesetMethod.invoke(changesetInstance, new Jongo(db));
            dao.save(changeEntry);

          }
          else if (changesetMethod.getParameterTypes().length == 0) {
            logger.debug("method with no params");
            
            changesetMethod.invoke(changesetInstance);
            dao.save(changeEntry);

          } 
          else {
            throw new MongobeeChangesetException("Changeset method has wrong arguments list: " + changeEntry);
          }
          logger.info(changeEntry + " applied");
        } else {
          logger.info(changeEntry + " passed over");
        }
      }

    }
    logger.info("Mongobee has finished his job.");
  }

  private void validateConfig() {
    if (isBlank(dbName)) {
      throw new MongobeeConfigurationException(
        "DB name is not set. It should be defined in MongoDB URI or via setter");
    }
    if (isBlank(changelogsScanPackage)) {
      throw new MongobeeConfigurationException(
        "Scan package for changelogs is not set: use appropriate setter");
    }
  }



  /**
   * Used DB name should be set here or via MongoDB URI (in a constructor)
   * @param dbName
   */
  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  public void setMongoClientURI(MongoClientURI mongoClientURI) {
    this.mongoClientURI = mongoClientURI;
  }

  /**
   * Package name where @Changelog-annotated classes are kept.
   * @param changelogsScanPackage
   */
  public void setChangelogsScanPackage(String changelogsScanPackage) {
    this.changelogsScanPackage = changelogsScanPackage;
  }

  /**
   * @return true if Mongobee runner is enabled and able to run, otherwise false
   */
  public boolean isEnabled() {
    return enabled;
  }
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

}
