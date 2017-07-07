package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StateTest {
  @Test public void defaultCollection(){
    assertEquals((new State()).getCollection() == null, false);
  }

  @Test public void getCollection(){
    ModelCollection col = new ModelCollection();
    State state = new State(col);
    assertEquals(state.getCollection(), col);
  }

  @Test public void child_parent(){
    State root = new State();
    // create child node
    State c1 = root.child("child1");
    assertEquals(c1.getId(), "child1");
    // fetch existing child node
    assertEquals(root.child("child1"), c1);
    // linking
    assertEquals(c1.child("child2").child("child3").getId(), "child1.child2.child3");
    assertEquals(c1.child("child2").child("child3").parent(), c1.child("child2"));
    // parent
    assertEquals(c1.parent(), root);
    assertEquals(c1.parent().getId(), "");
  }

  @Test public void childQuery(){
    State root = new State();
    assertEquals(root.childQuery("child1.child2.attribute"), root.child("child1").childQuery("child2.attribute"));
  }

  @Test public void jsonLoading(){
    // create root state (which also creates central state collection internally)
    State root = new State();
    // load json into state collection
    root.getCollection().loadJsonFromFile("testdata/StateTest-jsonLoading.json");
    State state = root.child("first");
    assertEquals(state.get("fooo"), "barr");
  }

  @Test public void collectionProtection(){
    State root = new State();
    assertEquals(root.getCollection().size(), 1); // contains the root state
    // check that the collection won't accept non-state instances
    assertEquals(root.getCollection().add(new Model()), false); // rejected
    assertEquals(root.getCollection().size(), 1); // nothing added
  }
}
