package com.fuse.cms;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import com.fuse.utils.Event;

public class AsyncOperation<ItemType> {
    private boolean bInstantDispatch;
    private boolean bDispatched, bDone, bSuccess, bExecuted;

    public Event<AsyncOperation<ItemType>> doneEvent, successEvent, failureEvent, abortEvent, executedEvent, noResultEvent;
    public Event<List<ItemType>> resultEvent;
    public Event<ItemType> singleResultEvent;

    public List<ItemType> result;

    public AsyncOperation(){
        bDispatched = false;
        bDone = false;
        bSuccess = false;
        bExecuted = false;
        doneEvent = new Event<>();
        successEvent = new Event<>();
        failureEvent = new Event<>();
        abortEvent = new Event<>();
        executedEvent = new Event<>();
        noResultEvent = new Event<>();
        resultEvent = new Event<>();
        singleResultEvent = new Event<>();
        result = new ArrayList<>();
        bInstantDispatch = true;
    }

    public boolean isDone() { return bDone; }
    public boolean isSuccess() { return bSuccess; }
    public boolean isFailure() { return bExecuted && !bSuccess; }
    public boolean isExecuted() { return bExecuted; }
    public boolean isAborted() { return bDone && !bExecuted; }
    public boolean isNoResult(){ return bDone && result.isEmpty(); }

    public AsyncOperation<ItemType> whenDone(Consumer<AsyncOperation<ItemType>> func){
        doneEvent.addListener(func); if(bDispatched && isDone()) func.accept(this); return this;}
    public AsyncOperation<ItemType> onSuccess(Consumer<AsyncOperation<ItemType>> func){
        successEvent.addListener(func); if(bDispatched && isSuccess()) func.accept(this); return this; }
    public AsyncOperation<ItemType> onFailure(Consumer<AsyncOperation<ItemType>> func){
        failureEvent.addListener(func); if(bDispatched && isFailure()) func.accept(this); return this; }
    public AsyncOperation<ItemType> whenAborted(Consumer<AsyncOperation<ItemType>> func){
        abortEvent.addListener(func); if(bDispatched && isAborted()) func.accept(this); return this; }
    public AsyncOperation<ItemType> whenExecuted(Consumer<AsyncOperation<ItemType>> func){
        executedEvent.addListener(func); if(bDispatched && isExecuted()) func.accept(this); return this; }
    public AsyncOperation<ItemType> withResult(Consumer<List<ItemType>> func){
        resultEvent.addListener(func); if(bDispatched && isDone()) func.accept(this.result); return this; }
    public AsyncOperation<ItemType> whenNoResult(Consumer<AsyncOperation<ItemType>> func){
        noResultEvent.addListener(func); if(isNoResult()) func.accept(this); return this; }

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

    public void abort(){
        bDone = true;
        bExecuted = false;

        if(bInstantDispatch){
            dispatch();
        }
    }

    public void dispatch(){
        bDispatched = true;

        if(isAborted())
            abortEvent.trigger(this);

        if(isExecuted())
            executedEvent.trigger(this);

        if(isSuccess())
            successEvent.trigger(this);

        if(isFailure())
            failureEvent.trigger(this);

        if(isDone()){
            doneEvent.trigger(this);
            resultEvent.trigger(this.result);

            if(result.isEmpty())
                noResultEvent.trigger(this);

            for(ItemType item : result)
                singleResultEvent.trigger(item);
        }
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
}
