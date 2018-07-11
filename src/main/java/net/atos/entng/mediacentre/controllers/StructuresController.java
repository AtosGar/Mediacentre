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
import java.util.HashMap;
import java.util.Map;

import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;

public class StructuresController {

    public class GARStructureMatiereEleveKey{
        private String uai;
        private String subCode;

        GARStructureMatiereEleveKey(String uai, String subCode){
            this.uai = uai;
            this.subCode = subCode;
        }

        @Override
        public int hashCode() {
            return (getUai() + getSubCode() ).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if( obj instanceof GARStructureMatiereEleveKey){
                GARStructureMatiereEleveKey key = (GARStructureMatiereEleveKey)obj;
                return key.getUai().equals(this.getUai()) &&
                        key.getSubCode().equals(this.getSubCode());
            }
            return false;
        }

        public String getUai() {
            return uai;
        }

        public String getSubCode() {
            return subCode;
        }

    }



    private     int counter = 0;            // nb of elements currently put in the file
    private     int nbElem = 10000;         // max elements authorized in a file
    private     int fileIndex = 0;          // index of the export file
    private     String pathExport = "";     // path where the generated files are put
    private     Element garEntEtablissement = null;
    private     Document doc = null;
    private     final Map<String, JsonObject> mapModules = new HashMap<>();

