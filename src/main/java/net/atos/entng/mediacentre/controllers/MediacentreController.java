package net.atos.entng.mediacentre.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import net.atos.entng.mediacentre.services.MediacentreService;
import net.atos.entng.mediacentre.services.impl.MediacentreServiceImpl;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Container;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Map;


/**
 * Vert.x backend controller for the application using Mongodb.
 */
public class MediacentreController extends BaseController {

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
        StudentsController studentsController = new StudentsController();
        TeachersController teachersController = new TeachersController();
        StructuresController structuresController = new StructuresController();
        GroupsController groupsController = new GroupsController();
        InChargeOfAssignementController inChargeOfAssignementController = new InChargeOfAssignementController();

        studentsController.exportStudents(mediacentreService, path);
        teachersController.exportTeachers(mediacentreService, path);
        structuresController.exportStructures(mediacentreService, path);
        groupsController.exportGroups(mediacentreService, path);
        inChargeOfAssignementController.exportInChargeOfAssignement(mediacentreService, path);

        // Export Teachers
        // Export Structures
        // Export Groups
        // Export In charge of Assignement

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

}
