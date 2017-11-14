package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Ignore;

import com.fuse.utils.Event;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

public class AsyncQueueTest {
  @Test public void size(){
    AsyncQueue q = new AsyncQueue();
    q.add(() -> new AsyncOperationBase(false));
    q.add(() -> new AsyncOperationBase(false));
    assertEquals(q.size(), 2);
    assertEquals(q.isEmpty(), false);
    q.update();
    assertEquals(q.size(), 1);
    assertEquals(q.isEmpty(), false);
    q.update();
    assertEquals(q.size(), 0);
    assertEquals(q.isEmpty(), true);
  }

  @Test public void update(){
    AsyncQueue q = new AsyncQueue();
    Event<Void> finisher1 = new Event<>();
    Event<Void> finisher2 = new Event<>();

    q.add(() -> {
      AsyncOperationBase op = new AsyncOperationBase();
      finisher1.whenTriggered(() -> op.finish());
      return op;
    });

    q.add(() -> {
      AsyncOperationBase op = new AsyncOperationBase();
      finisher2.whenTriggered(() -> op.finish());
      return op;
    });

    q.add(() -> new AsyncOperationBase(false));

    assertEquals(q.size(), 3);
    q.update(); // 'start' item 1, which removes it from the queue
    assertEquals(q.size(), 2);
    q.update(); // item 1 still busy
    assertEquals(q.size(), 2);
    q.update(); // item 1 still busy
    assertEquals(q.size(), 2);
    finisher1.trigger(null); // this completes item 1
    assertEquals(q.size(), 2);
    finisher2.trigger(null);
    assertEquals(q.size(), 2);
    q.update();
    finisher2.trigger(null);
    assertEquals(q.size(), 1);
    q.update();
    assertEquals(q.size(), 0);
  }
}
