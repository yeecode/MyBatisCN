/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.io;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * <p>ResolverUtil 用来定位某个class路径下满足任意条件的类们。通常的筛选条件是：<p/>
 * <pre>
 * 类继承了某个接口
 * 实现了某个类
 * 类拥有可定的注解
 * 基于Test类可以用来判断某各类是否满足这些条件
 * </pre>
 *
 * <p>ResolverUtil is used to locate classes that are available in the/a class path and meet
 * arbitrary conditions. The two most common conditions are that a class implements/extends
 * another class, or that is it annotated with a specific annotation. However, through the use
 * of the {@link Test} class it is possible to search using arbitrary conditions.</p>
 *
 * <p>A ClassLoader is used to locate all locations (directories and jar files) in the class
 * path that contain classes within certain packages, and then to load those classes and
 * check them. By default the ClassLoader returned by
 * {@code Thread.currentThread().getContextClassLoader()} is used, but this can be overridden
 * by calling {@link #setClassLoader(ClassLoader)} prior to invoking any of the {@code find()}
 * methods.</p>
 *
 * <p>General searches are initiated by calling the
 * {@link #find(org.apache.ibatis.io.ResolverUtil.Test, String)} ()} method and supplying
 * a package name and a Test instance. This will cause the named package <b>and all sub-packages</b>
 * to be scanned for classes that meet the test. There are also utility methods for the common
 * use cases of scanning multiple packages for extensions of particular classes, or classes
 * annotated with a specific annotation.</p>
 *
 * <p>The standard usage pattern for the ResolverUtil class is as follows:</p>
 *
 * <pre>
 * ResolverUtil&lt;ActionBean&gt; resolver = new ResolverUtil&lt;ActionBean&gt;();
 * resolver.findImplementation(ActionBean.class, pkg1, pkg2);
 * resolver.find(new CustomTest(), pkg1);
 * resolver.find(new CustomTest(), pkg2);
 * Collection&lt;ActionBean&gt; beans = resolver.getClasses();
 * </pre>
 *
 * @author Tim Fennell
 */
public class ResolverUtil<T> {
  /*
   * An instance of Log to use for logging in this class.
   */
  private static final Log log = LogFactory.getLog(ResolverUtil.class);

  /**
   * Test是一个筛选器，内部有一个抽象方法 matches 来判断指定类是否满足筛选条件。
   *
   * A simple interface that specifies how to test classes to determine if they
   * are to be included in the results produced by the ResolverUtil.
   */
  public interface Test {
    /**
     * Will be called repeatedly with candidate classes. Must return True if a class
     * is to be included in the results, false otherwise.
     */
    boolean matches(Class<?> type);
  }

  /**
   * A Test that checks to see if each class is assignable to the provided class. Note
   * that this test will match the parent type itself if it is presented for matching.
   */
  public static class IsA implements Test {
    private Class<?> parent;

    /** Constructs an IsA test using the supplied Class as the parent class/interface. */
    public IsA(Class<?> parentType) {
      this.parent = parentType;
    }

    /**
     * IsA类中的 matches方法可以判断目标类是否实现了某个接口或者继承了某个类
     * Returns true if type is assignable to the parent type supplied in the constructor.
     * */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && parent.isAssignableFrom(type);
    }

    @Override
    public String toString() {
      return "is assignable to " + parent.getSimpleName();
    }
  }


  /**
   * A Test that checks to see if each class is annotated with a specific annotation. If it
   * is, then the test returns true, otherwise false.
   */
  public static class AnnotatedWith implements Test {
    private Class<? extends Annotation> annotation;

    /** Constructs an AnnotatedWith test for the specified annotation type. */
    public AnnotatedWith(Class<? extends Annotation> annotation) {
      this.annotation = annotation;
    }

    /**
     * AnnotatedWith 类中的 matches方法可以判断目标类是否具有某个注解。
     * Returns true if the type is annotated with the class provided to the constructor.
     * */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && type.isAnnotationPresent(annotation);
    }

    @Override
    public String toString() {
      return "annotated with @" + annotation.getSimpleName();
    }
  }


  /**
   * 最终通过校验的类都会放到 matches 属性中
   * The set of matches being accumulated.
   * */
  private Set<Class<? extends T>> matches = new HashSet<>();


  /**
   * 查找类时使用的 ClassLoader。如果为 null，则将使用 Thread.currentThread().getContextClassLoader() 返回的 ClassLoader。
   *
   * The ClassLoader to use when looking for classes. If null then the ClassLoader returned
   * by Thread.currentThread().getContextClassLoader() will be used.
   */
  private ClassLoader classloader;


  /**
   * Provides access to the classes discovered so far. If no calls have been made to
   * any of the {@code find()} methods, this set will be empty.
   *
   * @return the set of classes that have been discovered.
   */
  public Set<Class<? extends T>> getClasses() {
    return matches;
  }

  /**
   * Returns the classloader that will be used for scanning for classes. If no explicit
   * ClassLoader has been set by the calling, the context class loader will be used.
   *
   * @return the ClassLoader that will be used to scan for classes
   */
  public ClassLoader getClassLoader() {
    return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
  }

  /**
   * Sets an explicit ClassLoader that should be used when scanning for classes. If none
   * is set then the context classloader will be used.
   *
   * @param classloader a ClassLoader to use when scanning for classes
   */
  public void setClassLoader(ClassLoader classloader) {
    this.classloader = classloader;
  }

  /**
   * Attempts to discover classes that are assignable to the type provided. In the case
   * that an interface is provided this method will collect implementations. In the case
   * of a non-interface class, subclasses will be collected.  Accumulated classes can be
   * accessed by calling {@link #getClasses()}.
   *
   * @param parent the class of interface to find subclasses or implementations of
   * @param packageNames one or more package names to scan (including subpackages) for classes
   */
  public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    Test test = new IsA(parent);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }

  /**
   * Attempts to discover classes that are annotated with the annotation. Accumulated
   * classes can be accessed by calling {@link #getClasses()}.
   *
   * @param annotation the annotation that should be present on matching classes
   * @param packageNames one or more package names to scan (including subpackages) for classes
   */
  public ResolverUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    Test test = new AnnotatedWith(annotation);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }



  /**
   * 筛选出指定路径下符合一定条件的类
   * @param test 测试条件
   * @param packageName 路径
   * @return ResolverUtil本身
   */
  public ResolverUtil<T> find(Test test, String packageName) {
    // 获取起始包路径
    String path = getPackagePath(packageName);
    try {
      // 找出包中的各个文件
      List<String> children = VFS.getInstance().list(path);
      for (String child : children) {
        // 对类文件进行测试
        if (child.endsWith(".class")) { // 必须是类文件
          // 测试是否满足测试条件。如果满足，则将该类文件记录下来
          addIfMatching(test, child);
        }
      }
    } catch (IOException ioe) {
      log.error("Could not read package: " + packageName, ioe);
    }
    return this;
  }

  /**
   * Converts a Java package name to a path that can be looked up with a call to
   * {@link ClassLoader#getResources(String)}.
   *
   * @param packageName The Java package name to convert to a path
   */
  protected String getPackagePath(String packageName) {
    return packageName == null ? null : packageName.replace('.', '/');
  }

  /**
   * Add the class designated by the fully qualified class name provided to the set of
   * resolved classes if and only if it is approved by the Test supplied.
   *
   * @param test the test used to determine if the class matches
   * @param fqn the fully qualified name of a class
   */
  /**
   * 判断一个类文件是否满足条件。如果满足则记录下来
   * @param test 测试条件
   * @param fqn 类文件全名
   */
  @SuppressWarnings("unchecked")
  protected void addIfMatching(Test test, String fqn) {
    try {
      // 转化为外部名称
      String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
      // 类加载器
      ClassLoader loader = getClassLoader();
      if (log.isDebugEnabled()) {
        log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
      }
      // 加载类文件
      Class<?> type = loader.loadClass(externalName);
      if (test.matches(type)) { // 执行测试
        // 测试通过则记录到matches属性中
        matches.add((Class<T>) type);
      }
    } catch (Throwable t) {
      log.warn("Could not examine class '" + fqn + "'" + " due to a " + t.getClass().getName() + " with message: " + t.getMessage());
    }
  }
}
