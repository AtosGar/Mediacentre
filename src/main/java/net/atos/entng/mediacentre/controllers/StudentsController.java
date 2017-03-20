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


public class StudentsController {

    /**
     *  export Students
     */
    public void exportStudents(final MediacentreService mediacentreService, final String path){
        mediacentreService.getUserExportData(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if( event.isRight()){
                    // write the content into xml file
                    final JsonArray students = event.right().getValue();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = null;
                    try {
                        docBuilder = docFactory.newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    }
                    // root elements
                    final Document doc = docBuilder.newDocument();
                    final Element garEntEleve = doc.createElement("men:GAR-ENT-Eleve");
                    doc.appendChild(garEntEleve);
                    garEntEleve.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
                    garEntEleve.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
                    garEntEleve.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
                    garEntEleve.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                    garEntEleve.setAttribute("Version", "1.0");
                    garEntEleve.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:GAREleve
                    String lastStudentId = "";
                    Element garEleve = null;
                    for( Object obj : students ){
                        if( obj instanceof JsonObject){
                            JsonObject jObj = (JsonObject) obj;
                            if( jObj.getString("u.id") != null && jObj.getString("u.id") != lastStudentId ) {
                                // new node
                                garEleve = doc.createElement("men:GAREleve");
                                garEntEleve.appendChild(garEleve);
                                //GARPersonIdentifiant
                                // TODO : faire le lien avec plusieurs établissements et récupérer l'UAI
                                MediacentreController.insertNode( "men:GARPersonIdentifiant"   , doc, garEleve, jObj.getString("u.id"));

                                // GARPersonProfils
                                Element garProfil = doc.createElement("men:GARPersonProfils");
                                MediacentreController.insertNode( "men:GARStructureUAI"           , doc, garProfil, jObj.getString("s.UAI"));
                                MediacentreController.insertNode( "men:GARPersonProfil"           , doc, garProfil, "National_ELV");
                                garEleve.appendChild(garProfil);

                                MediacentreController.insertNode( "men:GARPersonNomPatro"      , doc, garEleve, jObj.getString("u.lastName"));
                                MediacentreController.insertNode( "men:GARPersonNom"           , doc, garEleve, jObj.getString("u.displayName"));
                                MediacentreController.insertNode( "men:GARPersonPrenom"        , doc, garEleve, jObj.getString("u.firstName"));
                                if( jObj.getString("u.otherNames") != null ) {
                                    MediacentreController.insertNode( "men:GARPersonAutresPrenoms" , doc, garEleve, jObj.getString("u.firstName"));
                                } else {
                                    MediacentreController.insertNode( "men:GARPersonAutresPrenoms" , doc, garEleve, jObj.getString("u.otherNames"));
                                }
                                MediacentreController.insertNode( "men:GARPersonCivilite"      , doc, garEleve, jObj.getString(""));
                                MediacentreController.insertNode( "men:GARPersonStructRattach" , doc, garEleve, jObj.getString("s.UAI"));
                                MediacentreController.insertNode( "men:GARPersonEtab"          , doc, garEleve, jObj.getString("s.UAI"));
                                MediacentreController.insertNode("men:GARPersonDateNaissance"  , doc, garEleve, jObj.getString("u.birthDate"));
                                if( jObj.getString("s2.UAI") != null ) {
                                    MediacentreController.insertNode( "men:GARPersonEtab"          , doc, garEleve, jObj.getString("s2.UAI"));
                                }
                                lastStudentId = jObj.getString("u.id");
                            } else {
                                // add the s2.UAI to the object.
                                MediacentreController.insertNode( "men:GARPersonEtab"          , doc, garEleve, jObj.getString("s2.UAI"));
                                Element garProfil = doc.createElement("men:GARPersonProfils");
                                MediacentreController.insertNode( "men:GARStructureUAI"           , doc, garProfil, jObj.getString("s2.UAI"));
                                MediacentreController.insertNode( "men:GARPersonProfil"           , doc, garProfil, "National_ELV");
                                garEleve.appendChild(garProfil);
                            }
                        }
                    }

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
                                        Element garPersonMef = doc.createElement("men:GARPersonMEF");
                                        garEntEleve.appendChild(garPersonMef);
                                        MediacentreController.insertNode("men:GARStructureUAI", doc, garPersonMef, jObj.getString("s.UAI"));
                                        MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garPersonMef, jObj.getString("u.id"));
                                        MediacentreController.insertNode("men:GARMEFCode", doc, garPersonMef, jObj.getString("u.module"));
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
                                                    if( jObj.getArray("u.FieldOfStudy") != null && jObj.getArray("u.fieldOfStudy").size() > 0 ) {
                                                        JsonArray fosArray = jObj.getArray("u.FieldOfStudy");
                                                        for (int i = 0; i < fosArray.size(); i++) {
                                                            Element garEleveEnseignement = doc.createElement("men:GAREleveEnseignement");
                                                            garEntEleve.appendChild(garEleveEnseignement);
                                                            String fos = fosArray.get(i).toString();
                                                            MediacentreController.insertNode("men:GARStructureUAI",       doc, garEleveEnseignement, jObj.getString("s.UAI"));
                                                            MediacentreController.insertNode("men:GARPersonIdentifiant",  doc, garEleveEnseignement, jObj.getString("u.id"));
                                                            MediacentreController.insertNode("men:GARMatiereCode", doc, garEleveEnseignement, fos);
                                                        }
                                                    }
                                                }
                                        }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                Transformer transformer = transformerFactory.newTransformer();
                                                DOMSource source = new DOMSource(doc);
                                                StreamResult result = new StreamResult(new File(path + "\\Students.xml"));

                                                transformer.transform(source, result);

                                                System.out.println("Students.xml saved");
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


}
