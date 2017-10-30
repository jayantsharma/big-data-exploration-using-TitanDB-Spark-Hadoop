import static com.xlson.groovycsv.CsvParser.parseCsv

def parse(line, factory) {
    def columns = [ 'Id', 'Score' ]
    def data = parseCsv(line, readFirstLine:true, columnNames:columns)

    for (comment_data in data) {
      comment_data = comment_data.toMap()
      _id = "comment:${comment_data['Id']}".toString()
      def v1 = factory.vertex(_id)

      // Set properties
      v1.property('Id', comment_data['Id'])

      if (comment_data['Score'] != null && comment_data['Score'].isInteger() )
        v1.property('Score', comment_data['Score'].toInteger())
 
      return v1
    }

    // reaching here means corrupt data; return dummy vertex
    def dummy_vertex = factory.vertex("foo")
    return dummy_vertex
}