    /**
     *  export Structures
     */
    public void exportStructures(final MediacentreService mediacentreService, final String path, int nbElementPerFile){
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;

        mediacentreService.getAllModules(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray allModules = event.right().getValue();
                    for (Object obj : allModules) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            jObj.putString("m.attachment", jObj.getString("m.attachment"));
                            jObj.putString("m.stat", jObj.getString("m.stat"));
                            mapModules.put(jObj.getString("m.externalId"), jObj);
                        }
                    }

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
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                mediacentreService.getEtablissement(new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            // write the content into xml file
                                            final JsonArray structures = event.right().getValue();
                                            doc = fileHeader();
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            // men:GAREtab
                                            String lastStructureId = "none";
                                            String lastContract = "";
                                            String lastPhone = "";
                                            Element garEtab = null;
                                            for (Object obj : structures) {
                                                if (obj instanceof JsonObject) {
                                                    JsonObject jObj = (JsonObject) obj;
                                                    if (jObj.getString("s.UAI") != null && !jObj.getString("s.UAI").equals(lastStructureId)) {
                                                        // we finish the precedent node, if exists
                                                        if (!"none".equals(lastStructureId)) {
                                                            MediacentreController.insertNode("men:GARStructureContrat", doc, garEtab, lastContract);
                                                            MediacentreController.insertNode("men:GARStructureTelephone", doc, garEtab, lastPhone);
                                                            //MediacentreController.insertNode("men:GARStructureEmail", doc, garEtab, "null");
                                                        }
                                                        doc = testNumberOfOccurrences(doc, false);
                                                        lastStructureId = jObj.getString("s.UAI");
                                                        garEtab = doc.createElement("men:GAREtab");
                                                        garEntEtablissement.appendChild(garEtab);
                                                        //GARPersonIdentifiant
                                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garEtab, jObj.getString("s.UAI"));
                                                        MediacentreController.insertNode("men:GARStructureNomCourant", doc, garEtab, jObj.getString("s.name"));
                                                        if (jObj.getString("s2.UAI") != null) {
                                                            MediacentreController.insertNode("men:GAREtablissementStructRattachFctl", doc, garEtab, jObj.getString("s2.UAI"));
                                                        }
                                                        counter += 5;
                                                    } else {
                                                        if (jObj.getString("s2.UAI") != null) {
                                                            MediacentreController.insertNode("men:GAREtablissementStructRattachFctl", doc, garEtab, jObj.getString("s2.UAI"));
                                                            counter++;
                                                        }
                                                    }
                                                    lastContract = jObj.getString("s.contract");
                                                    lastPhone = jObj.getString("s.phone");
                                                }
                                            }
                                            // we finish the last node
                                            if (!"none".equals(lastStructureId)) {
                                                MediacentreController.insertNode("men:GARStructureContrat", doc, garEtab, lastContract);
                                                MediacentreController.insertNode("men:GARStructureTelephone", doc, garEtab, lastPhone);
                                                //MediacentreController.insertNode("men:GARStructureEmail", doc, garEtab, "null");
                                            }

                                            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            mediacentreService.getEtablissementMef(new Handler<Either<String, JsonArray>>() {
                                                @Override
                                                public void handle(Either<String, JsonArray> event) {
                                                    if (event.isRight()) {
                                                        final Map<GARStructureMatiereEleveKey, String> mapSubjectTeacher = new HashMap<>();
                                                        // write the content into xml file
                                                        JsonArray etablissementMef = event.right().getValue();
                                                        // men:GARMEF
                                                        for (Object obj : etablissementMef) {
                                                            if (obj instanceof JsonObject) {
                                                                JsonObject jObj = (JsonObject) obj;
                                                                if (jObj.getString("n.module") != null) {
                                                                    GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(jObj.getString("s.UAI"), jObj.getString("n.module"));
                                                                    mapSubjectTeacher.put(key, jObj.getString("n.moduleName"));
                                                                /*
                                                                Element garEtablissementMef = doc.createElement("men:GARMEF");
                                                                garEntEtablissement.appendChild(garEtablissementMef);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMef, jObj.getString("s.UAI"));
                                                                MediacentreController.insertNode("men:GARMEFCode", doc, garEtablissementMef, jObj.getString("n.module"));
                                                                MediacentreController.insertNode("men:GARMEFLibelle", doc, garEtablissementMef, jObj.getString("n.moduleName"));
                                                                counter += 3;
                                                                doc = testNumberOfOccurrences(doc);*/
                                                                }
                                                            }
                                                        }
                                                        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                                        mediacentreService.getEtablissementMefFromTeacher(new Handler<Either<String, JsonArray>>() {
                                                            @Override
                                                            public void handle(Either<String, JsonArray> event) {
                                                                if (event.isRight()) {
                                                                    //Map<GARStructureMatiereEleveKey, String> mapSubjectTeacher = new HashMap<>();
                                                                    JsonArray etablissementMefTeacher = event.right().getValue();
                                                                    // men:GARMef
                                                                    for (Object obj : etablissementMefTeacher) {
                                                                        // construct the map
                                                                        if (obj instanceof JsonObject) {
                                                                            JsonObject jObj = (JsonObject) obj;
                                                                            if (jObj.getArray("u.modules") != null && jObj.getArray("u.modules").size() > 0) {
                                                                                // getting the names
                                                                                for (Object module : jObj.getArray("u.modules")) {
                                                                                    if (module instanceof String) {
                                                                                        String mod = (String) module;
                                                                                        String[] parts = mod.split("\\$");
                                                                                        GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(mapStructures.get(parts[0]), parts[1]);
                                                                                        mapSubjectTeacher.put(key, parts[2]);
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }

                                                                    // now, making the nodes in xml file, with datas from mapSubjectStudent
                                                                    for (Map.Entry<GARStructureMatiereEleveKey, String> entry : mapSubjectTeacher.entrySet()) {
                                                                        GARStructureMatiereEleveKey key = entry.getKey();
                                                                        String subName = entry.getValue();
                                                                        if( subName != null && !"".equals(subName)) { // field men:GARMEFLibelle is mandatory
                                                                            Element garEtablissementMef = doc.createElement("men:GARMEF");
                                                                            garEntEtablissement.appendChild(garEtablissementMef);
                                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMef, key.getUai());
                                                                            MediacentreController.insertNode("men:GARMEFCode", doc, garEtablissementMef, MediacentreController.customSubString(key.getSubCode(), 255));
                                                                            MediacentreController.insertNode("men:GARMEFLibelle", doc, garEtablissementMef, MediacentreController.customSubString(subName, 255));

                                                                            JsonObject jObj = mapModules.get(key.getSubCode());
                                                                            String garmefstat11 = jObj.getString("m.stat");
                                                                            String garmefrattach = jObj.getString("m.attachment");

                                                                            MediacentreController.insertNode("men:GARMEFRattach", doc, garEtablissementMef, garmefrattach);
                                                                            MediacentreController.insertNode("men:GARMEFSTAT11", doc, garEtablissementMef, garmefstat11);

                                                                            counter += 4;
                                                                            doc = testNumberOfOccurrences(doc, false);
                                                                        }
                                                                    }
                                                                    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                                                    mediacentreService.getEtablissementMatiere(new Handler<Either<String, JsonArray>>() {
                                                                        @Override
                                                                        public void handle(Either<String, JsonArray> event) {
                                                                            if (event.isRight()) {
                                                                                final Map<GARStructureMatiereEleveKey, String> mapSubjectStudent = new HashMap<>();
                                                                                // write the content into xml file
                                                                                JsonArray etablissementMatiere = event.right().getValue();
                                                                                // men:GARMAtiere
                                                                                for (Object obj : etablissementMatiere) {
                                                                                    //Element garEtablissementMatiere = doc.createElement("men:GARMatiere");
                                                                                    //garEntEtablissement.appendChild(garEtablissementMatiere);
                                                                                    if (obj instanceof JsonObject) {
                                                                                        JsonObject jObj = (JsonObject) obj;
                                                                                        GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(jObj.getString("s.UAI"), jObj.getString("sub.code"));
                                                                                        mapSubjectStudent.put(key, jObj.getString("sub.label"));

                                                                                    /*MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMatiere, jObj.getString("s.UAI"));
                                                                                    MediacentreController.insertNode("men:GARMatiereCode", doc, garEtablissementMatiere, jObj.getString("sub.code"));
                                                                                    MediacentreController.insertNode("men:GARMatiereLibelle", doc, garEtablissementMatiere, jObj.getString("sub.label"));*/
                                                                                    }
                                                                                    counter += 4;
                                                                                    doc = testNumberOfOccurrences(doc, false);
                                                                                }

                                                                                //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                                                                mediacentreService.getEtablissementMatiereFromStudents(new Handler<Either<String, JsonArray>>() {
                                                                                    @Override
                                                                                    public void handle(Either<String, JsonArray> event) {
                                                                                        if (event.isRight()) {
                                                                                            JsonArray etablissementMatiereEleve = event.right().getValue();
                                                                                            // men:GARMAtiere
                                                                                            for (Object obj : etablissementMatiereEleve) {
                                                                                                // construct the map
                                                                                                if (obj instanceof JsonObject) {
                                                                                                    JsonObject jObj = (JsonObject) obj;
                                                                                                    if (jObj.getArray("u.fieldOfStudy") != null && jObj.getArray("u.fieldOfStudy").size() > 0) {
                                                                                                        String uai = jObj.getString("s.UAI");
                                                                                                        // getting the names
                                                                                                        String[] subNames = new String[jObj.getArray("u.fieldOfStudyLabels").size()];
                                                                                                        int cpt = 0;
                                                                                                        for (Object subName : jObj.getArray("u.fieldOfStudyLabels")) {
                                                                                                            if (subName instanceof String) {
                                                                                                                String strSubName = (String) subName;
                                                                                                                subNames[cpt] = strSubName;
                                                                                                                cpt++;
                                                                                                            }
                                                                                                        }
                                                                                                        // filling the map
                                                                                                        cpt = 0;
                                                                                                        for (Object subCode : jObj.getArray("u.fieldOfStudy")) {
                                                                                                            if (subCode instanceof String) {
                                                                                                                GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(uai, (String) subCode);
                                                                                                                mapSubjectStudent.put(key, subNames[cpt]);
                                                                                                                cpt++;
                                                                                                            }
                                                                                                        }

                                                                                                    }
                                                                                                }
                                                                                            }

                                                                                            // now, making the nodes in xml file, with datas from mapSubjectStudent
                                                                                            for (Map.Entry<GARStructureMatiereEleveKey, String> entry : mapSubjectStudent.entrySet()) {
                                                                                                GARStructureMatiereEleveKey key = entry.getKey();
                                                                                                String subName = entry.getValue();
                                                                                                if( subName != null && !"".equals(subName)) { // field men:GARMEFLibelle is mandatory
                                                                                                    Element garEtablissementMatiere = doc.createElement("men:GARMatiere");
                                                                                                    garEntEtablissement.appendChild(garEtablissementMatiere);
                                                                                                    MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMatiere, key.getUai());
                                                                                                    MediacentreController.insertNode("men:GARMatiereCode", doc, garEtablissementMatiere, MediacentreController.customSubString(key.getSubCode(), 255));
                                                                                                    MediacentreController.insertNode("men:GARMatiereLibelle", doc, garEtablissementMatiere, MediacentreController.customSubString(subName, 255));
                                                                                                    counter += 4;
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
                                                                                                StreamResult result = new StreamResult(new File(path + getExportFileName("Etab", fileIndex)));
                                                                                                transformer.transform(source, result);

                                                                                                System.out.println("Structures saved");
                                                                                                boolean res = false;
                                                                                                try {
                                                                                                    res = MediacentreController.isFileValid(pathExport + getExportFileName("Etab", fileIndex));
                                                                                                } catch (IOException e) {
                                                                                                    e.printStackTrace();
                                                                                                } catch (SAXException e) {
                                                                                                    e.printStackTrace();
                                                                                                }
                                                                                                if( res == false ){
                                                                                System.out.println("Error on file : " + pathExport + getExportFileName("Etab", fileIndex));
                                                                            } else {
                                                                                System.out.println("File valid : " + pathExport + getExportFileName("Etab", fileIndex));
                                                                    }
                                                                                            } catch (TransformerException tfe) {
                                                                                                tfe.printStackTrace();
                                                        /*        } catch (SAXException e) {
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
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        };
                        });
                }
            };
        });
    }


    /**
     * Export des établissements 1D
     * @param mediacentreService
     * @param path
     * @param nbElementPerFile
     */
    public void exportStructures_1D(final MediacentreService mediacentreService, final String path,
                                    int nbElementPerFile, final String exportUAIList1D){
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;

        mediacentreService.getAllModules(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray allModules = event.right().getValue();
                    for (Object obj : allModules) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            jObj.putString("m.attachment", jObj.getString("m.attachment"));
                            jObj.putString("m.stat", jObj.getString("m.stat"));
                            mapModules.put(jObj.getString("m.externalId"), jObj);
                        }
                    }

                    mediacentreService.getAllStructures_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
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
                                //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                mediacentreService.getEtablissement_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            // write the content into xml file
                                            final JsonArray structures = event.right().getValue();
                                            doc = fileHeader_1D();
                                            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            // men:GAREtab
                                            String lastStructureId = "none";
                                            String lastContract = "";
                                            String lastPhone = "";
                                            Element garEtab = null;
                                            for (Object obj : structures) {
                                                if (obj instanceof JsonObject) {
                                                    JsonObject jObj = (JsonObject) obj;

                                                    if (jObj.getString("s.UAI") != null && !jObj.getString("s.UAI").equals(lastStructureId)) {

                                                        // we finish the precedent node, if exists
                                                        if (!"none".equals(lastStructureId)) {
                                                            MediacentreController.insertNode("men:GARStructureContrat", doc, garEtab, lastContract);
                                                            MediacentreController.insertNode("men:GARStructureTelephone", doc, garEtab, lastPhone);
                                                            //MediacentreController.insertNode("men:GARStructureEmail", doc, garEtab, "null");
                                                        }
                                                        doc = testNumberOfOccurrences(doc, true);
                                                        lastStructureId = jObj.getString("s.UAI");
                                                        garEtab = doc.createElement("men:GAREtab");
                                                        garEntEtablissement.appendChild(garEtab);

                                                        //GARPersonIdentifiant
                                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garEtab, jObj.getString("s.UAI"));
                                                        MediacentreController.insertNode("men:GARStructureNomCourant", doc, garEtab, jObj.getString("s.name"));
                                                        counter += 5;
                                                    }
                                                    lastContract = jObj.getString("s.contract");
                                                    lastPhone = jObj.getString("s.phone");
                                                }
                                            }
                                            // we finish the last node
                                            if (!"none".equals(lastStructureId)) {
                                                MediacentreController.insertNode("men:GARStructureContrat", doc, garEtab, lastContract);
                                                MediacentreController.insertNode("men:GARStructureTelephone", doc, garEtab, lastPhone);
                                                MediacentreController.insertNode("men:GARStructureEmail", doc, garEtab, "null");
                                            }

                                            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                Transformer transformer = transformerFactory.newTransformer();
                                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                                DOMSource source = new DOMSource(doc);
                                                StreamResult result = new StreamResult(new File(path + getExportFileName("Etab", fileIndex)));
                                                transformer.transform(source, result);

                                                System.out.println("Structures saved");
                                                boolean res = false;
                                                try {
                                                    res = MediacentreController.isFileValid(pathExport + getExportFileName("Etab", fileIndex));
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                } catch (SAXException e) {
                                                    e.printStackTrace();
                                                }
                                                if( res == false ){
                                                    System.out.println("Error on file : " + pathExport + getExportFileName("Etab", fileIndex));
                                                } else {
                                                    System.out.println("File valid : " + pathExport + getExportFileName("Etab", fileIndex));
                                                }
                                            } catch (TransformerException tfe) {
                                                tfe.printStackTrace();

                                            }

                                        }
                                    }
                                });
                            }
                        };
                    });
                }
            };
        });
    }

    public void exportStructures_2D(final MediacentreService mediacentreService, final String path,
                                    int nbElementPerFile, final String exportUAIList2D){
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;

        mediacentreService.getAllModules(new Handler<Either<String, JsonArray>>() {

            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray allModules = event.right().getValue();
                    for (Object obj : allModules) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            jObj.putString("m.attachment", jObj.getString("m.attachment"));
                            jObj.putString("m.stat", jObj.getString("m.stat"));
                            mapModules.put(jObj.getString("m.externalId"), jObj);
                        }
                    }


                    mediacentreService.getAllStructures_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
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

                                // -------------------------------------
                                // GAREtab 2D
                                // -------------------------------------
                                mediacentreService.getEtablissement_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            // write the content into xml file
                                            final JsonArray structures = event.right().getValue();
                                            doc = fileHeader();
                                            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            // men:GAREtab
                                            String lastStructureId = "none";
                                            String lastContract = "";
                                            String lastPhone = "";
                                            Element garEtab = null;
                                            for (Object obj : structures) {
                                                if (obj instanceof JsonObject) {
                                                    JsonObject jObj = (JsonObject) obj;
                                                    if (jObj.getString("s.UAI") != null && !jObj.getString("s.UAI").equals(lastStructureId)) {
                                                        // we finish the precedent node, if exists
                                                        if (!"none".equals(lastStructureId)) {
                                                            MediacentreController.insertNode("men:GARStructureContrat", doc, garEtab, lastContract);
                                                            MediacentreController.insertNode("men:GARStructureTelephone", doc, garEtab, lastPhone);
                                                            //MediacentreController.insertNode("men:GARStructureEmail", doc, garEtab, "null");
                                                        }
                                                        doc = testNumberOfOccurrences(doc, false);
                                                        lastStructureId = jObj.getString("s.UAI");
                                                        garEtab = doc.createElement("men:GAREtab");
                                                        garEntEtablissement.appendChild(garEtab);
                                                        //GARPersonIdentifiant
                                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garEtab, jObj.getString("s.UAI"));
                                                        MediacentreController.insertNode("men:GARStructureNomCourant", doc, garEtab, jObj.getString("s.name"));
                                                        if (jObj.getString("s2.UAI") != null) {
                                                            MediacentreController.insertNode("men:GAREtablissementStructRattachFctl", doc, garEtab, jObj.getString("s2.UAI"));
                                                        }
                                                        counter += 5;
                                                    } else {
                                                        if (jObj.getString("s2.UAI") != null) {
                                                            MediacentreController.insertNode("men:GAREtablissementStructRattachFctl", doc, garEtab, jObj.getString("s2.UAI"));
                                                            counter++;
                                                        }
                                                    }
                                                    lastContract = jObj.getString("s.contract");
                                                    lastPhone = jObj.getString("s.phone");
                                                }
                                            }
                                            // we finish the last node
                                            if (!"none".equals(lastStructureId)) {
                                                MediacentreController.insertNode("men:GARStructureContrat", doc, garEtab, lastContract);
                                                MediacentreController.insertNode("men:GARStructureTelephone", doc, garEtab, lastPhone);
                                                //MediacentreController.insertNode("men:GARStructureEmail", doc, garEtab, "null");
                                            }

                                            // -------------------------------------
                                            // GAREtab 2D
                                            // -------------------------------------
                                            mediacentreService.getEtablissementMef_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
                                                @Override
                                                public void handle(Either<String, JsonArray> event) {
                                                    if (event.isRight()) {
                                                        final Map<GARStructureMatiereEleveKey, String> mapSubjectTeacher = new HashMap<>();
                                                        // write the content into xml file
                                                        JsonArray etablissementMef = event.right().getValue();
                                                        // men:GARMEF
                                                        for (Object obj : etablissementMef) {
                                                            if (obj instanceof JsonObject) {
                                                                JsonObject jObj = (JsonObject) obj;
                                                                if (jObj.getString("n.module") != null) {
                                                                    GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(jObj.getString("s.UAI"), jObj.getString("n.module"));
                                                                    mapSubjectTeacher.put(key, jObj.getString("n.moduleName"));
                                                                /*
                                                                Element garEtablissementMef = doc.createElement("men:GARMEF");
                                                                garEntEtablissement.appendChild(garEtablissementMef);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMef, jObj.getString("s.UAI"));
                                                                MediacentreController.insertNode("men:GARMEFCode", doc, garEtablissementMef, jObj.getString("n.module"));
                                                                MediacentreController.insertNode("men:GARMEFLibelle", doc, garEtablissementMef, jObj.getString("n.moduleName"));
                                                                counter += 3;
                                                                doc = testNumberOfOccurrences(doc);*/
                                                                }
                                                            }
                                                        }

                                                        // -------------------------------------
                                                        // GARMEF (Teachers)
                                                        // -------------------------------------
                                                        mediacentreService.getEtablissementMefFromTeacher_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
                                                            @Override
                                                            public void handle(Either<String, JsonArray> event) {
                                                                if (event.isRight()) {
                                                                    //Map<GARStructureMatiereEleveKey, String> mapSubjectTeacher = new HashMap<>();
                                                                    JsonArray etablissementMefTeacher = event.right().getValue();
                                                                    // men:GARMef
                                                                    for (Object obj : etablissementMefTeacher) {
                                                                        // construct the map
                                                                        if (obj instanceof JsonObject) {
                                                                            JsonObject jObj = (JsonObject) obj;
                                                                            if (jObj.getArray("u.modules") != null && jObj.getArray("u.modules").size() > 0) {
                                                                                // getting the names
                                                                                for (Object module : jObj.getArray("u.modules")) {
                                                                                    if (module instanceof String) {
                                                                                        String mod = (String) module;
                                                                                        String[] parts = mod.split("\\$");
                                                                                        if(mapStructures.get(parts[0]) != null) {
                                                                                            GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(mapStructures.get(parts[0]), parts[1]);
                                                                                            mapSubjectTeacher.put(key, parts[2]);
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }

                                                                    // now, making the nodes in xml file, with datas from mapSubjectStudent
                                                                    for (Map.Entry<GARStructureMatiereEleveKey, String> entry : mapSubjectTeacher.entrySet()) {
                                                                        GARStructureMatiereEleveKey key = entry.getKey();
                                                                        String subName = entry.getValue();
                                                                        if( subName != null && !"".equals(subName)) { // field men:GARMEFLibelle is mandatory
                                                                            Element garEtablissementMef = doc.createElement("men:GARMEF");
                                                                            garEntEtablissement.appendChild(garEtablissementMef);
                                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMef, key.getUai());
                                                                            MediacentreController.insertNode("men:GARMEFCode", doc, garEtablissementMef, MediacentreController.customSubString(key.getSubCode(), 255));
                                                                            MediacentreController.insertNode("men:GARMEFLibelle", doc, garEtablissementMef, MediacentreController.customSubString(subName, 255));

                                                                            JsonObject jObj = mapModules.get(key.getSubCode());
                                                                            String garmefstat11 = jObj.getString("m.stat");
                                                                            String garmefrattach = jObj.getString("m.attachment");

                                                                            MediacentreController.insertNode("men:GARMEFRattach", doc, garEtablissementMef, garmefrattach);
                                                                            MediacentreController.insertNode("men:GARMEFSTAT11", doc, garEtablissementMef, garmefstat11);

                                                                            counter += 4;
                                                                            doc = testNumberOfOccurrences(doc, false);
                                                                        }
                                                                    }

                                                                    // -------------------------------------
                                                                    // GARMatiere
                                                                    // -------------------------------------
                                                                    mediacentreService.getEtablissementMatiere_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
                                                                        @Override
                                                                        public void handle(Either<String, JsonArray> event) {
                                                                            if (event.isRight()) {
                                                                                final Map<GARStructureMatiereEleveKey, String> mapSubjectStudent = new HashMap<>();
                                                                                // write the content into xml file
                                                                                JsonArray etablissementMatiere = event.right().getValue();
                                                                                // men:GARMAtiere
                                                                                for (Object obj : etablissementMatiere) {
                                                                                    //Element garEtablissementMatiere = doc.createElement("men:GARMatiere");
                                                                                    //garEntEtablissement.appendChild(garEtablissementMatiere);
                                                                                    if (obj instanceof JsonObject) {
                                                                                        JsonObject jObj = (JsonObject) obj;
                                                                                        GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(jObj.getString("s.UAI"), jObj.getString("sub.code"));
                                                                                        mapSubjectStudent.put(key, jObj.getString("sub.label"));

                                                                                    /*MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMatiere, jObj.getString("s.UAI"));
                                                                                    MediacentreController.insertNode("men:GARMatiereCode", doc, garEtablissementMatiere, jObj.getString("sub.code"));
                                                                                    MediacentreController.insertNode("men:GARMatiereLibelle", doc, garEtablissementMatiere, jObj.getString("sub.label"));*/
                                                                                    }
                                                                                    counter += 4;
                                                                                    doc = testNumberOfOccurrences(doc, false);
                                                                                }

                                                                                // -------------------------------------
                                                                                // GARMatiere (students)
                                                                                // -------------------------------------
                                                                                mediacentreService.getEtablissementMatiereFromStudents_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
                                                                                    @Override
                                                                                    public void handle(Either<String, JsonArray> event) {
                                                                                        if (event.isRight()) {
                                                                                            JsonArray etablissementMatiereEleve = event.right().getValue();
                                                                                            // men:GARMAtiere
                                                                                            for (Object obj : etablissementMatiereEleve) {
                                                                                                // construct the map
                                                                                                if (obj instanceof JsonObject) {
                                                                                                    JsonObject jObj = (JsonObject) obj;
                                                                                                    if (jObj.getArray("u.fieldOfStudy") != null && jObj.getArray("u.fieldOfStudy").size() > 0) {
                                                                                                        String uai = jObj.getString("s.UAI");
                                                                                                        // getting the names
                                                                                                        String[] subNames = new String[jObj.getArray("u.fieldOfStudyLabels").size()];
                                                                                                        int cpt = 0;
                                                                                                        for (Object subName : jObj.getArray("u.fieldOfStudyLabels")) {
                                                                                                            if (subName instanceof String) {
                                                                                                                String strSubName = (String) subName;
                                                                                                                subNames[cpt] = strSubName;
                                                                                                                cpt++;
                                                                                                            }
                                                                                                        }
                                                                                                        // filling the map
                                                                                                        cpt = 0;
                                                                                                        for (Object subCode : jObj.getArray("u.fieldOfStudy")) {
                                                                                                            if (subCode instanceof String) {
                                                                                                                GARStructureMatiereEleveKey key = new GARStructureMatiereEleveKey(uai, (String) subCode);
                                                                                                                mapSubjectStudent.put(key, subNames[cpt]);
                                                                                                                cpt++;
                                                                                                            }
                                                                                                        }

                                                                                                    }
                                                                                                }
                                                                                            }

                                                                                            // now, making the nodes in xml file, with datas from mapSubjectStudent
                                                                                            for (Map.Entry<GARStructureMatiereEleveKey, String> entry : mapSubjectStudent.entrySet()) {
                                                                                                GARStructureMatiereEleveKey key = entry.getKey();
                                                                                                String subName = entry.getValue();
                                                                                                if( subName != null && !"".equals(subName)) { // field men:GARMEFLibelle is mandatory
                                                                                                    Element garEtablissementMatiere = doc.createElement("men:GARMatiere");
                                                                                                    garEntEtablissement.appendChild(garEtablissementMatiere);
                                                                                                    MediacentreController.insertNode("men:GARStructureUAI", doc, garEtablissementMatiere, key.getUai());
                                                                                                    MediacentreController.insertNode("men:GARMatiereCode", doc, garEtablissementMatiere, MediacentreController.customSubString(key.getSubCode(), 255));
                                                                                                    MediacentreController.insertNode("men:GARMatiereLibelle", doc, garEtablissementMatiere, MediacentreController.customSubString(subName, 255));
                                                                                                    counter += 4;
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
                                                                                                StreamResult result = new StreamResult(new File(path + getExportFileName("Etab", fileIndex)));
                                                                                                transformer.transform(source, result);

                                                                                                System.out.println("Structures saved");
                                                                                                boolean res = false;
                                                                                                try {
                                                                                                    res = MediacentreController.isFileValid(pathExport + getExportFileName("Etab", fileIndex));
                                                                                                } catch (IOException e) {
                                                                                                    e.printStackTrace();
                                                                                                } catch (SAXException e) {
                                                                                                    e.printStackTrace();
                                                                                                }
                                                                                                if( res == false ){
                                                                                                    System.out.println("Error on file : " + pathExport + getExportFileName("Etab", fileIndex));
                                                                                                } else {
                                                                                                    System.out.println("File valid : " + pathExport + getExportFileName("Etab", fileIndex));
                                                                                                }
                                                                                            } catch (TransformerException tfe) {
                                                                                                tfe.printStackTrace();
                                                        /*        } catch (SAXException e) {
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
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        };
                    });
                }
            };
        });
    }

    private Document testNumberOfOccurrences(Document doc, boolean is1D) {
        if (nbElem <= counter) {
            // close the full file
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(pathExport + getExportFileName("Etab", fileIndex)));

                transformer.transform(source, result);
                boolean res = false;
                try {
                    res = MediacentreController.isFileValid(pathExport + getExportFileName("Etab", fileIndex));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
                if( res == false ){
                    System.out.println("Error on file : " + pathExport + getExportFileName("Etab", fileIndex));
                } else {
                    System.out.println("File valid : " + pathExport + getExportFileName("Etab", fileIndex));
                }

                System.out.println("Etab" + fileIndex + " saved");
                fileIndex++;
                counter = 0;
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
 /*           } catch (SAXException e) {
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
        garEntEtablissement = doc.createElement("men:GAR-ENT-Etab");
        doc.appendChild(garEntEtablissement);
        garEntEtablissement.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
        garEntEtablissement.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntEtablissement.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntEtablissement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntEtablissement.setAttribute("Version", "1.0");
        garEntEtablissement.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
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
        garEntEtablissement = doc.createElement("men:GAR-ENT-Etab");
        doc.appendChild(garEntEtablissement);
        garEntEtablissement.setAttribute("xmlns:men", "http://data.education.fr/ns/gar/1d");
        garEntEtablissement.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntEtablissement.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntEtablissement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntEtablissement.setAttribute("Version", "1.0");
        garEntEtablissement.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
        return doc;
    }
/*
    public static List<String> loadLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        InputStream is = new FileInputStream(file);
        Reader tmpReader = new InputStreamReader(is, "utf-8");
        BufferedReader reader = new BufferedReader(tmpReader);
        try {
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                lines.add(line);
            }
            return lines;
        } finally {
            reader.close();
        }
    }
*/
}


