package com.fuse.cms;

import java.util.function.Supplier;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.fuse.utils.Event;

class Item {
  public Supplier<AsyncOperationBase> func;
  public float priority = 0.0f;

  public Item(Supplier<AsyncOperationBase> f){
    this.func = f;
  }

  public Item(Supplier<AsyncOperationBase> f, float prio) {
    this.func = f;
    this.priority = prio;
  }
}

public class AsyncQueue extends ConcurrentLinkedQueue<Item> {

  private AsyncOperationBase activeOperation = null;

  @Override public boolean add(Item item){
    // cope all our items into a temporary queue
    ConcurrentLinkedQueue<Item> tmp = new ConcurrentLinkedQueue<>();
    tmp.addAll(this);
    this.clear();

    Item it;

    // add all items from original queue that have a higher or equal priority
    while(true) {
      it = tmp.poll();
      if(it == null || it.priority < item.priority) break;
      super.add(it);
    }

    // add our new item
    boolean result = super.add(item);

    // add all remaining items with lower priority
    while(it != null) {
      super.add(it);
      it = tmp.poll();
    }

    return result;
  }

  /**
   * @param func operation logic to be added to the queue with default priority (0.0f)
   * @return true
   */
  public boolean add(Supplier<AsyncOperationBase> func){
    return this.add(new Item(func));
  }

  /**
   * @param func; operation logic to be added to the queue
   * @param priority; self-explanatory
   * @return true
   */
  public boolean add(Supplier<AsyncOperationBase> func, float priority){
    return this.add(new Item(func, priority));
  }

  /**
   * Process queue. When empty does nothing.
   * @return true when a new item was initiated.
   */
  public boolean update(){
    boolean result = false;

    // an operation currently active?
    if(this.activeOperation != null) {
      // current active operation done?
      if(this.activeOperation.isDone())
        // cleanup
        this.activeOperation = null;
    }

    // no current active operation; initiate next queued operation (if any)
    while(this.activeOperation == null && !this.isEmpty()) {
      // look for next queued operation
      Item item = this.poll();

      if(item != null) {
        // func.get() executes the logic that provides an AsyncOperation and returns it to us
        this.activeOperation = item.func.get();
        result = true;
      }
    }

    return result;
  }

  public boolean remove(Supplier<AsyncOperationBase> func){
    Item item = null;

    for(Item it : this){
      if(it.func == func) {
        item = it;
        break;
      }
    }

    return (item != null) ? this.remove(item) : false;
  }
}
