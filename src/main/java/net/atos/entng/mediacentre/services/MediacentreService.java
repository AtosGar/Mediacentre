package net.atos.entng.mediacentre.services;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface MediacentreService {

    void getUserExportData(Handler<Either<String, JsonArray>> handler);

    void getStudentExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getTeachersExportData(Handler<Either<String, JsonArray>> handler);

    void getTeachersExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getPersonMef(Handler<Either<String, JsonArray>> handler);

    void getPersonMefStat4_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getPersonStudentClasseMefStat4_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getEleveEnseignement(Handler<Either<String, JsonArray>> handler);

    void getPersonMefTeacher(Handler<Either<String, JsonArray>> handler);

    void getPersonMefstat4Teacher_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getEtablissement(Handler<Either<String, JsonArray>> handler);

    void getEtablissement_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getEtablissementMef(Handler<Either<String, JsonArray>> handler);

    void getEtablissementMefFromTeacher(Handler<Either<String, JsonArray>> handler);

    void getEtablissementMatiere(Handler<Either<String, JsonArray>> handler);

    void getEtablissementMatiereFromStudents(Handler<Either<String, JsonArray>> handler);

    void getGroupsExportData(Handler<Either<String, JsonArray>> handler);

    void getGroupsExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getDivisionsExportData(Handler<Either<String, JsonArray>> handler);

    void getDivisionsExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getPersonGroupe(Handler<Either<String, JsonArray>> handler);

    void getPersonGroupe_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getPersonGroupeStudent(Handler<Either<String, JsonArray>> handler);

    void getPersonGroupeStudent_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler);

    void getEnsGroupAndClassMatiere(Handler<Either<String, JsonArray>> handler);

    void getInChargeOfExportData(String groupName, Handler<Either<String, JsonArray>> handler);

    void getInChargeOfExportData_1D(String uaiExportList, String groupName, Handler<Either<String, JsonArray>> handler);

    void getUserStructures(String userId, Handler<Either<String, JsonArray>> handler);

    void getAllStructures(Handler<Either<String, JsonArray>> handler);

    void getAllStructures_1D(String uaiExportList,Handler<Either<String, JsonArray>> handler);

    void getAllModules(Handler<Either<String, JsonArray>> handler);

}
