package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import com.fuse.cms.Model;

/**
* Unit test for com.fuse.cms.Model.
*/
public class ModelTest {
  /**
  * Test Logic
  */
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

  private ModelBase model;

  @Test public void attributeChangeEvent(){
    strings = new ArrayList<String>();


    Model m = new Model();
    strings.clear();

    m.set("name", "Bob");

    m.attributeChangeEvent.addListener((AttributeChangeArgs args) -> {
      strings.add(args.value);
    }, this);

    assertEquals(joined(), "");
    m.set("name", "john");
    assertEquals(joined(), "john");

  }

  @Test public void transformAttribute(){
    strings = new ArrayList<String>();

    Model m = new Model();
    m.set("name", "John");

    {
      strings.clear();

      AttributeTransformer transformer = m.transformAttribute("name", (String value) -> {
        strings.add(":: "+value);
      }, this);

      assertEquals(joined(), ":: John");
      m.set("name", "Doe");
      assertEquals(joined(), ":: John:: Doe");

      transformer.stop();
      m.set("name", "Jane");
      assertEquals(joined(), ":: John:: Doe");
      transformer.start();
      m.set("name", "Dane");
      assertEquals(joined(), ":: John:: Doe:: Dane");

      m.stopTransformAttribute(this);
      m.set("name", "Bob");
      assertEquals(joined(), ":: John:: Doe:: Dane");
    }
  }

  @Test public void transform(){
    strings = new ArrayList<String>();

    strings.clear();
    Model m = new Model();
    m.set("name", "John");

    {
      ModelTransformer transformer = m.transform((ModelBase model) -> {
        strings.add(":: "+model.get("name"));
      }, this);

      assertEquals(joined(), ":: John");
      m.set("name", "Doe");
      assertEquals(joined(), ":: John:: Doe");

      transformer.stop();
      m.set("name", "Jane");
      assertEquals(joined(), ":: John:: Doe");
      transformer.start();
      m.set("name", "Dane");
      assertEquals(joined(), ":: John:: Doe:: Dane");

      m.stopTransform(this);
      m.set("name", "Bob");
      assertEquals(joined(), ":: John:: Doe:: Dane");
    }
  }

  @Test public void parseJson(){
    strings = new ArrayList<String>();

    Model m = new Model();
    assertEquals(m.size(), 0);
    m.parseJson("{\"age\": 25, \"zipcode\": \"AB1234\"}");
    assertEquals(m.size(), 2);
    assertEquals(m.get("age"), "25");
    assertEquals(m.get("zipcode"), "AB1234");
  }

  @Test public void loadJson_with_attributeTransform(){
    strings = new ArrayList<String>();

    Model m = new Model();
    strings.clear();

    m.set("name", "John");
    m.transformAttribute("name", (String newValue) -> {
      strings.add(newValue);
    }, this);
    assertEquals(joined(","), "John");

    m.set("name", "Oliver");
    assertEquals(joined(","), "John,Oliver");

    String json = "{\"name\": \"Bob\"}";
    m.parseJson(json);
    assertEquals(m.get("name"), "Bob");
    assertEquals(joined(","), "John,Oliver,Bob");
  }

  @Test public void follow_stopFollow(){
    System.out.println("TODO");
  }

  @Test public void getId(){
    Model m = new Model();
    assertEquals(m.getId(), "");
    m.set("id", "1013");
    assertEquals(m.getId(), "1013");
  }
}
