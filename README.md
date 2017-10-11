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
