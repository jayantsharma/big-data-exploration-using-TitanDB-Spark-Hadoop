PROJECT_DIR = "/home/jayant/work/masters/course_work/big_data_engg/nosql_project/titan-1.0.0-hadoop1"

g = TitanFactory.open(PROJECT_DIR + "/conf/se_dump.properties")
m = g.openManagement()
// vertex labels
user = m.makeVertexLabel("user").make()
//    song   = m.makeVertexLabel("song").make()

// edge labels
//    sungBy     = m.makeEdgeLabel("sungBy").make()
//    writtenBy  = m.makeEdgeLabel("writtenBy").make()
//    followedBy = m.makeEdgeLabel("followedBy").make()

// vertex and edge properties
blid            = m.makePropertyKey("bulkLoader.vertex.id").dataType(String.class).make()
user_id         = m.makePropertyKey("Id").dataType(Integer.class).make()
display_name    = m.makePropertyKey("DisplayName").dataType(String.class).make()
age             = m.makePropertyKey("Age").dataType(Float.class).make()
location        = m.makePropertyKey("Location").dataType(String.class).make()
upvotes         = m.makePropertyKey("UpVotes").dataType(Integer.class).make()
downvotes       = m.makePropertyKey("DownVotes").dataType(Integer.class).make()
reputation      = m.makePropertyKey("Reputation").dataType(Integer.class).make()

//    performances = m.makePropertyKey("performances").dataType(Integer.class).make()
//    weight       = m.makePropertyKey("weight").dataType(Integer.class).make()

// global indices
m.buildIndex("byBulkLoaderVertexId", Vertex.class).addKey(blid).buildCompositeIndex()
//    m.buildIndex("artistsByName", Vertex.class).addKey(name).indexOnly(artist).buildCompositeIndex()
//    m.buildIndex("songsByName", Vertex.class).addKey(name).indexOnly(song).buildCompositeIndex()
// vertex centric indices
//    m.buildEdgeIndex(followedBy, "followedByWeight", Direction.BOTH, Order.decr, weight)
m.commit()
g.close()

graph = GraphFactory.open(PROJECT_DIR + "/conf/hadoop-graph/users-hadoop-load.properties")

writeGraph = PROJECT_DIR + "/conf/se_dump.properties"
blvp = BulkLoaderVertexProgram.build().keepOriginalIds(true).writeGraph(writeGraph).
        intermediateBatchSize(10000).create(graph)
graph.compute(SparkGraphComputer).program(blvp).submit().get()
