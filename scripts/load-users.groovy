PROJECT_DIR = "/home/jayant/work/masters/course_work/big_data_engg/nosql_project/titan-1.0.0-hadoop1"

g = TitanFactory.open(PROJECT_DIR + "/conf/se_dump.properties")
m = g.openManagement()

// Create Vertex label : user
user = m.makeVertexLabel("user").make()

// User properties

// required by the Bulkloader program; the rest should be self explanatory
blid            = m.makePropertyKey("bulkLoader.vertex.id").dataType(String.class).make()
user_id         = m.makePropertyKey("Id").dataType(Integer.class).make()
display_name    = m.makePropertyKey("DisplayName").dataType(String.class).make()
age             = m.makePropertyKey("Age").dataType(Float.class).make()
location        = m.makePropertyKey("Location").dataType(String.class).make()
upvotes         = m.makePropertyKey("UpVotes").dataType(Integer.class).make()
downvotes       = m.makePropertyKey("DownVotes").dataType(Integer.class).make()
reputation      = m.makePropertyKey("Reputation").dataType(Integer.class).make()

// global index on bulkloader.vertex.id
m.buildIndex("byBulkLoaderVertexId", Vertex.class).addKey(blid).buildCompositeIndex()

m.commit()
g.close()

// Specifies settings for Hadoop-Gremlin - the processing powerhouse of Tinkerpop 
graph = GraphFactory.open(PROJECT_DIR + "/conf/hadoop-graph/users-hadoop-load.properties")

// Graph Engine settings
writeGraph = PROJECT_DIR + "/conf/se_dump.properties"
blvp = BulkLoaderVertexProgram.build().keepOriginalIds(true).writeGraph(writeGraph).
        intermediateBatchSize(10000).create(graph)
// Run over Spark (no config settings required, Spark is part of Hadoop-Gremlin)
graph.compute(SparkGraphComputer).program(blvp).submit().get()
