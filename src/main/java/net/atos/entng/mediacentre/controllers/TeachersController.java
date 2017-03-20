package net.atos.entng.mediacentre.controllers;

import fr.wseduc.webutils.Either;
import net.atos.entng.mediacentre.services.MediacentreService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class TeachersController {

    private     int counter = 0;            // nb of elements currently put in the file
    private     int nbElem = 10000;         // max elements authorized in a file
    private     int fileIndex = 0;          // index of the export file
    private     String pathExport = "";     // path where the generated files are put
    private     Element garEntEnseignant = null;
    private     Document doc = null;

    /**
     *  export Teachers
     */
    public void exportTeachers(final MediacentreService mediacentreService, final String path, int nbElementPerFile){
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;
        mediacentreService.getTeachersExportData(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if( event.isRight()){
                    // write the content into xml file
                    final JsonArray students = event.right().getValue();
                    doc = fileHeader();
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:GAREleve
                    String lastTeacherId = "";
                    Element garEnseignant = null;
                    for( Object obj : students ){
                        if( obj instanceof JsonObject){
                            JsonObject jObj = (JsonObject) obj;
                            if( jObj.getString("u.id") != null && jObj.getString("u.id") != lastTeacherId ) {
                                garEnseignant = doc.createElement("men:GAREnseignant");
                                garEntEnseignant.appendChild(garEnseignant);
                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, jObj.getString("u.id"));
                                MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEnseignant, jObj.getString("u.lastName"));
                                MediacentreController.insertNode("men:GARPersonNom", doc, garEnseignant, jObj.getString("u.displayName"));
                                MediacentreController.insertNode("men:GARPersonPrenom", doc, garEnseignant, jObj.getString("u.firstName"));
                                if (jObj.getString("u.otherNames") != null) {
                                    MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, jObj.getString("u.otherNames"));
                                } else {
                                    MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, jObj.getString("u.firstName"));
                                }
                                MediacentreController.insertNode("men:GARPersonCivilite", doc, garEnseignant, jObj.getString(""));
                                MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEnseignant, jObj.getString("s.UAI"));
                                MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEnseignant, jObj.getString("u.birthDate"));
                                counter += 9;
                                // EnsDisciplinesPostes
                                if( jObj.getArray("u.functions") != null && jObj.getArray("u.functions").size() > 0 ) {
                                    JsonArray functionsArray = jObj.getArray("u.functions");
                                    for (int i = 0; i < functionsArray.size(); i++) {
                                        String function = functionsArray.get(i).toString();
                                        String[] parts = function.split("\\$");
                                        Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, jObj.getString("s.UAI"));
                                        MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, parts[2]);
                                        garEnseignant.appendChild(garEnsDisciplinesPostes);
                                        counter += 3;
                                    }
                                }
                                // PersonProfils
                                Element garProfil = doc.createElement("men:GARPersonProfils");
                                MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, jObj.getString("s.UAI"));
                                if ("Personnel".equals(jObj.getString("p.name"))) {
                                    MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_doc");
                                } else {
                                    MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_ens");
                                }
                                garEnseignant.appendChild(garProfil);
                                MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, jObj.getString("s.UAI"));
                                if( jObj.getString("s2.UAI") != null ) {
                                    MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, jObj.getString("s2.UAI"));
                                }
                                lastTeacherId = jObj.getString("u.id");
                            } else {
                                if( jObj.getString("s2.UAI") != null ) {
                                    MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, jObj.getString("s2.UAI"));
                                }
                            }
                            counter += 6;
                            doc = testNumberOfOccurrences(doc);
                        }
                    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    mediacentreService.getPersonMefTeacher(new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray personMef = event.right().getValue();
                                // men:GARPersonMEF
                                for (Object obj : personMef) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if( jObj.getArray("u.modules") != null && jObj.getArray("u.modules").size() > 0 ) {
                                            JsonArray modulesArray = jObj.getArray("u.modules");
                                            for (int i = 0; i < modulesArray.size(); i++) {
                                                String module = modulesArray.get(i).toString();
                                                String[] parts = module.split("\\$");
                                                Element garPersonMef = doc.createElement("men:GARPersonMEF");
                                                garEntEnseignant.appendChild(garPersonMef);
                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, jObj.getString("s.UAI"));
                                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonMef, jObj.getString("u.id"));
                                                MediacentreController.insertNode("men:GARMEFCode", doc, garPersonMef, parts[1]);
                                                counter += 4;
                                            }
                                        }
                                    }
                                    doc = testNumberOfOccurrences(doc);
                                }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                try {
                                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                    Transformer transformer = transformerFactory.newTransformer();
                                    DOMSource source = new DOMSource(doc);
                                    StreamResult result = null;
                                    if( fileIndex == 0) {
                                        result = new StreamResult(new File(path + "\\Teachers.xml"));
                                    } else {
                                        result = new StreamResult(new File(path + "\\Teachers" + fileIndex + ".xml"));
                                    }

                                    transformer.transform(source, result);

                                    System.out.println("Teachers saved");
                                } catch (TransformerException tfe) {
                                    tfe.printStackTrace();
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private Document testNumberOfOccurrences(Document doc) {
        if (nbElem <= counter) {
            // close the full file
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(pathExport + "\\Teachers" + fileIndex + ".xml"));

                transformer.transform(source, result);

                System.out.println("Teachers" + fileIndex + ".xml saved");
                fileIndex++;
                counter = 0;
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
            }
            // open the new one
            return fileHeader();
        } else {
            return doc;
        }
    }

    private Document fileHeader(){
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        // root elements
        final Document doc = docBuilder.newDocument();
        garEntEnseignant = doc.createElement("men:GAR-ENT-Enseignent");
        doc.appendChild(garEntEnseignant);
        garEntEnseignant.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
        garEntEnseignant.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntEnseignant.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntEnseignant.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntEnseignant.setAttribute("Version", "1.0");
        garEntEnseignant.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
        return doc;
    }

}
