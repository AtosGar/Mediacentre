routes.define(function ($routeProvider) {
    $routeProvider
        .when('/resource/:resourceId', {
        action: 'displayResource'
    })
        .when('/list-resources', {
            action: 'listResources'
        })
        .otherwise({
            redirectTo: '/list-resources'
        });
});

function MediacentreController($scope, template, model, route, $location) {


    $scope.firstName = model.me.firstName;
    $scope.lastName = model.me.lastName;

    route({
        mainView: function () {
            template.open('main', 'main');
        }
    });

    $scope.exportXML = function(){
        model.exportXML(function(result) {
            
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

}
