package com.fuse.cms;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

public class CollectionTest {
  class TmpKlass {
    public String attr;
    public TmpKlass(){ attr = new String(); }
    public TmpKlass(String value){ attr = value; }
  }

  class TmpKlass2{
    public String name;
    public TmpKlass2(String newName){ name = newName; }
  }

  @Test public void accept(){
      Collection<TmpKlass> col = new Collection<TmpKlass>();
      assertEquals(col.size(), 0);
      TmpKlass instance = new TmpKlass();
      col.add(instance);
      assertEquals(col.size(), 1);
      col.accept((TmpKlass inst) -> {
        return inst.attr.equals("OK");
      });
      assertEquals(col.size(), 0);
      instance = new TmpKlass();
      col.add(instance);
      assertEquals(col.size(), 0);
      instance.attr = "NOPE";
      col.add(instance);
      assertEquals(col.size(), 0);
      instance.attr = "OK";
      col.add(instance);
      assertEquals(col.size(), 1);
  }

  @Test public void sync(){
      Collection<TmpKlass> source = new Collection<TmpKlass>();
      Collection<TmpKlass> target = new Collection<TmpKlass>();

      // add two models to source
      TmpKlass instance = new TmpKlass("#1");
      source.add(instance);
      instance = new TmpKlass("#2");
      source.add(instance);
      assertEquals(target.size(), 0);
      // sync target with source, should copy all models
      target.sync(source);
      assertEquals(target.size(), 2);
      assertEquals(target.get(0).attr, "#1");
      assertEquals(target.get(1).attr, "#2");

      // add third model to source
      instance = new TmpKlass("#3");
      source.add(instance);
      // should be synced to target
      assertEquals(target.size(), 3);
      assertEquals(target.get(2).attr, "#3");

      // remove item #2 from source
      source.remove(1);
      // should also be removed from target
      assertEquals(target.size(), 2);
      assertEquals(target.get(0).attr, "#1");
      assertEquals(target.get(1).attr, "#3");

      // stop sync from source
      target.stopSync(source);
      // add 4th model to source
      instance = new TmpKlass("#4");
      source.add(instance);
      // should NOT be synced to target
      assertEquals(target.size(), 2);

      // remove 1st model from source
      source.remove(0);
      // should NOT be synced to target
      assertEquals(target.size(), 2);
  }

  @Test public void sync_false(){
      Collection<TmpKlass> source = new Collection<TmpKlass>();
      Collection<TmpKlass> target = new Collection<TmpKlass>();

      // add two models to source
      TmpKlass instance = new TmpKlass("#1");
      source.add(instance);
      instance = new TmpKlass("#2");
      source.add(instance);
      assertEquals(target.size(), 0);
      // sync target with source, should copy all models
      target.sync(source, false /* not actively */);
      assertEquals(target.size(), 2);
      assertEquals(target.get(0).attr, "#1");
      assertEquals(target.get(1).attr, "#2");

      // add third model to source
      instance = new TmpKlass("#3");
      source.add(instance);
      // should NOT get synced to target
      assertEquals(target.size(), 2);
      assertEquals(target.get(0).attr, "#1");
      assertEquals(target.get(1).attr, "#2");
  }

  @Test public void filtered(){
      Collection<TmpKlass> col1 = new Collection<TmpKlass>();
      TmpKlass item = new TmpKlass("a");
      col1.add(new TmpKlass("ab"));
      col1.add(new TmpKlass("bc"));
      col1.add(new TmpKlass("cd"));
      Collection<TmpKlass> col2 = col1.filtered((TmpKlass tmp) -> { return tmp.attr == "bc"; });
      // copied the one matching model
      assertEquals(col2.size(), 1);
      assertEquals(col2.get(0), col1.get(1));
      // source collection unaffected
      assertEquals(col1.size(), 3);
      // registered listener for new models
      col1.add(new TmpKlass("bc"));
      assertEquals(col2.size(), 2);
      assertEquals(col1.size(), 4);
      assertEquals(col2.get(1), col1.get(3));
  }

  @Test public void withAll(){
      Collection<TmpKlass> col1 = new Collection<TmpKlass>();
      List<String> strings = new ArrayList<String>();
      col1.add(new TmpKlass("a"));
      col1.add(new TmpKlass("b"));
      col1.add(new TmpKlass("c"));
      col1.withAll((TmpKlass item) -> {
        strings.add(item.attr);
      }, this);

      assertEquals(strings.size(), 3);
      assertEquals(strings.get(0), "a");
      assertEquals(strings.get(1), "b");
      assertEquals(strings.get(2), "c");
      col1.add(new TmpKlass("d"));
      assertEquals(strings.size(), 4);
      assertEquals(strings.get(3), "d");

      col1.stopWithAll(this);
      col1.add(new TmpKlass("e"));
      assertEquals(strings.size(), 4);
      assertEquals(strings.get(3), "d");
  }

  @Test public void transform(){
      // create source collection with one item
      Collection<TmpKlass> col1 = new Collection<TmpKlass>();
      col1.add(new TmpKlass("a"));

      // create target collection with transformered items
      Collection<TmpKlass2> col2 = col1.transform((TmpKlass item) -> {
        return (new TmpKlass2("-"+item.attr));
      }, this);

      // call to transform has populated the target col with transformation of the existing item
      assertEquals(col2.size(), 1);
      assertEquals(col2.get(0).name, "-a");
      // add item to source; should show up in target
      col1.add(new TmpKlass("b"));
      assertEquals(col2.size(), 2);
      assertEquals(col2.get(1).name, "-b");
      // remove item form source; related item should also disappear in target
      col1.remove(0);
      assertEquals(col2.size(), 1);
      assertEquals(col2.get(0).name, "-b");
      // stop transforming
      col1.stopTransforms(this);
      // add another item to source; should not show up in target
      col1.add(new TmpKlass("c"));
      assertEquals(col2.size(), 1);
      assertEquals(col2.get(0).name, "-b");
  }
}
