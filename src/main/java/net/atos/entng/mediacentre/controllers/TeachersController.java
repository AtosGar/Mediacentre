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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;

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
        mediacentreService.getAllStructures(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // construct hashmap association between UAI and externalId
                    final Map<String, String> mapStructures = new HashMap<String, String>();
                    JsonArray allStructures = event.right().getValue();
                    for (Object obj : allStructures) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            mapStructures.put(jObj.getString("s.externalId"), jObj.getString("s.UAI"));
                        }
                    }
                    mediacentreService.getTeachersExportData(new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                final JsonArray students = event.right().getValue();
                                doc = fileHeader();
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                // men:GAREleve
                                String lastTeacherId = "";
                                String lastTeacherBirthDate = "";
                                Element garEnseignant = null;
                                ArrayList<String[]> listDisciplinesPostes = new ArrayList<String[]>();
                                for (Object obj : students) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (jObj.getString("u.id") != null && !lastTeacherId.equals(jObj.getString("u.id"))) {
                                            // add all the disciplinesPostes, because it's a new node
                                            if (garEnseignant != null) {
                                                MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEnseignant, lastTeacherBirthDate);
                                                for (String[] data : listDisciplinesPostes) {
                                                    Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                                    MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                                    MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, data[2]);
                                                    garEnseignant.appendChild(garEnsDisciplinesPostes);
                                                    counter += 3;
                                                }
                                            }
                                            doc = testNumberOfOccurrences(doc);
                                            listDisciplinesPostes = new ArrayList<String[]>();
                                            garEnseignant = doc.createElement("men:GAREnseignant");
                                            garEntEnseignant.appendChild(garEnseignant);
                                            MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, jObj.getString("u.id"));


                                            // GARPersonProfils - can have multiple
                                            Map<String, String> structProfile = new HashMap<String, String>();
                                            String profilType = "National_ens";
                                            if (jObj.getArray("u.functions") != null && jObj.getArray("u.functions").size() > 0) {
                                                JsonArray functionsArray = jObj.getArray("u.functions");
                                                for (int i = 0; i < functionsArray.size(); i++) {
                                                    String function = functionsArray.get(i).toString();
                                                    String[] parts = function.split("\\$");
                                                    String[] data = new String[3];
                                                    data[0] = parts[0]; //jObj.getString("s.UAI");
                                                    data[1] = parts[2];
                                                    data[2] = parts[3];
                                                    if( "DOC".equals(parts[1]) && "DOCUMENTATION".equals(parts[2]) ){
                                                        profilType = "National_doc";
                                                    }
                                                    structProfile.put(data[0].toString(),data[1]); // for  GARPersonProfils, because it can be multiple for 1 structure
                                                    listDisciplinesPostes.add(data);
                                                }
                                            }
                                            for (String key : structProfile.keySet()) {
                                                Element garProfil = doc.createElement("men:GARPersonProfils");
                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, mapStructures.get(key));
                                                //if ("Personnel".equals(jObj.getString("p.name"))) {
                                                MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, profilType/*"National_doc"*/);
                                                /*} else {
                                                    MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_ens");
                                                }*/
                                                garEnseignant.appendChild(garProfil);
                                            }
                                            MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEnseignant, jObj.getString("u.lastName"));
                                            MediacentreController.insertNode("men:GARPersonNom", doc, garEnseignant, jObj.getString("u.lastName"));
                                            MediacentreController.insertNode("men:GARPersonPrenom", doc, garEnseignant, jObj.getString("u.firstName"));
                                            if (jObj.getString("u.otherNames") != null) {
                                                MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, jObj.getString("u.otherNames"));
                                            } else {
                                                MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, jObj.getString("u.firstName"));
                                            }
                                            MediacentreController.insertNode("men:GARPersonCivilite", doc, garEnseignant, jObj.getString(""));
                                            MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEnseignant, jObj.getString("s.UAI"));
                                            MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, jObj.getString("s.UAI"));
                                            if (jObj.getString("s2.UAI") != null) {
                                                MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, jObj.getString("s2.UAI"));
                                            }
                                            lastTeacherId = jObj.getString("u.id");
                                            lastTeacherBirthDate = jObj.getString("u.birthDate");
                                        } else {
                                            if (jObj.getString("s2.UAI") != null) {
                                                MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, jObj.getString("s2.UAI"));
                                            }
                                            counter += 9;
                                            // EnsDisciplinesPostes : save all, in order to place them at the END of node  (after ALL s2.UAI)
                                            /*if (jObj.getArray("u.functions") != null && jObj.getArray("u.functions").size() > 0) {
                                                JsonArray functionsArray = jObj.getArray("u.functions");
                                                for (int i = 0; i < functionsArray.size(); i++) {
                                                    String function = functionsArray.get(i).toString();
                                                    String[] parts = function.split("\\$");
                                                    String[] data = new String[2];
                                                    data[0] = parts[0]; //jObj.getString("s.UAI");
                                                    data[1] = parts[2];
                                                    listDisciplinesPostes.add(data);
                                                }
                                            }*/
                                        }
                                        counter += 6;
                                        // end, so we add the last not added disciplinesPostes
                                    }

                                }

                                MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEnseignant, lastTeacherBirthDate);
                                for (String[] data : listDisciplinesPostes) {
                                    Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                    MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes,  mapStructures.get(data[0]));
                                    MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, data[2]);
                                    garEnseignant.appendChild(garEnsDisciplinesPostes);
                                    counter += 3;
                                }
                                doc = testNumberOfOccurrences(doc);

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
                                                    if (jObj.getArray("u.modules") != null && jObj.getArray("u.modules").size() > 0) {
                                                        JsonArray modulesArray = jObj.getArray("u.modules");
                                                        for (int i = 0; i < modulesArray.size(); i++) {
                                                            String module = modulesArray.get(i).toString();
                                                            String[] parts = module.split("\\$");
                                                            Element garPersonMef = doc.createElement("men:GARPersonMEF");
                                                            garEntEnseignant.appendChild(garPersonMef);
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, mapStructures.get(parts[0]));
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
                                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                                DOMSource source = new DOMSource(doc);
                                                StreamResult result = new StreamResult(new File(path + getExportFileName("Enseignant", fileIndex)));

                                                transformer.transform(source, result);
/*                                    boolean res = MediacentreController.isFileValid(pathExport + getExportFileName("Enseignants", fileIndex));
                                    if( res == false ){
                                        System.out.println("Error on file : " + pathExport + getExportFileName("Enseignants", fileIndex));
                                    } else {
                                        System.out.println("File valid : " + pathExport + getExportFileName("Enseignants", fileIndex));
                                    }*/

                                                System.out.println("Teachers saved");
                                            } catch (TransformerException tfe) {
                                                tfe.printStackTrace();
/*                                } catch (SAXException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();*/
                                            }
                                        }
                                    }
                                });
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
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(pathExport + getExportFileName("Enseignant", fileIndex)));

                transformer.transform(source, result);
 /*               boolean res = MediacentreController.isFileValid(pathExport + getExportFileName("Enseignants", fileIndex));
                if( res == false ){
                    System.out.println("Error on file : " + pathExport + getExportFileName("Enseignants", fileIndex));
                } else {
                    System.out.println("File valid : " + pathExport + getExportFileName("Enseignants", fileIndex));
                }*/
                System.out.println("Teachers" + fileIndex + " saved");
                fileIndex++;
                counter = 0;
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
      /*      } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();*/
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
        garEntEnseignant = doc.createElement("men:GAR-ENT-Enseignant");
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
