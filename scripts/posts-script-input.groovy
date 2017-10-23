import static com.xlson.groovycsv.CsvParser.parseCsv
import java.text.SimpleDateFormat

ISO_format = "YYYY-MM-DD'T'HH:mm:ss.SSS"
sdf = new SimpleDateFormat(ISO_format)

def parse(line, factory) {
    def columns = [ 'Id', 'PostTypeId', 'CreationDate', 'Score', 'Tags', 'AnswerCount', 'CommentCount', 'FavoriteCount' ]
    def data = parseCsv(line, readFirstLine:true, columnNames:columns)

    for (post_data in data) {
      post_data = post_data.toMap()
      _id = "post:${post_data['Id']}".toString()
      def v1 = factory.vertex(_id)

      // Set properties
      v1.property('Id', post_data['Id'])

      def string_properties = ['Tags']
      string_properties.each { if (post_data[it] != null) v1.property(it, post_data[it]) }

      if (post_data['CreationDate'] != null){
        v1.property('CreationDate', sdf.parse(post_data['CreationDate']))
      }

      def numeric_properties = [ 'PostTypeId', 'Score', 'AnswerCount', 'CommentCount', 'FavoriteCount' ]
      numeric_properties.each { 
        if (post_data[it] != null && post_data[it].isInteger() )
          v1.property(it, post_data[it].toInteger())
      }
 
//     def owner = factory.vertex(owner_user_id, 'user')
//     factory.edge(v1, owner, 'createdBy')
//     factory.edge(owner, v1, 'createdBy')

//     if (parent_id != null) {
//       def parent_post = factory.vertex(parent_id, 'post')
//       factory.edge(v1, parent_post, 'answerTo') 
//     }
// 
//     if (accepted_answer_id != null) {
//       def accepted_answer = factory.vertex(accepted_answer_id, 'post')
//       factory.edge(v1, accepted_answer, 'acceptedAnswer') 
//     }

      return v1
    }

    // reaching here means corrupt data; return dummy vertex
    def dummy_vertex = factory.vertex("foo")
    return dummy_vertex
}
