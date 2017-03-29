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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;

public class GroupsController {

    public class GAREnsGroupeMatiereKey{
        private String uai;
        private String uid;
        private String group;

        GAREnsGroupeMatiereKey(String uai, String uid, String group){
            this.uai = uai;
            this.uid = uid;
            this.group = group;
        }

        @Override
        public int hashCode() {
            return (getUai() + getUid() + getGroup()).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if( obj instanceof GAREnsGroupeMatiereKey){
                GAREnsGroupeMatiereKey key = (GAREnsGroupeMatiereKey)obj;
                return key.getUai().equals(this.getUai()) &&
                        key.getUid().equals(this.getUid()) &&
                        key.getGroup().equals(this.getGroup());
            }
            return false;
        }

        public String getUai() {
            return uai;
        }

        public String getUid() {
            return uid;
        }

        public String getGroup() {
            return group;
        }
    }

    private     int counter = 0;            // nb of elements currently put in the file
    private     int nbElem = 10000;         // max elements authorized in a file
    private     int fileIndex = 0;          // index of the export file
    private     String pathExport = "";     // path where the generated files are put
    private     Element garEntGroup = null;
    private     Document doc = null;

    /**
     * export Groups
     */
    public void exportGroups(final MediacentreService mediacentreService, final String path, final int nbElementPerFile) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;
        mediacentreService.getDivisionsExportData(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // write the content into xml file
                    final JsonArray divisions = event.right().getValue();
                    doc = fileHeader();
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:GAREleve
                    for (Object obj : divisions) {
                        Element garDivision = doc.createElement("men:GARGroupe");
                        garEntGroup.appendChild(garDivision);
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, jObj.getString("c.externalId"));
                            MediacentreController.insertNode("men:GARStructureUAI", doc, garDivision, jObj.getString("s.UAI"));
                            MediacentreController.insertNode("men:GARGroupeLibelle", doc, garDivision, jObj.getString("c.name"));
                            MediacentreController.insertNode("men:GARGroupeStatut", doc, garDivision, "DIVISION");
                            counter += 5;
                            doc = testNumberOfOccurrences(doc);
                        }
                    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    mediacentreService.getGroupsExportData(new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray groups = event.right().getValue();
                                // men:GARPersonMEF
                                String lastGroup = "";
                                Element garGroup = null;
                                for (Object obj : groups) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (!lastGroup.equals(jObj.getString("fg.id"))) {
                                            counter += 6;
                                            doc = testNumberOfOccurrences(doc);
                                            garGroup = doc.createElement("men:GARGroupe");
                                            garEntGroup.appendChild(garGroup);
                                            if( jObj.getString("fg.externalId") != null && "null".equals(jObj.getString("fg.externalId"))) {
                                                MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, jObj.getString("fg.externalId"));
                                            } else {
                                                MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, jObj.getString("fg.id"));
                                            }
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garGroup, jObj.getString("s.UAI"));
                                            MediacentreController.insertNode("men:GARGroupeLibelle", doc, garGroup, jObj.getString("fg.name"));
                                            MediacentreController.insertNode("men:GARGroupeStatut", doc, garGroup, "GROUPE");
                                            lastGroup = jObj.getString("fg.id");
                                        }
                                        if (jObj.getString("c.externalId") != null) {
                                            MediacentreController.insertNode("men:GARGroupeDivAppartenance", doc, garGroup, jObj.getString("c.externalId"));
                                        }
                                    }
                                }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                mediacentreService.getPersonGroupe(new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            // write the content into xml file
                                            JsonArray personGroupe = event.right().getValue();
                                            // men:GARPersonGroup
                                            Element garPersonGroup = null;
                                            for (Object obj : personGroupe) {
                                                if (obj instanceof JsonObject) {
                                                    JsonObject jObj = (JsonObject) obj;
                                                    garPersonGroup = doc.createElement("men:GARPersonGroupe");
                                                    garEntGroup.appendChild(garPersonGroup);
                                                    MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonGroup, jObj.getString("s.UAI"));
                                                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonGroup, jObj.getString("u.id"));
                                                    if( jObj.getString("fg.externalId") != null && "null".equals(jObj.getString("fg.externalId"))) {
                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, jObj.getString("fg.externalId"));
                                                    } else {
                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, jObj.getString("fg.id"));
                                                    }
                                                    counter += 4;
                                                    doc = testNumberOfOccurrences(doc);
                                                }
                                            }
                                        }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                        mediacentreService.getEnsGroupAndClassMatiere(new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> event) {
                                                if (event.isRight()) {
                                                    // write the content into xml file
                                                    JsonArray enGroupeAndClasseMatiere = event.right().getValue();
                                                    // men:GARPersonGroup
                                                    Element garEnGroupeMatiere = null;
                                                    String lastUserId = "";
                                                    String lastStructureId = "";
                                                    String lastGroupeCode = "";
                                                    // preparing hashmap for xml

                                                    Map<GAREnsGroupeMatiereKey, Set<String>> mapGroupes = new HashMap<GAREnsGroupeMatiereKey, Set<String>>();
                                                    Map<GAREnsGroupeMatiereKey, Set<String>> mapClasses = new HashMap<GAREnsGroupeMatiereKey, Set<String>>();
                                                    for (Object obj : enGroupeAndClasseMatiere) {
                                                        if (obj instanceof JsonObject) {
                                                            JsonObject jObj = (JsonObject) obj;
                                                            if (jObj.getArray("t.groups") != null && jObj.getArray("t.groups").size() > 0) {
                                                                JsonArray groups = jObj.getArray("t.groups");
                                                                for (int i = 0; i < groups.size(); i++) {
                                                                    String group = groups.get(i).toString();
                                                                    GAREnsGroupeMatiereKey key = new GAREnsGroupeMatiereKey(jObj.getString("s.UAI"), jObj.getString("u.id"), group);
                                                                    //String key = jObj.getString("s.UAI") + jObj.getString("u.id") + group;
                                                                    Set currentList = mapGroupes.get(key);
                                                                    if (currentList == null) {
                                                                        currentList = new HashSet<String>();
                                                                        mapGroupes.put(key, currentList);
                                                                    }
                                                                    currentList.add(jObj.getString("sub.code"));
                                                                }
                                                            }
                                                            if (jObj.getArray("t.classes") != null && jObj.getArray("t.classes").size() > 0) {
                                                                JsonArray classes = jObj.getArray("t.classes");
                                                                for (int i = 0; i < classes.size(); i++) {
                                                                    String classe = classes.get(i).toString();
                                                                    GAREnsGroupeMatiereKey key = new GAREnsGroupeMatiereKey(jObj.getString("s.UAI"), jObj.getString("u.id"), classe);
                                                                    Set currentList = mapClasses.get(key);
                                                                    if (currentList == null) {
                                                                        currentList = new HashSet<String>();
                                                                        mapClasses.put(key, currentList);
                                                                    }
                                                                    currentList.add(jObj.getString("sub.code"));
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // making xml
                                                    for (Map.Entry<GAREnsGroupeMatiereKey, Set<String>> entry : mapClasses.entrySet()) {
                                                        GAREnsGroupeMatiereKey key = entry.getKey();
                                                        Set<String> currentList = entry.getValue();
                                                        garEnGroupeMatiere = doc.createElement("men:GAREnsGroupeMatiere");
                                                        garEntGroup.appendChild(garEnGroupeMatiere);
                                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, key.getUai());
                                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, key.getUid());
                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, key.getGroup());
                                                        for (Object s : currentList) {
                                                            if (s instanceof String) {
                                                                String subject = (String) s;
                                                                MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, subject);
                                                                counter++;
                                                            }
                                                        }
                                                        counter += 5;
                                                        doc = testNumberOfOccurrences(doc);
                                                    }

                                                    for (Map.Entry<GAREnsGroupeMatiereKey, Set<String>> entry : mapGroupes.entrySet()) {
                                                        GAREnsGroupeMatiereKey key = entry.getKey();
                                                        Set<String> currentList = entry.getValue();
                                                        garEnGroupeMatiere = doc.createElement("men:GAREnsClasseMatiere");
                                                        garEntGroup.appendChild(garEnGroupeMatiere);
                                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, key.getUai());
                                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, key.getUid());
                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, key.getGroup());
                                                        for (Object s : currentList) {
                                                            if (s instanceof String) {
                                                                String subject = (String) s;
                                                                MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, subject);
                                                                counter++;
                                                            }
                                                        }
                                                        counter += 5;
                                                        doc = testNumberOfOccurrences(doc);
                                                    }

                                                }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                                                try {
                                                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                    Transformer transformer = transformerFactory.newTransformer();
                                                    DOMSource source = new DOMSource(doc);
                                                    StreamResult result = new StreamResult(new File(path + getExportFileName("Groupe", fileIndex)));

                                                    transformer.transform(source, result);
                                                    /*boolean res = MediacentreController.isFileValid(pathExport + getExportFileName("Groupes", fileIndex));
                                                    if( res == false ){
                                                        System.out.println("Error on file : " + pathExport + getExportFileName("Groupes", fileIndex));
                                                    } else {
                                                        System.out.println("File valid : " + pathExport + getExportFileName("Groupes", fileIndex));
                                                    }*/

                                                    System.out.println("Groupes saved");
                                                } catch (TransformerException tfe) {
                                                    tfe.printStackTrace();
                                              /*  } catch (SAXException e) {
                                                    e.printStackTrace();
                                                } catch (IOException e) {
                                                    e.printStackTrace();*/
                                                }
                                            }
                                        });
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
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(pathExport + getExportFileName("Groupe", fileIndex)));

                transformer.transform(source, result);
             /*   boolean res = MediacentreController.isFileValid(pathExport + getExportFileName("Groupes", fileIndex));
                if( res == false ){
                    System.out.println("Error on file : " + pathExport + getExportFileName("Groupes", fileIndex));
                } else {
                    System.out.println("File valid : " + pathExport + getExportFileName("Groupes", fileIndex));
                }*/

                System.out.println("Groupes" + fileIndex + " saved");
                fileIndex++;
                counter = 0;
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
           /* } catch (SAXException e) {
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
        garEntGroup = doc.createElement("men:GAR-ENT-Groupe");
        doc.appendChild(garEntGroup);
        garEntGroup.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
        garEntGroup.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntGroup.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntGroup.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntGroup.setAttribute("Version", "1.0");
        garEntGroup.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
        return doc;
    }
}