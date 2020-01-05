/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.datasource.jndi;

import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;

/**
 * @author Clinton Begin
 *
 * 使用Spring或者应用服务器这些框架时，会在外部生成一个数据源。
 * 这个工厂的功能是将外部配置的生成的数据源拿到
 *
 * 这个数据源的实现是为了能在如EJB 或应用服务器这类容器中使用，容器可以集中或在外部配置数据源，然后放置一个JNDI 上下文的引用。
 * 这种数据源配置只需要两个属性:
 * initial_contexto。这个属性用来在InitialContext 中寻找上下文。这是个可选属性，如果忽略，那么data_source 属性将会直接从InitialContext中寻找。
 * data_source。这是引用数据源实例位置的上下文路径。若提供了initial_context配置则会在其返回的上下文中进行查找，没有提供则直接在InitialContext中查找。
 *
 * <environment id="sit">
 *             <transactionManager type="JDBC"/>
 *             <dataSource type="JNDI" >
 *                 <property name="initial_context" value="java:/comp/env"/>
 *                 <property name="data_source" value="{datasource}"/>
 *             </dataSource>
 * </environment>
 */
public class JndiDataSourceFactory implements DataSourceFactory {

  public static final String INITIAL_CONTEXT = "initial_context";
  public static final String DATA_SOURCE = "data_source";
  public static final String ENV_PREFIX = "env.";

  private DataSource dataSource;

  /**
   * 配置数据源属性，其中包含了数据源的查找工作
   * @param properties 属性信息
   */
  @Override
  public void setProperties(Properties properties) {
    try {
      // 初始上下文环境
      InitialContext initCtx;
      // 获取配置信息，根据配置信息初始化环境
      Properties env = getEnvProperties(properties);
      if (env == null) {
        initCtx = new InitialContext();
      } else {
        initCtx = new InitialContext(env);
      }

      // 从配置信息中获取数据源信息
      if (properties.containsKey(INITIAL_CONTEXT)
          && properties.containsKey(DATA_SOURCE)) {
        // 定位到initial_context给出的起始环境
        Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
        // 从起始环境中寻找指定数据源
        dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
      } else if (properties.containsKey(DATA_SOURCE)) {
        // 从整个环境中寻找指定数据源
        dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
      }
    } catch (NamingException e) {
      throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
    }
  }

  /**
   * 获取数据源
   * @return 数据源
   */
  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  private static Properties getEnvProperties(Properties allProps) {
    final String PREFIX = ENV_PREFIX;
    Properties contextProperties = null;
    for (Entry<Object, Object> entry : allProps.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (key.startsWith(PREFIX)) {
        if (contextProperties == null) {
          contextProperties = new Properties();
        }
        contextProperties.put(key.substring(PREFIX.length()), value);
      }
    }
    return contextProperties;
  }

}
