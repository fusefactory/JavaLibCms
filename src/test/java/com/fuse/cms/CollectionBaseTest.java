package com.fuse.cms;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.ArrayList;

public class CollectionBaseTest extends TestCase
{
    public CollectionBaseTest( String testName ){
        super( testName );
    }

    public static Test suite(){
        return new TestSuite( CollectionBaseTest.class );
    }


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

    public void testApp(){
      strings = new ArrayList<String>();

      { _("addEvent");
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

      { _("removeEvent");
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

      { _("removeEvent on clear");

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

      { _("each with modifying functor");
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

      { _("beforeAddTest");
      }


      { _("findFirst()");
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
    }

    private void _(String name){
      System.out.println("TEST: "+name);
    }
}
