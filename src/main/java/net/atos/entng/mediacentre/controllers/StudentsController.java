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
import java.util.ArrayList;
import java.util.List;

import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;
import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName_1D;
import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName_2D;

public class StudentsController {

    private int counter = 0;            // nb of elements currently put in the file
    private int nbElem = 10000;         // max elements authorized in a file
    private int fileIndex = 0;          // index of the export file
    private String pathExport = "";     // path where the generated files are put
    private Element garEntEleve = null;
    private Document doc = null;
    private List<String> bannedUsers = new ArrayList<String>(); // users exclued because on multiple structures

    /**
     * export Students 2D
     */
    public void exportStudents(final MediacentreService mediacentreService, final String path, int nbElementPerFile, final Handler<List<String>> handler) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;
        mediacentreService.getUserExportData(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // write the content into xml file
                    final JsonArray students = event.right().getValue();
                    doc = fileHeader();
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:GAREleve
                    String lastStudentId = "";
                    String lastStudentBirthDate = "";
                    JsonObject lastjObj = null;
                    List<String> etabs = new ArrayList<String>();
                    Element garEleve = null;
                    for (Object obj : students) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            if (jObj.getString("s.UAI") != null || jObj.getString("s2.UAI") != null) {
                                if (jObj.getString("u.id") != null && !jObj.getString("u.id").equals(lastStudentId)) {
                                    // test if there is at least one
                                    if (lastjObj != null) {
                                        // start the new node
                                        garEleve = doc.createElement("men:GAREleve");
                                        garEntEleve.appendChild(garEleve);

                                        //GARPersonIdentifiant
                                        // TODO : faire le lien avec plusieurs établissements et récupérer l'UAI
                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleve, lastjObj.getString("u.id"));

                                        // GARPersonProfils
                                        Element garProfil = doc.createElement("men:GARPersonProfils");
                                        if (jObj.getString("s.UAI") != null) {
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s.UAI"));
                                        } else {
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                                        }
                                        MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                                        garEleve.appendChild(garProfil);

                                        // close last one
                                        MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEleve, lastjObj.getString("u.lastName"));
                                        MediacentreController.insertNode("men:GARPersonNom", doc, garEleve, lastjObj.getString("u.lastName"));
                                        MediacentreController.insertNode("men:GARPersonPrenom", doc, garEleve, lastjObj.getString("u.firstName"));
                                        if (lastjObj.getString("u.otherNames") != null) {
                                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.otherNames"));
                                        } else {
                                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.firstName"));
                                        }
                                        MediacentreController.insertNode("men:GARPersonCivilite", doc, garEleve, lastjObj.getString(""));
                                        MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEleve, lastjObj.getString("s.UAI"));
                                        // add all the  GARPersonEtab
                                        for (String etab : etabs) {
                                            MediacentreController.insertNode("men:GARPersonEtab", doc, garEleve, etab/*jObj.getString("s.UAI")*/);
                                        }
                                        etabs = new ArrayList<String>();
                                        MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEleve, lastStudentBirthDate);
                                        doc = testNumberOfOccurrences(doc, false);
                                    } else {
                                        if (lastjObj != null) {
                                            bannedUsers.add(lastjObj.getString("u.id"));
                                            etabs = new ArrayList<String>();
                                        }
                                    }
                                    etabs.add(jObj.getString("s.UAI"));
                                    if (jObj.getString("s2.UAI") != null) {
                                        etabs.add(jObj.getString("s2.UAI"));
                                    }
                                    lastStudentId = jObj.getString("u.id");
                                    lastStudentBirthDate = jObj.getString("u.birthDate");
                                    lastjObj = jObj;
                                    counter += 14;
                                } else {
                                    // add the GARPersonProfils
                                    if (jObj.getString("s2.UAI") != null) {
                                        etabs.add(jObj.getString("s2.UAI"));
                                        /*Element garProfil = doc.createElement("men:GARPersonProfils");
                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, jObj.getString("s2.UAI"));
                                        MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                                        garEleve.appendChild(garProfil);
                                        counter += 4;*/
                                    }
                                }
                            }
                        }

                    }
                    // close last one

                    // start the new node
                    garEleve = doc.createElement("men:GAREleve");
                    garEntEleve.appendChild(garEleve);

                    //GARPersonIdentifiant
                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleve, lastjObj.getString("u.id"));
                    Element garProfil = doc.createElement("men:GARPersonProfils");
                    if (lastjObj.getString("s.UAI") != null) {
                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s.UAI"));
                    } else {
                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                    }
