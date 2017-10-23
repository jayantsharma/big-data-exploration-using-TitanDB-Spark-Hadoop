import static com.xlson.groovycsv.CsvParser.parseCsv

def parse(line, factory) {
    def columns = ['Id','DisplayName','Age','Location','UpVotes','DownVotes','Reputation']
    def data = parseCsv(line, readFirstLine:true, columnNames:columns)
    
    for (user_data in data) {
      user_data = user_data.toMap()
      _id = "user:${user_data['Id']}".toString()
      def v1 = factory.vertex(_id)

      // Set properties
      v1.property('Id', user_data['Id'])

      def string_properties = ['DisplayName','Location']
      string_properties.each { if (user_data[it] != null) v1.property(it, user_data[it]) }
      
      def numeric_properties = ['Age','UpVotes','DownVotes','Reputation']
      numeric_properties.each { 
        if (user_data[it] != null && user_data[it].isInteger() )
          v1.property(it, user_data[it].toInteger())
      }

      return v1
    }

    // reaching here means corrupt data; return dummy vertex
    def dummy_vertex = factory.vertex("foo")
    return dummy_vertex
}
