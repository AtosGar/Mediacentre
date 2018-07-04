package net.atos.entng.mediacentre.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import net.atos.entng.mediacentre.services.MediacentreService;
import net.atos.entng.mediacentre.services.impl.MediacentreServiceImpl;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



/**
 * Vert.x backend controller for the application using Mongodb.
 */
public class MediacentreController extends BaseController {

    private static String exportFilePrefix = "";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static String fileDate = sdf.format(new Date());

    private final String PROJECT_TYPE_MIXTE_VALUE = "MIXTE";
    private final String PROJECT_TYPE_1D_VALUE = "1D";
    private final String PROJECT_TYPE_2D_VALUE = "2D";

    private final String UAI_1D_LIST_FILE = "ListeUAIExportGAR_1D.txt";
    private final String UAI_2D_LIST_FILE = "ListeUAIExportGAR_2D.txt";

    /**
     * Computation service
     */
    private final MediacentreService mediacentreService;

    /**
     * Permissions
     */
    private static final String
            /**
             * Join room
             */
            read_only = "mediacentre.view",
    /**
     * Create room
     */
    modify = "mediacentre.create",
    /**
     * Manage room
     */
    manage_ressource = "mediacentre.manager";

    /**
     * Creates a new controller.
     *
     * @param collection Name of the collection stored in the mongoDB database.
     */
    public MediacentreController(String collection) {
        super();
        mediacentreService = new MediacentreServiceImpl();
    }

