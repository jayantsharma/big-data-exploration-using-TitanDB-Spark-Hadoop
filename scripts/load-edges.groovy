PROJECT_DIR = "/home/jayant/work/masters/course_work/big_data_engg/nosql_project/titan-1.0.0-hadoop1"

g = TitanFactory.open(PROJECT_DIR + "/conf/se_dump.properties")
m = g.openManagement()
 
// Create Edge labels
// createdBy = m.makeEdgeLabel("createdBy").make()
// answerTo = m.makeEdgeLabel("answerTo").make()
// acceptedAnswer = m.makeEdgeLabel("acceptedAnswer").make()

m.commit()
g.close()

// Specifies settings for Hadoop-Gremlin - the processing powerhouse of Tinkerpop 
graph = GraphFactory.open(PROJECT_DIR + "/conf/hadoop-graph/edges-hadoop-load.properties")

// Graph Engine settings
writeGraph = PROJECT_DIR + "/conf/se_dump.properties"
blvp = BulkLoaderVertexProgram.build().keepOriginalIds(true).writeGraph(writeGraph).
        intermediateBatchSize(10000).create(graph)
// Run over Spark (no config settings required, Spark is part of Hadoop-Gremlin)
graph.compute(SparkGraphComputer).program(blvp).submit().get()
