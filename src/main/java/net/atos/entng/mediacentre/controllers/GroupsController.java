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

public class GroupsController {

    /**
     * export Groups
     */
    public void exportGroups(final MediacentreService mediacentreService, final String path) {
        mediacentreService.getDivisionsExportData(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    // write the content into xml file
                    final JsonArray divisions = event.right().getValue();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = null;
                    try {
                        docBuilder = docFactory.newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    }
                    // root elements
                    final Document doc = docBuilder.newDocument();
                    final Element garEntGroup = doc.createElement("men:GAR-ENT-Groupe");
                    doc.appendChild(garEntGroup);
                    garEntGroup.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
                    garEntGroup.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
                    garEntGroup.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
                    garEntGroup.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                    garEntGroup.setAttribute("Version", "1.0");
                    garEntGroup.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:GAREleve
                    for (Object obj : divisions) {
                        Element garDivision = doc.createElement("men:GARGroupe");
                        garEntGroup.appendChild(garDivision);
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            // TODO : faire le lien avec plusieurs établissements et récupérer l'UAI
                            MediacentreController.insertNode("men:GARGroupeCode", doc, garDivision, jObj.getString("c.externalId"));
                            MediacentreController.insertNode("men:GARStructureUAI", doc, garDivision, jObj.getString("s.UAI"));
                            MediacentreController.insertNode("men:GARGroupeLibelle", doc, garDivision, jObj.getString("c.name"));
                            MediacentreController.insertNode("men:GARGroupeStatut", doc, garDivision, "DIVISION");
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
                                                    for (Object obj : enGroupeAndClasseMatiere) {
                                                        if (obj instanceof JsonObject) {
                                                            JsonObject jObj = (JsonObject) obj;
                                                            // groups
                                                            if( jObj.getArray("t.groups") != null && jObj.getArray("t.groups").size() > 0 ) {
                                                                JsonArray groups = jObj.getArray("t.groups");
                                                                for (int i = 0; i < groups.size(); i++) {
                                                                    String group = groups.get(i).toString();
                                                                    garEnGroupeMatiere = doc.createElement("men:GAREnsGroupeMatiere");
                                                                    garEntGroup.appendChild(garEnGroupeMatiere);
                                                                    MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, jObj.getString("s.UAI"));
                                                                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, jObj.getString("u.id"));
                                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, group);
                                                                    MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, jObj.getString("sub.code"));

                                                                }
                                                            }
                                                            if( jObj.getArray("t.classes") != null && jObj.getArray("t.classes").size() > 0 ) {
                                                                JsonArray classes = jObj.getArray("t.classes");
                                                                for (int i = 0; i < classes.size(); i++) {
                                                                    String classe = classes.get(i).toString();
                                                                    garEnGroupeMatiere = doc.createElement("men:GAREnsClasseMatiere");
                                                                    garEntGroup.appendChild(garEnGroupeMatiere);
                                                                    MediacentreController.insertNode("men:GARStructureUAI", doc, garEnGroupeMatiere, jObj.getString("s.UAI"));
                                                                    MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garEnGroupeMatiere, jObj.getString("u.id"));
                                                                    MediacentreController.insertNode("men:GARGroupeCode", doc, garEnGroupeMatiere, classe);
                                                                    MediacentreController.insertNode("men:GARMatiereCode", doc, garEnGroupeMatiere, jObj.getString("sub.code"));

                                                                }
                                                            }
                                                        }
                                                    }
                                                }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                                                try {
                                                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                    Transformer transformer = transformerFactory.newTransformer();
                                                    DOMSource source = new DOMSource(doc);
                                                    StreamResult result = new StreamResult(new File(path + "\\Groupes.xml"));

                                                    transformer.transform(source, result);

                                                    System.out.println("Groupes.xml saved");
                                                } catch (TransformerException tfe) {
                                                    tfe.printStackTrace();
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
}
