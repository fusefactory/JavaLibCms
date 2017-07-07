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

class ModelFollower {
  private ModelBase source, target;
  private Object owner;

  public ModelFollower(ModelBase source, ModelBase target, Object owner){
    this.source = source;
    this.target = target;
    this.owner = owner;
    start();
  }

  public void start(){
    this.source.each((String key, String val) -> {
      this.target.set(key, val);
    });

    this.source.attributeChangeEvent.addListener((AttributeChangeArgs args) -> {
        this.target.set(args.attr, args.value);
    }, this);
  }

  public void stop(){
    this.source.attributeChangeEvent.removeListeners(this);
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
  private List<ModelFollower> modelFollowers;

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

  /**
   * Create a 'follow connection' between this model and the specified source model,
   * with the default null owner.
   * A 'follow connection' means;
   * this model adopts all of the source model's attributes
   * this model will register listeners to copy changes to the source model's attributes in the future
   *
   * @param source The source model to follow
   */
  public ModelFollower follow(ModelBase source){
    return follow(source, null);
  }

  /**
   * Create a 'follow connection' between this model and the specified
   * source model, and give the connection a specific owner.
   * A 'follow connection' means;
   * this model adopts all of the source model's attributes
   * this model will register listeners to copy changes to the source model's attributes in the future
   *
   * @param source The source model to follow
   * @param owner The owner of the connection (which can be used later to stop the connection; see the stopFollow methods)
   */
  public ModelFollower follow(ModelBase source, Object owner){
    return follow(source, owner, true);
  }

  /**
   * Create a 'follow connection' between this model and the specified
   * source model, give the connection a specific owner, and specify if the
   * the connection should be active (also listening to future changes to the source model)
   * or one-time only (just copy the current attributes).
   *
   * @param source The source model to follow
   * @param owner The owner of the connection (which can be used later to stop the connection; see the stopFollow methods)
   * @param active Flag to specify if the connection should stay active
   */
  public ModelFollower follow(ModelBase source, Object owner, boolean active){
    // create follower (this will also execute initial syncing between models)
    ModelFollower f = new ModelFollower(source, this, owner);

    // only store the follower if we want active syncing (not just once) ,
    // otherwise stop the follower and let the garbage collector pick it up
    if(active){
      this.modelFollowers.add(f);
    } else {
      f.stop();
    }

    return f;
  }

  /** Stop all active follow connections created using calls to this instance's follow methods */
  public List<ModelFollower> stopFollow(){
    return stopFollow(null);
  }

  /**
   * Stop all active follow connections
   * for a specific owner
   * that were created using calls to this instance's follow methods
   *
   * @param owner The owner of the follow connections that need to be stopped
   */
  public List<ModelFollower> stopFollow(Object owner){
    List<ModelFollower> stopped = new ArrayList<>();

    for(ModelFollower f : modelFollowers){
      if(owner == null || f.getOwner() == owner){
        stopped.add(f);
      }
    }

    for(ModelFollower f : stopped){
      f.stop();
      modelFollowers.remove(f);
    }

    return stopped;
  }
}
