package com.fuse.cms;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.function.BiConsumer;

import com.fuse.utils.Event;

public class MapCollection<K, V> extends Collection<Map.Entry<K,V>> {

    // attributes

    private boolean bAddAsyncLoadedResultsToCollection;
    private boolean bDispatchOnUpdate;
    private BiConsumer<K, AsyncOperation<V>> asyncLoader;
    private List<AsyncOperation<V>> activeAsyncOperations;

    // events

    public Event<AsyncOperation<V>> asyncOperationDoneEvent;

    // methods

    public MapCollection(){
        asyncLoader = null;
        asyncOperationDoneEvent = new Event<>();
        bAddAsyncLoadedResultsToCollection = true;
        bDispatchOnUpdate = false;
        activeAsyncOperations = new ArrayList<>();
    }

    public void update(){
        if(bDispatchOnUpdate){
            // use reverse loop an NOT the for(AsyncOperation instance : activeAsyncOperations) syntax
            // because a callback will remove the dispatched operation from activeAsyncOperations
            for(int i=activeAsyncOperations.size()-1; i>=0; i--){
                AsyncOperation<V> op = activeAsyncOperations.get(i);
                if(op.isDone()){
                    op.dispatch(); // this will remove is from activeAsyncOperations
                }
            }
        }
    }

    boolean hasKey(K key){
        return getForKey(key) != null;
    }

    public V getForKey(K key){
        Map.Entry<K, V> foundEntry = findFirst((Map.Entry<K, V> entry) -> {
            return compareKeys(key, entry.getKey());
        });

        if(foundEntry != null)
            return foundEntry.getValue();

        return null;
    }

    public AsyncOperation<V> getAsync(K key){
        AsyncOperation<V> op = new AsyncOperation<V>();
        op.setInstantDispatch(!bDispatchOnUpdate);
        activeAsyncOperations.add(op);

        op.doneEvent.addListener((AsyncOperation<V> doneOp) -> {
            this.asyncOperationDoneEvent.trigger(doneOp);
            activeAsyncOperations.remove(doneOp);
        });

        V cachedItem = this.getForKey(key);

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
