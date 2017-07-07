package com.fuse.cms;

/**
 * The State class is an extension to the Model class
 * designed for managing application state. It lets
 * you create a hierarchical state structure; every
 * State instance can access child- or parent-states,
 * and all these states have access to the central state
 * repository (which is simple a ModelCollection, as all
 * states are in the end models).
 */
public class State extends Model {
    private ModelCollection collection;

    /** Default constructor; creates it own collection
     * collection as central state-repository and should therefore
     * only be used to initialize an application's root state
     * (though the collection can be overwritten by a later call
     * to the setCollection method).
     *
     * It also creates a custom instantiator and filter for this collection, so
     * any item that gets added to it will be a valid State item.
     */
    public State(){
      ModelCollection col = new ModelCollection();
      // add instantiator so every item that gets added to this collection
      // is a state instance linked to this collection
      col.setInstantiator(() -> new State(col));
      // only accept State items into the collection (and filter-out any non-State items currently in collection)
      col.accept((Model m) -> m.getClass() == State.class);
      col.add(this);
      setCollection(col);
    }

    public State(ModelCollection collection){
        setCollection(collection);
    }

    public void setCollection(ModelCollection newCol){
        this.collection = newCol;
    }

    public ModelCollection getCollection(){
         return collection;
    }

    public State child(String name){
      return this.findById(childId(name), true);
    }

    public State parent(){
      String _id = parentId();
      return findById(_id, false);
    }

    public State childQuery(String query){
      return findById(childId(query), true);
    }

    private State findById(String id, boolean createIfNotExist){
      // find existing
      Model m = collection.findById(id);
      if(m != null)
        return (State) m;

      if(!createIfNotExist)
        return null;

      State newState = new State(collection);
      newState.set("id", id);
      collection.add(newState);
      return newState;
    }

    /**
     * Returns the id of the parent node
     *
     * @return String null if this is already the root state
     */
    private String parentId(){
      String _id = getId();

      if(_id.equals(""))
        return null;

      String[] parts = _id.split("\\.");

      if(parts.length < 1)
        return null;

      if(parts.length == 1)
        return "";

      String parentId = "";
      for(int i=0; i<parts.length-2; i++){
        parentId = parentId + parts[i] + ".";
      }
      parentId = parentId + parts[parts.length-2];
      return parentId;
    }

    private String childId(String childName){
      return getId().equals("") ? childName : getId()+"."+childName;
    }
}
