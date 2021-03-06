package com.fuse.cms;

import com.fuse.utils.Event;

public class AsyncOperationBase {
  protected boolean bInstantDispatch;
  protected boolean bDispatched, bDone, bSuccess, bExecuted;

  public Event<AsyncOperationBase> doneEvent;

  public AsyncOperationBase(){
      bDispatched = false;
      bDone = false;
      bSuccess = false;
      bExecuted = false;
      bInstantDispatch = true;
      doneEvent = new Event<>();
  }

  public AsyncOperationBase(boolean finishResult){
    this();
    this.finish(finishResult);
  }

  public AsyncOperationBase after(Runnable func){
    doneEvent.whenTriggered(func); if(bDispatched && isDone()) func.run(); return this; }

  public boolean isDone() { return bDone; }
  public boolean isSuccess() { return bSuccess; }
  public boolean isFailure() { return bExecuted && !bSuccess; }
  public boolean isExecuted() { return bExecuted; }
  public boolean isAborted() { return bDone && !bExecuted; }

  public void abort(){
      bDone = true;
      bExecuted = false;

      if(bInstantDispatch){
          dispatch();
      }
  }

  public void dispatch(){
    bDispatched = true;
    this.doneEvent.trigger(this);
  }

  public void finish(){
      finish(true);
  }

  public void finish(boolean success){
      bDone = true;
      bExecuted = true;
      bSuccess = success;

      if(bInstantDispatch){
          dispatch();
      }
  }

  public void setInstantDispatch(boolean newValue){
      bInstantDispatch = newValue;
  }

  // static factory methods

  public static AsyncOperationBase failure(){
    AsyncOperationBase op = new AsyncOperationBase(false);
    op.dispatch();
    return op;
  }
}
