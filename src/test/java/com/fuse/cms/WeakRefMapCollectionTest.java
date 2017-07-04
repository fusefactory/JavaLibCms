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

      col.setSyncInstanceLoader((String val) -> {
        return new Item(val);
      });

      Item it = col.getInstance("abc");
      assertEquals(it.value, "abc");
      assertEquals(col.size(),1);
    }
  }
}
