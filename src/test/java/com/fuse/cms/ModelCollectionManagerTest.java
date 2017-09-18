package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.fuse.utils.Event;

public class ModelCollectionManagerTest{
  // main routine invoked by test runner
  @Test public void get(){
    ModelCollectionManager man = new ModelCollectionManager();
    assertEquals(man.get("foo"), null);
    assertEquals(man.get("foobar", true) == null, false);
  }

  @Test public void loadJsonFromFile(){
    ModelCollectionManager man = new ModelCollectionManager();
    assertEquals(man.size(), 0);
    assertEquals(man.loadJsonFromFile("testdata/ModelCollectionManagerTest-loadJsonFromFile.json"), true);
    assertEquals(man.size(), 2);
    assertEquals(man.get("books").size(), 3);
    assertEquals(man.get("books").get(0).get("title"), "Breakfast of Champions");
    assertEquals(man.get("books").get(1).get("title"), "Cat's Cradle");
    assertEquals(man.get("books").get(2).get("title"), "Slaughterhouse 5");
    assertEquals(man.get("authors").size(), 1);
    assertEquals(man.get("authors").get(0).get("name"), "Kurt Vonnegut");
  }

  @Test public void loadJsonFromFile_withUpdates(){
    ModelCollectionManager man = new ModelCollectionManager();
    assertEquals(man.size(), 0);
    assertEquals(man.loadJsonFromFile("testdata/ModelCollectionManagerTest-loadJsonFromFile.json"), true);
    assertEquals(man.loadJsonFromFile("testdata/ModelCollectionManagerTest-loadJsonFromFile.json"), true);
    assertEquals(man.size(), 2);
    assertEquals(man.get("books").size(), 3);
    assertEquals(man.get("books").get(0).get("title"), "Breakfast of Champions");
    assertEquals(man.get("books").get(1).get("title"), "Cat's Cradle");
    assertEquals(man.get("books").get(2).get("title"), "Slaughterhouse 5");
    assertEquals(man.get("authors").size(), 1);
    assertEquals(man.get("authors").get(0).get("name"), "Kurt Vonnegut");
  }

  @Test public void reload_transform(){
    ModelCollectionManager man = new ModelCollectionManager();
    assertEquals(man.loadJsonFromFile("testdata/ModelCollectionManagerTest-reload-transform1.json"), true);
    Model m = man.get("products").get(0);
    assertEquals(m.get("title"), "Product 1");

    Event<String> evt = new Event<>();
    evt.enableHistory();

    m.transformAttribute("title", (String newValue) -> {
      evt.trigger(newValue);
    });

    // verify the attributetransformer ran with initial value
    assertEquals(evt.getHistory().get(0), "Product 1");
    assertEquals(evt.getHistory().size(), 1);
    // reload same data file; no changes
    assertEquals(man.loadJsonFromFile("testdata/ModelCollectionManagerTest-reload-transform1.json"), true);
    assertEquals(evt.getHistory().size(), 1);
    // (re-)load from another file with updates to the existing model, verify attribute transformer ran in response to the update
    assertEquals(man.loadJsonFromFile("testdata/ModelCollectionManagerTest-reload-transform2.json"), true);
    assertEquals(evt.getHistory().get(1), "Item 1");
    assertEquals(evt.getHistory().size(), 2);
  }
}
