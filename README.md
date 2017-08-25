# JavaLibCms

[![Build Status](https://travis-ci.org/fusefactory/JavaLibCms.svg?branch=master)](https://travis-ci.org/fusefactory/JavaLibCms)

"CMS for Java applications" - high level utility classes for managing data (collections) and setting up active data stream.

## Rational

The main data-container classes in this package (Model and Collection) and the event-based bindings are inspired by [Backbone.js](http://backbonejs.org/).

The main purpose of these classes is to facilitate building powerful data-level APIs
and help enforcing MVC-like application structure, where data (M; model) and Representation (V; view) are kept separated.

The Model class is basically a String-based key/value pairs container with events and an advanced API for handling data and dealing with data manipulations (see transformer usage examples below).

The Collection class is not much more than a general purpose list class (it extends the java.util.ArrayList) but with add/remove events to allow the user to hook into data manipulations, as well as high level API to iterate over, filter and transform the contents of the Collections (see usage examples below).

## JavaDocs
https://fusefactory.github.io/JavaLibCms/site/apidocs/index.html

## Installation
Use as maven/gradle/sbt/leiningen dependency with [JitPack](https://github.com/jitpack/maven-modular)
* https://jitpack.io/#fusefactory/JavaLibCms

For more info on jitpack see;
* https://github.com/jitpack/maven-simple
* https://jitpack.io/docs/?#building-with-jitpack

## Dependencies
This repo uses [maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) for dependency management (see the pom.xml file).

Runtime Dependencies are:
* org.json/json [(mvn)](https://mvnrepository.com/artifact/org.json/json)
* [fusefactory](http://fuseinteractive.it/)'s [JavaLibEvent package](https://github.com/fusefactory/JavaLibEvent) [(jitpack)](https://jitpack.io/#fusefactory/JavaLibEvent)



## USAGE: Model attribute transformers for creating a link between a single data value and a view object

In the example below, a model instance functions as data-source for the some custom documentView object. The transformAttribute method immediately runs the given lambda with the current value of the "title" attribute (only if there is one) and also runs the lambda for every new value when the attribute is updated in the future (until the transformer is remove). This essentially creates a link between the data source and the


```java
import com.fuse.cms.Model;

// Create a model with a 'title' attribute
Model model = new Model();
model.set("title", "New Title");

model.transformAttribute("title", (String value) -> {
    documentView.setTitleText(value);
});

// the documentView object is now initialized with the "New Title" text

model.set("title", "Updated Title");

// the documentView object is automatically updated with the "Updated Title" text
```

## USAGE: Model transformers for creating a link between a data model instance (multiple key/value pairs) and a view object

In the example below a model instance is used to populate a view object. A single model "transformer" is registered, which will be invoked immediately when the transformer is registered, as well as every time an attribute in that model is updated.

Note that though int- and float-based values are being used, all data inside Models is String-based. The get* and set methods of the Model can convert most native types from/into strings.

```java
import com.fuse.cms.Model;

// Create a model with an int-based age attribute and a float-based price attribute

Model model = new Model();
model.set("price", 9.99);
model.set("age", 13);

model.transform((ModelBase m) -> {
    int age = m.getInt("age");

    // use age attribute to set the right target audience tagline
    if(age < 10){
        someViewObject.textTagline("for Kid!");
    } else if(age < 20){
        someViewObject.textTagline("for Teens!");
    } else {
        someViewObject.textTagline("for Grownups!");
    }

    // use price attribute to set the price on our view object
    someViewObject.setPrice(m.getFloat("price"));
}); // outputs: "Teen: Bobby"

model.set("name", "bob"); // outputs: "Teen: bob"
model.set("age", "43"); // outputs: "Grownup: bob"
```


## Usage: Collection
_TODO_
* JSON loading
* filter
* syncing
* fetching
* transformers


## Usage: ModelCollectionManager
_TODO_
* load json file into multiple model collections

## Usage: AsyncFacade
_TODO_
* Robust interface for asynchronous (optionally threaded) operations.
