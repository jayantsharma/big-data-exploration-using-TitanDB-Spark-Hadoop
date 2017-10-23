## SETUP (TitanDB - HBase - Elasticsearch)

__NOTE__: All versions of the following support software have been selected because of limitations with TitanDB 1.0.0. The last public release of TitanDB was over 2 years ago, when they only supported Hadoop 1. Hence, we're stuck with an outdated ecosystem. Far from ideal.

### Elasticsearch
1. Get [Elasticsearch 1.5.2](https://github.com/elastic/elasticsearch/releases/tag/v1.5.2) from the _releases_ section of the Github repo.
2. Use [Maven](http://maven.apache.org) to build elasticsearch from the sourcecode: `mvn clean package -DskipTests`.
3. Unzip the built package, and execute `bin/elasticsearch`. Verify elastic is up using: `curl -X GET http://localhost:9200/`.

### Cassandra
2. Get [Cassandra 2.1.19]() from the _releases_ section of their Github repo.
2. Execute `bin/cassandra` from the root directory of the un-archived package. The default settings worked for me with storage PROJECT\_ROOT/data/data, so nothing to do here. 
2. You can start cassandra in the background with `cassandra -f` and forget about it.

### Spark
Spark is required because we need to process large XML files. This might sound like a snazzy decision, but the mature Spark XML library which creates Dataframes out of XML data was a no brainer.
1. Get [Spark 1.6.1](https://d3kbcqa49mib13.cloudfront.net/spark-1.6.1-bin-hadoop1.tgz).
2. Edit `conf/spark-env.sh` to set the HADOOP\_CONF\_DIR environment variable to point to the `conf` directory of your Hadoop installation, so that it integrates with HDFS. Now by default, File I/O happens with HDFS.
3. Add the following line to `conf/spark-defaults.conf`, so that the necessary libraries are loaded each time Spark is started.

### Hadoop/HDFS
Not an essential per se, but the Big Data requirement(parsing of XML files running into GBs with Spark) alongwith the way TitanDB's bulk loading program works(only reads from HDFS) requires this.
1. Get [Elasticsearch 1.5.2](https://github.com/elastic/elasticsearch/releases/tag/v1.5.2) from the _releases_ section of the Github repo.
2. Use [Maven](http://maven.apache.org) to build elasticsearch from the sourcecode: `mvn clean package -DskipTests`.
3. Unzip the built package, and execute `bin/elasticsearch`. Verify elastic is up using: `curl -X GET http://localhost:9200/`.

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
val df = sqlContext.read.format("com.databricks.spark.xml").option("rowTag", "row").load("input/Users.xml")
df.select("@Id", "@DisplayName", "@Age", "@Location", "@UpVotes", "@DownVotes", "@Reputation").write.format("com.databricks.spark.csv").option("header", "true").option("nullValue", "").save("output/users")
```
##### Load Data using spark-xml
- Modify in-place XML so it's readable
```sh
sed -i -e 's/\/>/><foo>bar<\/foo><\/row>/' Users.xml
```scala
// Take 1/n fraction of data points
var filtered_posts_users = df.filter(df.col("@OwnerUserId").isNotNull).select("@Id", "@OwnerUserId")
filtered_posts_users = filtered_posts_users.limit(filtered_posts_users.count() / 10)
val posts_then_users = filtered_posts_users.withColumnRenamed("@Id", "a").withColumn("c", lit(null: String)).withColumnRenamed("@OwnerUserId", "b").select("a","b","c")
val users_then_posts = filtered_posts_users.withColumnRenamed("@OwnerUserId", "a").withColumn("b", lit(null: String)).withColumnRenamed("@Id", "c").select("a","b","c")
posts_then_users.unionAll(users_then_posts).write.format("com.databricks.spark.csv").option("nullValue", "").save("graph/edges/posts_owners")

