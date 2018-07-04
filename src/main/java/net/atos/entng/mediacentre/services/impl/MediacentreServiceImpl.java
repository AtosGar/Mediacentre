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
    public void getUserStructures(String userId, Handler<Either<String, JsonArray>> handler){
        String query = "match (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User) " +
                "where u.id = {id} " +
                "return substring(s.UAI, 0, 44) as UAI, s.name as name " +
                "UNION match (s2:Structure)<-[DEPENDS]-(g:Group)<-[IN]-(u:User) " +
                "where u.id = {id} " +
                "return substring(s2.UAI, 0, 44) as UAI, s2.name as name;";
        JsonObject params = new JsonObject().putString("id", userId);
        neo4j.execute(query, params, validResultHandler(handler));
    }



    // -------------------------------------
    // Commun
    // -------------------------------------

    /**
     * getAllStructures
     * @param handler
     */
    @Override
    public void getAllStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "match (s:Structure) return substring(s.UAI, 0, 44) as `s.UAI`, s.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getAllModules (only used for 2D)
     * @param handler
     */
    @Override
    public void getAllModules(Handler<Either<String, JsonArray>> handler) {
        String query = "match (m:Module) return m.attachment, m.externalId, substring(m.stat, 0, 254) as `m.stat`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    // -------------------------------------
    // GAR-ENT-Groupe export methods 1D
    // -------------------------------------

    /**
     * getDivisionsExportData_1D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getDivisionsExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[BELONGS]-(c:Class) WHERE s.UAI in " + uaiExportList + " " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, substring(c.name, 0, 254) as `c.name`, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     *
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getGroupsExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "match (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[COMMUNIQUE]->(s:Structure) where u.profiles = ['Student'] and " +
                "u.source = 'AAF1D' and s.UAI in " + uaiExportList + " " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, c.name as cname, c.id as cid, c.externalId as cexternalId " +
                "order by `s.UAI`, c.externalId " +
                "union " +
                "match (u:User)-[COMMUNIQUE]->(s:Structure) where not u.profiles = ['Student'] and " +
                " u.source = 'AAF1D' and s.UAI in " + uaiExportList + " " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, null as cname, null as cid, null as cexternalId  " +
                "order by `s.UAI`;";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     *
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getPersonGroupeStudent_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)  " +
                " WHERE u.source = 'AAF1D' and s.UAI in " + uaiExportList + " " +
                " AND exists(c.externalId) RETURN u.id, c.externalId, s.UAI";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // -------------------------------------
    // GAR-ENT-Groupe export methods 2D
    // -------------------------------------

    // OLD 2D
    @Override
    public void getDivisionsExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[BELONGS]-(c:Class) " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, substring(c.name, 0, 254) as `c.name`, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getGroupsExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "match (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) where u.profiles = ['Student'] " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, c.name as cname, c.id as cid, c.externalId as cexternalId, substring(fg.id, 0, 254) as `fg.id`, fg.externalId, fg.name " +
                "order by `s.UAI`, fg.externalId, c.externalId " +
                "union " +
                "match (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) where not u.profiles = ['Student'] " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, null as cname, null as cid, null as cexternalId, substring(fg.id, 0, 254) as `fg.id`, fg.externalId, fg.name " +
                "order by `s.UAI`, fg.externalId;";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getPersonGroupe(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[BELONGS]->(s:Structure) " +
                "return distinct substring(fg.id, 0, 254) as `fg.id`, fg.externalId, substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getPersonGroupeStudent(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(c:Class)-[BELONGS]->(s:Structure) " +
                "return distinct pg.id, pg.externalId, substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getEnsGroupAndClassMatiere(Handler<Either<String, JsonArray>> handler) {
/*        String query = "match (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure) " +
                "return distinct u.id, collect(distinct t.groups), collect(distinct t.classes), collect( distinct sub.code), s.UAI order by u.id, s.UAI";*/
        String query = "match (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure) " +
                "return distinct u.id, t.groups, t.classes, sub.code,substring(s.UAI, 0, 44) as `s.UAI` order by u.id, `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    /**
     * getDivisionsExportData_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getDivisionsExportData_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[BELONGS]-(c:Class) WHERE s.UAI in " + uaiExportList + " " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, substring(c.name, 0, 254) as `c.name`, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getGroupsExportData_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getGroupsExportData_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "mAtCh (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) "+
                "where u.profiles = ['Student'] AND u.source = 'AAF' and s.UAI in " + uaiExportList + " " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, c.name as cname, c.id as cid, c.externalId as cexternalId, substring(fg.id, 0, 254) as `fg.id`, fg.externalId, fg.name " +
                "order by `s.UAI`, fg.externalId, c.externalId " +
                "union " +
                "match (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->(s:Structure) where not u.profiles = ['Student'] " +
                "return distinct substring(s.UAI, 0, 44) as `s.UAI`, s.name, null as cname, null as cid, null as cexternalId, substring(fg.id, 0, 254) as `fg.id`, fg.externalId, fg.name " +
                "order by `s.UAI`, fg.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getPersonGroupe_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getPersonGroupe_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[BELONGS]->(s:Structure) " +
                "where u.source = 'AAF' and s.UAI in " + uaiExportList + " " +
                "return distinct substring(fg.id, 0, 254) as `fg.id`, fg.externalId, substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    /**
     * getPersonGroupeStudent_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getPersonGroupeStudent_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(c:Class)-[BELONGS]->(s:Structure) " +
                "where u.source = 'AAF' and s.UAI in " + uaiExportList + " " +
                "return distinct pg.id, pg.externalId, substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`, c.id, c.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getEnsGroupAndClassMatiere_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getEnsGroupAndClassMatiere_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
       String query = "match (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure) " +
               "where u.source = 'AAF' and s.UAI in " + uaiExportList + " " +
                "return distinct u.id, t.groups, t.classes, sub.code,substring(s.UAI, 0, 44) as `s.UAI` order by u.id, `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    // -------------------------------------
    // GAR-ENT-Eleve export methods 1D
    // -------------------------------------

    /**
     *
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getStudentExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "WHERE p.name = 'Student'  AND u.source = 'AAF1D' AND s.UAI in " + uaiExportList +" " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "WHERE  s.UAI <> s2.UAI " +
                "RETURN distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, u.displayName, " +
                "substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id` ";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     *
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getPersonMefStat4_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student'  AND u.source = 'AAF1D'  AND s.UAI in " + uaiExportList +" " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.level, 0, 4) as `u.level`, substring(s.UAI, 0, 44) as `s.UAI` order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // -------------------------------------
    // GAR-ENT-Eleve export methods 2D
    // -------------------------------------

    // OLD 2D
    @Override
    public void getUserExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User) " +
                "where p.name = 'Student' " +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "where s is null or s.UAI <> s2.UAI " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, u.displayName, " +
                " substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id` ";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getPersonMef(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.module, 0, 254) as `u.module`, substring(s.UAI, 0, 44) as `s.UAI` order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getEleveEnseignement(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student' " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`, u.fieldOfStudy";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    /**
     * getStudentExportData_2D
     * getUserExportData
     * @param handler
     */
    @Override
    public void getStudentExportData_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student'   AND u.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "where s.UAI <> s2.UAI " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, u.displayName, " +
                " substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id` ";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getPersonMef_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getPersonMef_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student'    AND u.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.module, 0, 254) as `u.module`, substring(s.UAI, 0, 44) as `s.UAI` order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getEleveEnseignement_2D
     * @param handler
     */
    @Override
    public void getEleveEnseignement_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Student'  AND u.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(s.UAI, 0, 44) as `s.UAI`, u.fieldOfStudy";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // -------------------------------------
    // GAR-ENT-Enseignant export methods 1D
    // -------------------------------------

    /**
     *
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getClasseMefStat4_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)  " +
                "WHERE  str(u.profiles) CONTAINS 'Student' AND exists(c.externalId) " +
                "AND s.UAI in " + uaiExportList + "  RETURN distinct c.externalId,  substring(u.level, 0, 4) as mefstat4Code";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     *
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getTeacherClasse_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)  " +
                " WHERE str(u.profiles) CONTAINS 'Teacher'  AND u.source = 'AAF1D' and s.UAI in " + uaiExportList + " " +
                " AND exists(c.externalId) RETURN u.id, c.externalId, s.UAI";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }




    // -------------------------------------
    // GAR-ENT-Enseignant export methods 2D
    // -------------------------------------

    // OLD 2D
    @Override
    public void getTeachersExportData(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User) " +
                "where (p.name = 'Teacher') " +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "where s is null OR (s.UAI <> s2.UAI)" +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, u.displayName, substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, p.name, substring(s2.UAI, 0, 44) as `s2.UAI`, u.functions order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getPersonMefTeacher(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User) " + //-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where p.name = 'Teacher' " +
                "return distinct substring(u.id, 0, 63) as `u.id`, u.modules order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getTeachersExportData_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getTeachersExportData_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where (p.name = 'Teacher') AND u.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "where (s.UAI <> s2.UAI)" +
                "return distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, u.displayName, substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, p.name, substring(s2.UAI, 0, 44) as `s2.UAI`, u.functions order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getPersonMefTeacher_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getPersonMefTeacher_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User) -[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "where p.name = 'Teacher'  AND u.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "return distinct substring(u.id, 0, 63) as `u.id`, u.modules order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }



    // -------------------------------------
    // GAR-ENT-Etab export methods 1D
    // -------------------------------------

    /**
     * getAllStructures_1D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getAllStructures_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "match (s:Structure) "+
                "WHERE s.UAI in " + uaiExportList + " " +
                "return substring(s.UAI, 0, 44) as `s.UAI`, s.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    /**
     * getEtablissement_1D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getEtablissement_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI in " + uaiExportList +
                " OPTIONAL MATCH (s2:Structure)<-[HAS_ATTACHMENT]-(s:Structure) " +
                "RETURN distinct substring(s.UAI, 0, 44) as `s.UAI`,  substring(s.contract, 0, 44) as `s.contract`, substring(s.name, 0, 499) as `s.name`, " +
                " substring(s.phone, 0, 44) as `s.phone`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    // -------------------------------------
    // GAR-ENT-Etab export methods 2D
    // -------------------------------------

    // OLD 2D
    @Override
    public void getEtablissement(Handler<Either<String, JsonArray>> handler) {
        // the link with group and user is there to ensure we don't export structures without users in there.
        String query = "MATCH (s:Structure) OPTIONAL MATCH (s2:Structure)<-[HAS_ATTACHMENT]-(s:Structure)  RETURN " +
                "distinct substring(s.UAI, 0, 44) as `s.UAI`,  substring(s.contract, 0, 44) as `s.contract`, substring(s.name, 0, 499) as `s.name`, " +
                " substring(s.phone, 0, 44) as `s.phone`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `s.UAI`";
        // MATCH (s:Structure), (s2:Structure)-[HAS_ATTACHMENT]->(s3:Structure) where s.UAI = s2.UAI or s2 is null  return  s.UAI, s.contract, s.name, s.phone, s3.UAI LIMIT 25
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getEtablissementMef(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (n:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) RETURN distinct n.module, n.moduleName, " +
                "substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getEtablissementMefFromTeacher(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) RETURN distinct u.modules, " +
                "substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getEtablissementMatiere(Handler<Either<String, JsonArray>> handler) {
        String query = "match (sub:Subject)-[SUBJECT]->(s:Structure) "
                +" return sub.label, sub.code, substring(s.UAI, 0, 44) as `s.UAI` order by `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // OLD 2D
    @Override
    public void getEtablissementMatiereFromStudents(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) "+
                "RETURN distinct u.fieldOfStudyLabels, u.fieldOfStudy, substring(s.UAI, 0, 44) as `s.UAI` order by `s.UAI`;";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }


    /**
     * getAllStructures_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getAllStructures_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "match (s:Structure) "+
                "WHERE s.UAI in " + uaiExportList + " " +
                "return substring(s.UAI, 0, 44) as `s.UAI`, s.externalId";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getEtablissement_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getEtablissement_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        // the link with group and user is there to ensure we don't export structures without users in there.
        String query = "MATCH (s:Structure) WHERE s.UAI in " + uaiExportList +
                " OPTIONAL MATCH (s2:Structure)<-[HAS_ATTACHMENT]-(s:Structure) " +
                "RETURN distinct substring(s.UAI, 0, 44) as `s.UAI`,  substring(s.contract, 0, 44) as `s.contract`, substring(s.name, 0, 499) as `s.name`, " +
                " substring(s.phone, 0, 44) as `s.phone`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `s.UAI`";
        // MATCH (s:Structure), (s2:Structure)-[HAS_ATTACHMENT]->(s3:Structure) where s.UAI = s2.UAI or s2 is null  return  s.UAI, s.contract, s.name, s.phone, s3.UAI LIMIT 25
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getEtablissementMef_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getEtablissementMef_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (n:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "where n.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "RETURN distinct n.module, n.moduleName, " +
                "substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getEtablissementMefFromTeacher_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getEtablissementMefFromTeacher_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "WHERE u.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "RETURN distinct u.modules, substring(s.UAI, 0, 44) as `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getEtablissementMatiere_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getEtablissementMatiere_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "match (sub:Subject)-[SUBJECT]->(s:Structure) " +
                "WHERE s.UAI in " + uaiExportList +" " +
                "RETURN sub.label, sub.code, substring(s.UAI, 0, 44) as `s.UAI` order by `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    /**
     * getEtablissementMatiereFromStudents_2D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getEtablissementMatiereFromStudents_2D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) "+
                "WHERE u.source = 'AAF' AND s.UAI in " + uaiExportList +" " +
                "RETURN distinct u.fieldOfStudyLabels, u.fieldOfStudy, substring(s.UAI, 0, 44) as `s.UAI` order by `s.UAI`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    // ---------------------
    // GAR-ENT-RespAff 1D
    // ---------------------
    /**
     * GAR-ENT-RespAff Export methods 1D
     * @param uaiExportList
     * @param handler
     */
    @Override
    public void getTeachersExportData_1D(String uaiExportList, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure) " +
                "WHERE (p.name = 'Teacher') AND u.source = 'AAF1D' AND s.UAI in " + uaiExportList + " " +
                "OPTIONAL MATCH (pg:ProfileGroup)-[DEPENDS]->(s2:Structure) " +
                "WHERE s.UAI <> s2.UAI " +
                "RETURN distinct substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, " +
                "u.displayName, substring(u.firstName, 0, 499) as `u.firstName`, u.structures, u.birthDate, " +
                "substring(s.UAI, 0, 44) as `s.UAI`, p.name, substring(s2.UAI, 0, 44) as `s2.UAI`, u.functions order by `u.id`";
        neo4j.execute(query, new JsonObject(), validResultHandler(handler));
    }

    @Override
    public void getInChargeOfExportData_1D(String uaiExportList, String groupName, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->(n:ManualGroup)-[DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} and u.source = 'AAF1D' and s.UAI in " + uaiExportList + " " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI`" +
                "                union" +
                "                match (s:Structure)<-[d1:DEPENDS]-(pg:ProfileGroup)<-[i1:IN]-(u:User)-[i2:IN]->(n:ManualGroup)-[d2:DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} and u.source = 'AAF1D' and s.UAI in " + uaiExportList + " " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id`";
        JsonObject params = new JsonObject().putString("groupName", groupName);
        neo4j.execute(query, params, validResultHandler(handler));
    }


    // ---------------------
    // GAR-ENT-RespAff 2D
    // ---------------------

    /**
     * getInChargeOfExportData (OLD 2D)
     * @param groupName
     * @param handler
     */
    @Override
    public void getInChargeOfExportData(String groupName, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->(n:ManualGroup)-[DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI`" +
                "                union" +
                "                match (s:Structure)<-[d1:DEPENDS]-(pg:ProfileGroup)<-[i1:IN]-(u:User)-[i2:IN]->(n:ManualGroup)-[d2:DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id`";
        JsonObject params = new JsonObject().putString("groupName", groupName);
        neo4j.execute(query, params, validResultHandler(handler));
    }

    /**
     * GAR-ENT-RespAff Export methods 2D
     * @param uaiExportList
     * @param groupName
     * @param handler
     */
    @Override
    public void getInChargeOfExportData_2D(String uaiExportList, String groupName, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->(n:ManualGroup)-[DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} and u.source = 'AAF' and s.UAI in " + uaiExportList + " " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI`" +
                "                union" +
                "                match (s:Structure)<-[d1:DEPENDS]-(pg:ProfileGroup)<-[i1:IN]-(u:User)-[i2:IN]->(n:ManualGroup)-[d2:DEPENDS]->(s2:Structure)" +
                "                where n.name = {groupName} and u.source = 'AAF' and s.UAI in " + uaiExportList + " " +
                "                RETURN substring(u.id, 0, 63) as `u.id`, substring(u.lastName, 0, 499) as `u.lastName`, substring(u.firstName, 0, 499) as `u.firstName`, " +
                "                substring(u.email, 0, 254) as `u.email`, substring(s2.UAI, 0, 44) as `s2.UAI` order by `u.id`";
        JsonObject params = new JsonObject().putString("groupName", groupName);
        neo4j.execute(query, params, validResultHandler(handler));
    }

}
