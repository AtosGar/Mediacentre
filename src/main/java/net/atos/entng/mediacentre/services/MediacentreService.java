package net.atos.entng.mediacentre.services;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface MediacentreService {

    void getUserExportData(Handler<Either<String, JsonArray>> handler);

    void getTeachersExportData(Handler<Either<String, JsonArray>> handler);

    void getPersonMef(Handler<Either<String, JsonArray>> handler);


    void getEleveEnseignement(Handler<Either<String, JsonArray>> handler);

    void getPersonMefTeacher(Handler<Either<String, JsonArray>> handler);

    void getEtablissement(Handler<Either<String, JsonArray>> handler);

    void getEtablissementMef(Handler<Either<String, JsonArray>> handler);

    void getEtablissementMefFromTeacher(Handler<Either<String, JsonArray>> handler);

    void getEtablissementMatiere(Handler<Either<String, JsonArray>> handler);

    void getEtablissementMatiereFromStudents(Handler<Either<String, JsonArray>> handler);

    void getGroupsExportData(Handler<Either<String, JsonArray>> handler);

    void getDivisionsExportData(Handler<Either<String, JsonArray>> handler);

    void getPersonGroupe(Handler<Either<String, JsonArray>> handler);

    void getEnsGroupAndClassMatiere(Handler<Either<String, JsonArray>> handler);

    void getInChargeOfExportData(String groupName, Handler<Either<String, JsonArray>> handler);

    void getUserStructures(String userId, Handler<Either<String, JsonArray>> handler);

    void getAllStructures(Handler<Either<String, JsonArray>> handler);

}
