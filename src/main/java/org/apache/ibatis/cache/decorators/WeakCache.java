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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.LinkedList;

import org.apache.ibatis.cache.Cache;

/**
 * Weak Reference cache decorator.
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 *
 * @author Clinton Begin
 */
public class WeakCache implements Cache {
  // 强引用的对象列表
  private final Deque<Object> hardLinksToAvoidGarbageCollection;
  // 弱引用的对象列表
  private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
  // 被装饰对象
  private final Cache delegate;
  // 强引用对象的数目限制
  private int numberOfHardLinks;

  public WeakCache(Cache delegate) {
    this.delegate = delegate;
    this.numberOfHardLinks = 256;
    this.hardLinksToAvoidGarbageCollection = new LinkedList<>();
    this.queueOfGarbageCollectedEntries = new ReferenceQueue<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    removeGarbageCollectedItems();
    return delegate.getSize();
  }

  public void setSize(int size) {
    this.numberOfHardLinks = size;
  }

  /**
   * 向缓存写入一条信息
   * @param key 信息的键
   * @param value 信息的值
   */
  @Override
  public void putObject(Object key, Object value) {
    // 清除垃圾回收队列中的元素
    removeGarbageCollectedItems();
    // 向被装饰对象中放入的值是弱引用的句柄
    delegate.putObject(key, new WeakEntry(key, value, queueOfGarbageCollectedEntries));
  }

  /**
   * 从缓存中读取一条信息
   * @param key 信息的键
   * @return 信息的值
   */
  @Override
  public Object getObject(Object key) {
    Object result = null;
    // 假定被装饰对象只被该装饰器完全控制
    WeakReference<Object> weakReference = (WeakReference<Object>) delegate.getObject(key);
    if (weakReference != null) { // 取到了弱引用的句柄
      // 读取弱引用的对象
      result = weakReference.get();
      if (result == null) { // 弱引用的对象已经被清理
        // 直接删除该缓存
        delegate.removeObject(key);
      } else { // 弱引用的对象还存在
        // 将缓存的信息写入到强引用列表中，防止其被清理
        hardLinksToAvoidGarbageCollection.addFirst(result);
        if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) { // 强引用的对象数目超出限制
          // 从强引用的列表中删除该数据
          hardLinksToAvoidGarbageCollection.removeLast();
        }
      }
    }
    return result;
  }

  @Override
  public Object removeObject(Object key) {
    removeGarbageCollectedItems();
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    hardLinksToAvoidGarbageCollection.clear();
    removeGarbageCollectedItems();
    delegate.clear();
  }

  /**
   * 将值已经被JVM清理掉的缓存数据从缓存中删除
   */
  private void removeGarbageCollectedItems() {
    WeakEntry sv;
    while ((sv = (WeakEntry) queueOfGarbageCollectedEntries.poll()) != null) { // 轮询该垃圾回收队列
      // 将该队列中涉及的键删除
      delegate.removeObject(sv.key);
    }
  }

  private static class WeakEntry extends WeakReference<Object> {
    // 该变量不会被JVM清理掉，这里存储了目标对象的键
    private final Object key;

    private WeakEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
      super(value, garbageCollectionQueue);
      this.key = key;
    }
  }

}
