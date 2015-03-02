Nutch Crawler
=========
The BCube Crawler is a fork of the Apache Nutch project (version 1.9) tweaked to run on Amazon's ElasticMapReduce and optimized for web services and data discovery.


Motivation
----

Setting up a healthy Hadoop cluster is not always an easy task and the variables that make an application to perform well could add a lot of overhead to a project. ElasticMapReduce is -as its name suggests- a “MapReduce as a service” platform that allows users to create resizable Hadoop clusters and run MapReduce jobs. A key advantage of Amazon’s EMR platform is the flexibility to resize a cluster on the fly and the possibility of using spot instances to dynamically increase the computational power at lower costs. 

There are some limitations on EMR like preconfigured MR settings or that it only supports a set of [Hadoop distributions](http://docs.aws.amazon.com/ElasticMapReduce/latest/DeveloperGuide/emr-plan-hadoop-version.html). This and the fact that the “all-in-one” Crawl class was deprecated since Nutch 1.8 creates issues if we try to run Nutch using the EMR API.

In order to make use of the EMR API and automate our crawls we need to send a jar and a main class as entry point. This project adds back the main Crawl class deprecated in Nutch 1.6 and offers 3 important features when crawling using EMR

* **-fetchers**: Sets the number of nodes to use on each Fetch step
  This is important because on each round we can adjust the number of active nodes to match the current cluster size. Also because of the default values of mapred-site on EMR we ran into the issues described [here](http://stackoverflow.com/questions/10264183/why-does-nutch-only-run-the-fetch-step-on-one-hadoop-node-when-the-cluster-has) and this paramter will override the default 1 reducer for the generate-list step.

* **-deleteSegments**: If we don’t want to store parsed segments in our HDFS file system after they are indexed into Solr we set this parameter to true. This will save a lot of space in the cluster in case we don’t need the raw part-xxxxx files.

* **Solr Indexing**: The deprecated Crawl class indexed documents at the end of the crawl making the solr-indexer unstable for big crawls. The refactored class indexes documents into solr after each crawl round.

Running Nutch on Amazon's EMR
----

Running Nutch on Amazon's EMR is not a difficult thing to do, however we need to perform some steps before we can see indexed documents in a Solr instance. 

### Prerequsites

* [awscli](https://github.com/aws/aws-cli) via pip install
* [ant](http://ant.apache.org/) via brew or apt-get
* **JDK 1.6** openjdk or Oracle JDK

Before compiling Nutch make sure that you have the latest awscli client.

**Important**: The EMR API is not fully supported by awscli client and we need to activate it through our awscli configuration file (usually ~/.aws/config).
```sh
[default]
output = json
region = YOUR_AWS_REGION
aws_access_key_id = YOUR_ACCESS_KEY
aws_secret_access_key = YOUR_SECRET_KEY
[preview]
emr=true
```


### Compiling Nutch and uploading it to S3.

First we need to clone Nutch from NSIDC's repo and switch to the aws-emr branch.

```sh
git clone https://github.com/b-cube/nutch-crawler.git
cd nutch-crawler
```

Then we need to update the nutch-site.xml configuration file with the appropriate values, see this [Nutch tutorial](https://groups.drupal.org/node/105774) and [Nutch FAQ](http://wiki.apache.org/nutch/FAQ). Once we modify nutch-site.xml we proceed to compile Nutch with Ant.
Before we compile Nutch we need to set our JAVA_HOME to a working JRE environment. We can use `readlink -f $(which java)` to know our java path. 

```sh
ant clean runtime
```

This should create a new "runtime" folder with 2 directories, local and deploy. We are now ready to upload apache-nutch-1.6.job from the deploy directory to an AWS S3 bucket. Remember that EMR requires that our MR job/jar file is in a S3 bucket. 

```sh
aws s3 cp /path/to/apache-nutch-1.6.job s3://your-s3-bucket/
```
You also need to upload your seeds file to S3.

```sh
aws s3 cp /path/to/seeds.txt s3://your-s3-bucket/
```

Now we have all we need to run a crawl using EMR.

### Running a Nutch crawl on EMR.

Before launching a new crawl we should note some aspects of a web crawl and in particular a Nutch crawl.

* Nutch obeys robots.txt so be careful with the fetcher.max.crawl.delay property value. If your value is too low Nutch will discard most of the pages from long-waiting crawl delay websites.
* If you notice an uneven distribution of the fetch stem try different values for generate.max.per.host > 0. Since Nutch uses MR jobs it a cluster could be underutilized if a there is a single map running, see [Optimizing Crawls](https://wiki.apache.org/nutch/OptimizingCrawls).
* If you use/create [filter plugins](http://florianhartl.com/nutch-plugin-tutorial.html) remember to update your Solr schema every time you change or add a new field to index and restart your Solr instance.
* Nutch has 2 layers of configuration files, remember that *-site.xml will override *-default.xml
* Order matters, if we include a new plugin that adds a new field and that depends on another plugin then we should put it after its dependencies in the indexingfilter.order property on nutch-site.xml

An important thing to decide now is what instance type should we use for our crawls. EMR provides a variaty of EC2 instances and each comes with different MR capacities and pricing. In our case we picked **m1.medium** instances as they come with 2 mappers and one reducer and just enough ram for our Nutch jobs. You can see a list of available instance types [here](http://docs.aws.amazon.com/ElasticMapReduce/latest/DeveloperGuide/TaskConfiguration_H1.0.3.html) and their pricing [here](http://aws.amazon.com/elasticmapreduce/pricing/).

Given that we have a web-accessible Solr instance we are ready to start crawling using EMR. 


```sh
aws emr run-job-flow --name YOUR_CRAWL_NAME --instances $JOB_PROPERTIES --steps $HADOOP_STEPS --log-uri $YOUR_S3_BUCKET/log
```

* **YOUR_CRAWL_NAME**: The name of the Job in yout EMR console. It may describe the nature of your crawl.
* **$JOB_PROPERTIES**: A JSON hash with the following key/value pairs: 

        "{
            \"InstanceCount\": #{cluster_nodes},
            \"MasterInstanceType\": \"#{master_type}\",
            \"HadoopVersion\": \"1.0.3\",
            \"KeepJobFlowAliveWhenNoSteps\": false,
            \"SlaveInstanceType\": \"#{slave_type}\",
            \"Ec2KeyName\": \"x1\"
        }"

* **$HADOOP_STEPS**: A JSON array with the following key/value pairs:

        [
          { \"HadoopJarStep\":
            { 
          \"MainClass\": \"org.apache.nutch.crawl.Crawl\",
          \"Args\": [\"s3://YOUR_BUCKET/seeds.txt\",\"-dir\", \"crawl\", \"-depth\", \"$DEPTH\",\"-solr\", \"$SOLR\" , \"-topN\", \"$TOPN\", \"-fetchers\", \"$FETCHERS\", \"-deleteSegments\", \"$DELETESEG\"],
          \"Jar\": \"s3://YOUR_S3_BUCKET/apache-nutch-1.6.job\"}, 
          \"Name\": \"YOUR_CRAWL_NAME\"
           } 
        ]

    *   **-topN**: The maximun number of pages to crawl on each round.
    *   **-depth**: How many levels the crawl shouls go starting from the seed URLs.
    *   **-fetchers**: How many fetch list should be generated, hence how many nodes in the cluster should fetch. To optimize resources this value should be set to match the number of nodes in the cluster.
    *   **-deleteSegments**: Boolean, whether to delete all the indexed segments after each crawl or keep them.
    
To makes things easier The aws-emr branch comes with a **Rakefile** that can perform a call to AWS's EMR API for us.

**rake emr:crawl[PARAMS]**

* **S3 Bucket**: it defines where the EMR cluster will write its outputs (hadoop logs and crawled data) it has to be an S3 bucket defined in us-west-2.

* **S3 Bucket**: it defines where the seed list folder is, it has to be an S3 bucket defined in us-west-2.

* **Number of nodes**: defines the number of nodes for the cluster including the master node, effectively the core nodes will be n-1.  The number of nodes affects the overall performance of the crawl,
normally more is better however benchmarks have shown that crawling a **few domains** with more than 4 slaves becomes sub-optimal.

* **EC2 instance type**: defines what kind of EC2 instance type the EMR cluster.
For small or medium size cluster a m1.large type is more than enough. m1.medium types are normally a performing instance as well(not to mention that they are cheaper).

* **Crawling Depth**: Defines the number of levels to crawl from the initial seed list, this is better visualized here (slide 15), from 1 to 5 we will hardly find interesting xml files unless they are 
offered in the index page or the pages used in the seed list. Appropriate values range from 5 to 20 levels.

* **Max Documents per Level**: This is the top value of documents to fetch on each level, if there are more pages in a level than this value they will be ignored. The pages are ordered using a modified 
version of the page rank algorithm, details of its implementation can be found [here](https://wiki.apache.org/nutch/NewScoring).

* **Delete Segments After Indexing**: A boolean value telling Nutch if it should delete the parsed segments from HDFS after they are indexed into Solr.

* **Save Crawled Data**: A boolean value telling the cluster if it should copy the crawled data to an S3 bucket before the cluster gets destroyed after completed a crawl. 
The crawled data is not necessary unless we want all of the documents crawled and not just the xml files that are indexed.

* **Solr URL**: The Solr url where the found xml documents are going to be indexed. This is a temporary setup, in the near future we'll have predefined domains with basic authentication. 
Solr is used because the EMR branch comes from the 1.6 branch with no ElasticSearch support yet.

**Example**

```ruby
rake emr:crawl[your_s3_bucket,your_s3_bucket/urls,9,m1.medium,10,100000,false,false,http://your_solr_server:port/solr]
```
**note**: if you use zsh or a different shell you might need to scape the [ ] symbols

What this Rake task is doing is creating a new EMR cluster using Nutch's job file in your s3 bucket and your seed list as inputs. The output should be documents indexed into your Solr instance and Hadoop logs in your s3 bucket.

If you want to keep the content of the crawl for future usage or analysis you can do it by setting to true the Save_Crawled_Data parameter. This will copy all of Nutch's data structures back to your s3 bucket. 

**note**: Copying data back to an s3 bucket can take some time and depending on the size of your crawl it could be a lot of Gigabytes.

### NSIDC custom plugins


The aws-emr branch comes with a couple plugins that might be useful if we want to store raw content of a document into Solr.

* [NSIDC raw content plugins](https://github.com/nsidc/libre-nutch-raw-xml-plugin)

Since they are included in the branch we just need to add the correspondandt fields into our Solr schema and activate the plugins in our plugins chain on nutch-site.xml.



## TODO

* Add EMR spot instances to the Rake crawl task based on current price estimates.
* Modify the bcube-filter to discard documents based on regex expressions against document field values i.e. "mimetype like json*"
* Crawl with initial CrawlDB and LinkDB stored in AWS S3 from previous crawls.
* Create an ad-hoc scoring plugin based on words frequency.

Credits
------

This project is being developed by NSIDC under the NSF grant number [1343802](http://www.nsf.gov/awardsearch/showAward?AWD_ID=1343802)

(C) The National Snow and Ice Data Center 2014.

License
----

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
