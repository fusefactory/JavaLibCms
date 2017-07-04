package com.fuse.cms;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.ArrayList;

public class ModelCollectionManagerTest extends TestCase {
  public ModelCollectionManagerTest( String testName ){ super( testName ); }
  public static Test suite(){ return new TestSuite( ModelCollectionManagerTest.class ); }

  // private List<String> strings;
  String result;

  // main routine invoked by test runner
  public void testApp(){
    { _("get(<name>, <create-if-not-exist>)");
      ModelCollectionManager man = new ModelCollectionManager();
      assertEquals(man.get("foo"), null);
      assertEquals(man.get("foobar", true) == null, false);
    }

    { _("loadJsonFromFile");
      ModelCollectionManager man = new ModelCollectionManager();
      assertEquals(man.size(), 0);
      assertEquals(man.loadJsonFromFile("data/ModelCollectionManagerTest-loadJsonFromFile.json"), true);
      assertEquals(man.size(), 2);
      assertEquals(man.get("books").size(), 3);
      assertEquals(man.get("books").get(0).get("title"), "Breakfast of Champions");
      assertEquals(man.get("books").get(1).get("title"), "Cat's Cradle");
      assertEquals(man.get("books").get(2).get("title"), "Slaughterhouse 5");
      assertEquals(man.get("authors").size(), 1);
      assertEquals(man.get("authors").get(0).get("name"), "Kurt Vonnegut");
    }

    { _("loadJsonFromFile with updates");
      ModelCollectionManager man = new ModelCollectionManager();
      assertEquals(man.size(), 0);
      assertEquals(man.loadJsonFromFile("data/ModelCollectionManagerTest-loadJsonFromFile.json"), true);
      assertEquals(man.loadJsonFromFile("data/ModelCollectionManagerTest-loadJsonFromFile.json"), true);
      assertEquals(man.size(), 2);
      assertEquals(man.get("books").size(), 3);
      assertEquals(man.get("books").get(0).get("title"), "Breakfast of Champions");
      assertEquals(man.get("books").get(1).get("title"), "Cat's Cradle");
      assertEquals(man.get("books").get(2).get("title"), "Slaughterhouse 5");
      assertEquals(man.get("authors").size(), 1);
      assertEquals(man.get("authors").get(0).get("name"), "Kurt Vonnegut");
    }

    { _("reload-transform");
      ModelCollectionManager man = new ModelCollectionManager();
      assertEquals(man.loadJsonFromFile("data/ModelCollectionManagerTest-reload-transform1.json"), true);
      Model m = man.get("products").get(0);
      assertEquals(m.get("title"), "Product 1");
      result = "";
      m.transformAttribute("title", (String newValue) -> {
        result = newValue;
      });
      assertEquals(result, "Product 1");
      assertEquals(man.loadJsonFromFile("data/ModelCollectionManagerTest-reload-transform2.json"), true);
      assertEquals(m.get("title"), "Item 1");
      assertEquals(result, "Item 1");
    }
  }

  // private String joined(){
  //   return joined("");
  // }
  //
  // private String joined(String separator){
  //   String result = "";
  //
  //   for(int i=1; i<strings.size(); i++)
  //     result += separator + strings.get(i);
  //
  //   if(strings.size() > 0)
  //     result = strings.get(0) + result;
  //
  //   return result;
  // }

  private void _(String name){
    System.out.println("TEST: "+name);
  }
}
