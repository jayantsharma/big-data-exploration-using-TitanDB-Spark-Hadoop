## Introduction
We begin with an introduction to the Tech Stack that we used before briefly elucidating the schema of the dataset that was ingested. Onwards, the Data Ingestion methodology including reasons for choosing different parts of the stack.

The second part deals with Indexing and Retrieval of data using Gremlin from TitanDB. We conclude with a short summary and an attached appendix. The appendix includes detailed directions for Setup.

## Tech Stack
![tech-stack](https://github.com/jayantsharma/bigDataExplorationUsingTitandbAndHadoop/blob/master/images/tech_stack.png)

### Details
1. _Spark_ in conjunction with _HDFS_ for pre-processing of data.
1. _TitanDB_ configured with _Cassandra_ as the storage-backend and _ElasticSearch_ as the indexing-backend serves as our graph database.
1. Spark interacting with HDFS is responsible for _Bulk Ingestion_ of data in TitanDB.
1. _MapReduce_ leveraged to build indexes over the graph database.

## Schema
In contrast to most mainstream NoSQL technologies, TitanDB is not _schemaless_. Vertex and Edge labels as well as properties and their datatypes need to be declared before starting to use the database. This is generally done by a call to the graph's _ManagementSystem_ (for eg, refer: scripts/load-users.groovy).

![schema](https://github.com/jayantsharma/bigDataExplorationUsingTitandbAndHadoop/blob/master/images/schema.png)

The StackOverflow dataset, as noted in the proposal consists of 3 main elements:
1. Users - The Stack Overflow users
2. Posts - Questions or Answers
3. Comments - User comments on posts

Let's run through the sample schema pictured above.
1. John, Bill and Silly are 3 fictional users.
2. John asks the question "How to compute sum of ..." to which Bill answers "(1 to N toList) ...". Note that both the question and answer are posts, but in addition, the *answer\_post* has an _answerTo_ relationship with the *question\_post*.
3. Silly is the author of 2 comments, each of which are related to one of the posts.
4. Observations:
   - John is connected to Bill via their QnA.
   - John and Bill are both connected to Silly via comments on their respective posts.

## Data Ingestion
We walk through the strategy in detail explaining the reasons for our choices alongwith any nuances with the help of text as well as code.

### General Strategy
1. Read XML files from HDFS using Spark, filter out the data of interest and write to CSV again in HDFS.
2. Use BulkLoaderVertexProgram (running over Spark) to digest the resulting CSV file and add nodes to graph.

### Detailed Example for User nodes
#### Convert Data to CSV using Spark
##### Unarchive 7z
The dataset consists of XML files archived in 7z format. 7za is a lightweight-archiving utility that can be used to unarchive the XML files. The usage goes like this `7za x stackoverflow.com-Users.7z`.

##### Load Data using spark-xml
spark-xml is a Spark library that reads XML files into Spark-SQL dataframes. Since the XML read happens over Spark, the gut-feel is that this should be much faster than other solutions like Python XML Iterators. While differences might not be noticeable when running on a single machine, this nevertheless serves as a PoC for distributed processing of large XML files.

The first step is to modify XML in-place using the commandline tool `sed` so it's readable. This is a limitation of Spark-XML, in that it can't read self-closing XML tags.
```sh
# Insert closing tag </row>, while adding a dummy attribute foo with the value bar inside.
sed -i -e 's/\/>/><foo>bar<\/foo><\/row>/' Users.xml
```

##### Move data to HDFS
We've tried to do all File I/O via HDFS, to imitate a distributed processing framework. In line with this, after stream-processing with _sed_, the data is moved to HDFS from where Spark picks it up and later writes the output to HDFS itself.
```sh
# Create input directory if it doesn't exist
hadoop fs -ls
hadoop fs -mkdir input

# Move Users.xml to input
# moveFromLocal instead of copyFromLocal if you're short on space
hadoop fs -copyFromLocal Users.xml input/
```
##### Process XML in Spark
The following lines of codes are most conveniently implemented on Spark Shell (`bin/spark-shell`).
```scala
// In order to read XML into Spark Dataframes, Spark SQL is required.
import org.apache.spark.sql.SQLContext
val sqlContext = new SQLContext(sc)

// Read the <row> tags of the XML file that is loaded from the HDFS location "input/Users.xml". The <row> tags could more appropriately have been <user> tags, but that's how the file is formatted.
// Play with the Dataframe using df.show() or df.filter functions to get a sense of what the data looks like.
val df = sqlContext.read.format("com.databricks.spark.xml").option("rowTag", "row").load("input/Users.xml")

// Select the attributes of interest, write them using CSV format with no header and NULLs formatted as empty strings and write to graph/nodes/users directory in HDFS.
// The directory MUST be empty if it exists. If it doesn't exist, Spark will create it.
df.select("@Id", "@DisplayName", "@Age", "@Location", "@UpVotes", "@DownVotes", "@Reputation").write.format("com.databricks.spark.csv").option("nullValue", "").save("output/users")
```

#### Ingest Data in TitanDB
Loading the graph elements transactionally, i.e. one-by-one into the database may be a resonable strategy for small datasets, however, it quickly becomes infeasible for large datasets. Delving deeper into the literature around TitanDB, we discovered a utility program known as the *BulkLoaderVertexProgram*, that is built for exactly this use-case. But first, some tech details.

TitanDB heavily uses TinkerPop, the popular graph computing framework for a variety of purposes, including graph traversals and computations. It's easy to see that graph computations over large graphs is a non-trivial problem. To tackle it, TinkerPop provides _Hadoop-Gremlin_, a scalable graph engine for processing large graphs. Hadoop-Gremlin provides support for computing over Giraph, Spark or MapReduce. 

We are going to leverage Hadoop-Gremlin (formerly TinkerPop Furnace) to bulk-process our data. Hadoop-Gremlin allows bulk ingestion of data using its BulkLoaderVertexProgram (blvp, in short) via reading of standard as well as arbitrarily formatted files (using Hadoop's ScriptInputFormat). CSV is a custom or arbitrary format because the standard formats for graph ingestion are GraphML and GraphSON, based on XML and JSON respectively. The formats for both of these are fairly sophisticated and the general method of producing such files is generally by outputting a graph in one of these formats, which doesn't work for our case.

An obvious implication of the custom format is a script that can parse the format. Therefore, there are 3 files that we need to put in place for TitanDB to process the data.
1. *scripts/load\_users.groovy* : Creates the attributes and label of nodes (here, users) that we need.
2. *conf/hadoop-graph/users-hadoop-load.properties* : Specifies location of input data and the parsing script.
3. *scripts/users-script-input.groovy* : The parsing script itself, this needs to be in HDFS too.

Once these are in-place, just execute the *load\_users.groovy* script on the Gremlin shell:
```groovy
:load scripts/load_users.groovy
```

### Loading Edges
The process for loading edges varies because of a [long-standing bug](https://issues.apache.org/jira/browse/TINKERPOP-432) in the version of TinkerPop that TitanDB uses. Instead of using blvp, edges are therefore loaded transactionally. The below example creates directed edges with the label _createdBy_ from posts to users, signifying the relation that a given post was created by the connected user.
#### Process using Spark to get partial adjacency list (only out-edges, no in-edges)
```scala
// Get Dataframe as earlier, then continue using below
// Take a fraction of data points, if you so wish
var filtered_posts_users = df.filter(df.col("@OwnerUserId").isNotNull).select("@Id", "@OwnerUserId").limit(100000)
// write to CSV
filtered_posts_users.write.format("com.databricks.spark.csv").option("nullValue", "").save("graph/edges/posts_owners")
```
#### Ingesting in TitanDB
We copy the data from HDFS to the local filesystem and use just one script: *scripts/load_user_posts_edges.groovy* to load the edges into the graph. The script iterates over all lines in all files in the specified directory, loading edges one-by-one. This provides a convenient point to assess the efficacy of blvp by comparing the runtimes of two. 

In our framework, edges are lightweight connections between nodes without any properties. Hence, it stands to reason that they should be loaded with little effort. On my comp, where ingestion of 8 million users takes around 30 minutes, ingestion of even a 1 million edges easily surpasses that benchmark!

#### Ingestion Statistics
##### Nodes
vertex\_label | #
------
user | 7.6M
post | 3.4M
comments | 3.9M

##### Relationships
relationship\_type (edge\_label) | #
------
user <- post (createdBy) | 3.2M
post <- post (answerTo) | 2.4M
post <- comment (commentOn) | 250K
user <- comment (createdBy) | 250K

## Data Retrieval
Here are some basic queries to give the reader a flavor of Gremlin DSL:
```groovy
// Get user with id 42
g.V().has('bulkLoader.vertex.id', 'user:42').properties()

// Get a couple of posts created by user with id 42
// Note how the function "in" is used to traverse an edge
g.V().has('bulkLoader.vertex.id', 'user:42').in('createdBy').has('bulkLoader.vertex.id', textPrefix('post')).limit(2).properties()

// Get user who created a specific post
g.V().has('bulkLoader.vertex.id', 'post:42').out('createdBy').properties()

// Get answer to a question post
g.V().has('bulkLoader.vertex.id', 'comment:42').in('answerTo').properties()
```

Funkier things with Gremlin can consist of trying to connect users via their questions and answers or their comments. For instance, look at the snippets below:
```groovy
// Get users connected to a certain user via answers to his/her asked questions
g.V().has('bulkLoader.vertex.id', 'user:91').in('createdBy').has('PostTypeId', 1).in('answerTo').out('createdBy').values('DisplayName').dedup()

// Get users connected to a given user U via comments on questions asked by U
g.V().has('bulkLoader.vertex.id', 'user:91').in('createdBy').has("PostTypeId", 1).in('commentOn').out('createdBy').values("DisplayName").dedup()
```

However, consider a range scan or wildcard query like the following:
```groovy
// Find users with murakami in their name
g.V().has("DisplayName", textContainsRegex("murakami"))
```
TitanDB gives a warning on issuing this query, complaining that it will have to scan the entire graph. While graph scans can be forbidden by using a flag (force-index) on production environments, the solution is to use approprate indexes on items that are consistent with query patterns. 

This is where ElasticSearch comes in. TitanDB provides the option of creating 2 different types of graph-indexes, described briefly:
1. Composite Indexes - Defined on a pre-defined combination of properties that can only check for equality, i.e. no prefix or regex checks for strings and no less-than/greater-than checks for numbers. Since these are rather elementary kinds of indexes, these don't need the presence of an indexing backend.
2. Mixed Indexes - More powerful indexes which can be used in combination with one another to answer queries about arbitrary combinations of keys. These require the presence of an Indexing backend like Elastic/Lucene and allow more powerful search predicates.

### Indexing User properties
Our aim is to enable fast lookups for queries of the sort:
```groovy
// Get users with 'murakami' in their name(text-insensitive) with a reputation greater than 1000
g.V().has("DisplayName", textContainsRegex("murakami")).has("Reputation", gt(1000)).count()
```
It's obvious that this requires an index on the property 'DisplayName' and another one on 'Reputation' for _all_ users. Here's how you can build an index for DisplayName:
```groovy
mgmt = graph.openManagement()
display_name = mgmt.getPropertyKey("DisplayName")

/*
 Create an index using DisplayName
 Observe the Mapping.TEXT argument; this is the default and indexes DisplayName 
 by tokenizing it into words so that any query be default operates at the 
 word/token level, NOT the entire string
*/
usersByNameIndex = mgmt.buildIndex("usersByName", Vertex.class).addKey(display_name, Mapping.TEXT.asParameter()).buildMixedIndex("search")
mgmt.commit()
```
This and a couple more lines are generally all you need to do if you're creating the property alongwith creating the index. When that is not so, as was in our case, a _Reindexing_ procedure must be run to index the previously existing graph data. For large graphs, this can be run as a MapReduce job (the simpler procedure on my machine gives no hint of stopping even after 2 hours).

A painful Hadoop/MapReduce setup is required in order to do so. Unlike the earlier blvp program, in which Spark is run internally by TitanDB, running MapReduce job and task trackers are required in order to execute the Reindexing procedure. Make sure that the TitanDB jars are added to the CLASSPATH var of Hadoop. Here's a snippet that demonstrates how the procedure can be run:
```groovy
// Run a Titan-Hadoop job to reindex
mgmt = graph.openManagement()
mr = new MapReduceIndexManagement(graph)
mr.updateIndex(mgmt.getGraphIndex("usersByNameIndex"), SchemaAction.REINDEX).get()
```

After the index is registered and enabled, the magic becomes apparent as you try to execute the above discussed query. No warnings appear, results are displayed quickly without rendering the machine unusable. To give a more complete use-case, good indexes for the present dataset may efficiently answer questions of the sort: What are all the questions tagged 'C++' asked by the 5 most highly reputed users having 'Linus' in their name ?

## Learning
Two of the biggest learnings that we had from the course may be summarized thus:
2. Using outdated tech is a BAD idea. TitanDB is compatible with versions of Cassandra, ES, Hadoop and Spark that are found deep in git archives and their documentation is even more difficult to locate. Dev time is precious and should not be wasted endlessly on setups.
1. Build using tiny samples of data. 1GB files are unsuitable for testing out stuff no matter how obvious things may seem before trying them out. Large execution times mean longer feedback loops and hence, slower progress.

```groovy
graph = TitanFactory.open('conf/se_dump.properties')
mgmt = graph.openManagement()
name = mgmt.getPropertyKey("DisplayName")
/*
 Build a graph-centric index on all the users indexing them by their name.
*/
mgmt.buildIndex('usersByName', Vertex.class).addKey(name, Mapping.TEXT.asParameter()).indexOnly("user").buildMixedIndex("search")
```

### Enable this:
* g.V().has('name', textContains('hercules')).has('age', inside(20, 50)).order().by('age', decr).limit(10) 

## SETUP (TitanDB - Cassandra - Elasticsearch)

__NOTE__: All versions of the following support software have been selected because of limitations with TitanDB 1.0.0. The last public release of TitanDB was over 2 years ago, when they only supported Hadoop 1.

### Elasticsearch
1. Get [Elasticsearch 1.5.2](https://github.com/elastic/elasticsearch/releases/tag/v1.5.2) from the _releases_ section of the Github repo.
2. Use [Maven](http://maven.apache.org) to build elasticsearch from the sourcecode: `mvn clean package -DskipTests`.
3. Unzip the built package, and execute `bin/elasticsearch`. Verify elastic is up using: `curl -X GET http://localhost:9200/`.

### Cassandra
2. Get [Cassandra 2.1.19](http://www.apache.org/dyn/closer.lua/cassandra/2.1.19/apache-cassandra-2.1.19-bin.tar.gz) from the  _Downloads_ section of their website.
2. Execute `bin/cassandra` from the root directory of the un-archived package. The default settings worked for me with storage set to PROJECT\_ROOT/data/data. You might have to play with the JAVA\_HOME variable to get this right though.
2. You can start cassandra in the background with `cassandra -f` and forget about it.

### Spark
Spark is required because we need to process large XML files. This might sound like a snazzy decision, but the mature Spark XML library which creates Dataframes out of XML data was a no brainer.
1. Get [Spark 1.6.1](https://d3kbcqa49mib13.cloudfront.net/spark-1.6.1-bin-hadoop1.tgz).
2. Edit _conf/spark-env.sh_ to set the HADOOP\_CONF\_DIR environment variable to point to the _conf_ directory of your Hadoop installation, so that it integrates with HDFS. Now by default, File I/O happens with HDFS.
3. Add the following line to `conf/spark-defaults.conf`, so that the necessary libraries are loaded each time Spark is started.
```
spark.jars.packages   com.databricks:spark-xml_2.10:0.3.5,org.apache.hadoop:hadoop-client:1.2.1,com.databricks:spark-csv_2.10:1.5.0
```

### Hadoop/HDFS
Not an essential per se, but the Big Data requirement(parsing of XML files running into GBs with Spark) alongwith the way TitanDB's bulk loading program works(only reads from HDFS) requires this.
1. Get your preferred format from Apache Archives of [Hadoop 1.2.1](https://archive.apache.org/dist/hadoop/common/hadoop-1.2.1/).
2. Conf files from in the `conf/hadoop` directory(in repo) to be used. Specifically, `dfs.data.dir` and `dfs.name.dir` need to be set to some location in the local filesystem APART FROM /tmp, since /tmp is cleared every time the machine powers down.

### Titan
3. Get Titan 1.0.0 from the [Downloads](https://github.com/thinkaurelius/titan/wiki/Downloads) page @ Titan.
4. Edit `bin/gremlin.sh` to ensure CLASSPATH includes the path to Hadoop's conf directory. Refer `bin/gremlin.sh`.
4. Copy the opencsv and groovycsv jars from the *lib* directory of the repo to lib directory of your TitanDB project dir.
4. `bin/gremlin.sh` from inside the project directory, and you're good to go.
