package com.fuse.cms;


public class State {
    private ModelCollection collection;

    public State(){
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
}
