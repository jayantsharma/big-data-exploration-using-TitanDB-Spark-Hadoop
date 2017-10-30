/*
  For a deeper understanding, please refer: http://s3.thinkaurelius.com/docs/titan/1.0.0/index-admin.html.
*/

// Open a graph
graph = TitanFactory.open("conf/se_dump.properties")
g = graph.traversal()

// Create an index
mgmt = graph.openManagement()

reputation = mgmt.getPropertyKey("Reputation")
usersByRepIndex = mgmt.buildIndex("usersByRep", Vertex.class).addKey(reputation).buildMixedIndex("search")
mgmt.commit()

// Rollback or commit transactions on the graph which predate the index definition
graph.tx().rollback()

// Block until the SchemaStatus transitions from INSTALLED to REGISTERED
report = mgmt.awaitGraphIndexStatus(graph, "usersByRep").call()

// Run a Titan-Hadoop job to reindex
mgmt = graph.openManagement()
mr = new MapReduceIndexManagement(graph)
mr.updateIndex(mgmt.getGraphIndex("usersByRep"), SchemaAction.REINDEX).get()

// Enable the index
mgmt = graph.openManagement()
mgmt.updateIndex(mgmt.getGraphIndex("usersByRep"), SchemaAction.ENABLE_INDEX).get()
mgmt.commit()

// Block until the SchemaStatus is ENABLED
mgmt = graph.openManagement()
report = mgmt.awaitGraphIndexStatus(graph, "usersByRep").status(SchemaStatus.ENABLED).call()
mgmt.rollback()
