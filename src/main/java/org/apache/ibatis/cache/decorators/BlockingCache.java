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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * Simple blocking decorator
 *
 * Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 *
 * @author Eduardo Macarron
 *
 */
public class BlockingCache implements Cache {

  // 获取锁时的运行等待时间
  private long timeout;
  // 被装饰对象
  private final Cache delegate;
  // 锁的映射表。键为缓存记录的键，值为对应的锁。
  private final ConcurrentHashMap<Object, ReentrantLock> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
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
   * 向缓存写入一条信息
   * @param key 信息的键
   * @param value 信息的值
   */
  @Override
  public void putObject(Object key, Object value) {
    try {
      // 向缓存中放入数据
      delegate.putObject(key, value);
    } finally {
      // 因为已经放入了数据，因此释放锁
      releaseLock(key);
    }
  }

  /**
   * 从缓存中读取一条信息
   * @param key 信息的键
   * @return 信息的值
   */
  @Override
  public Object getObject(Object key) {
    // 获取锁
    acquireLock(key);
    // 读取结果
    Object value = delegate.getObject(key);
    if (value != null) {
      // 读取到结果后释放锁
      releaseLock(key);
    }
    // 如果缓存中没有读到结果，则不会释放锁。对应的锁会在从数据库读取了结果并写入到缓存后，在putObject中释放。

    // 返回查询到的缓存结果
    return value;
  }

  @Override
  public Object removeObject(Object key) {
    // despite of its name, this method is called only to release locks
    releaseLock(key);
    return null;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  /**
   * 找出指定键的锁
   * @param key 指定的键
   * @return 该键对应的锁
   */
  private ReentrantLock getLockForKey(Object key) {
    return locks.computeIfAbsent(key, k -> new ReentrantLock());
  }

  /**
   * 获取某个键的锁
   * @param key 数据的键
   */
  private void acquireLock(Object key) {
    // 找出指定对象的锁
    Lock lock = getLockForKey(key);
    if (timeout > 0) {
      try {
        boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        if (!acquired) {
          throw new CacheException("Couldn't get a lock in " + timeout + " for the key " +  key + " at the cache " + delegate.getId());
        }
      } catch (InterruptedException e) {
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    } else {
      // 锁住
      lock.lock();
    }
  }

  /**
   * 释放某个对象的锁
   * @param key 被锁的对象
   */
  private void releaseLock(Object key) {
    // 找出指定对象的锁
    ReentrantLock lock = locks.get(key);
    if (lock.isHeldByCurrentThread()) {
      // 解锁
      lock.unlock();
    }
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}