    @Override
    public void init(Vertx vertx, Container container, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, container, rm, securedActions);
    }

    /**
     * Displays the home view.
     *
     * @param request Client request
     */
    @Get("")
    @SecuredAction(value = read_only, type = ActionType.WORKFLOW)
    public void view(HttpServerRequest request) {
        renderView(request);
    }


    @Get("/exportXML")
    @ApiDoc("Export XML")
    /*    @SecuredAction("mediacentre.exportXML")*/
    public void exportXML(final HttpServerRequest request) {
       /* mediacentreService.getAllModules(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray allModules = event.right().getValue();
                    Map<String, JsonObject> mapModules = new HashMap<>();
                    for (Object obj : allModules) {
                        if (obj instanceof JsonObject) {
                            JsonObject jObj = (JsonObject) obj;
                            jObj.putString("m.attachment", jObj.getString("m.attachment"));
                            jObj.putString("m.stat", jObj.getString("m.stat"));
                            mapModules.put(jObj.getString("m.externalId"), jObj);
                        }
                    }*/

        final String path = container.config().getString("export-path", "/tmp");
        final int nbElementPerFile = container.config().getInteger("elementsPerFile", 10000);
        exportFilePrefix = container.config().getString("exportFilePrefix", "/tmp");
        String inChargeOfAssignementName = container.config().getString("inChargeOfAssignementGroupName", "Responsables d'affectation");

        String uaiList1DPath = container.config().getString("uai-2D-list-path");

        final String exportUAIList2D = getExportUAIListFromFile(uaiList1DPath);

        final StudentsController studentsController = new StudentsController();
        StructuresController structuresController = new StructuresController();
        final TeachersController teachersController = new TeachersController();
        final GroupsController groupsController = new GroupsController();
        InChargeOfAssignementController inChargeOfAssignementController = new InChargeOfAssignementController();
        fileDate = sdf.format(new Date());
        structuresController.exportStructures(mediacentreService, path, nbElementPerFile);
        teachersController.exportTeachers(mediacentreService, path, nbElementPerFile, new Handler<List<String>>() {
            @Override
            public void handle(final List<String> bannedUsers) {
                studentsController.exportStudents(mediacentreService, path, nbElementPerFile, new Handler<List<String>>() {
                    @Override
                    public void handle(final List<String> bannedUsersStudents) {
                        bannedUsers.addAll(bannedUsersStudents);
                        groupsController.exportGroups(mediacentreService, path, nbElementPerFile, bannedUsers);
                    }
                });
            }
        });

        final String emailDefault = container.config().getString("emailDefault", "noreply@noreply.fr");
        inChargeOfAssignementController.exportInChargeOfAssignement(mediacentreService, path, nbElementPerFile, inChargeOfAssignementName, emailDefault);
         /*       }
            }
        });*/
    }

    /**
     * Juin 2018
     * fonction d'export des fichiers XML GAR pour le premier degré
     * @param request
     */
    @Get("/exportXML_1D")
    @ApiDoc("Export XML_1D")
    public void exportXML_1D(final HttpServerRequest request) {

        final String path = container.config().getString("export-path", "/tmp") + "/1D/";

        final int nbElementPerFile = container.config().getInteger("elementsPerFile", 10000);

        final String emailDefault = container.config().getString("emailDefault", "noreply@noreply.fr");

        exportFilePrefix = container.config().getString("exportFilePrefix", "/tmp");

        String uaiList1DPath = path + UAI_1D_LIST_FILE;

        System.out.println(" PATH 1D : "+uaiList1DPath);

        final String exportUAIList1D = getExportUAIListFromFile(uaiList1DPath);

        String inChargeOfAssignementName = container.config().getString("inChargeOfAssignementGroupName", "Responsables d'affectation");

        final StudentsController studentsController = new StudentsController();

        StructuresController structuresController = new StructuresController();

        final TeachersController teachersController = new TeachersController();

        final GroupsController groupsController = new GroupsController();

        InChargeOfAssignementController inChargeOfAssignementController = new InChargeOfAssignementController();

        fileDate = sdf.format(new Date());

        /**
         * Begin export
         */
        // GAR-ENT-Etab
        structuresController.exportStructures_1D(mediacentreService, path, nbElementPerFile, exportUAIList1D);

        // GAR-ENT-Groupe
        groupsController.exportGroups_1D(mediacentreService, path, nbElementPerFile, exportUAIList1D);

        // GAR-ENT-RespAff
        inChargeOfAssignementController.exportInChargeOfAssignement_1D(mediacentreService, path, nbElementPerFile,
                inChargeOfAssignementName, emailDefault, exportUAIList1D);

        //GAR-ENT-Enseignant
        teachersController.exportTeachers_1D(mediacentreService, path, nbElementPerFile, exportUAIList1D);

        // GAR-ENT-Eleve
        studentsController.exportStudents_1D(mediacentreService, path, nbElementPerFile, exportUAIList1D);

    }

    /**
     * Juin 2018
     * fonction d'export des fichiers XML GAR pour le premier degré
     * @param request
     */
    @Get("/exportXML_2D")
    @ApiDoc("Export XML_2D")
    public void exportXML_2D(final HttpServerRequest request) {

        final String path = container.config().getString("export-path", "/tmp") + "/2D/";

        final int nbElementPerFile = container.config().getInteger("elementsPerFile", 10000);

        final String emailDefault = container.config().getString("emailDefault", "noreply@noreply.fr");

        exportFilePrefix = container.config().getString("exportFilePrefix", "/tmp");

        String uaiList2DPath = path + UAI_2D_LIST_FILE;

        System.out.println(" PATH 2D : "+uaiList2DPath);

        final String exportUAIList2D = getExportUAIListFromFile(uaiList2DPath);

        String inChargeOfAssignementName = container.config().getString("inChargeOfAssignementGroupName", "Responsables d'affectation");

        final StudentsController studentsController = new StudentsController();

        StructuresController structuresController = new StructuresController();

        final TeachersController teachersController = new TeachersController();

        final GroupsController groupsController = new GroupsController();

        InChargeOfAssignementController inChargeOfAssignementController = new InChargeOfAssignementController();

        fileDate = sdf.format(new Date());

        /**
         * Begin export
         */
        // GAR-ENT-Etab
        structuresController.exportStructures_2D(mediacentreService, path, nbElementPerFile, exportUAIList2D);

        // GAR-ENT-Groupe
        groupsController.exportGroups_2D(mediacentreService, path, nbElementPerFile, exportUAIList2D);

        // GAR-ENT-RespAff
        inChargeOfAssignementController.exportInChargeOfAssignement_2D(mediacentreService, path, nbElementPerFile,
                inChargeOfAssignementName, emailDefault, exportUAIList2D);

        //GAR-ENT-Enseignant
        teachersController.exportTeachers_2D(mediacentreService, path, nbElementPerFile, exportUAIList2D);

        // GAR-ENT-Eleve
        studentsController.exportStudents_2D(mediacentreService, path, nbElementPerFile, exportUAIList2D);

    }




    @Get("/isExportButtonVisible")
    @ApiDoc("Returns true if user is authorized to display export button")
    public void isExportButtonVisible(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final String userExportXML = container.config().getString("userExportXML", "admin.xmlgar");
                    final String userLogin = user.getLogin();
                    JsonObject obj = new JsonObject();
                    obj.putBoolean("isAuthorized", userLogin.equals(userExportXML));
                    renderJson(request, obj);
                }
            }
        });
    }

    @Get("/isExportMixte")
    @ApiDoc("Returns true if user is export mixte")
    public void isExportMixte(final HttpServerRequest request) {
        final String projectType = container.config().getString("ProjectType");

        JsonObject obj = new JsonObject();
        obj.putBoolean("isExportMixte", projectType.equals(PROJECT_TYPE_MIXTE_VALUE));

        renderJson(request, obj);
    }

    @Get("/isExport1D")
    @ApiDoc("Returns true if user is export 1D")
    public void isExport1D(final HttpServerRequest request) {
        final String projectType = container.config().getString("ProjectType");

        JsonObject obj = new JsonObject();
        obj.putBoolean("isExport1D", projectType.equals(PROJECT_TYPE_1D_VALUE));

        renderJson(request, obj);
    }

    @Get("/isExport2D")
    @ApiDoc("Returns true if user is export 2D")
    public void isExport2D(final HttpServerRequest request) {
        final String projectType = container.config().getString("ProjectType");

        JsonObject obj = new JsonObject();
        obj.putBoolean("isExport2D", projectType.equals(PROJECT_TYPE_2D_VALUE));

        renderJson(request, obj);
    }


    @Get("/getRessources/:structureUAI")
    @ApiDoc("Get user main structure")
    public void getRessources(final HttpServerRequest request) {
        final String uai = request.params().get("structureUAI");
        final String ident = container.config().getString("idEnt", "");
        MediacentreServiceImpl mediacentreService = new MediacentreServiceImpl();
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final String userId = user.getUserId();
                    MediacentreServiceImpl mediacentreService = new MediacentreServiceImpl();
                    final String hostInfo = container.config().getString("hostWS", "");
                    final String header = container.config().getString("headerWSX-Id-Ent", "");
                    HttpClient httpClient = vertx.createHttpClient().setHost(hostInfo).setPort(80);
                    String uri = "/ressources/" + ident + "/" + uai + "/" + userId;
                    //uri = "/ressources/ENTTEST1/0650499P-ET6/5577102-ET6";
                    httpClient.get(uri, new Handler<HttpClientResponse>() {
                        @Override
                        public void handle(HttpClientResponse httpClientResponse) {
                            System.out.println("Response received");
                            httpClientResponse.bodyHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer buffer) {
                                    String sBuffer = buffer.getString(0, buffer.length());
                                    JsonObject obj = new JsonObject(sBuffer);
                                    renderJson(request, obj);
                                }
                            });
                        }
                    }).putHeader("X-Id-Ent-Autorisation", header).putHeader("Accept", "application/json").end();
                }
            }
        });
    }


    @Get("/getUserStructures/:userId")
    @ApiDoc("Get user main structure")
    public void getUserStructures(final HttpServerRequest request) {
        final String userId = request.params().get("userId");
        mediacentreService.getUserStructures(userId, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray res = event.right().getValue();
                    JsonObject result = new JsonObject().putArray("structures", res);
                    renderJson(request, result);
                }
            }
        });
    }


    public static String customSubString(String str, int size) {
        if (str.length() > size) {
            str = str.substring(0, size);
        }
        return str;
    }

    /**
     * insert node in xml structure
     *
     * @param elementName : name of the node
     * @param doc         : document that is created
     * @param source      : parent node
     * @param value       : value of the node
     */
    public static void insertNode(String elementName, Document doc, Element source, String value) {
        if (value != null) {
            Element elem = doc.createElement(elementName);
            elem.appendChild(doc.createTextNode(value));
            source.appendChild(elem);
        }
    }

    /**
     * @param name      : name of the type of export
     * @param fileIndex : it is a number put at the end
     * @return
     */
    public static String getExportFileName(String name, int fileIndex) {
        String formattedIndex = String.format("%04d", fileIndex);
        String fileName = exportFilePrefix + "_GAR-ENT_Complet_" + fileDate + "_" + name + "_" + formattedIndex + ".xml";
        return fileName;
    }

    public static boolean isFileValid(String filePath) throws IOException, SAXException {
        //URL schemaFile = new URL("http://data.education.fr/ns/gar GAR-ENT.xsd");
/*        File schemaFile = new File("c:\\exportXML\\GAR-ENT.xsd");
        Source xmlFile = new StreamSource(new File(filePath));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        try {
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
            return true;
        } catch (SAXException e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid reason:" + e);
            return false;
        }*/
        return true;
    }

    public String getExportUAIListFromFile(String pathFile){
        ArrayList<String> uaiList = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(pathFile)))
        {
            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                uaiList.add("'" + sCurrentLine + "'");
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        return uaiList.toString();
    }



}
