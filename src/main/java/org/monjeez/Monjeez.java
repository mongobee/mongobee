package org.monjeez;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.monjeez.utils.MonjeezAnnotationUtils.fetchChangelogsAt;
import static org.monjeez.utils.MonjeezAnnotationUtils.fetchChangesetsAt;

/**
 * Monjeez runner
 * @author lstolowski
 * @since 26/07/2014
 */
public class Monjeez  implements InitializingBean {

  public static final String MONJEEZ_CHANGELOG_COLLECTION = "monjeezlog";
  private boolean enabled = false;
  
  private String host = ServerAddress.defaultHost();
  private int port = ServerAddress.defaultPort();
  
  private String dbName;
  private MongoAuth auth;
  private String changelogsBasePackage;


  @Override
  public void afterPropertiesSet() throws Exception {
    
    if (StringUtils.isBlank(dbName)) {
      throw new IllegalStateException("DB name is not set");
    }
    if (StringUtils.isBlank(changelogsBasePackage)) {
      throw new IllegalStateException("Base package for changelogs scanning (setChangelogsBasePackage(String)) is not set");
    }

    if(isEnabled()){
      executeMigration();
    }
    
  }

  private void executeMigration() throws UnknownHostException, ClassNotFoundException, 
          IllegalAccessException, InstantiationException, NoSuchMethodException, 
          InvocationTargetException {

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


    DB db = mongoClient.getDB(dbName);

    DBCollection changelog = db.getCollection(MONJEEZ_CHANGELOG_COLLECTION);


    final Set<Class<?>> changelogClasses = fetchChangelogsAt(changelogsBasePackage);

    for (Class<?> changelogClass : changelogClasses) {
      
      Constructor<?> constructor = changelogClass.getConstructor();
      Object changesetInstance = constructor.newInstance();

      List<Method> changesetMethods = fetchChangesetsAt(changesetInstance.getClass());


      for (Method changesetMethod : changesetMethods) {
        if (changesetMethod.getParameterCount() == 1 && changesetMethod.getParameterTypes()[0].equals(DB.class)) {
          System.out.println("parametr DB");
          
          changesetMethod.invoke(changesetInstance, db);
          
        } else if (changesetMethod.getParameterCount() == 0){
          System.out.println("no params, invoking ...");
          changesetMethod.invoke(changesetInstance);
          
        } else {
          System.out.println("Changeset changesetMethod not supported. Wrong arguments list");
        }
      }

    }


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

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

}