//                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                    MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                    garEleve.appendChild(garProfil);


                    MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEleve, lastjObj.getString("u.lastName"));
                    MediacentreController.insertNode("men:GARPersonNom", doc, garEleve, lastjObj.getString("u.lastName"));
                    MediacentreController.insertNode("men:GARPersonPrenom", doc, garEleve, lastjObj.getString("u.firstName"));
                    if (lastjObj.getString("u.otherNames") != null) {
                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.otherNames"));
                    } else {
                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.firstName"));
                    }
                    MediacentreController.insertNode("men:GARPersonCivilite", doc, garEleve, lastjObj.getString(""));
                    MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEleve, lastjObj.getString("s.UAI"));
                    // add all the  GARPersonEtab
                    for (String etab : etabs) {
                        MediacentreController.insertNode("men:GARPersonEtab", doc, garEleve, etab/*jObj.getString("s.UAI")*/);
                    }
                    MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEleve, lastStudentBirthDate);

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    mediacentreService.getPersonMef(new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray personMef = event.right().getValue();
                                // men:GARPersonMEF
                                for (Object obj : personMef) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (jObj.getString("u.module") != null && !bannedUsers.contains(jObj.getString("u.id"))) {
                                            Element garPersonMef = doc.createElement("men:GARPersonMEF");
                                            garEntEleve.appendChild(garPersonMef);
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, jObj.getString("s.UAI"));
                                            MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonMef, jObj.getString("u.id"));
                                            MediacentreController.insertNode("men:GARMEFCode", doc, garPersonMef, jObj.getString("u.module"));
                                            counter += 4;
                                            doc = testNumberOfOccurrences(doc, false);
                                        }
                                    }
                                }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                mediacentreService.getEleveEnseignement(new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            // write the content into xml file
                                            JsonArray eleveEnseignement = event.right().getValue();
                                            // men:GAREleveEnseignement
                                            for (Object obj : eleveEnseignement) {
                                                if (obj instanceof JsonObject) {
                                                    JsonObject jObj = (JsonObject) obj;
                                                    if (jObj.getArray("u.fieldOfStudy") != null
                                                            && jObj.getArray("u.fieldOfStudy").size() > 0
                                                            && !bannedUsers.contains(jObj.getString("u.id"))) {
                                                        JsonArray fosArray = jObj.getArray("u.fieldOfStudy");
                                                        for (int i = 0; i < fosArray.size(); i++) {
                                                            Element garEleveEnseignement = doc.createElement("men:GAREleveEnseignement");
                                                            garEntEleve.appendChild(garEleveEnseignement);
                                                            String fos = fosArray.get(i).toString();
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEleveEnseignement, jObj.getString("s.UAI"));
                                                            MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleveEnseignement, jObj.getString("u.id"));
                                                            MediacentreController.insertNode("men:GARMatiereCode", doc, garEleveEnseignement, MediacentreController.customSubString(fos, 255));
                                                            counter += 4;
                                                        }
                                                        doc = testNumberOfOccurrences(doc, false);
                                                    }
                                                }
                                            }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                Transformer transformer = transformerFactory.newTransformer();
                                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                                DOMSource source = new DOMSource(doc);

                                                StreamResult result = new StreamResult(new File(path + getExportFileName("Eleve", fileIndex)));
                                                transformer.transform(source, result);
                                                boolean res = false;
                                                try {
                                                    res = MediacentreController.isFileValid(pathExport + getExportFileName("Eleve", fileIndex));
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                } catch (SAXException e) {
                                                    e.printStackTrace();
                                                }
                                                if (res == false) {
                                                    System.out.println("Error on file : " + pathExport + getExportFileName("Eleves", fileIndex));
                                                } else {
                                                    System.out.println("File valid : " + pathExport + getExportFileName("Eleves", fileIndex));
                                                }

                                                System.out.println("Students saved");
                                            } catch (TransformerException tfe) {
                                                tfe.printStackTrace();
                                        /*    } catch (SAXException e) {
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
     * Export des élèves 1D
     * @param mediacentreService
     * @param path
     * @param nbElementPerFile
     * @param handler
     */
    public void exportStudents_1D(final MediacentreService mediacentreService, final String path, int nbElementPerFile,
                                  final String uaiExportList) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;

        mediacentreService.getStudentExportData_1D(uaiExportList, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // write the content into xml file
                    final JsonArray students = event.right().getValue();
                    doc = fileHeader_1D();

                    String lastStudentId = "";
                    String lastStudentBirthDate = "";
                    JsonObject lastjObj = null;
                    List<String> etabs = new ArrayList<String>();
                    Element garEleve = null;

                    /* GAREleve */
                    for (Object obj : students) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            if (jObj.getString("s.UAI") != null || jObj.getString("s2.UAI") != null) {
                                if (jObj.getString("u.id") != null && !jObj.getString("u.id").equals(lastStudentId)) {
                                    // test if there is at least one
                                    if (lastjObj != null) {
                                        // start the new node
                                        garEleve = doc.createElement("men:GAREleve");
                                        garEntEleve.appendChild(garEleve);

                                        //GARPersonIdentifiant
                                        // TODO : faire le lien avec plusieurs établissements et récupérer l'UAI
                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleve, lastjObj.getString("u.id"));

                                        // GARPersonProfils
                                        Element garProfil = doc.createElement("men:GARPersonProfils");
                                        if (jObj.getString("s.UAI") != null) {
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s.UAI"));
                                        } else {
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                                        }
                                        MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                                        garEleve.appendChild(garProfil);

                                        // close last one
                                        MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEleve, lastjObj.getString("u.lastName"));
                                        MediacentreController.insertNode("men:GARPersonNom", doc, garEleve, lastjObj.getString("u.lastName"));
                                        MediacentreController.insertNode("men:GARPersonPrenom", doc, garEleve, lastjObj.getString("u.firstName"));
                                        if (lastjObj.getString("u.otherNames") != null) {
                                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.otherNames"));
                                        } else {
                                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.firstName"));
                                        }
                                        MediacentreController.insertNode("men:GARPersonCivilite", doc, garEleve, lastjObj.getString(""));
                                        MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEleve, lastjObj.getString("s.UAI"));
                                        // add all the  GARPersonEtab
                                        for (String etab : etabs) {
                                            MediacentreController.insertNode("men:GARPersonEtab", doc, garEleve, etab/*jObj.getString("s.UAI")*/);
                                        }
                                        etabs = new ArrayList<String>();
                                        MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEleve, lastStudentBirthDate);
                                        doc = testNumberOfOccurrences(doc, true);
                                    } else {
                                        if (lastjObj != null) {

                                            etabs = new ArrayList<String>();
                                        }
                                    }
                                    etabs.add(jObj.getString("s.UAI"));
                                    if (jObj.getString("s2.UAI") != null) {
                                        etabs.add(jObj.getString("s2.UAI"));
                                    }
                                    lastStudentId = jObj.getString("u.id");
                                    lastStudentBirthDate = jObj.getString("u.birthDate");
                                    lastjObj = jObj;
                                    counter += 14;
                                } else {
                                    // add the GARPersonProfils
                                    if (jObj.getString("s2.UAI") != null) {
                                        etabs.add(jObj.getString("s2.UAI"));
                                    }
                                }
                            }
                        }

                    }
                    // close last one

                    // start the new node
                    garEleve = doc.createElement("men:GAREleve");
                    garEntEleve.appendChild(garEleve);

                    //GARPersonIdentifiant
                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleve, lastjObj.getString("u.id"));
                    Element garProfil = doc.createElement("men:GARPersonProfils");
                    if (lastjObj.getString("s.UAI") != null) {
                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s.UAI"));
                    } else {
                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                    }
