## SETUP (TitanDB - HBase - Elasticsearch)

### Elasticsearch
1. Get [Elasticsearch 1.5.2](https://github.com/elastic/elasticsearch/releases/tag/v1.5.2) from the _releases_ section of the Github repo.
2. Use [Maven](http://maven.apache.org) to build elasticsearch from the sourcecode: `mvn clean package -DskipTests`.
3. Unzip the built package, and execute `bin/elasticsearch`. Verify elastic is up using: `curl -X GET http://localhost:9200/`.

### HBase
2. Get HBase from the official website (the current version works well).
2. Execute `bin/start-hbase.sh` from the root directory of the un-archived package.

### Titan
3. Get Titan 1.0.0 from the [Downloads](https://github.com/thinkaurelius/titan/wiki/Downloads) page @ Titan.
4. `bin/gremlin.sh` from inside the project directory, and you're good to go.


## Data Processing

### General Strategy
1. Convert data to GraphML using Spark
2. Use BulkLoaderVertexProgram(running over Spark) to digest the resulting GraphML file and create graph


#### Convert Data to csv using Spark
##### Setting-up Spark
1. Download Hadoop __1.2.1__ and set it up in pseudo-distributed mode.
1. Download Spark __1.6.1__.
2. Modify _spark-env.sh_ and _spark-defaults.conf_ so that appropriate libraries are loaded and hadoop/hdfs is setup. [conf files in _conf_ directory]
3. Examples snippet
```scala
import org.apache.spark.sql.SQLContext
val sqlContext = new SQLContext(sc)
val df = sqlContext.read.format("com.databricks.spark.xml").option("rowTag", "row").load("input/users.xml")
df.select("@Id", "@DisplayName", "@Age", "@Location", "@UpVotes", "@DownVotes", "@Reputation").write.format("com.databricks.spark.csv").option("header", "true").save("output/users")
```
##### Load Data using spark-xml
- Modify in-place XML so it's readable
```sh
sed -i -e 's/\/>/><foo>bar<\/foo><\/row>/' Users.xml
```
