package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Ignore;

import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import com.fuse.utils.Event;

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

  @Test public void remove(){
    AsyncQueue q = new AsyncQueue();
    Supplier<AsyncOperationBase> f1 = () -> new AsyncOperationBase(false);
    Supplier<AsyncOperationBase> f2 = () -> new AsyncOperationBase(false);
    q.add(f1);
    q.add(f2);
    q.add(() -> new AsyncOperationBase(false));
    assertEquals(q.size(), 3);
    assertEquals(q.remove(f1), true);
    assertEquals(q.size(), 2);
    assertEquals(q.remove(f1), false);
    assertEquals(q.size(), 2);
    assertEquals(q.remove(f2), true);
    assertEquals(q.size(), 1);
  }

  @Test public void setDispatchOnUpdate(){
    AsyncQueue q = new AsyncQueue();
    Event<String> e = new Event<>();
    e.enableHistory();

    q.add(() -> {
      e.trigger("1");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    });

    q.add(() -> {
      e.trigger("2");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    });

    q.add(() -> {
      e.trigger("3");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    });

    assertEquals(q.size(), 3);
    assertEquals(e.getHistory().size(), 0);
    q.setDispatchOnUpdate(false);
    assertEquals(q.size(), 0);
    assertEquals(e.getHistory().get(0), "1");
    assertEquals(e.getHistory().get(1), "2");
    assertEquals(e.getHistory().get(2), "3");
    assertEquals(e.getHistory().size(), 3);

    q.add(() -> {
      e.trigger("4");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    });

    assertEquals(q.size(), 0);
    assertEquals(e.getHistory().size(), 4);
    assertEquals(e.getHistory().get(3), "4");
  }

  @Test public void addFirst(){
    AsyncQueue q = new AsyncQueue();
    Event<String> e = new Event<>();
    e.enableHistory();

    q.add(() -> {
      e.trigger("1");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    });

    q.add(() -> {
      e.trigger("2");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    }, 100);

    q.addFirst(() -> {
      e.trigger("3");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    });

    q.setDispatchOnUpdate(false); // executes all
    assertEquals(q.size(), 0); // executed
    assertEquals(e.getHistory().get(0), "3");
    assertEquals(e.getHistory().get(1), "2");
    assertEquals(e.getHistory().get(2), "1");
  }

  @Test public void addLast(){
    AsyncQueue q = new AsyncQueue();
    Event<String> e = new Event<>();
    e.enableHistory();

    q.add(() -> {
      e.trigger("1");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    }, -100);

    q.add(() -> {
      e.trigger("2");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    }, 100);

    q.addLast(() -> {
      e.trigger("3");
      AsyncOperationBase op = new AsyncOperationBase(false);
      op.dispatch();
      return op;
    });

    q.setDispatchOnUpdate(false); // executes all
    assertEquals(q.size(), 0); // executed
    assertEquals(e.getHistory().get(0), "2");
    assertEquals(e.getHistory().get(1), "1");
    assertEquals(e.getHistory().get(2), "3");
  }
}
