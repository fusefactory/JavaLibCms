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

    private boolean bDispatchOnUpdate;
    private Function<K, V> syncLoader;
    private BiConsumer<K, AsyncOperation<V>> asyncLoader;
    // TODO; turn into normal list, so we have no circular dependencies between AsyncFacade and Collection class
    private Map<K, AsyncOperation<V>> activeAsyncOperations;
    private MapCollection<K, V> usedMapCollection;
    public Event<AsyncOperation<V>> asyncOperationDoneEvent;

    public AsyncFacade(){
        syncLoader = null;
        asyncLoader = null;
        asyncOperationDoneEvent = new Event<>();
        bDispatchOnUpdate = false;
        activeAsyncOperations = null;
        usedMapCollection = null;
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
        if(usedMapCollection != null){
            return usedMapCollection.getForKey(key);
        }

        if(syncLoader == null)
            return null;
        return syncLoader.apply(key);
    }

    public AsyncOperation<V> getAsync(K key){
        // first see if there are any active asyncoperations for the same key
        if(activeAsyncOperations != null && activeAsyncOperations.containsKey(key))
            return activeAsyncOperations.get(key);

        AsyncOperation<V> op;
        // if we're using a map collection as "backend", it will provide the AsyncOperations
        if(usedMapCollection != null){
            op = usedMapCollection.getAsync(key);
        } else {
            op = new AsyncOperation<V>();
            op.setInstantDispatch(!bDispatchOnUpdate);
        }

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

        // the map collection will perform logic of completing the operation
        if(usedMapCollection != null){
            return op;
        }

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

    public void setDispatchOnUpdate(boolean value){
        bDispatchOnUpdate = value;
    }

    public void use(MapCollection map){
        usedMapCollection = map;
    }
};
