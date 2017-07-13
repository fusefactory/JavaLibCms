package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

import java.lang.Thread;

public class MapCollectionTest {
  // main routine invoked by test runner
  class Item{
    public int age;
    public Item(int age){ this.age = age; }
  }

  private List<AsyncOperation<Item>> operations;

  @Test public void setForKey_getForKey_removeKey(){
    operations = new ArrayList<AsyncOperation<Item>>();

    MapCollection<String, Item> col = new MapCollection<>();
    assertEquals(col.getForKey("bob"), null);
    col.setForKey("bob", new Item(45));
    assertEquals(col.getForKey("bob").age, 45);
    col.setForKey("bob", new Item(46));
    assertEquals(col.size(), 1);
    assertEquals(col.getForKey("bob").age, 46);
    col.removeKey("foobar"); // doesn't exist
    assertEquals(col.size(), 1);
    col.removeKey("bob");
    assertEquals(col.size(), 0);
  }

  @Test public void getAsync(){
    operations = new ArrayList<AsyncOperation<Item>>();

    List<AsyncOperation<Item>> collectionDoneOps = new ArrayList<>();

    MapCollection<String, Item> col = new MapCollection<>();
    col.asyncOperationDoneEvent.addListener((AsyncOperation<Item> opDone) -> {
      collectionDoneOps.add(opDone);
    });

    { // no content, no async loader registered
      AsyncOperation<Item> op = col.getAsync("").whenDone((AsyncOperation<Item> opDone) -> {
        operations.add(opDone);
      });

      assertEquals(op.isDone(), true);
      assertEquals(op.isAborted(), true);
      assertEquals(op.isExecuted(), false);
      assertEquals(op.isFailure(), false);
      assertEquals(op.isSuccess(), false);
      assertEquals(op.result.size(), 0); // operation resulted in zero items
      assertEquals(operations.size(), 1);
      assertEquals(operations.get(0), op);
      operations.clear();
      assertEquals(col.size(), 0);
      assertEquals(collectionDoneOps.size(), 1);
      collectionDoneOps.clear();
    }

    col.setAsyncLoader((String name, AsyncOperation<Item> op) -> {
      // just create one with age value of three
      Item newItem = new Item(3);
      op.add(newItem);
      op.finish();
    });

    { // new loader creates items with specified name and age zero
      AsyncOperation<Item> op = col.getAsync("bobby").whenDone((AsyncOperation<Item> opDone) -> {
        operations.add(opDone);
      });

      assertEquals(op.isDone(), true);
      assertEquals(op.isAborted(), false);
      assertEquals(op.isExecuted(), true); // executed
      assertEquals(op.isFailure(), false);
      assertEquals(op.isSuccess(), true);
      assertEquals(op.result.size(), 1); // operation resulted in one (new) item
      assertEquals(operations.size(), 1);
      assertEquals(operations.get(0), op);
      operations.clear();
      assertEquals(collectionDoneOps.size(), 1);
      collectionDoneOps.clear();
      assertEquals(col.size(), 1); // the operations new item is also added to our collection
      assertEquals(col.get(0).getValue(), op.result.get(0));
      // not async
      assertEquals(col.get(0).getKey().equals("bobby"), true);
      assertEquals(col.get(0).getValue().age, 3);
      assertEquals(col.getForKey("bobby").age, 3);
    }

    { // try getAsync on already loaded keyA
      List<AsyncOperation<Item>> abortedOps = new ArrayList<>();
      List<AsyncOperation<Item>> doneOps = new ArrayList<>();

      assertEquals(col.size(), 1);
      assertEquals(col.hasKey("bobby"), true);

      AsyncOperation<Item> op = col.getAsync("bobby")
      .whenDone((AsyncOperation<Item> opDone) -> {
        doneOps.add(opDone);
      })
      .whenAborted((AsyncOperation<Item> opAborted) -> {
        abortedOps.add(opAborted);
      });

      assertEquals(op.isDone(), true);
      assertEquals(op.isAborted(), true); // aborted
      assertEquals(op.isExecuted(), false);
      assertEquals(op.isFailure(), false);
      assertEquals(op.isSuccess(), false);
      assertEquals(op.result.size(), 1); // operation resulted in one (existing) item
      assertEquals(doneOps.size(), 1);
      assertEquals(abortedOps.size(), 1);
      assertEquals(col.size(), 1); // unchanged, nothing new added this operation
      assertEquals(collectionDoneOps.size(), 1);
      collectionDoneOps.clear();
    }
  }

  @Test public void setThreadedAsyncLoader(){
    operations = new ArrayList<AsyncOperation<Item>>();

    MapCollection<String, Item> col = new MapCollection<>();

    // no async loaded; will abort
    AsyncOperation<Item> op = col.getAsync("foobar");
    assertEquals(op.isDone(), true);
    assertEquals(op.isAborted(), true);
    assertEquals(col.size(), 0);

    // register threaded async loader
    col.setThreadedAsyncLoader((String name, AsyncOperation<Item> opAsync) -> {
      try{
        Thread.sleep(50);
      } catch(java.lang.InterruptedException exc){}

      // just create one with age value of three
      Item newItem = new Item(3);
      opAsync.add(newItem);
      opAsync.finish();
    });

    // register threaded value capture
    CompletableFuture<AsyncOperation<Item>> future = new CompletableFuture<>();
    op = col.getAsync("foobar").whenDone((AsyncOperation<Item> doneOp) -> {
      future.complete(doneOp);
    });

    // we gave th threaded loader a little delay, so it;s probably still running
    assertEquals(op.isDone(), false);

    try{
      assertEquals(future.get(), op);
    } catch(java.lang.InterruptedException exc){
      assertEquals("failure", "InterruptedException: "+exc.toString());
    } catch(java.util.concurrent.ExecutionException exc){
      assertEquals("failure", "ExecutaionException: "+exc.toString());
    }

    assertEquals(op.isDone(), true);
    assertEquals(op.isAborted(), false);
    assertEquals(op.isExecuted(), true);
    assertEquals(col.size(), 1);
    assertEquals(col.get(0).getKey().equals("foobar"), true);
  }

