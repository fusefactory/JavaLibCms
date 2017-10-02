package com.fuse.cms;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import org.json.*;

class ModelExtension {
  protected ModelBase model;
  protected Object owner;

  public ModelExtension(ModelBase model){
    this.model = model;
    this.owner = null;
  }

  public ModelExtension(ModelBase model, Object owner){
    this.model = model;
    this.owner = owner;
  }

  public void destroy(){
    stop();
    this.model = null;
    this.owner = null;
  }

  public void start(){
    // "virtual"; override
  }

  public void stop(){
    // "virtual"; override
  }

  public Object getOwner(){
     return owner;
  }
}


class AttributeTransformer {
  private ModelBase model;
  private String attr;
  private Consumer<String> func;
  private Object owner;

  public AttributeTransformer(ModelBase model, String attr, Consumer<String> func, Object owner, boolean active){
    this.owner = owner;
    this.model = model;
    this.attr = attr;
    this.func = func;

    if(model.has(attr))
      func.accept(model.get(attr));

    if(active){
      start();
    }
  };

  public void destroy(){
    stop();
    model = null;
    attr = null;
    func = null;
    owner = null;
  }

  public void start(){
    model.attributeChangeEvent.addListener((ModelBase.AttributeChangeArgs args) -> {
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

  public ModelTransformer(ModelBase model, Consumer<ModelBase> func, Object owner, boolean active){
    this.owner = owner;
    this.model = model;
    this.func = func;
    func.accept(model);
    if(active)
      start();
  };

  public void destroy(){
    stop();
    model = null;
    func = null;
    owner = null;
  }

  public void start(){
    model.changeEvent.addListener((ModelBase m) -> {
      this.func.accept(m);
    }, this);
  }

  public void stop(){
    if(this.model != null)
      model.changeEvent.removeListeners(this);
  }

  public Object getOwner(){
    return owner;
  }
}

class ModelFollower extends ModelExtension {
  private ModelBase source;

  public ModelFollower(ModelBase source, ModelBase target, Object owner){
    super(target, owner);
    this.source = source;
    start();
  }

  @Override
  public void destroy(){
    super.destroy();
    source = null;
  }

  @Override
  public void start(){
    if(this.source == null){
      // log warning?
      return;
    }

    this.source.each((String key, String val) -> {
      this.model.set(key, val);
    });

    this.source.attributeChangeEvent.addListener((ModelBase.AttributeChangeArgs args) -> {
        this.model.set(args.attr, args.value);
    }, this);
  }

  @Override
  public void stop(){
    if(this.source != null)
      this.source.attributeChangeEvent.removeListeners(this);
  }
}

class ModelJsonParser {
  private ModelBase model;
  private String jsonContent;
  private JSONObject jsonObject;

  public void destroy(){
    model = null;
    jsonContent = null;
  }

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
  private List<AttributeTransformer> attributeTransformers = null;
  private List<ModelTransformer> modelTransformers = null;
  private List<ModelExtension> extensions = null;

  // public Model(){
  // }

  public void destroy(){
    if(attributeTransformers!=null){
      for(AttributeTransformer t : attributeTransformers)
        t.destroy();
      attributeTransformers = null;
    }

    if(modelTransformers != null){
      for(ModelTransformer t : modelTransformers)
        t.destroy();
      modelTransformers = null;
    }

    while(extensions != null && !extensions.isEmpty()){
      ModelExtension ext = extensions.get(0);
      removeExtension(ext);
    }

    super.destroy();
  }

  protected void addExtensions(ModelExtension ext){
    if(extensions == null) // lazy-initialization
      extensions = new ArrayList<>();

    if(!extensions.contains(ext))
      extensions.add(ext);
  }

  protected void removeExtension(ModelExtension ext){
    if(extensions != null){
      extensions.remove(ext);

      if(extensions.isEmpty())
        extensions = null; // cleanup
    }

    ext.destroy();
  }

  protected List<ModelExtension> removeExtensionByOwner(Object owner){
    List<ModelExtension> result = new ArrayList<>();

    for(int idx=extensions.size()-1; idx>=0; idx--){
      ModelExtension ext = extensions.get(idx);

      if(ext.getOwner() == owner){
        removeExtension(ext);
        result.add(ext);
      }
    }

    return result;
  }

  public AttributeTransformer transformAttribute(String attr, Consumer<String> func){
    return transformAttribute(attr, func, null, true);
  }

  public AttributeTransformer transformAttribute(String attr, Consumer<String> func, boolean active){
    return transformAttribute(attr, func, null, active);
  }

  public AttributeTransformer transformAttribute(String attr, Consumer<String> func, Object owner){
    return transformAttribute(attr, func, owner, true);
  }

  public AttributeTransformer transformAttribute(String attr, Consumer<String> func, Object owner, boolean active){
    AttributeTransformer t = new AttributeTransformer(this, attr, func, owner, true);

    if(active){
      if(attributeTransformers == null)
        attributeTransformers = new ArrayList<AttributeTransformer>();
      attributeTransformers.add(t);
    } else {
      t.destroy();
    }

    return t;
  }

  public void stopTransformAttribute(Object owner){
    if(attributeTransformers == null)
      return;

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
    return transform(func, null, true);
  }

  public ModelTransformer transform(Consumer<ModelBase> func, boolean active){
    return transform(func, null, active);
  }

  public ModelTransformer transform(Consumer<ModelBase> func, Object owner){
    return transform(func, owner, true);
  }

  public ModelTransformer transform(Consumer<ModelBase> func, Object owner, boolean active){
    ModelTransformer t = new ModelTransformer(this, func, owner, active);

    if(active){
      if(modelTransformers == null)
        modelTransformers = new ArrayList<ModelTransformer>();

      modelTransformers.add(t);
    } else {
      t.destroy();
    }

    return t;
  }

  public void stopTransform(Object owner){
    if(modelTransformers == null)
      return;

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
   * @return The created ModelFollower extension instance
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
   * @return The created ModelFollower extension instance
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
   * @return The created ModelFollower extension instance
   */
  public ModelFollower follow(ModelBase source, Object owner, boolean active){
    // create follower (this will also execute initial syncing between models)
    ModelFollower f = new ModelFollower(source, this, owner);

    // only store the follower if we want active syncing (not just once) ,
    // otherwise stop the follower and let the garbage collector pick it up
    if(active){
      addExtensions(f);
    } else {
      f.stop();
    }

    return f;
  }

  /** Helper method that hides the somewhat confusing API and terminology of the follow methods */
  public void merge(ModelBase source){
    this.follow(source, null, false);
  }

  /**
   * Stop all active follow connections created using calls to this instance's follow methods
   * @return A list of stopped ModelFollower extension instances
   */
  public List<ModelExtension> stopFollow(){
    return stopFollow(null);
  }

  /**
   * Stop all active follow connections
   * for a specific owner
   * that were created using calls to this instance's follow methods
   *
   * @param owner The owner of the follow connections that need to be stopped
   * @return A list of stopped ModelFollower extension instances
   */
  public List<ModelExtension> stopFollow(Object owner){
    return removeExtensionByOwner(owner);
  }
}
