package com.fuse.cms;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.lang.Runnable;

import com.fuse.utils.Event;
import com.fuse.utils.Test;

class ColMod<T> {
  public T addItem;
  public Object removeItem;
  public boolean clear;
  public int removeIndex;

  public ColMod(){ removeIndex = -1; }
}

public class CollectionBase<T> extends ArrayList<T> {
  private int lockCount;
  private List<ColMod<T>> modQueue;
  /** null by default but can be set by caller using setInstantiator to be able to use the create method */
  private Supplier<T> instantiatorFunc;
  public Event<T> addEvent;
  public Event<T> removeEvent;
  public Test<T> beforeAddTest;

  public CollectionBase(){
    lockCount = 0;
    modQueue = new ArrayList<ColMod<T>>();
    addEvent = new Event<T>();
    removeEvent = new Event<T>();
    beforeAddTest = new Test<T>();
  }

  public boolean add(T item){
    if(isLocked()){
      ColMod<T> m = new ColMod<T>();
      m.addItem = item;
      modQueue.add(m);
      return false;
    }

    if(!beforeAddTest.test(item))
      return false;

    boolean result = super.add(item);
    addEvent.trigger(item);
    return result;
  }

  public T remove(int idx){
    if(isLocked()){
      ColMod<T> m = new ColMod<T>();
      m.removeIndex = idx;
      modQueue.add(m);
      return null;
    }

    removeEvent.trigger(get(idx));
    return super.remove(idx);
  }

  public boolean remove(Object item){
    if(isLocked()){
      ColMod<T> m = new ColMod<T>();
      m.removeItem = item;
      modQueue.add(m);
      return false;
    }

    removeEvent.trigger((T)item);
    return super.remove(item);
  }

  public void clear(){
    if(isLocked()){
      ColMod<T> m = new ColMod<T>();
      m.clear = true;
      modQueue.add(m);
      return;
    }

    for(int i=size()-1; i>=0; i--)
      remove(get(i));
  }

  public void each(Consumer<T> func){
    // "lock" our list from modifications so we can safely iterate over it
    // this way the logic in out func can safely call modificating methods
    // (like add() add remove()) without causing errors; the modification will
    // be queued and processed after we finished iterating and list is unlocked
    lock(() -> {
      Iterator it = iterator();
      while(it.hasNext()){
        T item = (T)it.next();
        func.accept(item);
      }
    });
  }

  public void eachWithIndex(BiConsumer<T, Integer> func){
    // "lock" our list from modifications so we can safely iterate over it
    // this way the logic in out func can safely call modificating methods
    // (like add() add remove()) without causing errors; the modification will
    // be queued and processed after we finished iterating and list is unlocked
    lock(() -> {
      Iterator it = iterator();
      int idx=0;
      while(it.hasNext()){
        T item = (T)it.next();
        func.accept(item, idx);
        idx += 1;
      }
    });
  }

  public boolean isLocked(){
    return lockCount > 0;
  }

  private void lock(Runnable func){
    beginLock();
      func.run();
    endLock();
  }

  private void beginLock(){
    lockCount++;
  }

  private void endLock(){
    lockCount--;

    // still locked (this was a nested lock)? nothing more to do
    if(isLocked())
      return;

    // process queue of modifications that build up during the lock
    for(ColMod<T> m : modQueue){
      if(m.addItem != null)
        add(m.addItem);
      if(m.removeItem != null)
        remove(m.removeItem);
      if(m.removeIndex != -1)
        remove(m.removeIndex);
      if(m.clear)
        clear();
    }

    modQueue.clear();
  }

  public void add(int index, T element){
    System.out.println("TODO CollectionBase.add(int, T) might affect collection without triggering events");
    super.add(index, element);
  }

  public boolean	addAll(Collection<? extends T> c){
    System.out.println("TODO CollectionBase.addAll might affect collection without triggering events");
    return super.addAll(c);
  }

  public boolean	addAll(int index, Collection<? extends T> c){
    System.out.println("TODO CollectionBase.addAll might affect collection without triggering events");
    return super.addAll(index, c);
  }

  public boolean	removeAll(Collection<?> c){
    System.out.println("TODO CollectionBase.removeAll might affect collection without triggering events");
    return super.removeAll(c);
  }

  public void removeRange(int fromIndex, int toIndex){
    System.out.println("TODO CollectionBase.removeRange might affect collection without triggering events");
    super.removeRange(fromIndex, toIndex);
  }

  public T set(int index, T element){
    System.out.println("TODO CollectionBase.set might affect collection without triggering events");
    return super.set(index, element);
  }

  T findFirst(Predicate<T> predicate){
    T resultItem = null;

    beginLock();
    {
      for(T item : this){
        if(predicate.test(item)){
          resultItem = item;
          break;
        }
      }
    }
    endLock();

    return resultItem;
  }

  /**
   * Gives the collection an instantiator which enables the use of the create method
   *
   * @param func The instantiator logic
   */
  public void setInstantiator(Supplier<T> func){
    this.instantiatorFunc = func;
  }

  /**
   * Creates an instance of the collection's item type, adds it to this collection and returns it
   *
   * @return The created instance
   */
  public T create(){
    if(this.instantiatorFunc == null)
      return null;
    T newInstance = this.instantiatorFunc.get();
    this.add(newInstance);
    return newInstance;
  }
}
