/*
  For a deeper understanding, please refer: http://s3.thinkaurelius.com/docs/titan/1.0.0/index-admin.html.
*/

// Open a graph
graph = TitanFactory.open("conf/se_dump.properties")
g = graph.traversal()

// Create an index
mgmt = graph.openManagement()

display_name = mgmt.getPropertyKey("DisplayName")
usersByNameIndex = mgmt.buildIndex("usersByName", Vertex.class).addKey(display_name, Mapping.TEXT.asParameter()).buildMixedIndex("search")
mgmt.commit()

// Rollback or commit transactions on the graph which predate the index definition
graph.tx().rollback()

// Block until the SchemaStatus transitions from INSTALLED to REGISTERED
report = mgmt.awaitGraphIndexStatus(graph, "usersByName").call()

// Run a Titan-Hadoop job to reindex
mgmt = graph.openManagement()
mr = new MapReduceIndexManagement(graph)
mr.updateIndex(mgmt.getGraphIndex("usersByName"), SchemaAction.REINDEX).get()

// Enable the index
mgmt = graph.openManagement()
mgmt.updateIndex(mgmt.getGraphIndex("usersByName"), SchemaAction.ENABLE_INDEX).get()
mgmt.commit()

// Block until the SchemaStatus is ENABLED
mgmt = graph.openManagement()
report = mgmt.awaitGraphIndexStatus(graph, "usersByName").status(SchemaStatus.ENABLED).call()
mgmt.rollback()
