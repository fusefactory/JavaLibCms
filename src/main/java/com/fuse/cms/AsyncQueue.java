package com.fuse.cms;

import java.util.function.Supplier;
import java.util.concurrent.ConcurrentLinkedDeque;
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

public class AsyncQueue extends ConcurrentLinkedDeque<Item> {

  private boolean bDispatchOnUpdate = true;
  private AsyncOperationBase activeOperation = null;
  private Runnable startNextCallback = null;

  public Event<Item> startEvent = new Event<>();

  public void destroy(){
    this.clear();
    this.startEvent.destroy();
  }

  @Override public boolean add(Item item){
    // cope all our items into a temporary queue
    ConcurrentLinkedDeque<Item> tmp = new ConcurrentLinkedDeque<>();
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

    if(!this.bDispatchOnUpdate) // we'll just run update ourselves
      this.update();

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

  public boolean addFirst(Supplier<AsyncOperationBase> func){
    Item it = this.peek();
    if(it != null)
      return this.add(func, it.priority+1);
    return this.add(func);
  }

  public boolean addLast(Supplier<AsyncOperationBase> func){
    Item it = this.peekLast();
    if(it != null)
      return this.add(func, it.priority-1);
    return this.add(func);
  }

  /**
   * Process queue. When empty does nothing.
   * @return true when a new item was initiated.
   */
  public boolean update(){
    // an operation currently active?
    if(this.activeOperation != null) {
      // current active operation done?
      if(this.activeOperation.isDone())
        // cleanup
        this.activeOperation = null;
    }

    return this.startNext();
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

  public void setDispatchOnUpdate(boolean dispatchOnUpdate){
    boolean change = (dispatchOnUpdate != this.bDispatchOnUpdate);
    this.bDispatchOnUpdate = dispatchOnUpdate;

    if(change && !this.bDispatchOnUpdate){
      // lazy-initialize our onFinish callback
      if(this.startNextCallback == null) {
        this.startNextCallback = () -> {
          this.activeOperation = null;

          // check if the queue hasn't been changed to dispatch-on-update in the mean time
          if(!this.bDispatchOnUpdate)
            this.startNext();
        };
      }

      // if there is a currently active operation, register our onFinish callback on it
      if(this.activeOperation != null)
        this.activeOperation.after(this.startNextCallback);
      else
         this.startNext();
    }
  }


  private boolean startNext(){
    boolean result = false;

    // no current active operation; initiate next queued operation (if any)
    while(this.activeOperation == null && !this.isEmpty()) {
      // look for next queued operation
      Item item = this.poll();

      if(item != null) {
        // func.get() executes the logic that provides an AsyncOperation
        this.activeOperation = item.func.get();
        if(!this.bDispatchOnUpdate) {
          this.activeOperation.after(this.startNextCallback);
        }

        this.startEvent.trigger(item);

        result = true;
      }
    }

    return result;
  }
}
