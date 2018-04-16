package com.fuse.cms;

import java.util.IdentityHashMap;

public class ResourceManager<K, V> extends IdentityHashMap<K, V> {
  public V get(K key, boolean createIfNecessary){
    for(K k : keySet()){
      if(compareFunc(k, key))
        return super.get(key);
    }

     V result = null;

    if(result == null && createIfNecessary){
      result = createFunc(key);
      if(result != null)
        this.put(key, result);
    }

    return result;
  }

  private boolean compareFunc(K a, K b){
    return a == b;
  }

  private V createFunc(K key){
    return null;
  }
}
