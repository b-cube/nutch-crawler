require "rspec/core/rake_task"
require "logger"

namespace :emr do

  @logger = Logger.new(STDOUT)
  @logger.level = Logger::WARN

  #  (bcube-test, bcube-seeds, 5, m1.large, 5, 2500, false, http://54.187.218.185:8983/solr/ )

  # reasonable max topn based on empiric tests:
  # cluster size = 4,  topn = 20000
  # cluster size = 8,  topn = 50000

  desc "Runs a new crawl/index cycle"
  task :crawl, [:s3_bucket, :s3_seeds, :ec2_nodes, :ec2_instance, :crawl_depth, :max_pages_per_level, :delete_segments_after_indexing, :save_data_back_to_s3, :solr_host] do |t, args|

    s3_bucket = args[:s3_bucket]
    s3_seeds = args[:s3_seeds]
    cluster_nodes = args[:ec2_nodes]
    master_type = args[:ec2_instance]
    slave_type = args[:ec2_instance]
    crawl_depth = args[:crawl_depth]
    max_pages_per_level = args[:max_pages_per_level]
    delete_segments_after_indexing = args[:delete_segments_after_indexing]
    save_crawled_data_to_s3 = args[:save_data_back_to_s3]
    solr_host = args[:solr_host]

    numFetchers = (cluster_nodes.to_i - 1).to_s

    job_properties = get_emr_properties(cluster_nodes, master_type, slave_type)
    hadoop_steps = get_hadoop_jar_steps(
      s3_bucket,
      s3_seeds,
      numFetchers, # number of fetchers has to be equal to the number of slave nodes
      crawl_depth,
      max_pages_per_level,
      delete_segments_after_indexing,
      save_crawled_data_to_s3,
      solr_host
    )

    bootstrap_actions = get_emr_bootstrap()

    puts "Running: aws emr  run-job-flow --name BCubeCrawl --instances '#{job_properties}' --steps '#{hadoop_steps}' --log-uri \"s3://#{s3_bucket}/logs\""

    run ("aws emr  run-job-flow --name BCubeCrawl --instances '#{job_properties}' --steps '#{hadoop_steps}' --log-uri \"s3://#{s3_bucket}/logs\"")

  end

  desc "Clear everything (yes, everything) out from the Solr instance"
  task :clear_solr, [:solr_host, :user, :password] do |t, args|
    run "curl -kv --user #{args[:user]}:#{args[:password]} '#{args[:solr_host]}/update?stream.body=<delete><query>*:*</query></delete>&commit=true'"
  end

  def run(command)
    @logger.info command
    system "#{command} >&2"
  end

  def get_hadoop_jar_steps (s3_bucket, s3_seeds, numFetchers, depth, topN, deleteSegments, save_data, solr_host)

    if save_data == "false"
      return "[
                { \"HadoopJarStep\":
                  {
                    \"MainClass\": \"org.apache.nutch.crawl.Crawl\",
                    \"Args\": [\"s3://#{s3_seeds}\", \"-dir\", \"crawl\", \"-depth\", \"#{depth}\", \"-solr\", \"#{solr_host}\" , \"-topN\", \"#{topN}\" , \"-fetchers\", \"#{numFetchers}\", \"-deleteSegments\", \"#{deleteSegments}\"],
                    \"Jar\": \"s3://bcube-nutch-job/apache-nutch-1.6.job\"
                  }, \"Name\": \"nutch-crawl\"
                }
              ]"
    else
      return "[
                { \"HadoopJarStep\":
                  {
                    \"MainClass\": \"org.apache.nutch.crawl.Crawl\",
                    \"Args\": [\"s3://#{s3_seeds}\", \"-dir\", \"crawl\", \"-depth\", \"#{depth}\", \"-solr\", \"#{solr_host}\" , \"-topN\", \"#{topN}\", \"-fetchers\", \"#{numFetchers}\", \"-deleteSegments\", \"#{deleteSegments}\"],
                    \"Jar\": \"s3://bcube-nutch-job/apache-nutch-1.6.job\"
                  }, \"Name\": \"nutch-crawl\"
                },
                { \"HadoopJarStep\":
                  {
                    \"MainClass\": \"org.apache.nutch.segment.SegmentMerger\",
                    \"Args\": [\"crawl/mergedsegments\", \"-dir\", \"crawl/segments\"],
                    \"Jar\": \"s3://bcube-nutch-job/apache-nutch-1.6.job\"
                  }, \"Name\": \"nutch-crawl\"
                },
                { \"HadoopJarStep\":
                  {
                    \"Args\": [\"--src\",\"hdfs:///user/hadoop/crawl/crawldb\",\"--dest\",\"s3://#{s3_bucket}/crawl/crawldb\",\"--srcPattern\",\".*\",\"--outputCodec\",\"snappy\"],
                    \"Jar\": \"s3://elasticmapreduce/libs/s3distcp/role/s3distcp.jar\"
                  }, \"Name\": \"crawlData2S3\"
                },
                { \"HadoopJarStep\":
                  {
                    \"Args\": [\"--src\",\"hdfs:///user/hadoop/crawl/linkdb\",\"--dest\",\"s3://#{s3_bucket}/crawl/linkdb\",\"--srcPattern\",\".*\",\"--outputCodec\",\"snappy\"],
                    \"Jar\": \"s3://elasticmapreduce/libs/s3distcp/role/s3distcp.jar\"
                  }, \"Name\": \"crawlData2S3\"
                },
                { \"HadoopJarStep\":
                  {
                    \"Args\": [\"--src\",\"hdfs:///user/hadoop/crawl/mergedsegments\",\"--dest\",\"s3://#{s3_bucket}/crawl/segments\",\"--srcPattern\",\".*\",\"--outputCodec\",\"snappy\"],
                    \"Jar\": \"s3://elasticmapreduce/libs/s3distcp/role/s3distcp.jar\"
                  }, \"Name\": \"crawlData2S3\"
                }
              ]".delete(" ")
    end
  end

  def get_emr_properties(cluster_nodes, master_type, slave_type)
    return "{
              \"InstanceCount\": #{cluster_nodes},
              \"MasterInstanceType\": \"#{master_type}\",
              \"HadoopVersion\": \"1.0.3\",
              \"KeepJobFlowAliveWhenNoSteps\": false,
              \"SlaveInstanceType\": \"#{slave_type}\",
              \"Ec2KeyName\": \"x1\"
            }".delete(" ")
  end

  def get_emr_bootstrap()
    return "[
             {
              \"Name\": \"Adjust MR\",
              \"ScriptBootstrapAction\": {
                  \"Path\": \"s3://elasticmapreduce/bootstrap-actions/configure-hadoop\",
                  \"Args\": [
                              \"--mapred-key-value\",\"mapred.tasktracker.map.tasks.maximum=16\",
                              \"--mapred-key-value\",\"mapred.tasktracker.reduce.tasks.maximum=8\"
                             ]
                  }
              }
            ]".delete(" ")
  end

end
