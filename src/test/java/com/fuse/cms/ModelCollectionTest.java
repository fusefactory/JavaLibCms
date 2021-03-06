package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Ignore;

import java.io.File;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;

public class ModelCollectionTest {
    // main routine invoked by test runner
    @Test public void findById(){
      ModelCollection col = new ModelCollection();
      assertEquals(col.findById("1"), null);
      Model m = new Model();
      m.set("id", "1");
      col.add(m);
      assertEquals(col.findById("1"), m);

      Model m2 = new Model();
      m2.set("id", "5");
      col.add(m2);
      assertEquals(col.findById("5"), m2);
      assertEquals(col.findById("1"), m);

      // create if not exist
      assertEquals(col.findById("10"), null);
      assertEquals(col.size(), 2);
      assertEquals(col.findById("10", true /* create */), col.get(2));
      assertEquals(col.size(), 3);
    }

    @Test public void findByAttr(){
      ModelCollection col = new ModelCollection();
      assertEquals(col.findByAttr("name", "bob"), null);
      Model m = new Model();
      m.set("name", "bob");
      col.add(m);
      assertEquals(col.findByAttr("name", "bob"), m);
      Model m2 = new Model();
      m2.set("name", "jane");
      col.add(m2);
      assertEquals(col.findByAttr("name", "bob"), m);
      assertEquals(col.findByAttr("name", "jane"), m2);
    }

    @Test public void loadJsonFromFile(){
      ModelCollection col = new ModelCollection();
      assertEquals(col.loadJsonFromFile("testdata/ModelCollectionTest-loadJsonFromFile.json"), true);
      assertEquals(col.size(), 3);
      assertEquals(col.get(0).get("id"), "1");
      assertEquals(col.get(1).get("id"), "2");
      assertEquals(col.get(2).get("id"), "3");
      assertEquals(col.get(0).get("value"), "1ne");
      assertEquals(col.get(1).get("value"), "2wo");
      assertEquals(col.get(2).get("value"), "3hree");
    }

    @Ignore @Test public void loadJsonFromFile_with_charset_encoding_options(){
      assertEquals("TODO", "see JsonLoader.charset in ModelCollection.java");
    }

    @Test public void loadJsonFromFile_with_updates(){
      ModelCollection col = new ModelCollection();
      assertEquals(col.loadJsonFromFile("testdata/ModelCollectionTest-loadJsonFromFile.json"), true);
      assertEquals(col.loadJsonFromFile("testdata/ModelCollectionTest-loadJsonFromFile.json"), true);
      assertEquals(col.size(), 3);
      assertEquals(col.get(0).get("id"), "1");
      assertEquals(col.get(1).get("id"), "2");
      assertEquals(col.get(2).get("id"), "3");
      assertEquals(col.get(0).get("value"), "1ne");
      assertEquals(col.get(1).get("value"), "2wo");
      assertEquals(col.get(2).get("value"), "3hree");
    }

    @Test public void toJsonString(){
      ModelCollection col = new ModelCollection();
      assertEquals(col.loadJsonFromFile("testdata/ModelCollectionTest-toJsonString.json"), true);
      assertEquals(col.size(), 3);
      assertEquals(col.toJsonString(), "[{\"id\":\"1\",\"value\":\"1ne\"},{\"id\":\"2\",\"value\":\"2wo\"},{\"id\":\"3\",\"value\":\"3hree\"}]");
    }

    @Test public void saveJsonToFile(){
      ModelCollection col = new ModelCollection();
      assertEquals(col.loadJsonFromFile("testdata/ModelCollectionTest-saveJsonToFile.json"), true);
      assertEquals(col.size(), 3);
      assertEquals(col.saveJsonToFile("testdata/ModelCollectionTest-saveJsonToFile.output.tmp.json"), true);

      String content = null;

      try {
        content = new String(Files.readAllBytes(Paths.get("testdata/ModelCollectionTest-saveJsonToFile.output.tmp.json")));
      } catch(java.io.IOException exc){
        System.out.println("IOException: "+exc.toString());
      }

      assertEquals(content, col.toJsonString());
      try{
          (new File("testdata/ModelCollectionTest-saveJsonToFile.output.tmp.json")).delete();
      } catch(Exception e) {
          // if any error occurs
          e.printStackTrace();
      }
    }

    @Test public void accept(){
      ModelCollection col = new ModelCollection();
      Model m = new Model();
      col.add(m);
      assertEquals(col.size(), 1);
      col.accept("age", "30");
      assertEquals(col.size(), 0);
      m = new Model();
      col.add(m);
      assertEquals(col.size(), 0);
      m = new Model();
      m.set("age", "20");
      col.add(m);
      assertEquals(col.size(), 0);
      m = new Model();
      m.set("age", "30");
      col.add(m);
      assertEquals(col.size(), 1);
    }

    @Test public void filtered(){
      ModelCollection col1 = new ModelCollection();
      Model m;
      m = new Model();
      m.set("age", "10");
      col1.add(m);
      m = new Model();
      m.set("age", "20");
      col1.add(m);
      m = new Model();
      m.set("age", "20");
      col1.add(m);
      ModelCollection col2 = col1.filtered("age", "20");
      // copied the one matching model
      assertEquals(col2.size(), 2);
      assertEquals(col2.get(0), col1.get(1));
      assertEquals(col2.get(1), col1.get(2));
      // source collection unaffected
      assertEquals(col1.size(), 3);
      // registered listener for new models
      m = new Model();
      m.set("age", "20");
      col1.add(m);
      assertEquals(col2.size(), 3);
      assertEquals(col1.size(), 4);
      assertEquals(col2.get(2), col1.get(3));
    }
}
