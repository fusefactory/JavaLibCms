package com.fuse.cms;

public class ModelCollectionBase extends Collection<Model> {

  /** Default constructor; only creates an instantiator for this collection using setInstantiator method */
  public ModelCollectionBase(){
    this.setInstantiator(() -> new Model());
  }

  public Model findById(String id){
    return findByAttr("id", id, false);
  }

  public Model findById(String id, boolean createIfNotFound){
    return findByAttr("id", id, createIfNotFound);
  }

  public Model findByAttr(String attr, String value){
    return findByAttr(attr, value, false);
  }

  public Model findByAttr(String attr, String value, boolean createIfNotFound){
    // System.out.println("findById: "+id);

    // TODO nicer functional programming implementation?
    for(int idx=0; idx<size(); idx++){
      Model m = get(idx);

      if(m.get(attr, "").equals(value)){
        // System.out.println("FOUND model with id: "+id);
        return m;
      }
    }

    if(!createIfNotFound)
      return null;

    Model m = this.create();
    m.set(attr, value);
    return m;
  }

  /**
   * Convenience method to only accept model with a specific attribute value into this
   * collection. This method only converts the given atribute name/value pair
   * into a simple lambda which is passed on to parent class' accept method.
   *
   * The accept method will both execute on all items currently in the collection
   * and test any items added in the future. See the Collection.accept for more details.
   *
   * @param attrName Name of the attribute for which we'll be specifying a specific required value
   * @param value Value that the specified attribute should have for the model to be accepted into our collection
   */
  public CollectionFilter accept(String attrName, String value){
    return super.accept((Model m) -> {
      String modelValue = m.get(attrName, null);
      return (value == null && modelValue == null) || ((value != null) && value.equals(modelValue));
    });
  }

  /** checks for existing model with same the primary attribute */
  public void loadModel(Model m){
    String primaryKeyAttributeName = "id"; // TODO: make instance var and configurable
    Model existing = null;

    // try to find existing model for this node
    if(m.has(primaryKeyAttributeName)){
      String _id = m.get(primaryKeyAttributeName);
      existing = this.findByAttr(primaryKeyAttributeName, _id);
    }

    if(existing == null){
      //this.add(m); // no existing model with same ID, simply add the model to our collection
      // use create, not add, otherwise StateTest fails
      existing = this.create();
    }

    existing.merge(m);
  }

  public void loadCollection(ModelCollectionBase col){
    for(Model m : col)
      this.loadModel(m);
  }
}
