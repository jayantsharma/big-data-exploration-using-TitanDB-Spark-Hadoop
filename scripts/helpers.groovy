class Helpers{
  def getOrCreate(graph, g, property, property_value){
    def iter = g.V().has(property, property_value)
    if(iter.hasNext())
      return iter.next()
    else {
      def vertex = graph.addVertex(property, property_value)
      return vertex
    }
  }
}
