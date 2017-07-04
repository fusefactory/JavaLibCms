package com.fuse.cms;

import java.util.logging.*;
import java.util.List;
import java.util.ArrayList;
import java.util.function.*;

class CollectionFilter<T> {
  private CollectionBase<T> collection;
  private List<Predicate<T>> filterFuncs;

  public CollectionFilter(CollectionBase<T> collection){
    filterFuncs = new ArrayList<Predicate<T>>();
    this.collection = collection;
  }

  public void addFilter(Predicate<T> filterFunc){
    filterFuncs.add(filterFunc);

    // apply filter to current items in collection
    this.collection.each((T item) -> {
      if(!filterFunc.test(item))
        this.collection.remove(item);
    });

    // register listener for items that get added later
    this.collection.beforeAddTest.addListener(filterFunc, this);
  }
};

class CollectionSyncer<T> {
  private Logger logger;
  private CollectionBase<T> collection;
  private List<CollectionBase<T>> activeSources, stoppedSources;
  private boolean bActive;

  public CollectionSyncer(CollectionBase<T> collection){
    bActive = true;
    this.logger = Logger.getLogger(CollectionBase.class.getName());
    this.collection = collection;
    this.activeSources = new ArrayList<CollectionBase<T>>();
    this.stoppedSources = new ArrayList<CollectionBase<T>>();
  }

  public void start(){
    for(CollectionBase<T> col : stoppedSources){
      activate(col);
      activeSources.add(col);
    }

    stoppedSources.clear();
    bActive = true;
  }

  public void stop(){
    for(CollectionBase<T> col : activeSources){
      deactivate(col);
      stoppedSources.add(col);
    }

    activeSources.clear();
    bActive = false;
  }

  public void addSource(CollectionBase<T> col){
    if(bActive){
      activate(col);
      activeSources.add(col);
      return;
    }

    stoppedSources.add(col);
  }

  public void removeSource(CollectionBase<T> col){
    if(activeSources.contains(col)){
      deactivate(col);
      activeSources.remove(col);
    }

    if(stoppedSources.contains(col)){
      stoppedSources.remove(col);
    }
  }

  private void activate(CollectionBase<T> col){
      activeSources.add(col);

      // add all current items from source which are not already in target
      col.each((T item) -> {
        if(this.collection.indexOf(item) == -1)
          this.collection.add(item);
      });

      // register listener that adds new items added to source, also to target
      col.addEvent.addListener((T item) -> {
        this.collection.add(item);
      }, this);

      // register listener that removes each item from target that gets removed from source
      col.removeEvent.addListener((T item) -> {
        if(this.collection.indexOf(item) != -1)
          this.collection.remove(item);
      }, this);
  }

  private void deactivate(CollectionBase<T> col){
    activeSources.remove(col);
    stoppedSources.add(col);
    col.addEvent.removeListeners(this);
    col.removeEvent.removeListeners(this);
  }
}


class CollectionTransformerBase {
  public Object owner;

  public void start(){}
  public void stop(){}
}

class CollectionTransformer<S,T> extends CollectionTransformerBase {
  class Link {
    public S source;
    public T target;
  }

  private CollectionBase<S> source;
  private CollectionBase<T> target;
  private List<Link> links;
  private Function<S,T> func;
  private boolean active;

  public CollectionTransformer(CollectionBase<S> source, CollectionBase<T> target, Function<S, T> func){
    this.source = source;
    this.target = target;
    this.links = new ArrayList<>();
    this.func = func;
    active = false;

    source.each((S item) -> {
      transform(item);
    });

    start();
  }

  private void transform(S item){
    Link link = new Link();
    link.source = item;
    link.target = this.func.apply(item);
    links.add(link);
    target.add(link.target);
  }

  @Override
  public void start(){
    if(active)
      return;

    source.addEvent.addListener((S sourceItem) -> {
      this.transform(sourceItem);
    }, this);

    source.removeEvent.addListener((S sourceItem) -> {
      Link foundLink = null;
      for(Link l : links){
        if(l.source == sourceItem){
          foundLink = l;
          break;
        }
      }

      if(foundLink != null){
        target.remove(foundLink.target);
        links.remove(foundLink);
      }
    }, this);

    active = true;
  }

  @Override
  public void stop(){
    if(!active)
      return;

    source.addEvent.removeListeners(this);
    source.removeEvent.removeListeners(this);
    active = false;
  }
}

public class Collection<T> extends CollectionBase<T> {

  private CollectionFilter<T> colFilter;
  private CollectionSyncer<T> colSyncer;
  private List<CollectionTransformerBase> collectionTransformers;

  /// virtual method, currently only implemented in ModelCollection
  public boolean loadJsonFromFile(String filePath){
    return false;
  }

  public Collection(){
    collectionTransformers = new ArrayList<>();
  }

  /// register filter logic
  public CollectionFilter<T> accept(Predicate<T> func){
    if(colFilter == null)
      colFilter = new CollectionFilter<T>(this);

    colFilter.addFilter(func);
    return colFilter;
  }

  /// default to active=true
  public CollectionSyncer<T> sync(CollectionBase<T> sourceCollection){
    return sync(sourceCollection, true);
  }

  /// copy content of sourceCollection and register listener to copy future items added to sourceCollection
  public CollectionSyncer<T> sync(CollectionBase<T> sourceCollection, boolean active){
    if(colSyncer == null)
      colSyncer = new CollectionSyncer<T>(this);

    colSyncer.addSource(sourceCollection);

    if(!active)
      colSyncer.removeSource(sourceCollection);

    return colSyncer;
  }

  /// stop copying sourceCollection (unregisters listener that monitors for newly added items on sourceCollection)
  public CollectionSyncer<T> stopSync(CollectionBase<T> sourceCollection){
    if(colSyncer == null)
      colSyncer = new CollectionSyncer<T>(this);

    colSyncer.removeSource(sourceCollection);
    return colSyncer;
  }

  /// create new collection instance which syncs from this but registers the given filter
  public Collection<T> filtered(Predicate<T> func){
    Collection<T> newCol = new Collection<T>();
    newCol.accept(func);
    newCol.sync(this);
    return newCol;
  }

  /// execute given consumer for all current and all future items
  public void forAll(Consumer<T> func, Object owner){
    // run for all current events
    this.each(func);
    // register listener to run for all future events
    this.addEvent.addListener(func, owner);
  }

  // stop registered "forAll" callbacks
  // TODO differentiate between forAll callbacks and addEvent listeners
  public void stopForAll(Object owner){
    this.addEvent.removeListeners(owner);
  }

  // returns a new collection which mirrores the current collection
  public <U> Collection<U> transform(Function<T, U> func){
    return transform(func, null);
  }

  public <U> Collection<U> transform(Function<T, U> func, Object owner){
    Collection<U> target = new Collection<U>();
    CollectionTransformer<T, U> transformer = new CollectionTransformer(this, target, func);
    transformer.owner = owner;
    collectionTransformers.add(transformer);
    return target;
  }

  public void stopTransforms(Object owner){
    for(CollectionTransformerBase trans : collectionTransformers){
      if(trans.owner == owner){
        trans.stop();
      }
    }
  }
}
