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

import static net.atos.entng.mediacentre.controllers.MediacentreController.getExportFileName;

public class InChargeOfAssignementController {

    /**
     *  export Structures
     */
    public void exportInChargeOfAssignement(final MediacentreService mediacentreService, final String path, int nbElementPerFile, String inChargeOfAssignementName){
        String groupName = "Responsables d'affectation";
        mediacentreService.getInChargeOfExportData(inChargeOfAssignementName, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if( event.isRight()){
                    // write the content into xml file
                    final JsonArray members = event.right().getValue();
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
                    for( Object obj : members ){
                        Element garRespAff = doc.createElement("men:GARRespAff");
                        garEntRespAff.appendChild(garRespAff);
                        if( obj instanceof JsonObject){
                            JsonObject jObj = (JsonObject) obj;
                            MediacentreController.insertNode( "men:GARPersonIdentifiant"             , doc, garRespAff, jObj.getString("u.id"));
                            MediacentreController.insertNode( "men:GARPersonNom"      , doc, garRespAff, jObj.getString("u.lastName"));
                            MediacentreController.insertNode( "men:GARPersonPrenom"         , doc, garRespAff, jObj.getString("u.firstName"));
                            MediacentreController.insertNode( "men:GARPersonCivilite"       , doc, garRespAff, "");
                            MediacentreController.insertNode( "men:GARPersonMail"       , doc, garRespAff, jObj.getString("u.email"));
                            MediacentreController.insertNode( "men:GARRespAffEtab"       , doc, garRespAff, jObj.getString("s.UAI"));
                        }
                    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    try {
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
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
