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

public class StructuresController {

    private     int counter = 0;            // nb of elements currently put in the file
    private     int nbElem = 10000;         // max elements authorized in a file
    private     int fileIndex = 0;          // index of the export file
    private     String pathExport = "";     // path where the generated files are put
    private     Element garEntEtablissement = null;
    private     Document doc = null;

    /**
     *  export Structures
     */
    public void exportStructures(final MediacentreService mediacentreService, final String path, int nbElementPerFile){
        counter = 0;
        pathExport = path;
        nbElem = nbElementPerFile;
        mediacentreService.getEtablissement(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if( event.isRight()){
                    // write the content into xml file
                    final JsonArray structures = event.right().getValue();
                    doc = fileHeader();
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // men:GAREtab
                    String lastStructureId = "";
                    Element garEtab = null;
                    for( Object obj : structures ){
                        if( obj instanceof JsonObject){
                            doc = testNumberOfOccurrences(doc);
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
                                counter += 5;
                            } else {
                                if( jObj.getString("s2.UAI") != null ) {
                                    MediacentreController.insertNode("men:GAREtablissementStrctRattachFctl", doc, garEtab, jObj.getString("s2.UAI"));
                                    counter ++;
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
                                        counter += 3;
                                        doc = testNumberOfOccurrences(doc);
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
                                                counter += 4;
                                                doc = testNumberOfOccurrences(doc);
                                            }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                Transformer transformer = transformerFactory.newTransformer();
                                                DOMSource source = new DOMSource(doc);
                                                StreamResult result = new StreamResult(new File(path + "\\" + getExportFileName("Etab", fileIndex)));
                                                transformer.transform(source, result);

                                                System.out.println("Structures saved");
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

    private Document testNumberOfOccurrences(Document doc) {
        if (nbElem <= counter) {
            // close the full file
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(pathExport + "\\" + getExportFileName("Etab", fileIndex)));

                transformer.transform(source, result);

                System.out.println("Etab" + fileIndex + " saved");
                fileIndex++;
                counter = 0;
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
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

}