  @Test public void setAddAsyncLoadedResultsToCollection(){
    operations = new ArrayList<AsyncOperation<Item>>();
    MapCollection<Integer, Item> col = new MapCollection<>();

    col.setAsyncLoader((Integer no, AsyncOperation<Item> op) -> {
      // just create one with age value of three
      Item newItem = new Item(no);
      op.add(newItem);
      op.finish();
    });

    col.getAsync(501);
    assertEquals(col.size(), 1);
    assertEquals(col.get(0).getValue().age, 501);

    col.setAddAsyncLoadedResultsToCollection(false);
    AsyncOperation<Item> op = col.getAsync(123);
    assertEquals(col.size(), 1);
    assertEquals(col.get(0).getValue().age, 501);
    assertEquals(op.result.size(), 1);
    assertEquals(op.result.get(0).age, 123);
  }

  @Test public void setDispatchOnUpdate(){
    operations = new ArrayList<AsyncOperation<Item>>();

    MapCollection<Integer, Item> col = new MapCollection<>();

    col.setAsyncLoader((Integer no, AsyncOperation<Item> op) -> {
      // just create one with age value of three
      Item newItem = new Item(no);
      op.add(newItem);
      op.finish();
    });

    List<String> results = new ArrayList<String>();

    col.asyncOperationDoneEvent.addListener((AsyncOperation<Item> op) -> {
      results.add("asyncOperationDoneEvent: "+Integer.toString(op.result.get(0).age));
    });

    // perform async operation
    col.getAsync(501).withSingleResult((Item item) -> {
      results.add("withSingleResult: "+Integer.toString(item.age));
    });

    // both callbacks should be called
    assertEquals(results.size(), 2);
    assertEquals(results.get(0), "asyncOperationDoneEvent: 501");
    assertEquals(results.get(1), "withSingleResult: 501");

    // now switch to dispatch on update
    col.setDispatchOnUpdate(true);

    // let's do another async operation
    results.clear();
    col.getAsync(602).withSingleResult((Item item) -> {
      results.add("withSingleResult: "+Integer.toString(item.age));
    });

    // neither callbacks should be called
    if(!results.isEmpty()){
      System.out.println(results.get(0));
    }
    assertEquals(results.size(), 0);

    // perform update (this sohuld invoke callbacks)
    col.update();

    // NOW the callback should be triggerd
    assertEquals(results.size(), 2);
    assertEquals(results.get(0), "asyncOperationDoneEvent: 602");
    assertEquals(results.get(1), "withSingleResult: 602");
  }

  @Test public void setSyncLoader(){
    operations = new ArrayList<AsyncOperation<Item>>();

    MapCollection<String, Item> col = new MapCollection<>();
    // without loader, getForKey for missing key simply returns null...
    assertEquals(col.getForKey("101"), null);
    assertEquals(col.size(), 0);

    // register simple sync loader that tries to convert string into integer
    col.setSyncLoader((String str) -> {
      try {
        Integer number = Integer.parseInt(str);
        return new Item(number);
      } catch (java.lang.NumberFormatException exc){
      }

      return null;
    }, false /* don't create threaded async loader*/, true /* create non-threaded async loader */);

    // no try getForKey with missing 101 key again
    Item it = col.getForKey("101");
    // should return an item and that item should be added to the collection
    assertEquals(it == null, false);
    assertEquals(it.age, 101);
    assertEquals(col.size(), 1);
    // non integer strings still return null and add nothing to collection
    assertEquals(col.getForKey("ABC"), null);
    assertEquals(col.size(), 1);

    // an async loader should also be generated from the syncLoader
    AsyncOperation<Item> op = col.getAsync("202");
    assertEquals(op.isSuccess(), true);
    assertEquals(op.result.get(0).age, 202);
    assertEquals(col.size(), 2);
    assertEquals(col.get(1).getValue().age, 202);
  }

  @Test public void getAsync_withSimultanousIdenticalRequests(){
    MapCollection<String, Item> col = new MapCollection<>();
    List<String> strings = new ArrayList<>();
    List<Item> items = new ArrayList<>();
    CompletableFuture<String> future = new CompletableFuture<>();

    col.setThreadedAsyncLoader((String s, AsyncOperation<Item> op) -> {
      strings.add(s);

      // some artificial delay
      try{
        Thread.sleep(50);
      } catch(java.lang.InterruptedException exc){
      }

      op.add(new Item(1));
      op.finish();
      future.complete(s);
    });

    // fetch same item simultanous couple of times
    col.getAsync("10").withSingleResult((Item it) -> items.add(it));
    col.getAsync("10").withSingleResult((Item it) -> items.add(it));
    col.getAsync("10").withSingleResult((Item it) -> items.add(it));

    try{
      assertEquals(future.get(), "10");
    } catch(java.lang.InterruptedException exc){
      assertEquals("failure", "InterruptedException: "+exc.toString());
    } catch(java.util.concurrent.ExecutionException exc){
      assertEquals("failure", "ExecutionException: "+exc.toString());
    }

    assertEquals(strings.size(), 1);
    assertEquals(strings.get(0), "10");
    assertEquals(items.size(), 3);
  }
}
