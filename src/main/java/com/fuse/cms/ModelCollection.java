package com.fuse.cms;

import java.util.logging.*;
import java.util.Iterator;
import java.nio.file.*;
import org.json.*;

class JsonLoader {
  private ModelCollectionBase collection;
  private Logger logger;
  private String primaryKeyAttributeName;

  public JsonLoader(ModelCollectionBase collection){
    logger = Logger.getLogger(JsonLoader.class.getName());
    this.collection = collection;
    primaryKeyAttributeName = "id";
  }

  public boolean loadFile(String filePath){
    logger.fine("loading collection data from file: " + filePath);

    String content;
    try {
      content = new String(Files.readAllBytes(Paths.get(filePath)));
    } catch(java.io.IOException exc){
      logger.warning("IOException: "+exc.toString());
      return false;
    }

    logger.finer("file content: " + content);
    return loadJson(content);
  }

  public boolean loadJson(String jsonText){
    JSONObject jsonObject = null;
    org.json.JSONException objException = null;
    try{
      jsonObject = new JSONObject(jsonText);
    } catch(org.json.JSONException exc){
      objException = exc;
    }

    if(jsonObject != null)
      return loadJson(jsonObject);

    JSONArray jsonArray = null;
    org.json.JSONException arrException = null;
    try{
      jsonArray = new JSONArray(jsonText);
    } catch(org.json.JSONException exc){
      arrException = exc;
    }

    if(jsonArray != null)
      return loadJson(jsonArray);

    logger.warning("Unknown/corrupted json format:\n"+jsonText);
    logger.warning("Could not parse as object: "+objException.toString());
    logger.warning("Could not parse as array: "+arrException.toString());
    return false;
  }

  public boolean loadJson(JSONObject json){
    logger.warning("ModelCollection.loadJson for JSONObject not implemented yet!");
    return false;
    // Iterator<String> it = json.keys();
    //
    // while(it.hasNext()){
    //   String attr = it.next();
    //   // model.set(attr, json.get(attr).toString());
    // }
    //
    // return true;
  }

  /**
   * Loads given json array by looping over each node and using the 'id' attribute
   * to find any existing model in our collection. If no model is found, one is created.
   * The found or created model is then populated with the data inside that json node.
   *
   * @param json The json array
   *
   * @return boolean Returns true is all json nodes were loaded successfully
   */
  public boolean loadJson(JSONArray json){
    boolean allGood = true;

    // loop over all json nodes
    for(int idx=0; idx<json.length(); idx++){
      JSONObject jsonObject = json.getJSONObject(idx);
      Model model = null;

      // try to find existing model for this node
      if(jsonObject.has(primaryKeyAttributeName)){
        String jsonId = jsonObject.get(primaryKeyAttributeName).toString();
        model = this.collection.findByAttr(primaryKeyAttributeName, jsonId);
      }

      // no model found, create one
      if(model == null){
        model = this.collection.create();
      }

      // load model with json node's data
      allGood &= model.loadJson(jsonObject);
    }

    // return true if all json nodes were loaded without issues
    return allGood;
  }
}

class JsonWriter {
  private ModelCollectionBase collection;
  private Logger logger;
}

public class ModelCollection extends ModelCollectionBase {

  @Override
  public boolean loadJsonFromFile(String filePath){
    return new JsonLoader(this).loadFile(filePath);
  }

  public boolean loadJson(JSONObject json){
    return new JsonLoader(this).loadJson(json);
  }

  public boolean loadJson(JSONArray json){
    return new JsonLoader(this).loadJson(json);
  }

  /**
   * Load json string, mostly used for testing
   *
   * @param jsonContent A string containing json data
   */
  public boolean loadJson(String jsonContent){
    return new JsonLoader(this).loadJson(jsonContent);
  }

  public String toJsonString(){
    return "";
  }

  public boolean saveJsonToFile(String filePath){
    return false;
  }

  /** Convenience method that creates a new ModelCollection that filters
   * on a specific attribute value (see accept ModelCollectionBase.accept method)
   * and syncs from this collection.
   *
   * @param attrName Name of filter attribute, see accept method
   * @param value Value of filter attribute, see accept method
   */
  public ModelCollection filtered(String attrName, String value){
    ModelCollection newCol = new ModelCollection();
    newCol.accept(attrName, value);
    newCol.sync(this);
    return newCol;
  }
}
