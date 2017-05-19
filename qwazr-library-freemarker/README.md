## Freemarker

[Freemarker](http://freemarker.org/) is a fast template engine.

To know how to build a template please [refer to the manual](http://freemarker.org/docs/index.html).


## Configuration

Here are common configurations examples.
The configuration file should be put in a json file in the
/var/lib/qwazr/etc directory.

### User a directory

Example of configuration where templates files are stored in a directory.
 
A minimal configuration:
 
```json
 {
   "library": {
    "freemarker_files": {
      "class": "com.qwazr.library.freemarker.FreeMarkerTool",
      "template_path": "/var/web/templates"
    }
  }
}
```
 
A typical configuration:
 
```json
{
  "library": {
   "freemarker_files": {
     "class": "com.qwazr.library.freemarker.FreeMarkerTool",
     "output_encoding": "UTF-8",
     "default_encoding": "UTF-8",
     "default_content_type": "TEXT/HTML",
     "template_path": "/var/web/templates"
   }
 }
}
```

### Using JAVA resources

Example of configuration where templates files are stored as JAVA resources
`/src/main/resources/...`

A minimal configuration:

```json
{
  "library": {
    "freemarker_classloader": {
      "class": "com.qwazr.library.freemarker.FreeMarkerTool",
      "use_classloader": true
    }
  }
}
```

Typical configuration:

```json
{
  "library": {
    "freemarker_classloader": {
      "class": "com.qwazr.library.freemarker.FreeMarkerTool",
      "use_classloader": true,
      "output_encoding": "UTF-8",
      "default_encoding": "UTF-8",
      "default_content_type": "TEXT/HTML"
    }
  }
 }
 ```
  
 ### Javascript usage
 
 ```javascript
 //Prepare some variables used in the template
 my_variables = {
    title: 'my_title',
    content: 'my_content'
 }
 // Return the template
 return freemarker.template('/path/to/template.ftl', my_variables)
 ```