model.exportXML = function () {
    http().get('/mediacentre/exportXML').done(function(result){
        if(typeof callback === 'function'){
            callback(result);
        }
    }.bind(this));
};

model.exportXML_1D = function () {
    http().get('/mediacentre/exportXML_1D').done(function(result){
        if(typeof callback === 'function'){
            callback(result);
        }
    }.bind(this));
};

model.exportXML_2D = function () {
    http().get('/mediacentre/exportXML_2D').done(function(result){
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

model.getStructures = function(userId, callback) {
    http().get('/mediacentre/getUserStructures/' + userId).done(function(result) {
        if(typeof callback === 'function'){
            callback(result);
        }
    });
}

model.isExportButtonVisible = function(callback) {
    http().get('/mediacentre/isExportButtonVisible').done(function(result) {
        if(typeof callback === 'function'){
            callback(result);
        }
    });
}

model.isExportMixte = function(callback) {
    http().get('/mediacentre/isExportMixte').done(function(result) {
        if(typeof callback === 'function'){
            callback(result);
        }
    });
}

model.isExport1D= function(callback) {
    http().get('/mediacentre/isExport1D').done(function(result) {
        if(typeof callback === 'function'){
            callback(result);
        }
    });
}

model.isExport2D = function(callback) {
    http().get('/mediacentre/isExport2D').done(function(result) {
        if(typeof callback === 'function'){
            callback(result);
        }
    });
}


