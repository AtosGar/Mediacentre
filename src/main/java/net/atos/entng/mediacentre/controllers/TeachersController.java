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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;
import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName_1D;
import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName_2D;

public class TeachersController {

    private int counter = 0;            // nb of elements currently put in the file
    private int nbElem = 10000;         // max elements authorized in a file
    private int fileIndex = 0;          // index of the export file
    private String pathExport = "";     // path where the generated files are put
    private Element garEntEnseignant = null;
    private Document doc = null;
    private String BOTHPROFILES = "both";
    private List<String> bannedUsers = new ArrayList<String>(); // users exclued because on multiple structures

    /**
     * export Teachers
     */
    public void exportTeachers(final MediacentreService mediacentreService, final String path,
                               int nbElementPerFile, final Handler<List<String>> handler) {
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
                                                if (lastjObj != null) {
                                                    // ad-d all the GARPersonProfils
                                                    Iterator it = structProfile.entrySet().iterator();

                                                    garEnseignant = doc.createElement("men:GAREnseignant");
                                                    garEntEnseignant.appendChild(garEnseignant);
                                                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, lastjObj.getString("u.id"));

                                                    while (it.hasNext()) {
                                                        Map.Entry pair = (Map.Entry) it.next();
                                                        // handle case where both profiles for 1 structure
                                                        if (BOTHPROFILES.equals(pair.getValue().toString())) {
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
                                                        if (data[2] != null) { // field men:GAREnsDisciplinePosteCode is mandatory
                                                            Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                                            MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                                            garEnseignant.appendChild(garEnsDisciplinesPostes);
                                                            counter += 3;
                                                        }
                                                    }
                                                    profilType = "National_ens";
                                                    etabs = new HashSet<String>();
                                                    doc = testNumberOfOccurrences(doc, false);

                                                } else {
                                                    if (lastjObj != null) {
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
                                                        if ("DOC".equals(parts[1]) && "DOCUMENTATION".equals(parts[2])) {
                                                            profilType = "National_doc";
                                                        } else {
                                                            profilType = "National_ens";
                                                        }

                                                        // test if it already exists with another profileType for the structure
                                                        String existingProfileType = structProfile.get(mapStructures.get(data[0].toString()));
                                                        if (existingProfileType != null && !profilType.equals(existingProfileType)) {
                                                            // we need to make a structure with both profiles
                                                            structProfile.put(mapStructures.get(data[0].toString()), BOTHPROFILES);
                                                        } else {
                                                            structProfile.put(mapStructures.get(data[0].toString()), profilType/*data[1]*/); // for  GARPersonProfils, because it can be multiple for 1 structure
                                                        }

                                                        etabs.add(mapStructures.get(data[0].toString()));
                                                        if (jObj.getString("s2.UAI") != null) {
                                                            etabs.add(jObj.getString("s2.UAI"));
                                                            if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                                structProfile.put(mapStructures.get(data[0].toString()), "National_ens");
                                                            }
                                                        }
                                                        listDisciplinesPostes.add(data);
                                                    }
                                                } else {
                                                    // we create an empty one, because there is no functions attribute
                                                    if (jObj.getString("s.UAI") != null) {
                                                        etabs.add(jObj.getString("s.UAI"));
                                                        if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                            structProfile.put(jObj.getString("s2.UAI"), "National_ens");
                                                        }
                                                    }
                                                    if (jObj.getString("s2.UAI") != null) {
                                                        etabs.add(jObj.getString("s2.UAI"));
                                                        if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                            structProfile.put(jObj.getString("s2.UAI"), "National_ens");
                                                        }
                                                    }
                                                }
                                                lastTeacherId = jObj.getString("u.id");
                                                lastTeacherBirthDate = jObj.getString("u.birthDate");
                                                lastjObj = jObj;
                                            } else {
                                                if (jObj.getString("s2.UAI") != null) {
                                                    etabs.add(jObj.getString("s2.UAI"));
                                                    if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                        structProfile.put(jObj.getString("s2.UAI"), "National_ens");
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
                                    if (data[2] != null) { // field men:GAREnsDisciplinePosteCode is mandatory
                                        Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                        MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                        garEnseignant.appendChild(garEnsDisciplinesPostes);
                                        counter += 3;
                                    }
                                }
                                doc = testNumberOfOccurrences(doc, false);

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
                                                doc = testNumberOfOccurrences(doc, false);
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
                                                if (res == false) {
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

    /**
     * Export des enseignants 1D
     * @param mediacentreService
     * @param path
     * @param nbElementPerFile
     * @param handler
     */
    public void exportTeachers_1D(final MediacentreService mediacentreService, final String path, int nbElementPerFile,
                                  final String exportUAIList1D) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;


        // Construct association MefStat4/Classe
        final Map<String, Set<String>> associationsClasseMefMap = new HashMap<String, Set<String>>();

        mediacentreService.getClasseMefStat4_1D(exportUAIList1D,
                new Handler<Either<String, JsonArray>>() {

            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {

                    JsonArray personClasseMef = event.right().getValue();

                    for (Object obj : personClasseMef) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;

                            String mef = jObj.getString("mefstat4Code");
                            String classe = jObj.getString("c.externalId");

                            Set<String> mefstatSet = associationsClasseMefMap.get(classe);

                            if(mefstatSet == null){
                                mefstatSet = new HashSet<String>();
                                associationsClasseMefMap.put(classe, mefstatSet);
                            }

                            mefstatSet.add(mef);


                        }
                    }
                }
            }
        });


        // Construct association  UAI/externalId
        final Map<String, String> mapStructures = new HashMap<String, String>();
        final Set<String> uaiSelectedSet = new HashSet<String>();

        mediacentreService.getAllStructures_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {

                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {

                            JsonArray allStructures = event.right().getValue();
                            for (Object obj : allStructures) {
                                if (obj instanceof JsonObject) {
                                    JsonObject jObj = (JsonObject) obj;
                                    mapStructures.put(jObj.getString("s.externalId"), jObj.getString("s.UAI"));
                                    uaiSelectedSet.add(jObj.getString("s.UAI"));
                                }
                            }
                        }
                    }
                });

        //
        mediacentreService.getTeachersExportData_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // write the content into xml file
                    final JsonArray teachers = event.right().getValue();
                    doc = fileHeader_1D();

                    // GAREnseignant
                    String lastTeacherId = "";
                    String lastTeacherBirthDate = "";
                    String profilType = "National_ens";

                    JsonObject lastjObj = null;
                    Element garEnseignant = null;
                    Map<String, String> structProfile = new HashMap<String, String>();
                    ArrayList<String[]> listDisciplinesPostes = new ArrayList<String[]>();

                    String previousUid = "";
                    JsonObject previousObj = null;
                    Set<String> etabs = new HashSet<String>();

                    for (Object obj : teachers) {
                        if (obj instanceof JsonObject) {

                            JsonObject jObj = (JsonObject) obj;
                            if (jObj.getString("s.UAI") != null || jObj.getString("s2.UAI") != null) {
                                if (jObj.getString("u.id") != null && !lastTeacherId.equals(jObj.getString("u.id"))) {

                                    // test if there is a last one
                                    if (lastjObj != null) {
                                        // ad-d all the GARPersonProfils
                                        Iterator it = structProfile.entrySet().iterator();

                                        garEnseignant = doc.createElement("men:GAREnseignant");
                                        garEntEnseignant.appendChild(garEnseignant);
                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, lastjObj.getString("u.id"));

                                        while (it.hasNext()) {
                                            Map.Entry pair = (Map.Entry) it.next();
                                            // handle case where both profiles for 1 structure
                                            if (BOTHPROFILES.equals(pair.getValue().toString())) {
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
                                            if (data[2] != null && mapStructures.get(data[0])!=null) { // field men:GAREnsDisciplinePosteCode is mandatory
                                                Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsSpecialitesPostes");
                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                                MediacentreController.insertNode("men:GAREnsSpecialitePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                                garEnseignant.appendChild(garEnsDisciplinesPostes);
                                                counter += 3;
                                            }
                                        }
                                        profilType = "National_ens";
                                        etabs = new HashSet<String>();
                                        doc = testNumberOfOccurrences(doc, true);

                                    } else {
                                        if (lastjObj != null) {
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
                                            if ("DOC".equals(parts[1]) && "DOCUMENTATION".equals(parts[2])) {
                                                profilType = "National_doc";
                                            } else {
                                                profilType = "National_ens";
                                            }


                                            if(mapStructures.get(data[0].toString()) != null) {
                                                // test if it already exists with another profileType for the structure

                                                String existingProfileType = structProfile.get(mapStructures.get(data[0].toString()));
                                                if (existingProfileType != null && !profilType.equals(existingProfileType)) {
                                                    // we need to make a structure with both profiles
                                                    structProfile.put(mapStructures.get(data[0].toString()), BOTHPROFILES);
                                                } else {
                                                    structProfile.put(mapStructures.get(data[0].toString()), profilType/*data[1]*/); // for  GARPersonProfils, because it can be multiple for 1 structure
                                                }

                                                etabs.add(mapStructures.get(data[0].toString()));

                                                if (jObj.getString("s2.UAI") != null) {
                                                    if (uaiSelectedSet.contains(jObj.getString("s2.UAI"))) {
                                                        etabs.add(jObj.getString("s2.UAI"));
                                                    }
                                                    if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                        structProfile.put(mapStructures.get(data[0].toString()), "National_ens");
                                                    }
                                                }
                                                listDisciplinesPostes.add(data);
                                            }
                                        }
                                    } else {
                                        // we create an empty one, because there is no functions attribute
                                        if (jObj.getString("s.UAI") != null && uaiSelectedSet.contains(jObj.getString("s.UAI"))) {

                                            etabs.add(jObj.getString("s.UAI"));
                                            if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                structProfile.put(jObj.getString("s2.UAI"), "National_ens");
                                            }

                                        }
                                        if (jObj.getString("s2.UAI") != null && uaiSelectedSet.contains(jObj.getString("s2.UAI"))) {
                                            etabs.add(jObj.getString("s2.UAI"));
                                            if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                structProfile.put(jObj.getString("s2.UAI"), "National_ens");
                                            }
                                        }
                                    }
                                    lastTeacherId = jObj.getString("u.id");
                                    lastTeacherBirthDate = jObj.getString("u.birthDate");
                                    lastjObj = jObj;
                                } else {
                                    if (jObj.getString("s2.UAI") != null  && uaiSelectedSet.contains(jObj.getString("s2.UAI"))) {
                                        etabs.add(jObj.getString("s2.UAI"));
                                        if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                            structProfile.put(jObj.getString("s2.UAI"), "National_ens");
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


                    if(lastjObj != null) {
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
                            if (data[2] != null && mapStructures.get(data[0]) != null ) { // field men:GAREnsDisciplinePosteCode is mandatory
                                Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsSpecialitesPostes");
                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                MediacentreController.insertNode("men:GAREnsSpecialitePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                garEnseignant.appendChild(garEnsDisciplinesPostes);
                                counter += 3;
                            }
                        }

                        doc = testNumberOfOccurrences(doc, true);
                    }

                    /**
                     * GARPersonMEFSTAT4
                     * **/
                    mediacentreService.getTeacherClasse_1D(exportUAIList1D, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray personMef = event.right().getValue();

                                // men:GARPersonMEFSTAT4
                                for (Object obj : personMef) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;

                                        String garStructureUai = jObj.getString("s.UAI");
                                        String classeExternalId = jObj.getString("c.externalId");
                                        String garPersonIdentifiant = jObj.getString("u.id");

                                        Set<String> mefstatSet = associationsClasseMefMap.get(classeExternalId);

                                        for(String garMefstat4Code : mefstatSet){
                                            Element garPersonMefstat4 = doc.createElement("men:GARPersonMEFSTAT4");
                                            garEntEnseignant.appendChild(garPersonMefstat4);

                                            MediacentreController.insertNode(
                                                    "men:GARStructureUAI", doc, garPersonMefstat4, garStructureUai);
                                            MediacentreController.insertNode(
                                                    "men:GARPersonIdentifiant", doc, garPersonMefstat4, garPersonIdentifiant);
                                            MediacentreController.insertNode(
                                                    "men:GARMEFSTAT4Code", doc, garPersonMefstat4, garMefstat4Code);

                                            counter += 4;
                                        }


                                    }
                                    doc = testNumberOfOccurrences(doc, true);
                                }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                try {
                                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                    Transformer transformer = transformerFactory.newTransformer();
                                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                    DOMSource source = new DOMSource(doc);
                                    StreamResult result = new StreamResult(new File(path + getExportFileName_1D("Enseignant", fileIndex)));

                                    transformer.transform(source, result);
                                    boolean res = false;
                                    try {
                                        res = MediacentreController.isFileValid(pathExport + getExportFileName_1D("Enseignant", fileIndex));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (SAXException e) {
                                        e.printStackTrace();
                                    }
                                    if (res == false) {
                                        System.out.println("Error on file : " + pathExport + getExportFileName_1D("Enseignant", fileIndex));
                                    } else {
                                        System.out.println("File valid : " + pathExport + getExportFileName_1D("Enseignant", fileIndex));
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

                        }
                    });
                }
            }
        });

    }

    /**
     * exportTeachers_2D
     * @param mediacentreService
     * @param path
     * @param nbElementPerFile
     * @param handler
     */
    public void exportTeachers_2D(final MediacentreService mediacentreService, final String path,
                               int nbElementPerFile, final String exportUAIList2D) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;
        mediacentreService.getAllStructures_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // construct hashmap association between UAI and externalId
                    final Map<String, String> mapStructures = new HashMap<String, String>();
                    final Set<String> uaiSelectedSet = new HashSet<String>();

                    JsonArray allStructures = event.right().getValue();
                    for (Object obj : allStructures) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            mapStructures.put(jObj.getString("s.externalId"), jObj.getString("s.UAI"));
                            uaiSelectedSet.add(jObj.getString("s.UAI"));
                        }
                    }

                    // ----------------------------------
                    // GAREnseignant & GARPersonProfils
                    // ----------------------------------
                    mediacentreService.getTeachersExportData_2D(exportUAIList2D,
                            new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                final JsonArray teachers = event.right().getValue();
                                doc = fileHeader();


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
                                                if (lastjObj != null) {
                                                    // ad-d all the GARPersonProfils
                                                    Iterator it = structProfile.entrySet().iterator();

                                                    garEnseignant = doc.createElement("men:GAREnseignant");
                                                    garEntEnseignant.appendChild(garEnseignant);
                                                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, lastjObj.getString("u.id"));

                                                    while (it.hasNext()) {
                                                        Map.Entry pair = (Map.Entry) it.next();
                                                        // handle case where both profiles for 1 structure
                                                        if (BOTHPROFILES.equals(pair.getValue().toString())) {
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
                                                        MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, etab);
                                                    }

                                                    MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEnseignant, lastTeacherBirthDate);

                                                    for (String[] data : listDisciplinesPostes) {
                                                        if (data[2] != null && mapStructures.get(data[0])!=null) { // field men:GAREnsDisciplinePosteCode is mandatory
                                                            Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                                            MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                                            garEnseignant.appendChild(garEnsDisciplinesPostes);
                                                            counter += 3;
                                                        }
                                                    }
                                                    profilType = "National_ens";
                                                    etabs = new HashSet<String>();
                                                    doc = testNumberOfOccurrences(doc, false);

                                                } else {
                                                    if (lastjObj != null) {
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
                                                        if ("DOC".equals(parts[1]) && "DOCUMENTATION".equals(parts[2])) {
                                                            profilType = "National_doc";
                                                        } else {
                                                            profilType = "National_ens";
                                                        }

                                                        // test if it already exists with another profileType for the structure

                                                        if(mapStructures.get(data[0].toString()) != null) {
                                                            String existingProfileType = structProfile.get(mapStructures.get(data[0].toString()));

                                                            if (existingProfileType != null && !profilType.equals(existingProfileType)) {
                                                                // we need to make a structure with both profiles
                                                                structProfile.put(mapStructures.get(data[0].toString()), BOTHPROFILES);
                                                            } else {
                                                                structProfile.put(mapStructures.get(data[0].toString()), profilType/*data[1]*/); // for  GARPersonProfils, because it can be multiple for 1 structure
                                                            }


                                                            etabs.add(mapStructures.get(data[0].toString()));


                                                            if (jObj.getString("s2.UAI") != null) {
                                                                if (uaiSelectedSet.contains(jObj.getString("s2.UAI"))) {
                                                                    etabs.add(jObj.getString("s2.UAI"));
                                                                }

                                                                if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                                    structProfile.put(mapStructures.get(data[0].toString()), "National_ens");
                                                                }
                                                            }
                                                            listDisciplinesPostes.add(data);
                                                        }
                                                    }
                                                } else {
                                                    // we create an empty one, because there is no functions attribute
                                                    if (jObj.getString("s.UAI") != null && uaiSelectedSet.contains(jObj.getString("s.UAI"))) {

                                                        etabs.add(jObj.getString("s.UAI"));

                                                        if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                            structProfile.put(jObj.getString("s2.UAI"), "National_ens");
                                                        }
                                                    }

                                                    if (jObj.getString("s2.UAI") != null && uaiSelectedSet.contains(jObj.getString("s2.UAI")))  {
                                                        etabs.add(jObj.getString("s2.UAI"));
                                                        if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                            structProfile.put(jObj.getString("s2.UAI"), "National_ens");
                                                        }
                                                    }
                                                }
                                                lastTeacherId = jObj.getString("u.id");
                                                lastTeacherBirthDate = jObj.getString("u.birthDate");
                                                lastjObj = jObj;
                                            } else {
                                                if (jObj.getString("s2.UAI") != null && uaiSelectedSet.contains(jObj.getString("s2.UAI"))) {
                                                    etabs.add(jObj.getString("s2.UAI"));
                                                    if (!structProfile.containsKey(jObj.getString("s2.UAI"))) {
                                                        structProfile.put(jObj.getString("s2.UAI"), "National_ens");
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

                                if(lastjObj != null) {
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
                                        if (data[2] != null && mapStructures.get(data[0]) != null ) { // field men:GAREnsDisciplinePosteCode is mandatory
                                            Element garEnsDisciplinesPostes = doc.createElement("men:GAREnsDisciplinesPostes");
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEnsDisciplinesPostes, mapStructures.get(data[0]));
                                            MediacentreController.insertNode("men:GAREnsDisciplinePosteCode", doc, garEnsDisciplinesPostes, MediacentreController.customSubString(data[2], 255));
                                            garEnseignant.appendChild(garEnsDisciplinesPostes);
                                            counter += 3;
                                        }
                                    }
                                }
                                doc = testNumberOfOccurrences(doc, false);

                                // ----------------------------------
                                // GARPersonMEF
                                // ----------------------------------
                                mediacentreService.getPersonMefTeacher_2D(exportUAIList2D, new Handler<Either<String, JsonArray>>() {
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

                                                            if(mapStructures.get(parts[0]) != null) {
                                                                Element garPersonMef = doc.createElement("men:GARPersonMEF");
                                                                garEntEnseignant.appendChild(garPersonMef);
                                                                MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, mapStructures.get(parts[0]));
                                                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonMef, jObj.getString("u.id"));
                                                                MediacentreController.insertNode("men:GARMEFCode", doc, garPersonMef, MediacentreController.customSubString(parts[1], 255));
                                                                counter += 4;
                                                            }
                                                        }
                                                    }
                                                }
                                                doc = testNumberOfOccurrences(doc, false);
                                            }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                Transformer transformer = transformerFactory.newTransformer();
                                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                                DOMSource source = new DOMSource(doc);
                                                StreamResult result = new StreamResult(new File(path + getExportFileName_2D("Enseignant", fileIndex)));

                                                transformer.transform(source, result);
                                                boolean res = false;
                                                try {
                                                    res = MediacentreController.isFileValid(pathExport + getExportFileName_2D("Enseignant", fileIndex));
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                } catch (SAXException e) {
                                                    e.printStackTrace();
                                                }
                                                if (res == false) {
                                                    System.out.println("Error on file : " + pathExport + getExportFileName_2D("Enseignant", fileIndex));
                                                } else {
                                                    System.out.println("File valid : " + pathExport + getExportFileName_2D("Enseignant", fileIndex));
                                                }

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
            }
        });
    }


    /**
     * Fonctions 1D
     */

    private void generateNodesGarEnseignant(JsonObject garEnseignantJsonObject,Element garEnseignant, Set<String> etabs){
        String uid = garEnseignantJsonObject.getString("u.id");
        String lastname = garEnseignantJsonObject.getString("u.lastName");
        String firstname = garEnseignantJsonObject.getString("u.firstName");
        JsonArray  structuresArray = garEnseignantJsonObject.getArray("u.structures");
        String birthdate = garEnseignantJsonObject.getString("u.birthDate");
        String s1uai= garEnseignantJsonObject.getString("s.UAI");
        String profilName = garEnseignantJsonObject.getString("p.name");
        String s2uai = garEnseignantJsonObject.getString("s2.UAI");
        JsonArray functionsArray = garEnseignantJsonObject.getArray("u.functions");

        etabs.add(s2uai);


        garEnseignant = doc.createElement("men:GAREnseignant");
        garEntEnseignant.appendChild(garEnseignant);
        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnseignant, uid);
        MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEnseignant, lastname);
        MediacentreController.insertNode("men:GARPersonNom", doc, garEnseignant, lastname);
        MediacentreController.insertNode("men:GARPersonPrenom", doc, garEnseignant, firstname);

        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEnseignant, firstname);

        MediacentreController.insertNode("men:GARPersonCivilite", doc, garEnseignant, "");
        MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEnseignant, s1uai);
        for (String etab : etabs) {
            MediacentreController.insertNode("men:GARPersonEtab", doc, garEnseignant, etab);
        }
        MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEnseignant, birthdate);
    }

    private void generateNodesGarPersonProfils_1D(Iterator it,  Element garEnseignant, final MediacentreService mediacentreService){
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            // handle case where both profiles for 1 structure
            if (BOTHPROFILES.equals(pair.getValue().toString())) {
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
    }

    /**
     * Répartie les documents tous les 1000
     * @param doc
     * @return
     */
    private Document testNumberOfOccurrences(Document doc, boolean is1D) {
        if (nbElem <= counter) {
            // close the full file
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DOMSource source = new DOMSource(doc);

                StreamResult result  ;

                if(is1D){
                    result = new StreamResult(new File(pathExport + getExportFileName_1D("Enseignant", fileIndex)));
                }else{
                    result = new StreamResult(new File(pathExport + getExportFileName_2D("Enseignant", fileIndex)));
                }

                transformer.transform(source, result);
                boolean res = false;
                try {

                    if(is1D){
                        res = MediacentreController.isFileValid(pathExport + getExportFileName_1D("Enseignant", fileIndex));
                    }else{
                        res = MediacentreController.isFileValid(pathExport + getExportFileName_2D("Enseignant", fileIndex));
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
                if (res == false) {
                    if(is1D){
                        System.out.println("Error on file : " + pathExport + getExportFileName_1D("Enseignant", fileIndex));
                    }else{
                        System.out.println("Error on file : " + pathExport + getExportFileName_2D("Enseignant", fileIndex));
                    }
                } else {

                    if(is1D){
                        System.out.println("File valid : " + pathExport + getExportFileName_1D("Enseignant", fileIndex));
                    }else{
                        System.out.println("File valid : " + pathExport + getExportFileName_2D("Enseignant", fileIndex));
                    }
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
            if(is1D){
                return fileHeader_1D();
            }else{
                return fileHeader();
            }
        } else {
            return doc;
        }
    }

    private Document fileHeader() {
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

    private Document fileHeader_1D() {
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
        garEntEnseignant.setAttribute("xmlns:men", "http://data.education.fr/ns/gar/1d");
        garEntEnseignant.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntEnseignant.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntEnseignant.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntEnseignant.setAttribute("Version", "1.0");
        garEntEnseignant.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
        return doc;
    }

}
