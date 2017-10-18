import static com.xlson.groovycsv.CsvParser.parseCsv

def parse(line, factory) {
    def columns = ['Id','DisplayName','Age','Location','UpVotes','DownVotes','Reputation']
    def user_data = parseCsv(line, readFirstLine:true, columnNames:columns).next().toMap()

    def v1 = factory.vertex(user_data['Id'], 'user')
    user_data.each { v1.property(it.key, it.value) }
    return v1
}
