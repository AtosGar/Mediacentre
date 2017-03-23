package net.atos.entng.mediacentre.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import net.atos.entng.mediacentre.services.MediacentreService;
import net.atos.entng.mediacentre.services.impl.MediacentreServiceImpl;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


/**
 * Vert.x backend controller for the application using Mongodb.
 */
public class MediacentreController extends BaseController {

    private static String exportFilePrefix = "";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static String fileDate = sdf.format(new Date());

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
     *  @param collection Name of the collection stored in the mongoDB database.
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
        String path = container.config().getString("export-path", "/tmp");
        int nbElementPerFile = container.config().getInteger("elementsPerFile", 10000);
        exportFilePrefix = container.config().getString("exportFilePrefix", "/tmp");
        String inChargeOfAssignementName = container.config().getString("inChargeOfAssignementGroupName", "Responsables d'affectation");

        StudentsController studentsController = new StudentsController();
        TeachersController teachersController = new TeachersController();
        StructuresController structuresController = new StructuresController();
        GroupsController groupsController = new GroupsController();
        InChargeOfAssignementController inChargeOfAssignementController = new InChargeOfAssignementController();

        studentsController.exportStudents(mediacentreService, path, nbElementPerFile);
        teachersController.exportTeachers(mediacentreService, path, nbElementPerFile);
        structuresController.exportStructures(mediacentreService, path, nbElementPerFile);
        groupsController.exportGroups(mediacentreService, path, nbElementPerFile);
        inChargeOfAssignementController.exportInChargeOfAssignement(mediacentreService, path, nbElementPerFile, inChargeOfAssignementName);

    }

    @Get("/userStructure/:userId")
    @ApiDoc("Get user main structure")
    public void getUserStructure(final HttpServerRequest request) {
        final String userId = request.params().get("userId");
        MediacentreServiceImpl mediacentreService = new MediacentreServiceImpl();
        mediacentreService.getUserStructure(userId, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if( event.isRight()){
                    JsonObject res = event.right().getValue();
                    JsonObject result = new JsonObject().putString("structure", res.getString("UAI"));
                    renderJson(request, result);
                }
            };
        });
    }

    /**
     * insert node in xml structure
     * @param elementName : name of the node
     * @param doc : document that is created
     * @param source : parent node
     * @param value : value of the node
     */
    public static void insertNode(String elementName, Document doc, Element source, String value ){
        if( value != null ) {
            Element elem = doc.createElement(elementName);
            elem.appendChild(doc.createTextNode(value));
            source.appendChild(elem);
        }
    }

    /**
     *
     * @param name : name of the type of export
     * @param fileIndex : it is a number put at the end
     * @return
     */
    public static String getExportFileName(String name, int fileIndex){
        String formattedIndex = String.format ("%04d", fileIndex);
        String fileName = exportFilePrefix + "_GAR-ENT_Complet_" + fileDate + "_" + name + "_" + formattedIndex + ".xml";
        return fileName;
    }

}
