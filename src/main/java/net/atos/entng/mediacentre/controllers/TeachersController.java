package net.atos.entng.mediacentre.controllers;

import fr.wseduc.webutils.Either;
import net.atos.entng.mediacentre.services.MediacentreService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
import java.io.IOException;
import java.util.*;

import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;

public class TeachersController {

    private     int counter = 0;            // nb of elements currently put in the file
    private     int nbElem = 10000;         // max elements authorized in a file
    private     int fileIndex = 0;          // index of the export file
    private     String pathExport = "";     // path where the generated files are put
    private     Element garEntEnseignant = null;
    private     Document doc = null;
    private     String BOTHPROFILES = "both";
    private     List<String> bannedUsers = new ArrayList<String>(); // users exclued because on multiple structures

    /**
     *  export Teachers
     */
    public void exportTeachers(final MediacentreService mediacentreService, final String path, int nbElementPerFile, final Handler<List<String>> handler){
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
                                final JsonArray teachers = event.right().getValue();
                                doc = fileHeader();
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                // men:GAREleve
                                String lastTeacherId = "";
                                String lastTeacherBirthDate = "";
                                String profilType = "National_ens";
                                Set<String> etabs = new HashSet<String>();
                                JsonObject lastjObj = null;
                                Element garEnseignant = null;
                                Map<String, String> structProfile = new HashMap<String, String>();
                                ArrayList<String[]> listDisciplinesPostes = new ArrayList<String[]>();
                                for (Object obj : teachers) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (jObj.getString("s.UAI") != null || jObj.getString("s2.UAI") != null) {
                                            if (jObj.getString("u.id") != null && !lastTeacherId.equals(jObj.getString("u.id"))) {
                                                // test if there is a last one
                                                if (lastjObj != null && etabs.size() < 2){
                                                    // add all the GARPersonProfils
                                                    Iterator it = structProfile.entrySet().iterator();

                                                    garEnseignant = doc.createElement("men:GAREnseignant");
                                                    garEntEnseignant.appendChild(garEnseignant);
                                                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, jObj.getString("u.id"));

                                                    while (it.hasNext()) {
                                                        Map.Entry pair = (Map.Entry)it.next();
                                                        // handle case where both profiles for 1 structure
                                                        if( BOTHPROFILES.equals(pair.getValue().toString())) {
                                                            Element garProfil = doc.createElement("men:GARPersonProfils");
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, pair.getKey().toString());
                                                            MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_doc");
                                                            garEnseignant.appendChild(garProfil);
                                                            garProfil = doc.createElement("men:GARPersonProfils");
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, pair.getKey().toString());
                                                            MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_ens");
                                                            garEnseignant.appendChild(garProfil);
                                                        } else {
                                                            //1 profile for 1 structure
                                                            Element garProfil = doc.createElement("men:GARPersonProfils");
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, pair.getKey().toString());
                                                            MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, pair.getValue().toString());
                                                            garEnseignant.appendChild(garProfil);
                                                        }
                                                    }

                                                    MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEnseignant, lastjObj.getString("u.lastName"));
                                                    MediacentreController.insertNode("men:GARPersonNom", doc, garEnseignant, lastjObj.getString("u.lastName"));
                                                    MediacentreController.insertNode("men:GARPersonPrenom", doc, garEnseignant, lastjObj.getString("u.firstName"));
                                                    if (lastjObj.getString("u.otherNames") != null) {
                                                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, lastjObj.getString("u.otherNames"));
                                                    } else {
                                                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, lastjObj.getString("u.firstName"));
                                                    }
                                                    MediacentreController.insertNode("men:GARPersonCivilite", doc, garEnseignant, lastjObj.getString(""));
                                                    MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEnseignant, lastjObj.getString("s.UAI"));

                                                    // add all the  GARPersonEtab
                                                    for (String etab : etabs) {
                                                        MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, etab/*jObj.getString("s.UAI")*/);
                                                    }

                                                    MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEnseignant, lastTeacherBirthDate);

                                                    for (String[] data : listDisciplinesPostes) {
                                                        if( data[2] != null) { // field men:GAREnsDisciplinePosteCode is mandatory
                                                            Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                                            MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                                            garEnseignant.appendChild(garEnsDisciplinesPostes);
                                                            counter += 3;
                                                        }
                                                    }
                                                    profilType = "National_ens";
                                                    etabs = new HashSet<String>();
                                                    doc = testNumberOfOccurrences(doc);
                                                } else {
                                                    if( lastjObj != null && etabs.size() >= 2) {
                                                        bannedUsers.add(lastjObj.getString("u.id"));
                                                        etabs = new HashSet<String>();
                                                    }
                                                }

                                                listDisciplinesPostes = new ArrayList<String[]>();

                                                // GARPersonProfils - can have multiple
                                                structProfile = new HashMap<String, String>();
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
                                                        } else {
                                                            profilType = "National_ens";
                                                        }

                                                        // test if it already exists with another profileType for the structure
                                                        String existingProfileType = structProfile.get(mapStructures.get(data[0].toString()));
                                                        if( existingProfileType != null && !profilType.equals(existingProfileType)) {
                                                            // we need to make a structure with both profiles
                                                            structProfile.put(mapStructures.get(data[0].toString()), BOTHPROFILES);
                                                        } else {
                                                            structProfile.put(mapStructures.get(data[0].toString()),profilType/*data[1]*/); // for  GARPersonProfils, because it can be multiple for 1 structure
                                                        }

                                                        etabs.add(mapStructures.get(data[0].toString()));
                                                        if( jObj.getString("s2.UAI") != null ) {
                                                            etabs.add(jObj.getString("s2.UAI"));
                                                            if( !structProfile.containsKey(jObj.getString("s2.UAI"))){
                                                                structProfile.put(mapStructures.get(data[0].toString()),"National_ens");
                                                            }
                                                        }
                                                        listDisciplinesPostes.add(data);
                                                    }
                                                } else {
                                                    // we create an empty one, because there is no functions attribute
                                                    if( jObj.getString("s.UAI") != null ) {
                                                        etabs.add(jObj.getString("s.UAI"));
                                                        if( !structProfile.containsKey(jObj.getString("s2.UAI"))){
                                                            structProfile.put(jObj.getString("s2.UAI"),"National_ens");
                                                        }
                                                    }
                                                    if( jObj.getString("s2.UAI") != null ) {
                                                        etabs.add(jObj.getString("s2.UAI"));
                                                        if( !structProfile.containsKey(jObj.getString("s2.UAI"))){
                                                            structProfile.put(jObj.getString("s2.UAI"),"National_ens");
                                                        }
                                                    }
                                                }
                                                lastTeacherId = jObj.getString("u.id");
                                                lastTeacherBirthDate = jObj.getString("u.birthDate");
                                                lastjObj = jObj;
                                            } else {
                                                if (jObj.getString("s2.UAI") != null) {
                                                    etabs.add(jObj.getString("s2.UAI"));
                                                    if( !structProfile.containsKey(jObj.getString("s2.UAI"))){
                                                        structProfile.put(jObj.getString("s2.UAI"),"National_ens");
                                                    }
                                                }
                                                counter += 9;
                                            }
                                        }
                                        counter += 6;
                                        // end, so we add the last not added disciplinesPostes
                                    }

                                }
                                //close last one
                                // add all the GARPersonProfils
                                if( etabs.size() < 2 ) {
                                    garEnseignant = doc.createElement("men:GAREnseignant");
                                    garEntEnseignant.appendChild(garEnseignant);
                                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, lastjObj.getString("u.id"));

                                    for (String etab : etabs) {
                                        Element garProfil = doc.createElement("men:GARPersonProfils");
                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, etab);
                                        //if ("Personnel".equals(jObj.getString("p.name"))) {
                                        MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, profilType);
                                        garEnseignant.appendChild(garProfil);
                                    }

                                    MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEnseignant, lastjObj.getString("u.lastName"));
                                    MediacentreController.insertNode("men:GARPersonNom", doc, garEnseignant, lastjObj.getString("u.lastName"));
                                    MediacentreController.insertNode("men:GARPersonPrenom", doc, garEnseignant, lastjObj.getString("u.firstName"));
                                    if (lastjObj.getString("u.otherNames") != null) {
                                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, lastjObj.getString("u.otherNames"));
                                    } else {
                                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, lastjObj.getString("u.firstName"));
                                    }
                                    MediacentreController.insertNode("men:GARPersonCivilite", doc, garEnseignant, lastjObj.getString(""));
                                    MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEnseignant, lastjObj.getString("s.UAI"));

                                    // add all the  GARPersonEtab
                                    for (String etab : etabs) {
                                        MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, etab/*jObj.getString("s.UAI")*/);
                                    }

                                    MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEnseignant, lastTeacherBirthDate);

                                    for (String[] data : listDisciplinesPostes) {
                                        if( data[2] != null) { // field men:GAREnsDisciplinePosteCode is mandatory
                                            Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                            MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                            garEnseignant.appendChild(garEnsDisciplinesPostes);
                                            counter += 3;
                                        }
                                    }
                                    doc = testNumberOfOccurrences(doc);
                                } else {
                                    if( lastjObj != null && etabs.size() >= 2) {
                                        bannedUsers.add(lastjObj.getString("u.id"));
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
                                                    if (jObj.getArray("u.modules") != null
                                                        && jObj.getArray("u.modules").size() > 0
                                                        && !bannedUsers.contains(jObj.getString("u.id"))) {
                                                        JsonArray modulesArray = jObj.getArray("u.modules");
                                                        for (int i = 0; i < modulesArray.size(); i++) {
                                                            String module = modulesArray.get(i).toString();
                                                            String[] parts = module.split("\\$");
                                                            Element garPersonMef = doc.createElement("men:GARPersonMEF");
                                                            garEntEnseignant.appendChild(garPersonMef);
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, mapStructures.get(parts[0]));
                                                            MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonMef, jObj.getString("u.id"));
                                                            MediacentreController.insertNode("men:GARMEFCode", doc, garPersonMef, MediacentreController.customSubString(parts[1], 255));
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
                                                boolean res = false;
                                                try {
                                                    res = MediacentreController.isFileValid(pathExport + getExportFileName("Enseignant", fileIndex));
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                } catch (SAXException e) {
                                                    e.printStackTrace();
                                                }
                                                if( res == false ){
                                        System.out.println("Error on file : " + pathExport + getExportFileName("Enseignant", fileIndex));
                                                } else {
                                        System.out.println("File valid : " + pathExport + getExportFileName("Enseignant", fileIndex));
                                    }

                                                System.out.println("Teachers saved");
                                            } catch (TransformerException tfe) {
                                                tfe.printStackTrace();
/*                                } catch (SAXException e) {
                                                e.printStackTrace();
                                            } catch (IOException e) {
                                    e.printStackTrace();*/
                                            }
                                        }
                                        handler.handle(bannedUsers);
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
                boolean res = false;
                try {
                    res = MediacentreController.isFileValid(pathExport + getExportFileName("Enseignant", fileIndex));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
                if( res == false ){
                    System.out.println("Error on file : " + pathExport + getExportFileName("Enseignant", fileIndex));
                } else {
                    System.out.println("File valid : " + pathExport + getExportFileName("Enseignant", fileIndex));
                }
                System.out.println("Teachers" + fileIndex + " saved");
                fileIndex++;
                counter = 0;
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
/*            } catch (SAXException e) {
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
