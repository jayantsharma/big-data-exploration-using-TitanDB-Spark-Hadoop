import static com.xlson.groovycsv.CsvParser.parseCsv

def parse(line, factory) {
    def parts = line.split(/,/, -1)
    def id = parts[0]
    def label = parts[1] != "" ? "user" : "post"
//    def v1 = factory.vertex("${label}:${id}".toString(), label)
    def v1 = factory.vertex(id, label)
    if (label == "user") {
//      def v2 = factory.vertex("post:${parts[1]}".toString())
      def v2 = factory.vertex(parts[1])
      factory.edge(v1, v2, "createdBy")
    } else {
//      def v2 = factory.vertex("user:${parts[2]}".toString())
      def v2 = factory.vertex(parts[2])
      factory.edge(v2, v1, "createdBy")
    }
    return v1
}
