package com.fuse.cms;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.ArrayList;
import com.fuse.cms.ModelBase;

/**
 * Unit test for com.fuse.cms.Model.
 */
public class ModelBaseTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ModelBaseTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ModelBaseTest.class );
    }

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

    public void testApp(){
      strings = new ArrayList<String>();

      { _(".set and .get");
        ModelBase m = new ModelBase();
          m.set("name", "John");
          assertEquals(m.get("name"), "John");
          assertEquals(m.get("nama", "Jane"), "Jane");
      }

      { _("each");
        ModelBase m = new ModelBase();
        m.set("name", "John");
        m.each((String key, String value) -> {
          strings.add(key + " --> " + value);
        });

        assertEquals(joined(), "name --> John");
      }

      { _("each with modifications");
        model = new ModelBase();
        model.set("name", "John");
        model.set("lastname", "Doe");

        model.each((String key, String value) -> {
          model.set("_"+key, Integer.toString(model.size())+": "+value);
        });

        assertEquals(model.size(), 4);
        assertEquals(model.get("_lastname"), "2: Doe");
        assertEquals(model.get("_name"), "2: John");
      }

      { _("attributeChangeEvent");

        strings.clear();
        model.attributeChangeEvent.addListener((AttributeChangeArgs arg) -> {
          strings.add(arg.attr+"//"+arg.value);
        }, this);

        model.set("aa", "bb");
        assertEquals(joined(), "aa//bb");
      }

      { _("copy");
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

      { _(".getBool");
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

      { _(".getInt");
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

      { _(".getFloat");
        ModelBase m = new ModelBase();
        assertEquals(m.getFloat("foobar"), 0.0f);
        m.set("no", "1.0");
        assertEquals(m.getFloat("no"), 1.0f);
        m.set("no", "104.5");
        assertEquals(m.getFloat("no"), 104.5f);
        m.set("no", "foo");
        assertEquals(m.getFloat("no"), 0.0f);
        m.set("no", "1.3foobar");
        assertEquals(m.getFloat("no"), 0.0f);
        // w/ default value
        assertEquals(m.getFloat("foobarxx", 142.8f), 142.8f);
      }

      { _(".getVec3");
        ModelBase model = new ModelBase();
        assertEquals(model.getVec3("pos")[0], 0.0f);
        assertEquals(model.getVec3("pos")[1], 0.0f);
        assertEquals(model.getVec3("pos")[2], 0.0f);
        model.set("pos", "200,0,10");
        assertEquals(model.getVec3("pos")[0], 200.0f);
        assertEquals(model.getVec3("pos")[1], 0.0f);
        assertEquals(model.getVec3("pos")[2], 10.0f);
      }
    }

    private void _(String name){
      System.out.println("TEST: "+name);
    }
}
