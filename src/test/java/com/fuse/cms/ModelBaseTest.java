package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import com.fuse.cms.ModelBase;

public class ModelBaseTest {

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


  @Test public void set_and_get(){
    strings = new ArrayList<String>();
    ModelBase m = new ModelBase();
    m.set("name", "John");
    assertEquals(m.get("name"), "John");
    assertEquals(m.get("nama", "Jane"), "Jane");

  }

  @Test public void each(){
    strings = new ArrayList<String>();
    ModelBase m = new ModelBase();
    m.set("name", "John");
    m.each((String key, String value) -> strings.add(key + " --> " + value));

    assertEquals(joined(), "name --> John");
  }

  @Test public void eachWithModifications(){
    ModelBase model = new ModelBase();
    model.set("name", "John");
    model.set("lastname", "Doe");

    model.each((String key, String value) -> {
      model.set("_"+key, Integer.toString(model.size())+": "+value);
    });

    assertEquals(model.size(), 4);
    assertEquals(model.get("_lastname"), "2: Doe");
    assertEquals(model.get("_name"), "2: John");
  }

  @Test public void attributeChangeEvent(){
    strings = new ArrayList<String>();
    ModelBase model = new ModelBase();

    strings.clear();
    model.attributeChangeEvent.addListener((AttributeChangeArgs arg) -> {
      strings.add(arg.attr+"//"+arg.value);
    }, this);

    model.set("aa", "bb");
    assertEquals(joined(), "aa//bb");
  }

  @Test public void copy(){
    ModelBase m1, m2;
    m1 = new ModelBase();
    m2 = new ModelBase();
    m1.set("a", "1");
    m1.set("b", "2");
    m2.set("c", "3");
    m2.copy(m1);
    assertEquals(m2.get("a"), "1");
    assertEquals(m2.get("b"), "2");
    assertEquals(m2.get("c"), "3");
  }

  @Test public void getBool(){
    ModelBase m = new ModelBase();
    assertEquals(m.getBool("foobar"), false);
    m.set("bAttr", "false");
    assertEquals(m.getBool("bAttr"), false);
    m.set("bAttr", "true");
    assertEquals(m.getBool("bAttr"), true);
    m.set("bAttr", "0");
    assertEquals(m.getBool("bAttr"), false);
    m.set("bAttr", "1");
    assertEquals(m.getBool("bAttr"), true);
    // any unknown value gives false
    m.set("bAttr", "2");
    assertEquals(m.getBool("bAttr"), false);
    m.set("bAttr", "foo");
    assertEquals(m.getBool("bAttr"), false);
    // w/ default value
    assertEquals(m.getBool("foobarxx", true), true);
  }

  @Test public void getInt(){
    ModelBase m = new ModelBase();
    assertEquals(m.getInt("foobar"), 0);
    m.set("no", "1");
    assertEquals(m.getInt("no"), 1);
    m.set("no", "100");
    assertEquals(m.getInt("no"), 100);
    // invalid formats
    m.set("no", "abc");
    assertEquals(m.getInt("no"), 0);
    m.set("no", "103.5");
    assertEquals(m.getInt("no"), 0);
    // w/ default value
    assertEquals(m.getInt("foobarxx", 123), 123);
  }

  @Test public void set_int(){
    ModelBase m = new ModelBase();
    assertEquals(m.get("foo"), null);
    m.set("foo", 44);
    assertEquals(m.get("foo"), "44");
  }

  @Test public void getFloat(){
    ModelBase m = new ModelBase();
    assertEquals(m.getFloat("foobar"), 0.0f, 0.000001f);
    m.set("no", "1.0");
    assertEquals(m.getFloat("no"), 1.0f, 0.000001f);
    m.set("no", "104.5");
    assertEquals(m.getFloat("no"), 104.5f, 0.000001f);
    m.set("no", "foo");
    assertEquals(m.getFloat("no"), 0.0f, 0.000001f);
    m.set("no", "1.3foobar");
    assertEquals(m.getFloat("no"), 0.0f, 0.000001f);
    // w/ default value
    assertEquals(m.getFloat("foobarxx", 142.8f), 142.8f, 0.000001f);
  }

  @Test public void getVec3(){
    ModelBase model = new ModelBase();
    assertEquals(model.getVec3("pos")[0], 0.0f, 0.000001f);
    assertEquals(model.getVec3("pos")[1], 0.0f, 0.000001f);
    assertEquals(model.getVec3("pos")[2], 0.0f, 0.000001f);
    model.set("pos", "200,0,10");
    assertEquals(model.getVec3("pos")[0], 200.0f, 0.000001f);
    assertEquals(model.getVec3("pos")[1], 0.0f, 0.000001f);
    assertEquals(model.getVec3("pos")[2], 10.0f, 0.000001f);
  }
}
