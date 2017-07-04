package com.fuse.cms;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.ArrayList;
import com.fuse.cms.Model;

/**
 * Unit test for com.fuse.cms.Model.
 */
public class ModelTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ModelTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ModelTest.class );
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

      { _(".attributeChangeEvent");
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

      { _(".transformAttribute");
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

      { _(".transform");
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

      { _(".parseJson");
        Model m = new Model();
        assertEquals(m.size(), 0);
        m.parseJson("{\"age\": 25, \"zipcode\": \"AB1234\"}");
        assertEquals(m.size(), 2);
        assertEquals(m.get("age"), "25");
        assertEquals(m.get("zipcode"), "AB1234");
      }

      { _("loadJson with attributeTransform");
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
    }

    private void _(String name){
      System.out.println("TEST: "+name);
    }
}
