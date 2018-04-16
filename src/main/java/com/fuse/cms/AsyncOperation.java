package com.fuse.cms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fuse.utils.Event;

public class AsyncOperation<ItemType> extends AsyncOperationBase {

    public Event<AsyncOperation<ItemType>> successEvent, failureEvent, abortEvent, executedEvent, noResultEvent;
    public Event<List<ItemType>> resultEvent;
    public Event<ItemType> singleResultEvent;

    public List<ItemType> result;

    public AsyncOperation(){
        super();
        successEvent = new Event<>();
        failureEvent = new Event<>();
        abortEvent = new Event<>();
        executedEvent = new Event<>();
        noResultEvent = new Event<>();
        resultEvent = new Event<>();
        singleResultEvent = new Event<>();
        result = new ArrayList<>();
    }

    public AsyncOperation(boolean result){
      this();
      this.finish(result);
    }

    public boolean isNoResult(){ return bDone && result.isEmpty(); }

    public AsyncOperation<ItemType> whenDone(Consumer<AsyncOperation<ItemType>> func){
        doneEvent.addListener((AsyncOperationBase base) -> func.accept(this)); if(bDispatched && isDone()) func.accept(this); return this;}
    public AsyncOperation<ItemType> whenDone(Runnable func){
        doneEvent.whenTriggered(func); if(bDispatched && isDone()) func.run(); return this;}

    public AsyncOperation<ItemType> onSuccess(Consumer<AsyncOperation<ItemType>> func){
        successEvent.addListener(func); if(bDispatched && isSuccess()) func.accept(this); return this; }
    public AsyncOperation<ItemType> onSuccess(Runnable func){
        successEvent.whenTriggered(func); if(bDispatched && isSuccess()) func.run(); return this; }

    public AsyncOperation<ItemType> onFailure(Consumer<AsyncOperation<ItemType>> func){
        failureEvent.addListener(func); if(bDispatched && isFailure()) func.accept(this); return this; }
    public AsyncOperation<ItemType> onFailure(Runnable func){
        failureEvent.whenTriggered(func); if(bDispatched && isFailure()) func.run(); return this; }

    public AsyncOperation<ItemType> whenAborted(Consumer<AsyncOperation<ItemType>> func){
        abortEvent.addListener(func); if(bDispatched && isAborted()) func.accept(this); return this; }

    public AsyncOperation<ItemType> whenExecuted(Consumer<AsyncOperation<ItemType>> func){
        executedEvent.addListener(func); if(bDispatched && isExecuted()) func.accept(this); return this; }

    public AsyncOperation<ItemType> whenNoResult(Consumer<AsyncOperation<ItemType>> func){
        noResultEvent.addListener(func); if(isNoResult()) func.accept(this); return this; }
    public AsyncOperation<ItemType> whenNoResult(Runnable func){
        noResultEvent.whenTriggered(func); if(isNoResult()) func.run(); return this; }

    public AsyncOperation<ItemType> withResult(Consumer<List<ItemType>> func){
        resultEvent.addListener(func); if(bDispatched && isDone()) func.accept(this.result); return this; }

    public AsyncOperation<ItemType> withSingleResult(Consumer<ItemType> func){
        singleResultEvent.addListener(func);
        if(bDispatched && isDone())
            for(ItemType item : result)
                func.accept(item);
        return this;
    }

    public void add(ItemType item){
        result.add(item);
    }

    @Override
    public void dispatch(){
        super.dispatch();

        if(isAborted())
            abortEvent.trigger(this);

        if(isExecuted())
            executedEvent.trigger(this);

        if(isSuccess())
            successEvent.trigger(this);

        if(isFailure())
            failureEvent.trigger(this);

        if(isDone()){
            resultEvent.trigger(this.result);

            if(result.isEmpty())
                noResultEvent.trigger(this);

            for(ItemType item : result)
                singleResultEvent.trigger(item);
        }
    }
}
