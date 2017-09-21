package com.fuse.cms;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.BiConsumer;

import com.fuse.utils.Event;

public class AsyncFacade<K, V>/* extends Collection<Map.Entry<K,V>> */{

    private boolean bDispatchOnUpdate = false;
    private boolean bRecycleActiveOperations = true;
    private Function<K, V> syncLoader = null;
    private Function<K, List<V>> syncListLoader = null;
    private BiConsumer<K, AsyncOperation<V>> asyncLoader;
    private Map<K, AsyncOperation<V>> activeAsyncOperations = null;
    private Integer threadPriority = null;

    public Event<AsyncOperation<V>> asyncOperationDoneEvent;


    public AsyncFacade(){
        asyncOperationDoneEvent = new Event<>();
    }

    public void update(){
        if(bDispatchOnUpdate && activeAsyncOperations != null){
            Object[] keys = activeAsyncOperations.keySet().toArray();
            for(int i=keys.length-1; i>=0; i--){
                AsyncOperation<V> op = activeAsyncOperations.get(keys[i]);
                if(op!=null && op.isDone()){
                    op.dispatch(); // this will remove is from activeAsyncOperations
                }
            }
        }
    }

    public V getSync(K key){
        if(this.syncLoader != null)
            return syncLoader.apply(key);

        if(this.syncListLoader != null) {
        	List<V> list = syncListLoader.apply(key);
        	return list.get(0);
        }
        
        return null;
    }
    
    public List<V> getSyncList(K key){
    	List<V> result = new ArrayList<>();

        if(this.syncListLoader != null) {
        	List<V> list = syncListLoader.apply(key);
        	if(list != null)
        		result.addAll(list);

        } else if(this.syncLoader != null) {
            V item = syncLoader.apply(key);
            if(item!=null)
            	result.add(item);
        }

        return result;
    }

    public AsyncOperation<V> getAsync(K key){
        // first see if there are any active asyncoperations for the same key
        if(bRecycleActiveOperations && activeAsyncOperations != null && activeAsyncOperations.containsKey(key))
            return activeAsyncOperations.get(key);

        AsyncOperation<V> op;
        op = new AsyncOperation<V>();
        op.setInstantDispatch(!bDispatchOnUpdate);

        // could not initialize in constructor, because
        // if a MapCollection initializes another MapCollection in its contstructor
        // you get an infinite recursive loop, so create activeAsyncOperations map here
        // if necessary
        if(activeAsyncOperations == null)
            activeAsyncOperations = new HashMap<>();

        activeAsyncOperations.put(key, op);

        op.doneEvent.addListener((AsyncOperation<V> doneOp) -> {
            this.asyncOperationDoneEvent.trigger(doneOp);
            activeAsyncOperations.remove(key);
        });

        // CACHING; currently no caching mechanism implemented for AsyncFacade
        // V cachedItem = this.getForKey(key, false);
        // if(cachedItem != null){
        //     op.add(cachedItem);
        //     op.abort();
        // }
        // if(op.isDone())
        //     return op;

        if(this.asyncLoader != null){
            this.asyncLoader.accept(key, op);
        } else {
            op.abort();
        }

        return op;
    }

    public void setAsyncLoader(BiConsumer<K, AsyncOperation<V>> newLoader){
        asyncLoader = newLoader;
    }

    /**
     * Conveniene method that wraps the given async loader in a theaded runner
     * @param newLoader The (non-threaded) async loader logic
     */
    public void setThreadedAsyncLoader(BiConsumer<K, AsyncOperation<V>> newLoader){
        // create wrapping lambda which creates a threaded runner
        setAsyncLoader((K key, AsyncOperation<V> op) -> {
            // create thread to run the given loader in
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run(){
                    // all the thread does is run the loader
                    newLoader.accept(key, op);
                }
            });

            if(this.threadPriority != null)
                thread.setPriority(this.threadPriority);

            thread.start();
        });
    }

    public void setSyncLoader(Function<K, V> syncLoader){
        setSyncLoader(syncLoader, true, false);
    }

    public void setSyncLoader(Function<K, V> syncLoader, boolean createThreadedAsyncLoader, boolean createRegularAsyncLoader){
        this.syncLoader = syncLoader;

        if(createThreadedAsyncLoader){
            setThreadedAsyncLoader(convertToAsync(syncLoader));
            return; // don't continue with creating a non-threaded asyncLoader (which would overwrite the threaded async loader)
        }

        // create loader as non-threaded asyncLoader (maybe caller has already implemented a threading mechanism?)
        setAsyncLoader(convertToAsync(syncLoader));
    }

    private BiConsumer<K, AsyncOperation<V>> convertToAsync(Function<K, V> func){
        return (K key, AsyncOperation<V> op) -> {
            // get "result" using sync loader
            V result = func.apply(key);

            // if result is not null (which should be returned to indicate failure),
            // add it to our operation's result
            if(result != null){
                op.add(result);
            }

            // finalize async operation and indicate if it was a success
            op.finish(result != null);
        };
    }

    public void setSyncLoaderList(Function<K, List<V>> syncLoader){
        setSyncLoaderList(syncLoader, true, false);
    }

    public void setSyncLoaderList(Function<K, List<V>> syncLoader, boolean createThreadedAsyncLoader, boolean createRegularAsyncLoader){
    	this.syncListLoader = syncLoader;

        this.syncLoader = ((K key) -> {
        	List<V> result = syncLoader.apply(key);
        	return (result == null || result.size() < 1) ? null : result.get(0);
        });

        if(createThreadedAsyncLoader){
            setThreadedAsyncLoader(convertToAsyncList(syncLoader));
            return; // don't continue with creating a non-threaded asyncLoader (which would overwrite the threaded async loader)
        }

        // create loader as non-threaded asyncLoader (maybe caller has already implemented a threading mechanism?)
        setAsyncLoader(convertToAsyncList(syncLoader));
    }

    private BiConsumer<K, AsyncOperation<V>> convertToAsyncList(Function<K, List<V>> func){
        return (K key, AsyncOperation<V> op) -> {
            // get "result" using sync loader
            List<V> result = func.apply(key);

            // if result is not null (which should be returned to indicate failure),
            // add it to our operation's result
            if(result != null){
              for(V item : result)
                op.add(item);
            }

            // finalize async operation and indicate if it was a success
            op.finish(result != null);
        };
    }

    public void setDispatchOnUpdate(boolean value){
        bDispatchOnUpdate = value;
    }

    public boolean getDispatchOnUpdate(){
        return bDispatchOnUpdate;
    }

    public void setThreadPriority(Integer newPrio){
        this.threadPriority = newPrio;
    }

    public Integer getThreadPriority(){
        return this.threadPriority;
    }

    public boolean getRecycleActiveOperations() {
    	return bRecycleActiveOperations;
    }

    public void setRecycleActiveOperations(boolean enable) {
    	bRecycleActiveOperations = enable;
    }
};
