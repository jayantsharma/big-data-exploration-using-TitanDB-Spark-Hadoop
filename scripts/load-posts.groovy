PROJECT_DIR = "/home/jayant/work/masters/course_work/big_data_engg/nosql_project/titan-1.0.0-hadoop1"

g = TitanFactory.open(PROJECT_DIR + "/conf/se_dump.properties")
m = g.openManagement()

// Create Vertex label : post
post = m.makeVertexLabel("post").make()

// Post properties
PostTypeId        = m.makePropertyKey("PostTypeId").dataType(Integer.class).make()
CreationDate      = m.makePropertyKey("CreationDate").dataType(java.util.Date).make()
Score             = m.makePropertyKey("Score").dataType(Integer.class).make()
Tags              = m.makePropertyKey("Tags").dataType(String.class).make()
AnswerCount       = m.makePropertyKey("AnswerCount").dataType(Integer.class).make()
CommentCount      = m.makePropertyKey("CommentCount").dataType(Integer.class).make()
FavoriteCount     = m.makePropertyKey("FavoriteCount").dataType(Integer.class).make()

m.commit()
g.close()

// Specifies settings for Hadoop-Gremlin - the processing powerhouse of Tinkerpop 
graph = GraphFactory.open(PROJECT_DIR + "/conf/hadoop-graph/posts-hadoop-load.properties")

// Graph Engine settings
writeGraph = PROJECT_DIR + "/conf/se_dump.properties"
blvp = BulkLoaderVertexProgram.build().keepOriginalIds(true).writeGraph(writeGraph).
        intermediateBatchSize(10000).create(graph)
graph.compute(SparkGraphComputer).program(blvp).submit().get()
