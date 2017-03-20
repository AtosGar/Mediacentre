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

public class StructuresController {

    /**
     *  export Structures
     */
    public void exportStructures(final MediacentreService mediacentreService, final String path){
        mediacentreService.getEtablissement(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if( event.isRight()){
                    // write the content into xml file
                    final JsonArray structures = event.right().getValue();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = null;
                    try {
                        docBuilder = docFactory.newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    }
                    // root elements
                    final Document doc = docBuilder.newDocument();
                    final Element garEntEtablissement = doc.createElement("men:GAR-ENT-Etab");
                    doc.appendChild(garEntEtablissement);
                    garEntEtablissement.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
                    garEntEtablissement.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
                    garEntEtablissement.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
                    garEntEtablissement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                    garEntEtablissement.setAttribute("Version", "1.0");
                    garEntEtablissement.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:GAREtab
                    String lastStructureId = "";
                    Element garEtab = null;
                    for( Object obj : structures ){
                        if( obj instanceof JsonObject){
                            JsonObject jObj = (JsonObject) obj;
                            if( jObj.getString("s.UAI") != null && jObj.getString("s.UAI") != lastStructureId ) {
                                garEtab = doc.createElement("men:GAREtab");
                                garEntEtablissement.appendChild(garEtab);
                                //GARPersonIdentifiant
                                MediacentreController.insertNode("men:GARStructureUAI", doc, garEtab, jObj.getString("s.UAI"));
                                MediacentreController.insertNode("men:GARStructureNomCourant", doc, garEtab, jObj.getString("s.name"));
                                MediacentreController.insertNode("men:GARStructureContrat", doc, garEtab, jObj.getString("s.contract"));
                                MediacentreController.insertNode("men:GARStructureTelephone", doc, garEtab, jObj.getString("s.phone"));
                                if( jObj.getString("s2.UAI") != null ) {
                                    MediacentreController.insertNode("men:GAREtablissementStrctRattachFctl", doc, garEtab, jObj.getString("s2.UAI"));
                                }
                            } else {
                                if( jObj.getString("s2.UAI") != null ) {
                                    MediacentreController.insertNode("men:GAREtablissementStrctRattachFctl", doc, garEtab, jObj.getString("s2.UAI"));
                                }
                            }
                        }

                    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    mediacentreService.getEtablissementMef(new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                // write the content into xml file
                                JsonArray etablissementMef = event.right().getValue();
                                // men:GARMEF
                                for (Object obj : etablissementMef) {
                                    Element garEtablissementMef = doc.createElement("men:GARPersonMEF");
                                    garEntEtablissement.appendChild(garEtablissementMef);
                                    if (obj instanceof JsonObject) {
                                        JsonObject jObj = (JsonObject) obj;
                                        MediacentreController.insertNode("men:GARStructureUAI",       doc, garEtablissementMef, jObj.getString("s.UAI"));
                                        MediacentreController.insertNode("men:GARMEFCode",  doc, garEtablissementMef, jObj.getString("n.module"));
                                        MediacentreController.insertNode("men:GARMEFLibelle",            doc, garEtablissementMef, jObj.getString("n.moduleName"));
                                    }
                                }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                mediacentreService.getEtablissementMatiere(new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            // write the content into xml file
                                            JsonArray etablissementMatiere = event.right().getValue();
                                            // men:GARMAtiere
                                            for (Object obj : etablissementMatiere) {
                                                Element garEtablissementMatiere = doc.createElement("men:GARMatiere");
                                                garEntEtablissement.appendChild(garEtablissementMatiere);
                                                if (obj instanceof JsonObject) {
                                                    JsonObject jObj = (JsonObject) obj;
                                                    MediacentreController.insertNode("men:GARStructureUAI",    doc, garEtablissementMatiere, jObj.getString("s.UAI"));
                                                    MediacentreController.insertNode("men:GARMatiereCode",     doc, garEtablissementMatiere, jObj.getString("sub.code"));
                                                    MediacentreController.insertNode("men:GARMatiereLibelle",  doc, garEtablissementMatiere, jObj.getString("sub.libelle"));
                                                }
                                            }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                Transformer transformer = transformerFactory.newTransformer();
                                                DOMSource source = new DOMSource(doc);
                                                StreamResult result = new StreamResult(new File(path + "\\Structures.xml"));

                                                transformer.transform(source, result);

                                                System.out.println("Structures.xml saved");
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


