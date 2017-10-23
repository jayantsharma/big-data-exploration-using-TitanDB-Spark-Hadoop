// Shorthand
PROJECT_DIR = "/home/jayant/work/masters/course_work/big_data_engg/nosql_project/titan-1.0.0-hadoop1"

graph = TitanFactory.open(PROJECT_DIR + "/conf/se_dump.properties")
m = graph.openManagement()
 
// Create Edge labels
// createdBy = m.makeEdgeLabel("createdBy").make()
// answerTo = m.makeEdgeLabel("answerTo").make()
// acceptedAnswer = m.makeEdgeLabel("acceptedAnswer").make()

m.commit()

g = graph.traversal()

i = 0
new File("${PROJECT_DIR}/data", 'part-00000').eachLine { line ->
  i += 1
  def parts = line.split(/,/, -1)
  v1 = g.V().has('bulkLoader.vertex.id', "post:${parts[0]}").next()
  v2 = g.V().has('bulkLoader.vertex.id', "user:${parts[1]}").next()
  println("${i} -> ${v2.property('DisplayName')}")
  v1.addEdge('createdBy', v2)
}
