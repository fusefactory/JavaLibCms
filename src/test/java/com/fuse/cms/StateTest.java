package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StateTest {
  @Test
  public void getCollection(){
    ModelCollection col = new ModelCollection();
    State state = new State(col);
    assertEquals(state.getCollection(), col);
  }
}
