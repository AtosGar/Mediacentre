var mediacentreBehaviours = {
	workflow: {
		exportXML: 'net.atos.entng.mediacentre.controllers.ResourceController|exportXML',
	}
};

Behaviours.register('support', {
	behaviours: supportBehaviours,
	workflow: function(){
		var workflow = { };
		var supportWorkflow = supportBehaviours.workflow;
		for(var prop in supportWorkflow){
			if(model.me.hasWorkflow(supportWorkflow[prop])){
				workflow[prop] = true;
			}
		}
		return workflow;
	}
});