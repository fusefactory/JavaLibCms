package com.fuse.cms;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.Runnable;
import java.lang.reflect.Array;
import java.util.function.BiConsumer;

import com.fuse.utils.Event;

public class ModelBase {

  private class Mod {
    public String setAttr;
    public String setValue;
  }

  public class AttributeChangeArgs {
    public ModelBase model;
    public String attr;
    public String value;
    public String previousValue;
  }

  private Map<String, String> attributes;
  private int lockCount = 0;
  private List<Mod> modQueue = null;

  public Event<AttributeChangeArgs> attributeChangeEvent;
  public Event<ModelBase> changeEvent;

  public ModelBase(){
    attributes = new HashMap<String, String>();
    changeEvent = new Event<ModelBase>();
    attributeChangeEvent = new Event<AttributeChangeArgs>();
  }

  public void destroy(){
    changeEvent.destroy();
    attributeChangeEvent.destroy();
    attributes.clear();
  }

  public boolean has(String attr){
    return attributes.containsKey(attr);
  }

  public String get(String attr){
    return attributes.get(attr);
  }

  public String get(String attr, String defaultValue){
    String result = get(attr);
    return (result == null) ? defaultValue : result;
  }

  /**
   * Changes the value of the specified attribute to the specified value.
   * * Triggers changeEvent if the attribute value was changed.
   * * Triggers attributeChangeEvent if the attribute value was changed.
   * Trigger
   * @param attr The name/key of the attribute
   * @param value The new value
   */
  public void set(String attr, String val){
    if(isLocked()){
      Mod mod = new Mod();
      mod.setAttr = attr;
      mod.setValue = val;
      if(modQueue == null)
        modQueue = new ArrayList<>();
      modQueue.add(mod);
      return;
    }

    String existing = attributes.get(attr);
    attributes.put(attr,  val);

    if(existing == null || !existing.equals(val)){
      changeEvent.trigger(this);

      AttributeChangeArgs args = new AttributeChangeArgs();
      args.model = this;
      args.attr = attr;
      args.value = val;
      args.previousValue = existing;
      attributeChangeEvent.trigger(args);
    }
  }

  public void each(BiConsumer<String, String> func){
    lock(() -> {
      for(Map.Entry<String, String> pair : attributes.entrySet()){
        func.accept(pair.getKey(), pair.getValue());
      }
    });
  }

  public boolean isLocked(){
    return lockCount > 0;
  }

  private void lock(Runnable func){
    lockCount++;
    func.run();
    lockCount--;

    if(isLocked())
      return;

    // lock lifted; process mod queue if there is one
    if(modQueue == null)
      return;

    for(Mod mod : modQueue){
      set(mod.setAttr, mod.setValue);
    }

    modQueue.clear();
    modQueue = null;
  }

  public int size(){
    return attributes.size();
  }

  public Set<String> getAttributeNames(){
    return attributes.keySet();
  }

  public void copy(ModelBase other){
    other.each((attr, val) -> {
      set(attr,val);
    });
  }

  /** Simply use the id attribute as id */
  public String getId(){
    return get("id", "");
  }

  public boolean getBool(String attr){
    return getBool(attr, false);
  }

  public boolean getBool(String attr, boolean defaultValue){
    String str = this.get(attr);
    if(str == null) return defaultValue;
    return str == "1" || Boolean.parseBoolean(str);
  }

  public int getInt(String attr){
    return getInt(attr, 0);
  }

  public int getInt(String attr, int defaultValue){
    String str = this.get(attr);
    if(str == null) return defaultValue;
    try {
      return Integer.parseInt(str);
    } catch (java.lang.NumberFormatException exc){
    }
    return defaultValue;
  }

  public long getLong(String attr){
    return getLong(attr, 0l);
  }

  public long getLong(String attr, long defaultValue){
    String str = this.get(attr);
    if(str == null) return defaultValue;

    try {
      return Long.parseLong(str);
    } catch (java.lang.NumberFormatException exc){
    }
    return defaultValue;
  }

  public float getFloat(String attr){
    return getFloat(attr, 0.0f);
  }

  public float getFloat(String attr, float defaultValue){
    String str = this.get(attr);
    if(str == null) return defaultValue;
    try {
      return Float.parseFloat(str);
    } catch (java.lang.NumberFormatException exc){
    }
    return defaultValue;
  }

  public void set(String attr, float value){
    set(attr, Float.toString(value));
  }

  /**
   * Converts value to a string and sets attribute using default set method.
   * @param attr Attribute to give a numeric value
   * @param value Number to convert to string
   */
  public void set(String attr, int value){
    set(attr, Integer.toString(value));
  }

  public void set(String attr, long value){
    set(attr, Long.toString(value));
  }

  public void set(String attr, boolean value){
    set(attr, Boolean.toString(value));
  }

  public float[] getVec3(String attr){
    float[] defaultValue = new float[]{0.0f,0.0f,0.0f};
    return getVecX(attr, 3, defaultValue);
  }

  public float[] getVec3(String attr, float[] defaultValue){
    return getVecX(attr, 3, defaultValue);
  }

  public float[] getVecX(String attr, int count, float[] defaultValue){
    String[] parts = get(attr, "").split(",");

    float[] result = new float[count];
    for(int i=0; i<count; i++){
      if(Array.getLength(parts) > i){
        try {
          result[i] = Float.parseFloat(parts[i]);
          continue;
        } catch (java.lang.NumberFormatException exc){}
      }

      if(Array.getLength(defaultValue) > i){
        result[i] = defaultValue[i];
        continue;
      }

      result[i] = 0.0f;
    }

    return result;
  }
}
