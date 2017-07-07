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

  public boolean loadJson(JSONArray json){
    boolean allGood = true;

    for(int idx=0; idx<json.length(); idx++){
      JSONObject jsonObject = json.getJSONObject(idx);
      Model model = null;
      Model newModel = null;

      if(jsonObject.has(primaryKeyAttributeName)){
        String jsonId = jsonObject.get(primaryKeyAttributeName).toString();
        model = this.collection.findByAttr(primaryKeyAttributeName, jsonId);
      }

      if(model == null){
        newModel = new Model();
        model = newModel;
      }

      allGood &= model.loadJson(jsonObject);

      // add to collection (if we created a new model) AFTER parsing json
      if(newModel != null)
        this.collection.add(newModel);
    }

    return allGood;
  }
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
