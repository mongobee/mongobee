package org.monjeez;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.apache.commons.lang3.StringUtils;
import org.monjeez.changeset.Changeset;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Monjeez runner
 * @author lstolowski
 * @since 26/07/2014
 */
public class Monjeez  implements InitializingBean {

  private static final String MONJEEZ_CHANGELOG_COLLECTION = "monjeezlog";
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


    final Set<BeanDefinition> changesets = fetchChangesets();

    for (BeanDefinition changeset : changesets) {
      
      Class<?> clazz = Class.forName(changeset.getBeanClassName());
      Constructor<?> constructor = clazz.getConstructor();
      Object changesetInstance = constructor.newInstance();

      List<Method> methodsAnnotatedWith = getMethodsAnnotatedWith(changesetInstance.getClass(), Changeset.class);


      for (Method method : methodsAnnotatedWith) {
        if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(DB.class)) {
          System.out.println("parametr DB");
          
          method.invoke(changesetInstance, db);
          
        } else if (method.getParameterCount() == 0){
          System.out.println("no params, invoking ...");
          method.invoke(changesetInstance);
          
        } else {
          System.out.println("Changeset method not supported. Wrong arguments list");
        }
      }

    }


  }


  private Set<BeanDefinition> fetchChangesets(){


    ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);

    scanner.addIncludeFilter(new AnnotationTypeFilter(Changeset.class));

    return scanner.findCandidateComponents(changelogsBasePackage);
  }

  public List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends Annotation> annotation) {
    final List<Method> methods = new ArrayList<>();
    Class<?> klass = type;
    while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
      // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
      final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(klass.getDeclaredMethods()));
      for (final Method method : allMethods) {
        if (annotation == null || method.isAnnotationPresent(annotation)) {
          Annotation annotInstance = method.getAnnotation(annotation);
          // TODO process annotInstance
          methods.add(method);
        }
      }
      // move to the upper class in the hierarchy in search for more methods
      klass = klass.getSuperclass();
    }
    return methods;
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
