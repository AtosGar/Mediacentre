function MediacentreController($scope, template, model, route, $location) {

    this.initialize = function() {
        //$scope.ressources = {"listeRessources":{"ressource":[{"idRessource":"http://n2t.net/ark:/99999/r1xxxxxxxx","idType":"ARK","nomRessource":"R01_MULTI_TOUT - Manuel numérique élève (100% numérique) - Multisupports (tablettes + PC/Mac)","idEditeur":"378901946_0000000000000000","nomEditeur":"Worldline","urlVignette":"https://vignette.validation.test-gar.education.fr/VAtest1/gar/153.png","typePresentation":{"code":"MAN","nom":"manuels numériques"},"typePedagogique":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-014","nom":"manuel d\u0027enseignement"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-006","nom":"étude de cas"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-027","nom":"activité pédagogique"},{"uri":"http://data.education.fr/voc/scolomfr/concept/lecture","nom":"cours / présentation"}],"typologieDocument":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024","nom":"livre numérique"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-039","nom":"site Web"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-034","nom":"présentation multimédia"}],"niveauEducatif":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-022-num-018","nom":"6e"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-022-num-620","nom":"cycle 3 (2016)"}],"domaineEnseignement":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1004","nom":"habiter un espace de faible densité (géographie 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1003","nom":"habiter une métropole (géographie 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1002","nom":"géographie (6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2571","nom":"littoral industrialo-portuaire (géographie 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2572","nom":"littoral touristique (géographie 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2573","nom":"la répartition de la population mondiale et ses dynamiques (géographie 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2567","nom":"les métropoles et leurs habitants (géographie 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2569","nom":"habiter un espace à forte(s) contrainte(s) naturelle(s) ou/et de grande biodiversité (géographie 6e)"}],"urlAccesRessource":"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\u003dRU5UVEVTVDE\u003d\u0026idEtab\u003dMDY1MDQ5OVAtRVQ2\u0026idSrc\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMXh4eHh4eHh4","nomSourceEtiquetteGar":"Accessible via le Gestionnaire d’accès aux ressources (GAR)","distributeurTech":"378901946_0000000000000000","validateurTech":"378901946_0000000000000000"},{"idRessource":"http://n2t.net/ark:/99999/r11xxxxxxxx","idType":"ARK","nomRessource":"R11_MULTI_STDS - Manuel numérique élève (100% numérique) - Multisupports (tablettes + PC/Mac)","idEditeur":"378901946_0000000000000000","nomEditeur":"Worldline","urlVignette":"https://vignette.validation.test-gar.education.fr/VAtest1/gar/135.png","typePresentation":{"code":"DIC","nom":"ressources de référence, dictionnaires et encyclopédies"},"typePedagogique":[{"uri":"http://data.education.fr/voc/scolomfr/concept/lecture","nom":"cours / présentation"}],"typologieDocument":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024","nom":"livre numérique"}],"niveauEducatif":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-022-num-023","nom":"3e"}],"domaineEnseignement":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1391","nom":"français (cycle 4)"}],"urlAccesRessource":"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\u003dRU5UVEVTVDE\u003d\u0026idEtab\u003dMDY1MDQ5OVAtRVQ2\u0026idSrc\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMTF4eHh4eHh4eA\u003d\u003d","nomSourceEtiquetteGar":"Accessible via le Gestionnaire d’accès aux ressources (GAR)","distributeurTech":"378901946_0000000000000000","validateurTech":"378901946_0000000000000000"},{"idRessource":"http://n2t.net/ark:/99999/r14xxxxxxxx","idType":"ARK","nomRessource":"R14_ELEVE_RIEN - Manuel numérique élève (100% numérique) - Multisupports (tablettes + PC/Mac)","idEditeur":"378901946_0000000000000000","nomEditeur":"Worldline","urlVignette":"https://vignette.validation.test-gar.education.fr/VAtest1/gar/152.png","typePresentation":{"code":"MAN","nom":"manuels numériques"},"typePedagogique":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-006","nom":"étude de cas"}],"typologieDocument":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024","nom":"livre numérique"}],"niveauEducatif":[],"domaineEnseignement":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2550","nom":"des chrétiens dans l\u0027Empire (histoire 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2551","nom":"les relations de l\u0027Empire romain avec la Chine des Han (histoire 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-993","nom":"l\u0027Empire romain dans le monde antique (histoire 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2544","nom":"la « révolution » néolithique (histoire 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2549","nom":"conquêtes, paix romaine et romanisation (histoire 6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-990","nom":"histoire (6e)"},{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-991","nom":"la longue histoire de l\u0027humanité et des migrations (histoire 6e)"}],"urlAccesRessource":"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\u003dRU5UVEVTVDE\u003d\u0026idEtab\u003dMDY1MDQ5OVAtRVQ2\u0026idSrc\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMTR4eHh4eHh4eA\u003d\u003d","nomSourceEtiquetteGar":"Accessible via le Gestionnaire d’accès aux ressources (GAR)","distributeurTech":"378901946_0000000000000000","validateurTech":"378901946_0000000000000000"},{"idRessource":"http://n2t.net/ark:/99999/r20xxxxxxxx","idType":"ARK","nomRessource":"R20_ELEVE_1SEUL - Manuel numérique élève (100% numérique) - Multisupports (tablettes + PC/Mac)","idEditeur":"378901946_0000000000000000","nomEditeur":"Worldline","urlVignette":"https://vignette.validation.test-gar.education.fr/VAtest1/gar/114.png","typePresentation":{"code":"MAN","nom":"manuels numériques"},"typePedagogique":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-027","nom":"activité pédagogique"},{"uri":"http://data.education.fr/voc/scolomfr/concept/lecture","nom":"cours / présentation"}],"typologieDocument":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024","nom":"livre numérique"}],"niveauEducatif":[],"domaineEnseignement":[{"uri":"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1357","nom":"français (cycle 3)"}],"urlAccesRessource":"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\u003dRU5UVEVTVDE\u003d\u0026idEtab\u003dMDY1MDQ5OVAtRVQ2\u0026idSrc\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMjB4eHh4eHh4eA\u003d\u003d","nomSourceEtiquetteGar":"Accessible via le Gestionnaire d’accès aux ressources (GAR)","distributeurTech":"378901946_0000000000000000","validateurTech":"378901946_0000000000000000"}]}};

        //model.getUserStructure(model.me.userId, function(result) {
            model.getRessources('ENTTEST1', /*result.structure, */ function(result){
                $scope.ressources = result;
            //GET /ressources/{idENT}/{UAI}/{GARPersonneIdentifiant}
            alert($scope.ressources);
            $scope.display = {};
            $scope.display.detail = false;
        });        
    }
    
    
    $scope.firstName = model.me.firstName;
    $scope.lastName = model.me.lastName;

    route({
        mainView: function () {
            template.open('main', 'main');
        }
    });

    $scope.exportXML = function(){
        model.exportXML(function(result) {
            notify.info('mediacentre.export.launched');
        });
    }


    $scope.userIsLocalAdmin = function(ticket){
        // SUPER_ADMIN
        if( model.me.functions.SUPER_ADMIN ) {
            return true;
        }

        // ADMIN_LOCAL
        var isLocalAdmin = (model.me.functions &&
        model.me.functions.ADMIN_LOCAL && model.me.functions.ADMIN_LOCAL.scope );

        if(ticket && ticket.school_id) {
            // if parameter "ticket" is supplied, check that current user is local administrator for the ticket's school
            return isLocalAdmin && _.contains(model.me.functions.ADMIN_LOCAL.scope, ticket.school_id);
        }
        return isLocalAdmin;
    };

    $scope.viewDetail = function(index){
        template.open('detail', 'detail');
        $scope.display.detail = true;
        $scope.detail = $scope.ressources.listeRessources.ressource[index];
    }

    $scope.retour = function() {
        $scope.display.detail = false;
    }

    this.initialize();
}
