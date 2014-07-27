package org.monjeez;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.apache.commons.lang3.StringUtils;
import org.monjeez.changeset.ChangeEntry;
import org.monjeez.dao.ChangeEntryDao;
import org.monjeez.exception.MonjeezChangesetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.monjeez.utils.MonjeezAnnotationUtils.createChangeEntryFor;
import static org.monjeez.utils.MonjeezAnnotationUtils.fetchChangelogsAt;
import static org.monjeez.utils.MonjeezAnnotationUtils.fetchChangesetsAt;

/**
 * Monjeez runner
 * @author lstolowski
 * @since 26/07/2014
 */
public class Monjeez  implements InitializingBean {
  Logger logger = LoggerFactory.getLogger(Monjeez.class);

  private boolean enabled = false;
  
  private String host = ServerAddress.defaultHost();
  private int port = ServerAddress.defaultPort();
  
  private String dbName;
  private MongoAuth auth;
  private String changelogsBasePackage;

  private ChangeEntryDao changeEntryDao;
  

  @Override
  public void afterPropertiesSet() throws Exception {
    
    if (StringUtils.isBlank(dbName)) {
      throw new IllegalStateException("DB name is not set");
    }
    if (StringUtils.isBlank(changelogsBasePackage)) {
      throw new IllegalStateException("Base package for changelogs scanning (setChangelogsBasePackage(String)) is not set");
    }

    if(isEnabled()){
      execute();
    }
    
  }

  void execute() throws UnknownHostException, NoSuchMethodException, IllegalAccessException, 
                        InvocationTargetException, InstantiationException {

    MongoClient mongoClient = getMongoClient();
    DB db = mongoClient.getDB(dbName);
    changeEntryDao = new ChangeEntryDao(db);
    
    changeEntryDao.checkConnection();

    final Set<Class<?>> changelogClasses = fetchChangelogsAt(changelogsBasePackage);

    for (Class<?> changelogClass : changelogClasses) {
      
      Constructor<?> constructor = changelogClass.getConstructor();
      Object changesetInstance = constructor.newInstance();

      List<Method> changesetMethods = fetchChangesetsAt(changesetInstance.getClass());


      for (Method changesetMethod : changesetMethods) {

        ChangeEntry changeEntry = createChangeEntryFor(changesetMethod);
        if (changeEntryDao.isChangeNew(changeEntry)) {

          if (changesetMethod.getParameterCount() == 1 && changesetMethod.getParameterTypes()[0].equals(DB.class)) {
            logger.debug("method with DB argument");

            changesetMethod.invoke(changesetInstance, db);
            changeEntryDao.save(changeEntry);

          } else if (changesetMethod.getParameterCount() == 0) {
            logger.debug("method with no params");
            
            changesetMethod.invoke(changesetInstance);
            changeEntryDao.save(changeEntry);

          } else {
            throw new MonjeezChangesetException("Changeset method has wrong arguments list: " + changeEntry);
          }
          
          
        }
      }

    }


  }

  

  private MongoClient getMongoClient() throws UnknownHostException {
    MongoClient mongoClient;
    if (auth != null) {
      MongoCredential credentials = MongoCredential.createMongoCRCredential(
              auth.getDbName(),
              StringUtils.isNotBlank(auth.getDbName()) ? auth.getDbName() : dbName,
              auth.getPassword().toCharArray());
      mongoClient = new MongoClient(new ServerAddress(host), Arrays.asList(credentials));
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
