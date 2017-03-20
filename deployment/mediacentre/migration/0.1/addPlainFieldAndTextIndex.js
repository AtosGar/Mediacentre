db.forum.subjects.find({"messages.content" : { "$exists" : true}}, { "_id" : 1, "messages" : 1 }).forEach(function(forum) {
    var f = forum.messages;
    for (var i = 0; i < f.length; i++) {
        var text =  f[i]["content"].replace(/<[^>]*>/g, '');
        f[i]["contentPlain"] = text;
    }
    db.forum.subjects.update({"_id" : forum._id}, { $set : { "messages" : f}});
});
db.forum.subjects.createIndex({ "title": "text", "messages.contentPlain": "text" });