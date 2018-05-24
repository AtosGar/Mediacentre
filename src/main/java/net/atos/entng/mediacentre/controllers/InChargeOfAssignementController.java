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
import java.util.Collections;
import java.util.List;

import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;

public class InChargeOfAssignementController {

    /**
     *  export Structures
     */
    public void exportInChargeOfAssignement(final MediacentreService mediacentreService, final String path, int nbElementPerFile, String inChargeOfAssignementName, final String defaultEmail){
        String groupName = "Responsables d'affectation";
        mediacentreService.getInChargeOfExportData(inChargeOfAssignementName, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if( event.isRight()){
                    // write the content into xml file
                    final JsonArray members2 = event.right().getValue();

                    List<String> jsonValues = new ArrayList<String>();
                    for (int i = 0; i < members2.size(); i++)
                        jsonValues.add(members2.get(i).toString());
                    Collections.sort(jsonValues);
                    final JsonArray members = new JsonArray();
                    for (int i = 0; i < jsonValues.size(); i++) {
                        members.add(new JsonObject(jsonValues.get(i)));
                    }

                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = null;
                    try {
                        docBuilder = docFactory.newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    }
                    // root elements
                    final Document doc = docBuilder.newDocument();
                    final Element garEntRespAff = doc.createElement("men:GAR-ENT-RespAff");
                    doc.appendChild(garEntRespAff);
                    garEntRespAff.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
                    garEntRespAff.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
                    garEntRespAff.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
                    garEntRespAff.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                    garEntRespAff.setAttribute("Version", "1.2");
                    garEntRespAff.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:garRespAff
                    String lastUserId = "";
                    Element garRespAff = null;
                    for( Object obj : members ){
                        if( obj instanceof JsonObject){
                            JsonObject jObj = (JsonObject) obj;
                            if( !lastUserId.equals(jObj.getString("u.id")) ) {
                                garRespAff = doc.createElement("men:GARRespAff");
                                garEntRespAff.appendChild(garRespAff);
                                MediacentreController.insertNode("men:GARPersonIdentifiant", doc, garRespAff, jObj.getString("u.id"));
                                MediacentreController.insertNode("men:GARPersonNom", doc, garRespAff, jObj.getString("u.lastName"));
                                MediacentreController.insertNode("men:GARPersonPrenom", doc, garRespAff, jObj.getString("u.firstName"));
                                MediacentreController.insertNode("men:GARPersonCivilite", doc, garRespAff, "");
                                if( jObj.getString("u.email") != null && !"".equals(jObj.getString("u.email"))  ){
                                    MediacentreController.insertNode("men:GARPersonMail", doc, garRespAff, jObj.getString("u.email"));
                                } else {
                                    // put the default email from parameter in ent-core.json
                                    MediacentreController.insertNode("men:GARPersonMail", doc, garRespAff, defaultEmail);
                                }
                                MediacentreController.insertNode("men:GARRespAffEtab", doc, garRespAff, jObj.getString("s2.UAI"));
                                lastUserId = jObj.getString("u.id");
                            } else {
                                MediacentreController.insertNode("men:GARRespAffEtab", doc, garRespAff, jObj.getString("s2.UAI"));
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
                        StreamResult result = new StreamResult(new File(path + getExportFileName("RespAff", 0)));

                        transformer.transform(source, result);

                        System.out.println("RespAff saved");
                    } catch (TransformerException tfe) {
                        tfe.printStackTrace();
                    }
                }
            }
        });
    }
}
