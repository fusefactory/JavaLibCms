package com.fuse.cms;

public class ModelCollectionBase extends Collection<Model> {

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

    Model m = new Model();
    m.set(attr, value);
    this.add(m);
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
}
