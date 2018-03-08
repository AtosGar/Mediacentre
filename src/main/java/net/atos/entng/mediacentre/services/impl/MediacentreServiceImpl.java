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
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User) " +
                "where p.name = 'Student' and length(u.structures) < 2 " +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "where s is null or s.UAI <> s2.UAI " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, u.displayName, " +
                " substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id` ";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonMef(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' and length(u.structures) < 2 " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.module, 0, 254) as `u.module`, substring(s.UAI, 0, 44) as `s.UAI` order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEleveEnseignement(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' and length(u.structures) < 2 " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`, u.fieldOfStudy";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getTeachersExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)" +
                "where (p.name = 'Teacher') and length(u.structures) < 2 " +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure)" +
                "where s is null OR (s.UAI <> s2.UAI)" +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, u.displayName, substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, p.name, substring(s2.UAI, 0, 44) as `s2.UAI`, u.functions order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonMefTeacher(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User) " + //-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Teacher' and length(u.structures) < 2 " +
                "return distinct substring(u.id, 0, 63) as `u.id`, u.modules order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissement(Handler<Either<String, JsonArray>> handler) {
        String query = " MATCH (s:Structure) OPTIONAL MATCH (s2:Structure)<-[HAS_ATTACHMENT]-(s:Structure)  RETURN " +
                "distinct substring(s.UAI, 0, 44) as `s.UAI`,  substring(s.contract, 0, 44) as `s.contract`, substring(s.name, 0, 499) as `s.name`, " +
                " substring(s.phone, 0, 44) as `s.phone`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `s.UAI`";
        // MATCH (s:Structure), (s2:Structure)-[HAS_ATTACHMENT]->(s3:Structure) where s.UAI = s2.UAI or s2 is null  return  s.UAI, s.contract, s.name, s.phone, s3.UAI LIMIT 25
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMef(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (n:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) where length(n.structures) < 2 RETURN distinct n.module, n.moduleName, " +
                "substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMefFromTeacher(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) where length(u.structures) < 2 RETURN distinct u.modules, " +
                "substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMatiere(Handler<Either<String, JsonArray>> handler) {
        String query = "match (sub:Subject)-[SUBJECT]->(s:Structure) return sub.label, sub.code, substring(s.UAI, 0, 44) as `s.UAI` order by `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getEtablissementMatiereFromStudents(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) where length(u.structures) < 2 RETURN distinct u.fieldOfStudyLabels, u.fieldOfStudy, substring(s.UAI, 0, 44) as `s.UAI` order by `s.UAI`;";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getGroupsExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "match (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) where u.profiles = ['Student'] and length(u.structures) < 2 " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, c.name as cname, c.id as cid, c.externalId as cexternalId, substring(fg.id, 0, 254) as `fg.id`, fg.externalId, fg.name " +
                "order by `s.UAI`, fg.externalId, c.externalId " +
                "union " +
                "match (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) where not u.profiles = ['Student'] and length(u.structures) < 2 " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, null as cname, null as cid, null as cexternalId, substring(fg.id, 0, 254) as `fg.id`, fg.externalId, fg.name " +
                "order by `s.UAI`, fg.externalId;";

/*        String query = "match (s:Structure)<-[BELONGS]-(c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s2:Structure) " +
                " where s.id = s2.id and u.profiles = ['Student'] " +
                " return distinct s.UAI, s.name, c.name, c.id, c.externalId, fg.id, fg.externalId, fg.name order by s.UAI, fg.externalId, c.externalId";*/
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getDivisionsExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[BELONGS]-(c:Class) " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, substring(c.name, 0, 254) as `c.name`, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonGroupe(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[BELONGS]->(s:Structure) where length(u.structures) < 2 " +
                "return distinct substring(fg.id, 0, 254) as `fg.id`, fg.externalId, substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getPersonGroupeStudent(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(c:Class)-[BELONGS]->(s:Structure) where length(u.structures) < 2 " +
                "return distinct pg.id, pg.externalId, substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }



    @Override
    public void getEnsGroupAndClassMatiere(Handler<Either<String, JsonArray>> handler) {
/*        String query = "match (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure) " +
                "return distinct u.id, collect(distinct t.groups), collect(distinct t.classes), collect( distinct sub.code), s.UAI order by u.id, s.UAI";*/
        String query = "match (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure) where length(u.structures) < 2 " +
                "return distinct u.id, t.groups, t.classes, sub.code,substring(s.UAI, 0, 44) as `s.UAI` order by u.id, `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getInChargeOfExportData(String groupName, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->(n:ManualGroup)-[DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} and length(u.structures) < 2 " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI`" +
                "                union" +
                "                match (s:Structure)<-[d1:DEPENDS]-(pg:ProfileGroup)<-[i1:IN]-(u:User)-[i2:IN]->(n:ManualGroup)-[d2:DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} and length(u.structures) < 2 " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id`";

        /*String query = "MATCH (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->(n:ManualGroup) " +
                "where n.name = {groupName} " +
                "RETURN u.id, u.lastName, u.firstName, u.email, s.UAI";*/
        JsonObject params = new JsonObject().putString("groupName", groupName);
        neo4j.execute(query, params, validResultHandler(handler));
    }

    @Override
    public void getUserStructures(String userId, Handler<Either<String, JsonArray>> handler){
        String query = "match (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User) " +
                "where u.id = {id} and length(u.structures) < 2 " +
                "return substring(s.UAI, 0, 44) as `s.UAI`, s.name as name " +
                "UNION match (s2:Structure)<-[DEPENDS]-(g:Group)<-[IN]-(u:User) " +
                "where u.id = {id} and length(u.structures) < 2 " +
                "return substring(s2.UAI, 0, 44) as `s.UAI`, s2.name as name;";
        JsonObject params = new JsonObject().putString("id", userId);
        neo4j.execute(query, params, validResultHandler(handler));
    }

    @Override
    public void getAllStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "match (s:Structure) return substring(s.UAI, 0, 44) as `s.UAI`, s.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    @Override
    public void getAllModules(Handler<Either<String, JsonArray>> handler) {
        String query = "match (m:Module) return m.attachment, m.externalId, substring(m.stat, 0, 254) as `m.stat`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


}
