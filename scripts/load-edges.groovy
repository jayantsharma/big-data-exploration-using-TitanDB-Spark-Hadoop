PROJECT_DIR = System.getenv("SE_DUMP_HOME")

g = TitanFactory.open(PROJECT_DIR + "/conf/se_dump.properties")
m = g.openManagement()

// vertex labels
// user = m.makeVertexLabel("post").make()
// 
// // edge labels
// createdBy = m.makeEdgeLabel("createdBy").make()
// answerTo = m.makeEdgeLabel("answerTo").make()
// acceptedAnswer = m.makeEdgeLabel("acceptedAnswer").make()
// 
// // vertex and edge properties
// PostTypeId        = m.makePropertyKey("PostTypeId").dataType(Integer.class).make()
// CreationDate      = m.makePropertyKey("CreationDate").dataType(java.util.Date).make()
// Score             = m.makePropertyKey("Score").dataType(Integer.class).make()
// Title             = m.makePropertyKey("Title").dataType(String.class).make()
// Tags              = m.makePropertyKey("Tags").dataType(String.class).make()
// AnswerCount       = m.makePropertyKey("AnswerCount").dataType(Integer.class).make()
// CommentCount      = m.makePropertyKey("CommentCount").dataType(Integer.class).make()
// FavoriteCount     = m.makePropertyKey("FavoriteCount").dataType(Integer.class).make()
// 
// ParentId          = m.makePropertyKey("ParentId").dataType(Integer.class).make()
// AcceptedAnswerId  = m.makePropertyKey("AcceptedAnswerId").dataType(Integer.class).make()
// OwnerUserId       = m.makePropertyKey("OwnerUserId").dataType(Integer.class).make()

// global indices
//    m.buildIndex("artistsByName", Vertex.class).addKey(name).indexOnly(artist).buildCompositeIndex()
//    m.buildIndex("songsByName", Vertex.class).addKey(name).indexOnly(song).buildCompositeIndex()
// vertex centric indices
//    m.buildEdgeIndex(followedBy, "followedByWeight", Direction.BOTH, Order.decr, weight)
m.commit()
g.close()

graph = GraphFactory.open(PROJECT_DIR + "/conf/hadoop-graph/edges-hadoop-load.properties")

writeGraph = PROJECT_DIR + "/conf/se_dump.properties"
blvp = BulkLoaderVertexProgram.build().keepOriginalIds(true).writeGraph(writeGraph).
        intermediateBatchSize(10000).create(graph)
graph.compute(SparkGraphComputer).program(blvp).submit().get()
