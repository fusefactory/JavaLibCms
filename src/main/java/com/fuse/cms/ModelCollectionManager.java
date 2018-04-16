package com.fuse.cms;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import org.json.JSONObject;


class ModelCollectionManagerJsonLoader {
  private ModelCollectionManager manager;
  private Logger logger;
  private Charset charset;

  public ModelCollectionManagerJsonLoader(ModelCollectionManager manager){
    logger = Logger.getLogger(JsonLoader.class.getName());
    this.manager = manager;
    this.charset = Charset.forName("UTF-8");
  }

  public boolean loadFile(String filePath){
    logger.fine("loading collection data from file: " + filePath);

    String content;
    try {
      content = new String(Files.readAllBytes(Paths.get(filePath)), this.charset);
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

    // JSONArray jsonArray = null;
    // org.json.JSONException arrException = null;
    // try{
    //   jsonArray = new JSONArray(jsonText);
    // } catch(org.json.JSONException exc){
    //   arrException = exc;
    // }
    //
    // if(jsonArray != null)
    //   return loadJson(jsonArray);

    logger.warning("Unknown/corrupted json format:\n"+jsonText);
    logger.warning("Could not parse as object: "+objException.toString());
    // logger.warning("Could not parse as array: "+arrException.toString());
    return false;
  }

  public boolean loadJson(JSONObject json){
    Iterator<String> it = json.keys();
    ModelCollection col;
    boolean allGood = true;

    while(it.hasNext()){
      String name = it.next();
      col = this.manager.get(name, true);
      allGood &= col.loadJson(json.getJSONArray(name));
    }

    return allGood;
  }

  // public boolean loadJson(JSONArray json){
  //   boolean allGood = true;
  //
  //   for(int idx=0; idx<json.length(); idx++){
  //     JSONObject jsonObject = json.getJSONObject(idx);
  //     Model model = null;
  //     Model newModel = null;
  //
  //     if(jsonObject.has("id")){
  //       String jsonId = jsonObject.get("id").toString();
  //       model = this.collection.findById(jsonId);
  //     }
  //
  //     if(model == null){
  //       newModel = new Model();
  //       model = newModel;
  //     }
  //
  //     allGood &= model.loadJson(jsonObject);
  //
  //     // add to collection (if we created a new model) AFTER parsing json
  //     if(newModel != null)
  //       this.collection.add(newModel);
  //   }
  //
  //   return allGood;
  // }
}


public class ModelCollectionManager extends HashMap<String, ModelCollection> {
  public boolean loadJsonFromFile(String path){
    return (new ModelCollectionManagerJsonLoader(this)).loadFile(path);
  }

  public ModelCollection get(String name, boolean createIfNecessary){
    ModelCollection col = super.get(name);

    if(col == null && createIfNecessary){
      col = new ModelCollection();
      this.put(name, col);
    }

    return col;
  }
}
