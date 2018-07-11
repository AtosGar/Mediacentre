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
import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName_1D;
import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName_2D;

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
    public void exportGroups(final MediacentreService mediacentreService, final String path, final int nbElementPerFile, final List<String> bannedUsers) {
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
                            String grpCode = jObj.getString("c.externalId");
                            String[] parts = grpCode.split("\\$");

                            if( parts.length >= 2 ) {
                                MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, MediacentreController.customSubString(parts[1], 255));
                            } else {
                                MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, "");
                            }
                            MediacentreController.insertNode("men:GARStructureUAI", doc, garDivision, jObj.getString("s.UAI"));
                            MediacentreController.insertNode("men:GARGroupeLibelle", doc, garDivision, jObj.getString("c.name"));
                            MediacentreController.insertNode("men:GARGroupeStatut", doc, garDivision, "DIVISION");
                            counter += 5;
                            doc = testNumberOfOccurrences(doc, false);
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
                                List<String> lGroupes = new ArrayList<String>();
                                Element garGroup = null;
                                for (Object obj : groups) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (!lastGroup.equals(jObj.getString("fg.id"))) {
                                            if( !lGroupes.contains(jObj.getString("s.UAI") + jObj.getString("fg.externalId").split("\\$")[1])) {
                                                counter += 6;
                                                doc = testNumberOfOccurrences(doc, false);
                                                garGroup = doc.createElement("men:GARGroupe");
                                                garEntGroup.appendChild(garGroup);
                                                if (jObj.getString("fg.externalId") != null && !"null".equals(jObj.getString("fg.externalId"))) {
                                                    String grpCode = jObj.getString("fg.externalId");
                                                    String[] parts = grpCode.split("\\$");
                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, MediacentreController.customSubString(parts[1],255));
                                                } else {
                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, jObj.getString("fg.id"));
                                                }
                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garGroup, jObj.getString("s.UAI"));
                                                MediacentreController.insertNode("men:GARGroupeLibelle", doc, garGroup, jObj.getString("fg.name"));
                                                MediacentreController.insertNode("men:GARGroupeStatut", doc, garGroup, "GROUPE");
                                                lastGroup = jObj.getString("fg.id");
                                                lGroupes.add(jObj.getString("s.UAI") + jObj.getString("fg.externalId").split("\\$")[1]);
                                            }
                                        }
                                        if (jObj.getString("cexternalId") != null) {
                                            String grpCode = jObj.getString("cexternalId");
                                            String[] parts = grpCode.split("\\$");
                                            String classe = parts[1];
                                            MediacentreController.insertNode("men:GARGroupeDivAppartenance", doc, garGroup, classe);
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
                                                    // do not include persons present on multible structures
                                                    if( !bannedUsers.contains(jObj.getString("u.id"))) {
                                                        garPersonGroup = doc.createElement("men:GARPersonGroupe");
                                                        garEntGroup.appendChild(garPersonGroup);
                                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonGroup, jObj.getString("s.UAI"));
                                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonGroup, jObj.getString("u.id"));
                                                        if (jObj.getString("fg.externalId") != null && !"null".equals(jObj.getString("fg.externalId"))) {
                                                            String grpCode = jObj.getString("fg.externalId");
                                                            String[] parts = grpCode.split("\\$");
                                                            MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, MediacentreController.customSubString(parts[1], 255));
                                                        } else {
                                                            MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, jObj.getString("fg.id"));
                                                        }
                                                        counter += 4;
                                                        doc = testNumberOfOccurrences(doc, false);
                                                    }
                                                }
                                            }
                                        }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                        mediacentreService.getPersonGroupeStudent(new Handler<Either<String, JsonArray>>() {
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
                                                            if (!bannedUsers.contains(jObj.getString("u.id"))) {
                                                                garPersonGroup = doc.createElement("men:GARPersonGroupe");
                                                                garEntGroup.appendChild(garPersonGroup);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonGroup, jObj.getString("s.UAI"));
                                                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonGroup, jObj.getString("u.id"));
                                                                if (jObj.getString("c.externalId") != null && !"null".equals(jObj.getString("c.externalId"))) {
                                                                    String grpCode = jObj.getString("c.externalId");
                                                                    String[] parts = grpCode.split("\\$");
                                                                    if (parts.length < 2) {
                                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, "null");
                                                                    } else {
                                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, MediacentreController.customSubString(parts[1], 255));
                                                                    }
                                                                } else {
                                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, MediacentreController.customSubString(jObj.getString("c.id"), 255));
                                                                }
                                                                counter += 4;
                                                                doc = testNumberOfOccurrences(doc, false);
                                                            }
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
                                                                    if (!bannedUsers.contains(jObj.getString("u.id"))) {
                                                                        if (jObj.getArray("t.groups") != null && jObj.getArray("t.groups").size() > 0) {
                                                                            JsonArray groups = jObj.getArray("t.groups");
                                                                            for (int i = 0; i < groups.size(); i++) {
                                                                                String group = groups.get(i).toString();
                                                                                GAREnsGroupeMatiereKey key = new GAREnsGroupeMatiereKey(jObj.getString("s.UAI"), jObj.getString("u.id"), group);
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
                                                            }

                                                            // making xml
                                                            for (Map.Entry<GAREnsGroupeMatiereKey, Set<String>> entry : mapGroupes.entrySet()) {
                                                                GAREnsGroupeMatiereKey key = entry.getKey();
                                                                Set<String> currentList = entry.getValue();
                                                                garEnGroupeMatiere = doc.createElement("men:GAREnsGroupeMatiere");
                                                                garEntGroup.appendChild(garEnGroupeMatiere);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, key.getUai());
                                                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, key.getUid());
                                                                String grpCode = key.getGroup();
                                                                String[] parts = grpCode.split("\\$");
                                                                MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(parts[1], 255));
                                                                for (Object s : currentList) {
                                                                    if (s instanceof String) {
                                                                        String subject = (String) s;
                                                                        MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(subject, 255));
                                                                        counter++;
                                                                    }
                                                                }
                                                                counter += 5;
                                                                doc = testNumberOfOccurrences(doc, false);
                                                            }

                                                            for (Map.Entry<GAREnsGroupeMatiereKey, Set<String>> entry : mapClasses.entrySet()) {
                                                                GAREnsGroupeMatiereKey key = entry.getKey();
                                                                Set<String> currentList = entry.getValue();
                                                                garEnGroupeMatiere = doc.createElement("men:GAREnsClasseMatiere");
                                                                garEntGroup.appendChild(garEnGroupeMatiere);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, key.getUai());
                                                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, key.getUid());
                                                                String grpCode = key.getGroup();
                                                                String[] parts = grpCode.split("\\$");
                                                                MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(parts[1], 255));
                                                                for (Object s : currentList) {
                                                                    if (s instanceof String) {
                                                                        String subject = (String) s;
                                                                        MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(subject, 255));
                                                                        counter++;
                                                                    }
                                                                }
                                                                counter += 5;
                                                                doc = testNumberOfOccurrences(doc, false);
                                                            }

                                                        }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                                                        try {
                                                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                            Transformer transformer = transformerFactory.newTransformer();
                                                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                                            DOMSource source = new DOMSource(doc);
                                                            StreamResult result = new StreamResult(new File(path + getExportFileName("Groupe", fileIndex)));

                                                            transformer.transform(source, result);
                                                            boolean res = false;
                                                            try {
                                                                res = MediacentreController.isFileValid(pathExport + getExportFileName("Groupe", fileIndex));
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            } catch (SAXException e) {
                                                                e.printStackTrace();
                                                            }
                                                            if( res == false ){
                                                                System.out.println("Error on file : " + pathExport + getExportFileName("Groupes", fileIndex));
                                                            } else {
                                                                System.out.println("File valid : " + pathExport + getExportFileName("Groupes", fileIndex));
                                                            }

                                                            System.out.println("Groupes saved");
                                                        } catch (TransformerException tfe) {
                                                            tfe.printStackTrace();
                                                      /*  } catch (SAXException e) {
                                                            e.printStackTrace();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();*/
                                                        }
                                                    }
                                                }); // end getEnsGroupAndClassMatiere
                                            }
                                        }); // end getPersonGroupeStudents
                                    }
                                }); // end getPersonGroupe
                            }
                         }
                    }); // end getGroupsExportData
                }
            }
        }); // end getDivisionsExportData
    } // end exportGroups


    /**
     * Export des groupes 1D
     * @param mediacentreService
     * @param path
     * @param nbElementPerFile
     * @param exportUAIList1D
     */
    public void exportGroups_1D(final MediacentreService mediacentreService, final String path,
                                final int nbElementPerFile, final String exportUAIList1D) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;
        mediacentreService.getDivisionsExportData_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {

                if (event.isRight()) {
                    // write the content into xml file
                    final JsonArray divisions = event.right().getValue();
                    doc = fileHeader_1D();

                    // men:GAREleve
                    for (Object obj : divisions) {
                        Element garDivision = doc.createElement("men:GARGroupe");

                        garEntGroup.appendChild(garDivision);

                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            String grpCode = jObj.getString("c.externalId");
                            String[] parts = grpCode.split("\\$");

                            /*if( parts.length >= 2 ) {
                                MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, MediacentreController.customSubString(parts[1], 255));
                            } else {
                                MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, "");
                            }*/
                            MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision,grpCode);
                            MediacentreController.insertNode("men:GARStructureUAI", doc, garDivision, jObj.getString("s.UAI"));
                            MediacentreController.insertNode("men:GARGroupeLibelle", doc, garDivision, jObj.getString("c.name"));
                            MediacentreController.insertNode("men:GARGroupeStatut", doc, garDivision, "DIVISION");
                            counter += 5;
                            doc = testNumberOfOccurrences(doc, true);
                        }
                    }


                    mediacentreService.getGroupsExportData_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {

                            if (event.isRight()) {

                                // write the content into xml file
                                JsonArray groups = event.right().getValue();
                                // men:GARPersonMEF
                                String lastGroup = "";
                                List<String> lGroupes = new ArrayList<String>();
                                Element garGroup = null;

                                /* GARGroupe non récupéré par OPEN ENT NG */
                                /*
                                for (Object obj : groups) {
                                    if (obj instanceof JsonObject) {

                                        JsonObject jObj = (JsonObject) obj;

                                        if (!lastGroup.equals(jObj.getString("cid"))) {

                                            if( !lGroupes.contains(jObj.getString("s.UAI") + jObj.getString("c.externalId"))) {
                                                counter += 6;
                                                doc = testNumberOfOccurrences(doc);
                                                garGroup = doc.createElement("men:GARGroupe");
                                                garEntGroup.appendChild(garGroup);

                                                if (jObj.getString("c.externalId") != null && !"null".equals(jObj.getString("c.externalId"))) {
                                                    String grpCode = jObj.getString("c.externalId");

                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, grpCode);

                                                } else {
                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, jObj.getString("fg.id"));
                                                }

                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garGroup, jObj.getString("s.UAI"));
                                                MediacentreController.insertNode("men:GARGroupeLibelle", doc, garGroup, jObj.getString("fg.name"));
                                                MediacentreController.insertNode("men:GARGroupeStatut", doc, garGroup, "GROUPE");
                                                lastGroup = jObj.getString("cid");
                                                lGroupes.add(jObj.getString("s.UAI") + jObj.getString("c.externalId"));
                                            }
                                        }

                                        if (jObj.getString("cexternalId") != null) {
                                            String grpCode = jObj.getString("cexternalId");
                                            //String[] parts = grpCode.split("\\$");
                                            //String classe = parts[1];
                                            MediacentreController.insertNode("men:GARGroupeDivAppartenance", doc, garGroup, grpCode);
                                        }
                                    }
                                }*/

                                /* GARPersonGroupe */
                                mediacentreService.getTeacherClasse_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
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
                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, jObj.getString("c.externalId"));

                                                    counter += 4;
                                                    doc = testNumberOfOccurrences(doc, true);
                                                }
                                            }
                                        }

                                        mediacentreService.getPersonGroupeStudent_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
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
                                                            MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, jObj.getString("c.externalId"));

                                                            counter += 4;
                                                            doc = testNumberOfOccurrences(doc, true);

                                                        }
                                                    }
                                                }

                                                /* Génération du fichier */
                                                try {
                                                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                    Transformer transformer = transformerFactory.newTransformer();
                                                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                                    DOMSource source = new DOMSource(doc);
                                                    StreamResult result = new StreamResult(new File(path + getExportFileName_1D("Groupe", fileIndex)));

                                                    transformer.transform(source, result);
                                                    boolean res = false;
                                                    try {
                                                        res = MediacentreController.isFileValid(pathExport + getExportFileName_1D("Groupe", fileIndex));
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    } catch (SAXException e) {
                                                        e.printStackTrace();
                                                    }
                                                    if( res == false ){
                                                        System.out.println("Error on file : " + pathExport + getExportFileName_1D("Groupes", fileIndex));
                                                    } else {
                                                        System.out.println("File valid : " + pathExport + getExportFileName_1D("Groupes", fileIndex));
                                                    }

                                                    System.out.println("Groupes saved");
                                                } catch (TransformerException tfe) {
                                                    tfe.printStackTrace();

                                                }

                                            }
                                        }); // end getPersonGroupeStudents
                                    }
                                }); // end getPersonGroupe
                            }
                        }
                    }); // end getGroupsExportData
                }
            }
        }); // end getDivisionsExportData
    } // end exportGroups


    /**
     * export Groups
     */
    public void exportGroups_2D(final MediacentreService mediacentreService, final String path,
                                final int nbElementPerFile, final String exportUAIList2D) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;

        // ---------------------
        // GARGroupe
        // ---------------------
        mediacentreService.getDivisionsExportData_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
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
                            String grpCode = jObj.getString("c.externalId");
                            String[] parts = grpCode.split("\\$");

                            if( parts.length >= 2 ) {
                                MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, MediacentreController.customSubString(parts[1], 255));
                            } else {
                                MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, "");
                            }
                            MediacentreController.insertNode("men:GARStructureUAI", doc, garDivision, jObj.getString("s.UAI"));
                            MediacentreController.insertNode("men:GARGroupeLibelle", doc, garDivision, jObj.getString("c.name"));
                            MediacentreController.insertNode("men:GARGroupeStatut", doc, garDivision, "DIVISION");
                            counter += 5;
                            doc = testNumberOfOccurrences(doc, false);
                        }
                    }

                    // ---------------------
                    // GARPersonGroupe
                    // ---------------------
                    mediacentreService.getGroupsExportData_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray groups = event.right().getValue();
                                // men:GARPersonMEF
                                String lastGroup = "";
                                List<String> lGroupes = new ArrayList<String>();
                                Element garGroup = null;
                                for (Object obj : groups) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (!lastGroup.equals(jObj.getString("fg.id"))) {
                                            if( !lGroupes.contains(jObj.getString("s.UAI") + jObj.getString("fg.externalId").split("\\$")[1])) {
                                                counter += 6;
                                                doc = testNumberOfOccurrences(doc, false);
                                                garGroup = doc.createElement("men:GARGroupe");
                                                garEntGroup.appendChild(garGroup);
                                                if (jObj.getString("fg.externalId") != null && !"null".equals(jObj.getString("fg.externalId"))) {
                                                    String grpCode = jObj.getString("fg.externalId");
                                                    String[] parts = grpCode.split("\\$");
                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, MediacentreController.customSubString(parts[1],255));
                                                } else {
                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garGroup, jObj.getString("fg.id"));
                                                }
                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garGroup, jObj.getString("s.UAI"));
                                                MediacentreController.insertNode("men:GARGroupeLibelle", doc, garGroup, jObj.getString("fg.name"));
                                                MediacentreController.insertNode("men:GARGroupeStatut", doc, garGroup, "GROUPE");
                                                lastGroup = jObj.getString("fg.id");
                                                lGroupes.add(jObj.getString("s.UAI") + jObj.getString("fg.externalId").split("\\$")[1]);
                                            }
                                        }
                                        if (jObj.getString("cexternalId") != null) {
                                            String grpCode = jObj.getString("cexternalId");
                                            String[] parts = grpCode.split("\\$");
                                            String classe = parts[1];
                                            MediacentreController.insertNode("men:GARGroupeDivAppartenance", doc, garGroup, classe);
                                        }
                                    }
                                }
                                // ---------------------
                                // GARPersonGroupe
                                // ---------------------
                                mediacentreService.getPersonGroupe_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
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
                                                    // do not include persons present on multible structures

                                                        garPersonGroup = doc.createElement("men:GARPersonGroupe");
                                                        garEntGroup.appendChild(garPersonGroup);
                                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonGroup, jObj.getString("s.UAI"));
                                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonGroup, jObj.getString("u.id"));
                                                        if (jObj.getString("fg.externalId") != null && !"null".equals(jObj.getString("fg.externalId"))) {
                                                            String grpCode = jObj.getString("fg.externalId");
                                                            String[] parts = grpCode.split("\\$");
                                                            MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, MediacentreController.customSubString(parts[1], 255));
                                                        } else {
                                                            MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, jObj.getString("fg.id"));
                                                        }
                                                        counter += 4;
                                                        doc = testNumberOfOccurrences(doc, false);

                                                }
                                            }
                                        }

                                        // ----------------------------
                                        // GARPersonGroupe (Student)
                                        // ----------------------------
                                       mediacentreService.getPersonGroupeStudent_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
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
                                                                if (jObj.getString("c.externalId") != null && !"null".equals(jObj.getString("c.externalId"))) {
                                                                    String grpCode = jObj.getString("c.externalId");
                                                                    String[] parts = grpCode.split("\\$");
                                                                    if (parts.length < 2) {
                                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, "null");
                                                                    } else {
                                                                        MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, MediacentreController.customSubString(parts[1], 255));
                                                                    }
                                                                } else {
                                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garPersonGroup, MediacentreController.customSubString(jObj.getString("c.id"), 255));
                                                                }
                                                                counter += 4;
                                                                doc = testNumberOfOccurrences(doc, false);

                                                        }
                                                    }
                                                }

                                                // --------------------------------------------
                                                // GAREnsGroupeMatiere & GAREnsClasseMatiere
                                                // --------------------------------------------
                                                mediacentreService.getEnsGroupAndClassMatiere_2D(exportUAIList2D,  new Handler<Either<String, JsonArray>>() {
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
                                                            for (Map.Entry<GAREnsGroupeMatiereKey, Set<String>> entry : mapGroupes.entrySet()) {
                                                                GAREnsGroupeMatiereKey key = entry.getKey();
                                                                Set<String> currentList = entry.getValue();
                                                                garEnGroupeMatiere = doc.createElement("men:GAREnsGroupeMatiere");
                                                                garEntGroup.appendChild(garEnGroupeMatiere);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, key.getUai());
                                                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, key.getUid());
                                                                String grpCode = key.getGroup();
                                                                String[] parts = grpCode.split("\\$");
                                                                MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(parts[1], 255));
                                                                for (Object s : currentList) {
                                                                    if (s instanceof String) {
                                                                        String subject = (String) s;
                                                                        MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(subject, 255));
                                                                        counter++;
                                                                    }
                                                                }
                                                                counter += 5;
                                                                doc = testNumberOfOccurrences(doc, false);
                                                            }

                                                            for (Map.Entry<GAREnsGroupeMatiereKey, Set<String>> entry : mapClasses.entrySet()) {
                                                                GAREnsGroupeMatiereKey key = entry.getKey();
                                                                Set<String> currentList = entry.getValue();
                                                                garEnGroupeMatiere = doc.createElement("men:GAREnsClasseMatiere");
                                                                garEntGroup.appendChild(garEnGroupeMatiere);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, key.getUai());
                                                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, key.getUid());
                                                                String grpCode = key.getGroup();
                                                                String[] parts = grpCode.split("\\$");
                                                                MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(parts[1], 255));
                                                                for (Object s : currentList) {
                                                                    if (s instanceof String) {
                                                                        String subject = (String) s;
                                                                        MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, MediacentreController.customSubString(subject, 255));
                                                                        counter++;
                                                                    }
                                                                }
                                                                counter += 5;
                                                                doc = testNumberOfOccurrences(doc, false);
                                                            }

                                                        }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                                                        try {
                                                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                            Transformer transformer = transformerFactory.newTransformer();
                                                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                                            DOMSource source = new DOMSource(doc);
                                                            StreamResult result = new StreamResult(new File(path + getExportFileName_2D("Groupe", fileIndex)));

                                                            transformer.transform(source, result);
                                                            boolean res = false;
                                                            try {
                                                                res = MediacentreController.isFileValid(pathExport + getExportFileName_2D("Groupe", fileIndex));
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            } catch (SAXException e) {
                                                                e.printStackTrace();
                                                            }
                                                            if( res == false ){
                                                                System.out.println("Error on file : " + pathExport + getExportFileName_2D("Groupes", fileIndex));
                                                            } else {
                                                                System.out.println("File valid : " + pathExport + getExportFileName_2D("Groupes", fileIndex));
                                                            }

                                                            System.out.println("Groupes saved");
                                                        } catch (TransformerException tfe) {
                                                            tfe.printStackTrace();
                                                      /*  } catch (SAXException e) {
                                                            e.printStackTrace();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();*/
                                                        }
                                                    }
                                                }); // end getEnsGroupAndClassMatiere
                                            }
                                        }); // end getPersonGroupeStudents
                                    }
                                }); // end getPersonGroupe
                            }
                        }
                    }); // end getGroupsExportData
                }
            }
        }); // end getDivisionsExportData
    } // end exportGroups


    private Document testNumberOfOccurrences(Document doc, boolean is1D) {
        if (nbElem <= counter) {
            // close the full file
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DOMSource source = new DOMSource(doc);

                StreamResult result;
                if(is1D){
                    result = new StreamResult(new File(pathExport + getExportFileName_1D("Groupe", fileIndex)));
                }else{
                    result = new StreamResult(new File(pathExport + getExportFileName_2D("Groupe", fileIndex)));
                }


                transformer.transform(source, result);
                boolean res = false;
                try {

                    if(is1D){
                        res = MediacentreController.isFileValid(pathExport + getExportFileName_1D("Groupe", fileIndex));
                    }else{
                        res = MediacentreController.isFileValid(pathExport + getExportFileName_2D("Groupe", fileIndex));
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
                if( res == false ){
                    if(is1D){
                        System.out.println("Error on file : " + pathExport + getExportFileName_1D("Groupe", fileIndex));
                    }else{
                        System.out.println("Error on file : " + pathExport + getExportFileName_2D("Groupe", fileIndex));
                    }
                } else {
                    if(is1D){
                        System.out.println("File valid : " + pathExport + getExportFileName_1D("Groupe", fileIndex));
                    }else{
                        System.out.println("File valid : " + pathExport + getExportFileName_2D("Groupe", fileIndex));
                    }
                }

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
            if(is1D){
                return fileHeader_1D();
            }else{
                return fileHeader();
            }
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

    private Document fileHeader_1D(){
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
        garEntGroup.setAttribute("xmlns:men", "http://data.education.fr/ns/gar/1d");
        garEntGroup.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntGroup.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntGroup.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntGroup.setAttribute("Version", "1.0");
        garEntGroup.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
        return doc;
    }
}