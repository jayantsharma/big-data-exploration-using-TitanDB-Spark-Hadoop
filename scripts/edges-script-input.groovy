import static com.xlson.groovycsv.CsvParser.parseCsv

def parse(line, factory) {
    def parts = line.split(/,/, -1)
    def id = parts[0]
    if (id != null && id.isInteger()){
      def label = parts[1] != "" ? "post" : "user"
      def v1 = factory.vertex("${label}:${id}".toString())
      if (label == "post") {
        id = parts[1]
        if (id != null && id.isInteger()) {
          def v2 = factory.vertex("user:${id}".toString())
          factory.edge(v1, v2, "createdBy")
        }
      } else {
        id = parts[2]
        if (id != null && id.isInteger()) {
          def v2 = factory.vertex("post:${id}".toString())
          factory.edge(v2, v1, "createdBy")
        }
      }
      return v1
    }
    else {
      // reaching here means corrupt data; return dummy edge
      def dummy_vertex = factory.vertex("foo")
      return dummy_vertex
    }
}