//                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                    MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                    garEleve.appendChild(garProfil);


                    MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEleve, lastjObj.getString("u.lastName"));
                    MediacentreController.insertNode("men:GARPersonNom", doc, garEleve, lastjObj.getString("u.lastName"));
                    MediacentreController.insertNode("men:GARPersonPrenom", doc, garEleve, lastjObj.getString("u.firstName"));

                    if (lastjObj.getString("u.otherNames") != null) {
                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.otherNames"));
                    } else {
                        MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.firstName"));
                    }

                    MediacentreController.insertNode("men:GARPersonCivilite", doc, garEleve, lastjObj.getString(""));
                    MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEleve, lastjObj.getString("s.UAI"));

                    // add all the  GARPersonEtab
                    for (String etab : etabs) {
                        MediacentreController.insertNode("men:GARPersonEtab", doc, garEleve, etab/*jObj.getString("s.UAI")*/);
                    }
                    MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEleve, lastStudentBirthDate);

                   /* GARPersonMEFSTAT4 : Adapté depuis le 2D pour le 1D (GarPersonMEF) */
                    mediacentreService.getPersonMefStat4_1D(uaiExportList, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray personMef = event.right().getValue();
                                // men:GARPersonMEF
                                for (Object obj : personMef) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (jObj.getString("u.level") != null) {
                                            Element garPersonMef = doc.createElement("men:GARPersonMEFSTAT4");
                                            garEntEleve.appendChild(garPersonMef);
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, jObj.getString("s.UAI"));
                                            MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonMef, jObj.getString("u.id"));
                                            MediacentreController.insertNode("men:GARMEFSTAT4Code", doc, garPersonMef, jObj.getString("u.level"));
                                            counter += 4;
                                            doc = testNumberOfOccurrences(doc, true);
                                        }
                                    }
                                }

                                try {
                                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                    Transformer transformer = transformerFactory.newTransformer();
                                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                    DOMSource source = new DOMSource(doc);

                                    StreamResult result = new StreamResult(new File(path + getExportFileName_1D("Eleve", fileIndex)));
                                    transformer.transform(source, result);
                                    boolean res = false;
                                    try {
                                        res = MediacentreController.isFileValid(pathExport + getExportFileName_1D("Eleve", fileIndex));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (SAXException e) {
                                        e.printStackTrace();
                                    }
                                    if (res == false) {
                                        System.out.println("Error on file : " + pathExport + getExportFileName_1D("Eleves", fileIndex));
                                    } else {
                                        System.out.println("File valid : " + pathExport + getExportFileName_1D("Eleves", fileIndex));
                                    }

                                    System.out.println("Students saved");
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


    /**
     * exportStudents_2D
     * @param mediacentreService
     * @param path
     * @param nbElementPerFile
     * @param handler
     */
    public void exportStudents_2D(final MediacentreService mediacentreService, final String path, int nbElementPerFile,
                                  final String uaiExportList) {
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;

        // -----------------------------------
        // GAR-ENT-Eleve & GARPersonProfils
        // -----------------------------------
        mediacentreService.getStudentExportData_2D(uaiExportList, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // write the content into xml file
                    final JsonArray students = event.right().getValue();
                    doc = fileHeader();

                   // men:GAREleve
                    String lastStudentId = "";
                    String lastStudentBirthDate = "";
                    JsonObject lastjObj = null;
                    List<String> etabs = new ArrayList<String>();
                    Element garEleve = null;
                    for (Object obj : students) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            if (jObj.getString("s.UAI") != null || jObj.getString("s2.UAI") != null) {
                                if (jObj.getString("u.id") != null && !jObj.getString("u.id").equals(lastStudentId)) {
                                    // test if there is at least one
                                    if (lastjObj != null) {
                                        // start the new node
                                        garEleve = doc.createElement("men:GAREleve");
                                        garEntEleve.appendChild(garEleve);

                                        //GARPersonIdentifiant
                                        // TODO : faire le lien avec plusieurs établissements et récupérer l'UAI
                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleve, lastjObj.getString("u.id"));

                                        // GARPersonProfils
                                        Element garProfil = doc.createElement("men:GARPersonProfils");
                                        if (jObj.getString("s.UAI") != null) {
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s.UAI"));
                                        } else {
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                                        }
                                        MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                                        garEleve.appendChild(garProfil);

                                        // close last one
                                        MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEleve, lastjObj.getString("u.lastName"));
                                        MediacentreController.insertNode("men:GARPersonNom", doc, garEleve, lastjObj.getString("u.lastName"));
                                        MediacentreController.insertNode("men:GARPersonPrenom", doc, garEleve, lastjObj.getString("u.firstName"));
                                        if (lastjObj.getString("u.otherNames") != null) {
                                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.otherNames"));
                                        } else {
                                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.firstName"));
                                        }
                                        MediacentreController.insertNode("men:GARPersonCivilite", doc, garEleve, lastjObj.getString(""));
                                        MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEleve, lastjObj.getString("s.UAI"));
                                        // add all the  GARPersonEtab
                                        for (String etab : etabs) {
                                            MediacentreController.insertNode("men:GARPersonEtab", doc, garEleve, etab/*jObj.getString("s.UAI")*/);
                                        }
                                        etabs = new ArrayList<String>();
                                        MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEleve, lastStudentBirthDate);
                                        doc = testNumberOfOccurrences(doc, false);
                                    } else {
                                        if (lastjObj != null) {
                                            bannedUsers.add(lastjObj.getString("u.id"));
                                            etabs = new ArrayList<String>();
                                        }
                                    }
                                    etabs.add(jObj.getString("s.UAI"));
                                    if (jObj.getString("s2.UAI") != null) {
                                        etabs.add(jObj.getString("s2.UAI"));
                                    }
                                    lastStudentId = jObj.getString("u.id");
                                    lastStudentBirthDate = jObj.getString("u.birthDate");
                                    lastjObj = jObj;
                                    counter += 14;
                                } else {
                                    // add the GARPersonProfils
                                    if (jObj.getString("s2.UAI") != null) {
                                        etabs.add(jObj.getString("s2.UAI"));
                                        /*Element garProfil = doc.createElement("men:GARPersonProfils");
                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, jObj.getString("s2.UAI"));
                                        MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                                        garEleve.appendChild(garProfil);
                                        counter += 4;*/
                                    }
                                }
                            }
                        }

                    }
                    // close last one

                    if(lastjObj != null) {
                        // start the new node
                        garEleve = doc.createElement("men:GAREleve");
                        garEntEleve.appendChild(garEleve);

                        //GARPersonIdentifiant
                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleve, lastjObj.getString("u.id"));
                        Element garProfil = doc.createElement("men:GARPersonProfils");
                        if (lastjObj.getString("s.UAI") != null) {
                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s.UAI"));
                        } else {
                            MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                        }
