package com.fuse.cms;

import java.util.function.Function;
import java.lang.ref.WeakReference;

public class WeakRefMapCollection<K, V> extends MapCollection<K, WeakReference<V>> {

  public void setSyncInstanceLoader(Function<K, V> instanceLoader){
      setSyncInstanceLoader(instanceLoader, true, false);
  }

  public void setSyncInstanceLoader(Function<K, V> instanceLoader, boolean createThreadedAsyncLoader, boolean createRegularAsyncLoader){
    Function<K, WeakReference<V>> wrapperFunc = (K key) -> {
        V result = instanceLoader.apply(key);

        if(result == null)
            return null;

        return new WeakReference<V>(result);
    };

    setSyncLoader(wrapperFunc, createThreadedAsyncLoader, createRegularAsyncLoader);
  }

  public V getInstance(K key){
    WeakReference<V> weakRef = getForKey(key);

    if(weakRef == null)
        return null;

    return weakRef.get();
  }
}
