package com.fuse.cms;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import org.json.*;

class AttributeTransformer {
  private ModelBase model;
  private String attr;
  private Consumer<String> func;
  private Object owner;

  public AttributeTransformer(ModelBase model, String attr, Consumer<String> func, Object owner){
    this.owner = owner;
    this.model = model;
    this.attr = attr;
    this.func = func;
    start();
    if(model.has(attr))
      func.accept(model.get(attr));
  };

  public void start(){
    model.attributeChangeEvent.addListener((AttributeChangeArgs args) -> {
      if(args.attr.equals(attr))
        this.func.accept(args.value);
    }, this);
  }

  public void stop(){
    model.attributeChangeEvent.removeListeners(this);
  }

  public Object getOwner(){
    return owner;
  }
}

class ModelTransformer {
  private ModelBase model;
  private Consumer<ModelBase> func;
  private Object owner;

  public ModelTransformer(ModelBase model, Consumer<ModelBase> func, Object owner){
    this.owner = owner;
    this.model = model;
    this.func = func;
    start();
    func.accept(model);
  };

  public void start(){
    model.changeEvent.addListener((ModelBase m) -> {
      this.func.accept(m);
    }, this);
  }

  public void stop(){
    model.changeEvent.removeListeners(this);
  }

  public Object getOwner(){
    return owner;
  }
}


class ModelJsonParser {
  private ModelBase model;
  private String jsonContent;
  private JSONObject jsonObject;

  public ModelJsonParser(ModelBase model, String json){
    this.model = model;
    jsonContent = json;
  }

  public ModelJsonParser(ModelBase model, JSONObject json){
    this.model = model;
    this.jsonObject = json;
  }

  public boolean parse(){
    if(this.jsonObject == null)
      this.jsonObject = new JSONObject(jsonContent);

    Iterator<String> it = jsonObject.keys();

    while(it.hasNext()){
      String attr = it.next();
      model.set(attr, jsonObject.get(attr).toString());
    }

    return true;
  }
}

public class Model extends ModelBase {
  private List<AttributeTransformer> attributeTransformers;
  private List<ModelTransformer> modelTransformers;

  public Model(){
    attributeTransformers = new ArrayList<AttributeTransformer>();
    modelTransformers = new ArrayList<ModelTransformer>();
  }

  public AttributeTransformer transformAttribute(String attr, Consumer<String> func){
    return transformAttribute(attr, func, null);
  }

  public AttributeTransformer transformAttribute(String attr, Consumer<String> func, Object owner){
    AttributeTransformer t = new AttributeTransformer(this, attr, func, owner);
    attributeTransformers.add(t);
    return t;
  }

  public void stopTransformAttribute(Object owner){
    Iterator it = attributeTransformers.iterator();
    while(it.hasNext()){
      AttributeTransformer t = (AttributeTransformer)it.next();
      if(t.getOwner() == owner){
        t.stop();
        it.remove();
      }
    }
  }



  public ModelTransformer transform(Consumer<ModelBase> func){
    return transform(func, null);
  }

  public ModelTransformer transform(Consumer<ModelBase> func, Object owner){
    ModelTransformer t = new ModelTransformer(this, func, owner);
    modelTransformers.add(t);
    return t;
  }

  public void stopTransform(Object owner){
    Iterator it = modelTransformers.iterator();
    while(it.hasNext()){
      ModelTransformer t = (ModelTransformer)it.next();
      if(t.getOwner() == owner){
        t.stop();
        it.remove();
      }
    }
  }


  public boolean parseJson(String json){
    return (new ModelJsonParser(this, json)).parse();
  }

  public boolean loadJson(JSONObject json){
    return (new ModelJsonParser(this, json)).parse();
  }
}
