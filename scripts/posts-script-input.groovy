import static com.xlson.groovycsv.CsvParser.parseCsv
import java.text.SimpleDateFormat

ISO_format = "YYYY-MM-DD'T'HH:mm:ss.SSS"
sdf = new SimpleDateFormat(ISO_format)

def parse(line, factory) {
    def columns = [ 'Id', 'PostTypeId', 'ParentId', 'AcceptedAnswerId', 'CreationDate', 'Score', 'OwnerUserId', 'Title', 'Tags', 'AnswerCount', 'CommentCount', 'FavoriteCount' ]
    def post_data = parseCsv(line, readFirstLine:true, columnNames:columns).next().toMap()

    def parent_id = post_data.remove('ParentId')
    def accepted_answer_id = post_data.remove('AcceptedAnswerId')
    def owner_user_id = post_data.remove('OwnerUserId')

    def v1 = factory.vertex(post_data['Id'], 'post')
    post_data['CreationDate'] = sdf.parse(post_data['CreationDate'])
    post_data.each { if (it.value != null) v1.property(it.key, it.value) }
 
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
