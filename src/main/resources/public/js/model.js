model.exportXML = function () {
    http().get('/mediacentre/exportXML').done(function(result){
        if(typeof callback === 'function'){
            callback(result);
        }
    }.bind(this));
};

model.getRessources = function(ident, callback) {
    http().get('/mediacentre/getRessources/' + ident).done(function (result) {
        if (typeof callback === 'function') {
            callback(result);
        }
    });
}
    //GET /ressources/{idENT}/{UAI}/{GARPersonneIdentifiant}
   /* var config = {headers:  {
        'X-Id-Ent-Autorisation': 'cn',
        'Accept': 'application/json'
    }}*/
    //http().get('http://list-ressource.gar.as8677.net/ressources/' + ident + '/' + uai + '/' + userId, config).done(function(ressources) {
    //http().get('http://list-ressource.gar.as8677.net/ressources/ENTTEST1/0650499P-ET6/5577102-ET6', config).done(function(ressources) {
     /*   this.load(ressources);
    }.bind(this));*/


model.getUserStructure = function(userId, callback) {
    http().get('/mediacentre/userStructure/' + userId).done(function(result) {
        if(typeof callback === 'function'){
            callback(result);
        }
    });
}


