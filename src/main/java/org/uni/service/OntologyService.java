package org.uni.service;

import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class OntologyService {

    private static final String FILE_PATH = "ontology/RPGGameOntology.rdf";
    private static final String BASE = "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology";

    private static final String NS = BASE + "#";


    private OntModel model;
    //OntDocumentManager dm = model.getDocumentManager();

    public OntologyService() {
        loadOntology();

    }

    private void loadOntology() {
        model = ModelFactory.createOntologyModel(
                OntModelSpec.OWL_MEM_RULE_INF);

        OntDocumentManager dm = model.getDocumentManager();

        dm.addAltEntry("http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology",
                "file:src/main/resources/ontology/RPGGameOntology.rdf");

        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream(FILE_PATH)) {

            model.read(in, BASE, "RDF/XML");


            System.out.println("Ontology loaded.");

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    private void saveOntology() {
        try (OutputStream out = new FileOutputStream("src/main/resources/" + FILE_PATH)) {
            model.write(out, "RDF/XML", BASE);
            System.out.println("Ontology saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllCharacters() {
        List<String> characters = new ArrayList<>();
        OntClass cls = model.getOntClass(NS + "Character");
        if (cls == null) {
            return characters;
        }

        ExtendedIterator<? extends OntResource> iterator = cls.listInstances();

        while (iterator.hasNext()) {
            OntResource resource = iterator.next();

            if (resource.getURI() != null) {
                characters.add(resource.getLocalName());
            }
        }
        return characters;
    }

    public List<String> getIndividualsByClass(String className) {
        List<String> result = new ArrayList<>();


        OntClass cls = model.getOntClass(NS + className);

        if (cls == null) {
            return result;
        }
        ExtendedIterator<? extends OntResource> iterator = cls.listInstances();

        while (iterator.hasNext()) {
            OntResource resource = iterator.next();

            if (resource.getURI() != null) {
                result.add(resource.getLocalName());
            }
        }
        return result;
    }

    public int getIntProperty(String individualName, String propertyName) {
        String value = getPropertyValue(individualName, propertyName);
        if (value == null) return 0;

        if (value.contains("^^")) {
            value = value.split("\\^\\^")[0];
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getIntFromIndividual(String individualName, String propertyName) {
        if (model == null || individualName == null || propertyName == null) return 0;


        Individual individual = model.getIndividual(NS + individualName.trim());
        if (individual == null) {
            System.out.println("Individual not found! " + individualName);
            return 0;
        }

        DatatypeProperty property = model.getDatatypeProperty(NS + propertyName.trim());
        if (property == null) return 0;

        var value = individual.getPropertyValue(property);
        if (value != null && value.isLiteral()) {
            return value.asLiteral().getInt();
        }
        return 0;
    }

    public void addIndividual(String className, String individualName) {
        OntClass cls = model.getOntClass(NS + className);

        if (cls == null) {
            return;
        }

        if (model.getOntClass(NS + individualName) != null) {
            return;
        }

        model.createIndividual(NS + individualName, cls);

        saveOntology();
    }

    public void deleteIndividual(String individualName) {
        var individual = model.getIndividual(NS + individualName);

        if (individual == null) {
            return;
        }

        model.removeAll(individual, null, null);
        model.removeAll(null, null, individual);

        saveOntology();
    }

    public List<String> getPropertiesOfIndividual(String individualName, String propertyName) {
        List<String> result = new ArrayList<>();

        var individual = model.getIndividual(NS + individualName);

        if (individual == null) {
            return result;
        }

        var proeprty = model.getProperty(NS + propertyName);

        if (individual == null) {
            return result;
        }

        var iterator = individual.listPropertyValues(proeprty);

        while (iterator.hasNext()) {
            var node = iterator.next();

            if (node.isResource()) {
                result.add(
                        node.asResource().getLocalName()
                );
            }
        }
        return result;
    }

    public void updateIndividualProperty(String individualName, String propertyName, int newValue) {
        var subject = model.getIndividual(NS + individualName);
        if (subject == null) return;

        var property = model.getProperty(NS + propertyName);
        if (property == null) return;

        subject.removeAll(property);

        subject.addProperty(property, model.createTypedLiteral(newValue));

        saveOntology();
    }

    public List<String> getAllClasses() {
        List<String> result = new ArrayList<>();

        var iterator = model.listClasses();

        while (iterator.hasNext()) {
            var cls = iterator.next();

            if (cls.getURI() != null) {
                result.add(cls.getLocalName());
            }
        }
        return result;
    }


    public boolean IndividualExists(String name) {
        return model.getIndividual(NS + name) != null;
    }

    public void addPropertyToIndividual(String subjectName, String propertyName, String objectName) {
        var subject = model.getIndividual(NS + subjectName);

        if (subject == null) {
            return;
        }

        var property = model.getObjectProperty(NS + propertyName);

        if (property == null) {
            return;
        }

        var object = model.getResource(NS + objectName);

        subject.addProperty(property, object);

        saveOntology();
    }

    public void removePropertyFromIndividual(String subjectName, String propertyName, String objectName) {

        var subject = model.getResource(NS + subjectName);

        var property = model.getProperty(NS + propertyName);

        var object = model.getResource(NS + objectName);

        model.remove(subject, property, object);

        saveOntology();
    }

    public List<String> getInferredTypes(String individualName) {
        List<String> result = new ArrayList<>();

        var individual = model.getIndividual(NS + individualName);

        if (individual == null) {
            return result;
        }

        ExtendedIterator<?> iterator = individual.listRDFTypes(false);

        while (iterator.hasNext()) {
            var resource = iterator.next();

            if (resource instanceof OntResource ontResource) {
                if (ontResource.getURI() != null) {
                    result.add(
                            ontResource.getLocalName()
                    );
                }
            }
        }
        return result;
    }

    public String getPropertyValue(String individualName, String propertyName) {
        if (model == null || individualName == null || propertyName == null) return null;
        String ind = individualName.trim().replace(" ", "");
        String prop = propertyName.trim().replace(" ", "");

        if (ind.isEmpty() || prop.isEmpty()) {
            System.out.println("Tried to search an empty individual!");
            return null;
        }
        String queryString =
                "SELECT ?object WHERE {\n" +
                        "    <" + NS + ind + "> <" + NS + prop + "> ?object .\n" +
                        "}";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                var node = soln.get("object");
                if (node != null) {
                    if (node.isResource()) {
                        return node.asResource().getLocalName();
                    } else {
                        return node.toString();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("SPARQL Error: " + e.getMessage());
        }
        return null;
    }
}