//                        MediacentreController.insertNode("men:GARStructureUAI", doc, garProfil, lastjObj.getString("s2.UAI"));
                        MediacentreController.insertNode("men:GARPersonProfil", doc, garProfil, "National_elv");
                        garEleve.appendChild(garProfil);


                        MediacentreController.insertNode("men:GARPersonNomPatro", doc, garEleve, lastjObj.getString("u.lastName"));
                        MediacentreController.insertNode("men:GARPersonNom", doc, garEleve, lastjObj.getString("u.lastName"));
                        MediacentreController.insertNode("men:GARPersonPrenom", doc, garEleve, lastjObj.getString("u.firstName"));
                        if (lastjObj.getString("u.otherNames") != null) {
                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.otherNames"));
                        } else {
                            MediacentreController.insertNode("men:GARPersonAutresPrenoms", doc, garEleve, lastjObj.getString("u.firstName"));
                        }
                        MediacentreController.insertNode("men:GARPersonCivilite", doc, garEleve, lastjObj.getString(""));
                        MediacentreController.insertNode("men:GARPersonStructRattach", doc, garEleve, lastjObj.getString("s.UAI"));
                        // add all the  GARPersonEtab
                        for (String etab : etabs) {
                            MediacentreController.insertNode("men:GARPersonEtab", doc, garEleve, etab/*jObj.getString("s.UAI")*/);
                        }
                        MediacentreController.insertNode("men:GARPersonDateNaissance", doc, garEleve, lastStudentBirthDate);
                    }
                    // --------------------
                    // GARPersonMEF
                    // --------------------
                    mediacentreService.getPersonMef_2D(uaiExportList, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray personMef = event.right().getValue();
                                // men:GARPersonMEF
                                for (Object obj : personMef) {
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        if (jObj.getString("u.module") != null && !bannedUsers.contains(jObj.getString("u.id"))) {
                                            Element garPersonMef = doc.createElement("men:GARPersonMEF");
                                            garEntEleve.appendChild(garPersonMef);
                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, jObj.getString("s.UAI"));
                                            MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonMef, jObj.getString("u.id"));
                                            MediacentreController.insertNode("men:GARMEFCode", doc, garPersonMef, jObj.getString("u.module"));
                                            counter += 4;
                                            doc = testNumberOfOccurrences(doc, false);
                                        }
                                    }
                                }

                                // ------------------------
                                // GAREleveEnseignement
                                // ------------------------
                                mediacentreService.getEleveEnseignement_2D(uaiExportList, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            // write the content into xml file
                                            JsonArray eleveEnseignement = event.right().getValue();
                                            // men:GAREleveEnseignement
                                            for (Object obj : eleveEnseignement) {
                                                if (obj instanceof JsonObject) {
                                                    JsonObject jObj = (JsonObject) obj;
                                                    if (jObj.getArray("u.fieldOfStudy") != null
                                                            && jObj.getArray("u.fieldOfStudy").size() > 0
                                                            && !bannedUsers.contains(jObj.getString("u.id"))) {
                                                        JsonArray fosArray = jObj.getArray("u.fieldOfStudy");
                                                        for (int i = 0; i < fosArray.size(); i++) {
                                                            Element garEleveEnseignement = doc.createElement("men:GAREleveEnseignement");
                                                            garEntEleve.appendChild(garEleveEnseignement);
                                                            String fos = fosArray.get(i).toString();
                                                            MediacentreController.insertNode("men:GARStructureUAI", doc, garEleveEnseignement, jObj.getString("s.UAI"));
                                                            MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEleveEnseignement, jObj.getString("u.id"));
                                                            MediacentreController.insertNode("men:GARMatiereCode", doc, garEleveEnseignement, MediacentreController.customSubString(fos, 255));
                                                            counter += 4;
                                                        }
                                                        doc = testNumberOfOccurrences(doc, false);
                                                    }
                                                }
                                            }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();

                                                Transformer transformer = transformerFactory.newTransformer();

                                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                                                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                                                DOMSource source = new DOMSource(doc);

                                                StreamResult result = new StreamResult(new File(path + getExportFileName_2D("Eleve", fileIndex)));

                                                transformer.transform(source, result);

                                                boolean res = false;

                                                try {

                                                    res = MediacentreController.isFileValid(pathExport + getExportFileName_2D("Eleve", fileIndex));

                                                } catch (IOException e) {

                                                    e.printStackTrace();

                                                } catch (SAXException e) {

                                                    e.printStackTrace();

                                                }
                                                if (res == false) {

                                                    System.out.println("Error on file : " + pathExport + getExportFileName_2D("Eleves", fileIndex));

                                                } else {

                                                    System.out.println("File valid : " + pathExport + getExportFileName_2D("Eleves", fileIndex));
                                                }

                                                System.out.println("Students saved");
                                            } catch (TransformerException tfe) {
                                                tfe.printStackTrace();
                                        /*    } catch (SAXException e) {
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

    /**
     *
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
                    result = new StreamResult(new File(pathExport + getExportFileName_1D("Eleve", fileIndex)));
                }else{
                    result = new StreamResult(new File(pathExport + getExportFileName_2D("Eleve", fileIndex)));
                }

                transformer.transform(source, result);

                System.out.println("Students" + fileIndex + " saved");
                boolean res = false;
                try {

                    if(is1D){
                        res = MediacentreController.isFileValid(pathExport + getExportFileName_1D("Eleve", fileIndex));
                    }else{
                        res = MediacentreController.isFileValid(pathExport + getExportFileName_2D("Eleve", fileIndex));
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
                if (res == false) {

                    if(is1D){
                        System.out.println("Error on file : " + pathExport + getExportFileName_1D("Eleve", fileIndex));
                    }else{
                        System.out.println("Error on file : " + pathExport + getExportFileName_2D("Eleve", fileIndex));
                    }

                } else {

                    if(is1D){
                        System.out.println("File valid : " + pathExport + getExportFileName_1D("Eleves", fileIndex));
                    }else{
                        System.out.println("File valid : " + pathExport + getExportFileName_2D("Eleves", fileIndex));
                    }
                }
                fileIndex++;
                counter = 0;
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
          /*  } catch (SAXException e) {
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
        garEntEleve = doc.createElement("men:GAR-ENT-Eleve");
        doc.appendChild(garEntEleve);
        garEntEleve.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
        garEntEleve.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntEleve.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntEleve.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntEleve.setAttribute("Version", "1.0");
        garEntEleve.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
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
        garEntEleve = doc.createElement("men:GAR-ENT-Eleve");
        doc.appendChild(garEntEleve);
        garEntEleve.setAttribute("xmlns:men", "http://data.education.fr/ns/gar/1d");
        garEntEleve.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        garEntEleve.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        garEntEleve.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        garEntEleve.setAttribute("Version", "1.0");
        garEntEleve.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
        return doc;
    }
}
