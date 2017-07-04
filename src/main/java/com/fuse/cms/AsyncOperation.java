package com.fuse.cms;

import java.util.function.Consumer;
import com.fuse.utils.Event;

public class AsyncOperation<ItemType> {
    private boolean bInstantDispatch;
    private boolean bDispatched, bDone, bSuccess, bExecuted;
    private Consumer<AsyncOperation<ItemType>> doneFunc, successFunc, failureFunc, abortFunc, executedFunc, noResultFunc;
    private Consumer<Collection<ItemType>> resultFunc;
    private Consumer<ItemType> singleResultFunc;

    public Event<AsyncOperation<ItemType>> doneEvent;
    public Collection<ItemType> result;

    public AsyncOperation(){
        bDispatched = false;
        bDone = false;
        bSuccess = false;
        bExecuted = false;
        doneEvent = new Event<>();
        result = new Collection<>();
        bInstantDispatch = true;
    }

    public boolean isDone() { return bDone; }
    public boolean isSuccess() { return bSuccess; }
    public boolean isFailure() { return bExecuted && !bSuccess; }
    public boolean isExecuted() { return bExecuted; }
    public boolean isAborted() { return bDone && !bExecuted; }
    public boolean isNoResult(){ return bDone && result.isEmpty(); }

    public AsyncOperation<ItemType> whenDone(Consumer<AsyncOperation<ItemType>> func){
        doneFunc = func; if(bDispatched && isDone()) func.accept(this); return this;}
    public AsyncOperation<ItemType> onSuccess(Consumer<AsyncOperation<ItemType>> func){
        successFunc = func; if(bDispatched && isSuccess()) func.accept(this); return this; }
    public AsyncOperation<ItemType> onFailure(Consumer<AsyncOperation<ItemType>> func){
        failureFunc = func; if(bDispatched && isFailure()) func.accept(this); return this; }
    public AsyncOperation<ItemType> whenAborted(Consumer<AsyncOperation<ItemType>> func){
        abortFunc = func; if(bDispatched && isAborted()) func.accept(this); return this; }
    public AsyncOperation<ItemType> whenExecuted(Consumer<AsyncOperation<ItemType>> func){
        executedFunc = func; if(bDispatched && isExecuted()) func.accept(this); return this; }

    public AsyncOperation<ItemType> withResult(Consumer<Collection<ItemType>> func){
        resultFunc = func; if(bDispatched && isDone()) func.accept(this.result); return this; }

    public AsyncOperation<ItemType> withSingleResult(Consumer<ItemType> func){
        singleResultFunc = func;
        if(bDispatched && isDone() && func != null)
        for(ItemType item : result){
            func.accept(item);
        }

        return this;
    }

    public AsyncOperation<ItemType> whenNoResult(Consumer<AsyncOperation<ItemType>> func){
        noResultFunc = func; if(isNoResult()) func.accept(this); return this; }

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

        if(isAborted() && abortFunc != null)
            abortFunc.accept(this);

        if(isDone() && doneFunc != null)
            doneFunc.accept(this);

        if(isExecuted() && executedFunc != null)
            executedFunc.accept(this);

        if(isSuccess() && successFunc != null)
            successFunc.accept(this);

        if(isFailure() && failureFunc  != null)
            failureFunc.accept(this);

        if(isDone()){
            if(doneFunc != null)
                doneFunc.accept(this);

            doneEvent.trigger(this);

            if(resultFunc != null)
                resultFunc.accept(this.result);

            if(result.isEmpty() && noResultFunc != null)
                noResultFunc.accept(this);

            if(singleResultFunc != null){
                for(ItemType item : result){
                    singleResultFunc.accept(item);
                }
            }
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
