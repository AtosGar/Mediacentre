model.exportXML = function () {
    http().get('/mediacentre/exportXML').done(function(result){
        if(typeof callback === 'function'){
            callback(result);
        }
    }.bind(this));
};