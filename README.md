# JavaLibCms
CMS for Java applications; high level utility classes for managing data (collections)

### Usage: Model attribute transformers

```java
import com.fuse.cms.Model;

// Create a model with a 'title' attribute
Model model = new Model();
model.set("title", "New Model");

// register an attribute transformer for the title attribute;
// the transformer is called for the current value (if there is one)
// and every future value when the attribute is updated
model.transformAttribute("title", (String value) -> {
    System.out.println("Model title: " + value);
}); // outputs: "Model title: New Model"

model.set("title", "Updated Model"); // outputs: "Model title: Updated Model"
```

### Usage: Model transformers

```java
import com.fuse.cms.Model;

// Create a model with some attributes
Model model = new Model();
model.set("name", "Bobby");
model.set("age", "13");

// register a model transformer;
// the transformer is called for the current model state,
// and every time the model is updated
model.transform((ModelBase m) -> {
    int age = m.getInt("age");

    if(age < 10){
        System.out.println("Kid: " + m.get("name"));
    } else if(age < 20){
        System.out.println("Teen: " + m.get("name"));
    } else {
        System.out.println("Grownup: " + m.get("name"));
    }
}); // outputs: "Teen: Bobby"

model.set("name", "bob"); // outputs: "Teen: bob"
model.set("age", "43"); // outputs: "Grownup: bob"
```


### Usage: Collection
_TODO_
* JSON loading
* filter
* syncing
* fetching
* transformers


### Usage: MapCollection
_TODO_
* getAsync

### Usage: ResourceManager
_TODO_
