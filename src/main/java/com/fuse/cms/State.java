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
     * any item that gets added to it will be a valid State item (see setCollection method).
     */
    public State(){
      ModelCollection col = new ModelCollection();
      setCollection(col);
      col.add(this);
    }

    public State(ModelCollection collection){
        setCollection(collection);
    }

    /**
     * Set 'central state repository' collection; this method will register
     * an instantiator (used in child method) and filter on the collection to
     * 'protect' it from non-State items.
     *
     * @param col The collection to use a central state repository.
     */
    public void setCollection(ModelCollection col){
      // add instantiator so every item that gets added to this collection
      // is a state instance linked to this collection
      col.setInstantiator(() -> {
        State s = new State();
        s.setSafeCollection(col);
        return s;
      });

      // only accept State items into the collection (and filter-out any non-State items currently in collection)
      col.accept((Model m) -> m.getClass() == State.class);

      setSafeCollection(col);
    }

    /**
     * Sets this State's collection reference to a collection which is
     * declared to be 'protected' from non-State items (see setCollection method)
     */
    private void setSafeCollection(ModelCollection newCol){
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

      State newState = (State)collection.create();
      newState.set("id", id);
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
