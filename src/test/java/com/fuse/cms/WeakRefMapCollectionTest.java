package com.fuse.cms;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.ArrayList;

public class WeakRefMapCollectionTest extends TestCase {
  public WeakRefMapCollectionTest( String testName ){ super( testName ); }
  public static Test suite(){ return new TestSuite( WeakRefMapCollectionTest.class ); }
  private void TEST(String name){ System.out.println("TEST: "+name); }

  // main routine invoked by test runner
  class Item{
    public String value;
    public Item(String val){ this.value = val; }
  }

  public void testApp(){
    { TEST("getInstance()");
      WeakRefMapCollection<String, Item> col = new WeakRefMapCollection<String, Item>();

      List<String> loaderStrings = new ArrayList<String>();

      // register loader which simply generates an item from the given key string
      // and records the key for every loader invocation
      col.setSyncInstanceLoader((String val) -> {
        loaderStrings.add(val);
        return new Item(val);
      });

      { // get non-existant Item but don't create if doesn't exist
        Item it = col.getInstance("abc", false);
        assertEquals(it, null);
        assertEquals(col.size(),0);
      }

      { // get non-existant Item and call loader if doesn't exist
        Item it = col.getInstance("abc");
        assertEquals(it.value, "abc"); // got an item
        assertEquals(col.size(),1); // item was created and added to collection
        assertEquals(col.get(0).getValue().get() == null, false); // collection has a non-expired weka reference to the created item
        assertEquals(loaderStrings.size(), 1); // loader was called
      }

      { // request already loaded (and cached) item again; should not run the loader
        Item it = col.getInstance("abc");
        assertEquals(it.value, "abc");
        assertEquals(it, col.get(0).getValue().get());
        assertEquals(col.size(),1); // same size, nothing added/removed
        assertEquals(loaderStrings.size(), 1); // loder not called this time, item was "cached"
      }

      { // request expired item; should reload
        col.getForKey("abc").clear(); // explicitly expire previously loaded weakref for test
        Item it = col.getInstance("abc"); // request same item
        assertEquals(it.value, "abc"); // returned valid item
        assertEquals(col.size(),1); // reloaded item should have replace previous item
        assertEquals(col.get(0).getValue().get(), it);
        assertEquals(loaderStrings.size(), 2); // loader should be invoked again
      }
    }
  }
}
