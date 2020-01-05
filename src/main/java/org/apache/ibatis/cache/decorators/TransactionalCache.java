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
package org.apache.ibatis.cache.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * The 2nd level cache transactional buffer.
 * <p>
 * This class holds all cache entries that are to be added to the 2nd level cache during a Session.
 * Entries are sent to the cache when commit is called or discarded if the Session is rolled back.
 * Blocking cache support has been added. Therefore any get() that returns a cache miss
 * will be followed by a put() so any lock associated with the key can be released.
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class TransactionalCache implements Cache {

  private static final Log log = LogFactory.getLog(TransactionalCache.class);

  // 被装饰的对象
  private final Cache delegate;
  // 事务提交后是否直接清理缓存
  private boolean clearOnCommit;
  // 事务提交时需要写入缓存的数据
  private final Map<Object, Object> entriesToAddOnCommit;
  // 缓存查询未命中的数据
  private final Set<Object> entriesMissedInCache;

  public TransactionalCache(Cache delegate) {
    this.delegate = delegate;
    this.clearOnCommit = false;
    this.entriesToAddOnCommit = new HashMap<>();
    this.entriesMissedInCache = new HashSet<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  /**
   * 从缓存中读取一条信息
   * @param key 信息的键
   * @return 信息的值
   */
  @Override
  public Object getObject(Object key) {
    // 从缓存中读取对应的数据
    Object object = delegate.getObject(key);
    if (object == null) { // 缓存未命中
      // 记录该缓存未命中
      entriesMissedInCache.add(key);
    }
    if (clearOnCommit) { // 如果设置了提交时立马清除，则直接返回null
      return null;
    } else {
      // 返回查询的结果
      return object;
    }
  }

  /**
   * 向缓存写入一条信息
   * @param key 信息的键
   * @param object 信息的值
   */
  @Override
  public void putObject(Object key, Object object) {
    // 先放入到entriesToAddOnCommit列表中暂存
    entriesToAddOnCommit.put(key, object);
  }

  @Override
  public Object removeObject(Object key) {
    return null;
  }

  @Override
  public void clear() {
    clearOnCommit = true;
    entriesToAddOnCommit.clear();
  }

  /**
   * 提交事务
   */
  public void commit() {
    if (clearOnCommit) { // 如果设置了事务提交后清理缓存
      // 清理缓存
      delegate.clear();
    }
    // 将为写入缓存的操作写入缓存
    flushPendingEntries();
    // 清理环境
    reset();
  }

  /**
   * 回滚事务
   */
  public void rollback() {
    // 删除缓存未命中的数据
    unlockMissedEntries();
    reset();
  }

  /**
   * 清理环境
   */
  private void reset() {
    clearOnCommit = false;
    entriesToAddOnCommit.clear();
    entriesMissedInCache.clear();
  }

  /**
   * 将未写入缓存的数据写入缓存
   */
  private void flushPendingEntries() {
    // 将entriesToAddOnCommit中的数据写入缓存
    for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
      delegate.putObject(entry.getKey(), entry.getValue());
    }
    // 将entriesMissedInCache中的数据写入缓存
    for (Object entry : entriesMissedInCache) {
      if (!entriesToAddOnCommit.containsKey(entry)) {
        delegate.putObject(entry, null);
      }
    }
  }

  /**
   * 删除缓存未命中的数据
   */
  private void unlockMissedEntries() {
    for (Object entry : entriesMissedInCache) {
      try {
        delegate.removeObject(entry);
      } catch (Exception e) {
        log.warn("Unexpected exception while notifiying a rollback to the cache adapter."
            + "Consider upgrading your cache adapter to the latest version.  Cause: " + e);
      }
    }
  }

}
