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
}
