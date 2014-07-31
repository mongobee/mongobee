package org.mongobee;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoURI;
import com.mongodb.ServerAddress;
import org.apache.commons.lang3.StringUtils;
import org.jongo.Jongo;
import org.mongobee.changeset.ChangeEntry;
import org.mongobee.dao.ChangeEntryDao;
import org.mongobee.exception.MongobeeChangesetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongobee.utils.MongobeeAnnotationUtils.*;

/**
 * Mongobee runner
 * 
 * @author lstolowski
 * @since 26/07/2014
 */
public class Mongobee implements InitializingBean {
  Logger logger = LoggerFactory.getLogger(Mongobee.class);

  private boolean enabled = false;
  
  private String host = ServerAddress.defaultHost();
  private int port = ServerAddress.defaultPort();
  
  private String dbName;
  private MongoAuth auth;
  private String changelogsBasePackage;
  private MongoURI mongoURI;

  private boolean jobExecuted; // flag to ensure that mongobee is executed once per instance
  
  private ChangeEntryDao changeEntryDao;


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
    if (jobExecuted){
      return;
    } else {
      jobExecuted = true;
    }
           
    validateConfig();

    logger.info("Mongobee has started the data migration sequence..");
    
    MongoClient mongoClient = getMongoClient();
    DB db = mongoClient.getDB(dbName);
    changeEntryDao = new ChangeEntryDao(db);
    
    for (Class<?> changelogClass : fetchChangelogsAt(changelogsBasePackage)) {
      
      Object changesetInstance = changelogClass.getConstructor().newInstance();

      List<Method> changesetMethods = fetchChangesetsAt(changesetInstance.getClass());

      for (Method changesetMethod : changesetMethods) {

        ChangeEntry changeEntry = createChangeEntryFor(changesetMethod);
        if (changeEntryDao.isNewChange(changeEntry)) {

          if (changesetMethod.getParameterTypes().length == 1 
                      && changesetMethod.getParameterTypes()[0].equals(DB.class)) {
            logger.debug("method with DB argument");

            changesetMethod.invoke(changesetInstance, db);
            changeEntryDao.save(changeEntry);

          }
          else if (changesetMethod.getParameterTypes().length == 1 
                      && changesetMethod.getParameterTypes()[0].equals(Jongo.class)) {
            logger.debug("method with Jongo argument");

            changesetMethod.invoke(changesetInstance, new Jongo(db));
            changeEntryDao.save(changeEntry);

          }
          else if (changesetMethod.getParameterTypes().length == 0) {
            logger.debug("method with no params");
            
            changesetMethod.invoke(changesetInstance);
            changeEntryDao.save(changeEntry);

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
    if (StringUtils.isBlank(dbName)) {
      throw new IllegalStateException("DB name is not set");
    }
    if (StringUtils.isBlank(changelogsBasePackage)) {
      throw new IllegalStateException("Base package for changelogs scanning (setChangelogsBasePackage(String)) is not set");
    }
  }

  private MongoClient getMongoClient() throws UnknownHostException {
    MongoClient mongoClient;
    if (auth != null) {
      MongoCredential credentials = MongoCredential.createMongoCRCredential(
              auth.getUsername(),
              (isNotBlank(auth.getDbName())) ? auth.getDbName() : dbName,
              (auth.getPassword() != null)   ? auth.getPassword().toCharArray() : new char[0]
      );
      mongoClient = new MongoClient(new ServerAddress(host), asList(credentials));
    } else {
      mongoClient = new MongoClient(new ServerAddress(host));
    }
    return mongoClient;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setChangelogsBasePackage(String changelogsBasePackage) {
    this.changelogsBasePackage = changelogsBasePackage;
  }

  public void setAuth(MongoAuth auth) {
    this.auth = auth;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  public boolean isEnabled() {
    return enabled;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

}
