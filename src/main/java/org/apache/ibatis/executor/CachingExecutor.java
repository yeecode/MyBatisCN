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
package org.apache.ibatis.executor;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.TransactionalCacheManager;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class CachingExecutor implements Executor {

  // 被装饰的执行器
  private final Executor delegate;
  // 事务缓存管理器
  private final TransactionalCacheManager tcm = new TransactionalCacheManager();

  public CachingExecutor(Executor delegate) {
    this.delegate = delegate;
    delegate.setExecutorWrapper(this);
  }

  @Override
  public Transaction getTransaction() {
    return delegate.getTransaction();
  }

  @Override
  public void close(boolean forceRollback) {
    try {
      //issues #499, #524 and #573
      if (forceRollback) {
        tcm.rollback();
      } else {
        tcm.commit();
      }
    } finally {
      delegate.close(forceRollback);
    }
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  /**
   * 更新数据库数据，INSERT/UPDATE/DELETE三种操作都会调用该方法
   * @param ms 映射语句
   * @param parameterObject 参数对象
   * @return 数据库操作结果
   * @throws SQLException
   */
  @Override
  public int update(MappedStatement ms, Object parameterObject) throws SQLException {
    // 根据要求判断语句执行前是否要清除二级缓存，如果需要，清除二级缓存
    flushCacheIfRequired(ms);
    return delegate.update(ms, parameterObject);
  }

  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    BoundSql boundSql = ms.getBoundSql(parameterObject);
    CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
    return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override
  public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
    flushCacheIfRequired(ms);
    return delegate.queryCursor(ms, parameter, rowBounds);
  }

  /**
   * 查询数据库中的数据
   * @param ms 映射语句
   * @param parameterObject 参数对象
   * @param rowBounds 翻页限制条件
   * @param resultHandler 结果处理器
   * @param key 缓存的键
   * @param boundSql 查询语句
   * @param <E> 结果类型
   * @return 结果列表
   * @throws SQLException
   */
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {
    // 获取MappedStatement对应的缓存，可能的结果有：该命名空间的缓存、共享的其它命名空间的缓存、无缓存
    Cache cache = ms.getCache();
    // 如果映射文件未设置<cache>或<cache-ref>则，此处cache变量为null
    if (cache != null) { // 存在缓存
      // 根据要求判断语句执行前是否要清除二级缓存，如果需要，清除二级缓存
      flushCacheIfRequired(ms);
      if (ms.isUseCache() && resultHandler == null) { // 该语句使用缓存且没有输出结果处理器
        // 二级缓存不支持含有输出参数的CALLABLE语句，故在这里进行判断
        ensureNoOutParams(ms, boundSql);
        // 从缓存中读取结果
        @SuppressWarnings("unchecked")
        List<E> list = (List<E>) tcm.getObject(cache, key);
        if (list == null) { // 缓存中没有结果
          // 交给被包装的执行器执行
          list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
          // 缓存被包装执行器返回的结果
          tcm.putObject(cache, key, list); // issue #578 and #116
        }
        return list;
      }
    }
    // 交由被包装的实际执行器执行
    return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return delegate.flushStatements();
  }

  @Override
  public void commit(boolean required) throws SQLException {
    delegate.commit(required);
    tcm.commit();
  }

  @Override
  public void rollback(boolean required) throws SQLException {
    try {
      delegate.rollback(required);
    } finally {
      if (required) {
        tcm.rollback();
      }
    }
  }

  private void ensureNoOutParams(MappedStatement ms, BoundSql boundSql) {
    if (ms.getStatementType() == StatementType.CALLABLE) {
      for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
        if (parameterMapping.getMode() != ParameterMode.IN) {
          throw new ExecutorException("Caching stored procedures with OUT params is not supported.  Please configure useCache=false in " + ms.getId() + " statement.");
        }
      }
    }
  }

  @Override
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
  }

  @Override
  public boolean isCached(MappedStatement ms, CacheKey key) {
    return delegate.isCached(ms, key);
  }

  @Override
  public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
    delegate.deferLoad(ms, resultObject, property, key, targetType);
  }

  @Override
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  /**
   * 根据要求判断语句执行前是否要清除二级缓存，如果需要，清除二级缓存
   * 注意：默认情况下，非SELECT语句的isFlushCacheRequired方法会返回true
   * @param ms MappedStatement
   */
  private void flushCacheIfRequired(MappedStatement ms) {
    // 获取MappedStatement对应的缓存
    Cache cache = ms.getCache();
    if (cache != null && ms.isFlushCacheRequired()) { // 存在缓存且该操作语句要求执行前清除缓存
      // 清除事务中的缓存
      tcm.clear(cache);
    }
  }

  @Override
  public void setExecutorWrapper(Executor executor) {
    throw new UnsupportedOperationException("This method should not be called");
  }

}
