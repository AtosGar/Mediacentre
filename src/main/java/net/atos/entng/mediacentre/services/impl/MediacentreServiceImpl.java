package net.atos.entng.mediacentre.services.impl;


import fr.wseduc.webutils.Either;
import net.atos.entng.mediacentre.services.MediacentreService;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class MediacentreServiceImpl implements MediacentreService {

    private Neo4j neo4j = Neo4j.getInstance();

    @Override
    public void getUserExportData(Handler<Either<String, JsonArray>> handler) {
 /*       String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' " +
                "return u.id, u.lastName, u.displayName, u.firstName, u.structures, u.birthDate, s.UAI limit 25";*/
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "where s.UAI <> s2.UAI " +
                "return distinct u.id, u.lastName, u.displayName, u.firstName, u.structures, u.birthDate, s.UAI, s2.UAI order by u.id ";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonMef(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' " +
                "return distinct u.id, u.module, s.UAI order by u.id";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEleveEnseignement(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' " +
                "return distinct u.id, s.UAI, u.fieldOfStudy";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getTeachersExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)" +
                "where (p.name = 'Teacher')" +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure)" +
                "where s is null OR (s.UAI <> s2.UAI)" +
                "return distinct u.id, u.lastName, u.displayName, u.firstName, u.structures, u.birthDate, s.UAI, p.name, s2.UAI, u.functions order by u.id";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonMefTeacher(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Teacher' " +
                "return distinct u.id, u.modules, s.UAI order by u.id";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissement(Handler<Either<String, JsonArray>> handler) {
        String query = " MATCH (s:Structure) OPTIONAL MATCH (s2:Structure)<-[HAS_ATTACHMENT]-(s:Structure)  RETURN distinct s.UAI, s.contract, s.name, s.phone, s2.UAI order by s.UAI";
        // MATCH (s:Structure), (s2:Structure)-[HAS_ATTACHMENT]->(s3:Structure) where s.UAI = s2.UAI or s2 is null  return  s.UAI, s.contract, s.name, s.phone, s3.UAI LIMIT 25
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMef(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (n:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) RETURN distinct n.module, n.moduleName, s.UAI";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMefFromTeacher(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) RETURN distinct u.modules, s.UAI";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMatiere(Handler<Either<String, JsonArray>> handler) {
        String query = "match (sub:Subject)-[SUBJECT]->(s:Structure) return sub.label, sub.code, s.UAI order by s.UAI";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMatiereFromStudents(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) RETURN distinct u.fieldOfStudyLabels, u.fieldOfStudy, s.UAI order by s.UAI;";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getGroupsExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "match (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) where u.profiles = ['Student'] " +
        "return distinct s.UAI, s.name, c.name as cname, c.id as cid, c.externalId as cexternalId, fg.id, fg.externalId, fg.name order by s.UAI, fg.externalId, c.externalId " +
        "union " +
        "match (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) where not u.profiles = ['Student'] " +
        "return distinct s.UAI, s.name, null as cname, null as cid, null as cexternalId, fg.id, fg.externalId, fg.name order by s.UAI, fg.externalId;";

/*        String query = "match (s:Structure)<-[BELONGS]-(c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s2:Structure) " +
                " where s.id = s2.id and u.profiles = ['Student'] " +
                " return distinct s.UAI, s.name, c.name, c.id, c.externalId, fg.id, fg.externalId, fg.name order by s.UAI, fg.externalId, c.externalId";*/
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getDivisionsExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[BELONGS]-(c:Class) " +
                "return distinct s.UAI, s.name, c.name, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonGroupe(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[BELONGS]->(s:Structure) " +
                "return distinct fg.id, fg.externalId, u.id, s.UAI";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonGroupeStudent(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(c:Class)-[BELONGS]->(s:Structure) return distinct pg.id, pg.externalId, u.id, s.UAI, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }



    @Override
    public void getEnsGroupAndClassMatiere(Handler<Either<String, JsonArray>> handler) {
/*        String query = "match (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure) " +
                "return distinct u.id, collect(distinct t.groups), collect(distinct t.classes), collect( distinct sub.code), s.UAI order by u.id, s.UAI";*/
        String query = "match (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure) " +
                "return distinct u.id, t.groups, t.classes, sub.code, s.UAI order by u.id, s.UAI";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getInChargeOfExportData(String groupName, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->(n:ManualGroup)-[DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName}" +
                "                RETURN u.id, u.lastName, u.firstName, u.email, s2.UAI" +
                "                union" +
                "                match (s:Structure)<-[d1:DEPENDS]-(pg:ProfileGroup)<-[i1:IN]-(u:User)-[i2:IN]->(n:ManualGroup)-[d2:DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName}" +
                "                RETURN u.id, u.lastName, u.firstName, u.email, s2.UAI order by u.id";

        /*String query = "MATCH (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->(n:ManualGroup) " +
                "where n.name = {groupName} " +
                "RETURN u.id, u.lastName, u.firstName, u.email, s.UAI";*/
        JsonObject params = new JsonObject().putString("groupName", groupName);
        neo4j.execute(query, params, validResultHandler(handler));
    }

    @Override
    public void getUserStructures(String userId, Handler<Either<String, JsonArray>> handler){
        String query = "match (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User) " +
                "where u.id = {id} " +
                "return s.UAI as UAI, s.name as name " +
                "UNION match (s2:Structure)<-[DEPENDS]-(g:Group)<-[IN]-(u:User) " +
                "where u.id = {id} " +
                "return s2.UAI as UAI, s2.name as name;";
        JsonObject params = new JsonObject().putString("id", userId);
        neo4j.execute(query, params, validResultHandler(handler));
    }

    @Override
    public void getAllStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "match (s:Structure) return s.UAI, s.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

}
