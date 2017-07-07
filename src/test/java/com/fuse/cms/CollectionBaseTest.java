package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

public class CollectionBaseTest {

    private List<String> strings;

    private String joined(){
      return joined("");
    }

    private String joined(String separator){
      String result = "";

      for(int i=1; i<strings.size(); i++)
        result += separator + strings.get(i);

      if(strings.size() > 0)
        result = strings.get(0) + result;

      return result;
    }

    private CollectionBase<Model> collection;

    @Test public void addEvent(){
      strings = new ArrayList<String>();

      strings.clear();
      collection = new CollectionBase();

      collection.addEvent.addListener((Model model) -> {
        strings.add("added: "+model.get("name"));
      });

      Model m = new Model();
      m.set("name", "FirstModel");
      assertEquals(joined(), "");
      collection.add(m);
      assertEquals(joined(), "added: FirstModel");
      assertEquals(collection.size(), 1);
    }

    @Test public void removeEvent(){
      strings = new ArrayList<String>();

      strings.clear();
      collection = new CollectionBase();

      collection.removeEvent.addListener((Model model) -> {
        strings.add("removed: "+model.get("name"));
      });

      Model m = new Model();
      m.set("name", "FirstModel");
      assertEquals(joined(), "");
      collection.add(m);
      assertEquals(joined(), "");
      collection.remove(m);
      assertEquals(joined(), "removed: FirstModel");
      assertEquals(collection.size(), 0);
    }

    @Test public void removeEventOnClear(){
      strings = new ArrayList<String>();

      collection = new CollectionBase();
      Model m = new Model();
      m.set("name", "#1");
      collection.add(m);
      m = new Model();
      m.set("name", "#2");
      collection.add(m);

      strings.clear();
      collection.removeEvent.addListener((Model model) -> {
        strings.add("removed: "+model.get("name"));
      });

      collection.clear();
      assertEquals(joined(), "removed: #2removed: #1");
      assertEquals(collection.size(), 0);
    }

    @Test public void eachWithModifyingFunctor(){
      strings = new ArrayList<String>();

      collection = new CollectionBase();
      Model m = new Model();
      m.set("name", "#1");
      collection.add(m);
      m = new Model();
      m.set("name", "#2");
      collection.add(m);

      strings.clear();
      collection.each((Model im) -> {
        Model newModel = new Model();
        newModel.copy(im);
        collection.add(newModel);
        strings.add(im.get("name") +": "+Integer.toString(collection.size()));
      });

      assertEquals(collection.size(), 4);
      assertEquals(joined(), "#1: 2#2: 2");

      strings.clear();
      collection.each((Model im) -> {
        collection.remove(im);
        strings.add(im.get("name") +": "+Integer.toString(collection.size()));
      });

      // all removes are performed at end of each iteration
      assertEquals(collection.size(), 0);
      // none of the removes occured while iterating
      assertEquals(joined(), "#1: 4#2: 4#1: 4#2: 4");

      // create three dummy items
      m = new Model();
      m.set("idx", "2");
      collection.add(m);
      m = new Model();
      m.set("idx", "1");
      collection.add(m);
      m = new Model();
      m.set("idx", "0");
      collection.add(m);

      strings.clear();

      collection.each((Model im) -> {
        // remove by index
        collection.remove(Integer.parseInt(im.get("idx")));
        strings.add(im.get("idx") +": "+Integer.toString(collection.size()));
      });

      // all removes are performed at end of each iteration
      assertEquals(collection.size(), 0);
      // none of the removes occured while iterating
      assertEquals(joined(", "), "2: 3, 1: 3, 0: 3");
    }

    @Test public void beforeAddTest(){
      strings = new ArrayList<String>();
      System.out.println("TODO");
    }

    @Test public void findFirst(){
      strings = new ArrayList<String>();
      CollectionBase<Model> col = new CollectionBase<>();
      Model m1 = new Model();
      m1.set("name", "a");
      col.add(m1);
      Model m2 = new Model();
      m2.set("name", "b");
      col.add(m2);

      Model foundModel =col.findFirst((Model mm) -> {
        return mm.get("name").equals("b");
      });

      assertEquals(foundModel, m2);
    }

    @Test public void eachWithIndex(){
      CollectionBase<Model> col = new CollectionBase<>();
      col.add(new Model());
      col.add(new Model());

      List<String> strs = new ArrayList<>();
      col.eachWithIndex((Model m, Integer idx) -> {
        strs.add(Integer.toString(idx)+" for "+col.indexOf(m));
      });

      assertEquals(strs.size(), 2);
      assertEquals(strs.get(0), "0 for 0");
      assertEquals(strs.get(1), "1 for 1");
    }

    @Test public void create_and_setInstantiator(){
      class StrItem {
        public String str;
      }

      // a collectin without instantiator can't create new items
      CollectionBase<StrItem> col = new CollectionBase<>();
      assertEquals(col.create(), null);
      assertEquals(col.size(), 0);
      // give the collection an instantiator using setInstantiator
      col.setInstantiator(() -> {
        return new StrItem();
      });

      // now it can create items
      StrItem stri = col.create();
      assertEquals(stri == null, false);
      assertEquals(col.size(), 1);
      stri.str = "oi";
      assertEquals(col.get(0).str, "oi");
    }
}
