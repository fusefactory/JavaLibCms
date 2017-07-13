package com.fuse.cms;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.function.Function;
import java.util.function.BiConsumer;

import com.fuse.utils.Event;

public class MapCollection<K, V> extends Collection<Map.Entry<K,V>> {

    // attributes

    private boolean bAddAsyncLoadedResultsToCollection;
    private boolean bDispatchOnUpdate;
    private Function<K, V> syncLoader;
    private BiConsumer<K, AsyncOperation<V>> asyncLoader;
    private MapCollection<K, AsyncOperation<V>> activeAsyncOperations;

    // events

    public Event<AsyncOperation<V>> asyncOperationDoneEvent;

    // methods

    public MapCollection(){
        syncLoader = null;
        asyncLoader = null;
        asyncOperationDoneEvent = new Event<>();
        bAddAsyncLoadedResultsToCollection = true;
        bDispatchOnUpdate = false;
        activeAsyncOperations = null;
    }

    public void update(){
        if(bDispatchOnUpdate && activeAsyncOperations != null){
            activeAsyncOperations.each((Map.Entry<K, AsyncOperation<V>> pair) -> {
                AsyncOperation<V> op = pair.getValue();
                if(op.isDone()){
                    op.dispatch(); // this will remove is from activeAsyncOperations
                }
            });
        }
    }

    boolean hasKey(K key){
        return getForKey(key) != null;
    }

    public V getForKey(K key){
        return getForKey(key, true);
    }

    public V getForKey(K key, boolean useSyncLoader){
        Map.Entry<K, V> foundEntry = findFirst((Map.Entry<K, V> entry) -> {
            return compareKeys(key, entry.getKey());
        });

        if(foundEntry != null)
            return foundEntry.getValue();

        if(useSyncLoader && syncLoader != null){
            V val = syncLoader.apply(key);
            if(val != null){
                setForKey(key, val);
                return val;
            }
        }

        return null;
    }

    public void removeKey(K key){
        Map.Entry<K, V> foundEntry = findFirst((Map.Entry<K, V> entry) -> {
            return compareKeys(key, entry.getKey());
        });

        if(foundEntry != null)
            this.remove(foundEntry);
    }

    public AsyncOperation<V> getAsync(K key){
        if(activeAsyncOperations != null && activeAsyncOperations.hasKey(key))
            return activeAsyncOperations.getForKey(key);

        // first see if there are any active asyncoperations for the same key
        AsyncOperation<V> op = new AsyncOperation<V>();
        op.setInstantDispatch(!bDispatchOnUpdate);

        // could not initialize in constructor, because
        // if a MapCollection initializes another MapCollection in its contstructor
        // you get an infinite recursive loop, so create activeAsyncOperations map here
        // if necessary
        if(activeAsyncOperations == null)
            activeAsyncOperations = new MapCollection<>();

        activeAsyncOperations.setForKey(key, op);

        op.doneEvent.addListener((AsyncOperation<V> doneOp) -> {
            this.asyncOperationDoneEvent.trigger(doneOp);
            activeAsyncOperations.removeKey(key);
        });

        V cachedItem = this.getForKey(key, false);

        if(cachedItem != null){
            op.add(cachedItem);
            op.abort();
        }

        if(op.isDone())
            return op;

        {   // copy all items added to the operation's result collection to our collection
            op.result.addEvent.addListener((V newItem) -> {
                if(this.bAddAsyncLoadedResultsToCollection)
                    this.setForKey(key, newItem);
            });
        }

        if(this.asyncLoader != null){
            this.asyncLoader.accept(key, op);
        } else {
            op.abort();
        }

        return op;
    }

    public void setForKey(K key, V value){
        removeKey(key); // remove existing
        Map.Entry<K,V> entry = new AbstractMap.SimpleEntry<>(key, value);
        add(entry);
    }

    public void setAsyncLoader(BiConsumer<K, AsyncOperation<V>> newLoader){
        asyncLoader = newLoader;
    }

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

    public BiConsumer<K, AsyncOperation<V>> convertToAsync(Function<K, V> func){
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

    public boolean compareKeys(K keyA, K keyB){
        // check for any custom registered compare filterFuncs
        return defaultKeyComparator(keyA, keyB);
    }

    public boolean defaultKeyComparator(K keyA, K keyB){
        return keyA.equals(keyB);
    }

    public void setAddAsyncLoadedResultsToCollection(boolean newValue){
        bAddAsyncLoadedResultsToCollection = newValue;
    }

    public void setDispatchOnUpdate(boolean value){
        bDispatchOnUpdate = value;
    }
};
