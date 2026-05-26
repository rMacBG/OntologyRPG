package org.uni.service;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class OntologyService {

 private static final String FILE_PATH = "src/main/java/ontology/RPGOntology.owl";
 private static final String BASE = "http://www.semanticweb.org/rpg";

 private static final String NS = BASE + "#";


 private OntModel model;

 public OntologyService() {
  loadOntology();

 }

 private void loadOntology() {
  model = ModelFactory.createOntologyModel(
          OntModelSpec.OWL_MEM_RULE_INF
  );

  try (InputStream in =
               new FileInputStream(FILE_PATH)) {

   model.read(in, BASE);

   System.out.println("Ontology loaded.");

  } catch (Exception e) {

   e.printStackTrace();
  }
 }

 private void saveOntology() {
    try(OutputStream out = new FileOutputStream(FILE_PATH)){
        model.write(out, "RDF/XML-ABBREV", BASE);
    } catch (Exception ex){
        System.out.println(ex.getMessage());
        return;
    }
 }

 public List<String> getAllCharacters() {
  List<String> characters = new ArrayList<>();
  OntClass cls = model.getOntClass(NS + "Character");
  if(cls == null){
   return characters;
  }

  ExtendedIterator<? extends OntResource> iterator = cls.listInstances();

  while(iterator.hasNext()){
   OntResource resource = iterator.next();

   if(resource.getURI() != null){
     characters.add(resource.getLocalName());
   }
  }
  return characters;
}

public List<String> getIndividualsByClass(String className){
  List<String> result = new ArrayList<>();

  OntClass cls = model.getOntClass(NS + className);

  ExtendedIterator<? extends OntResource> iterator = cls.listInstances();

  while(iterator.hasNext()){
     OntResource resource = iterator.next();

     if(resource.getURI() != null){
        result.add(resource.getLocalName());
     }
  }
  return result;
}
}
