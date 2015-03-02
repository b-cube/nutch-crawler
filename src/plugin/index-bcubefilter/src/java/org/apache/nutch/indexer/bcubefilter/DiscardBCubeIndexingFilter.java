/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nutch.indexer.bcubefilter;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.parse.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Discards a document if the content is empty
 */
public class DiscardBCubeIndexingFilter implements IndexingFilter {
  public static final Logger LOG = LoggerFactory.getLogger(DiscardBCubeIndexingFilter.class);

  private Configuration conf;

 /**
  * The {@link DiscardBCubeIndexingFilter} filter object 
  *  
  * @param doc The {@link NutchDocument} object
  * @param parse The relevant {@link Parse} object passing through the filter 
  * @param url URL to be filtered for anchor text
  * @param datum The {@link CrawlDatum} entry
  * @param inlinks The {@link Inlinks} containing anchor text
  * @return filtered NutchDocument
  */
  public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks)
    throws IndexingException {

    // types
    List<String> xml_types = new ArrayList<String>();
    
    xml_types.add("xml");
    xml_types.add("json");
    xml_types.add("text/xml");
    xml_types.add("application/xml");
    xml_types.add("application/json");    
    xml_types.add("application/opensearchdescription+xml");
    xml_types.add("application/opensearch+xml");
    
    if (doc.getField("type") != null) {    
	    for (Object mimeType : doc.getField("type").getValues()) {
	      if (mimeType != null) {
	    	  if (xml_types.contains(mimeType.toString())) {
	    		  return doc;
	    	  }
	      }
	    }
	    return null;
    } else {
      LOG.error("The index-more plugin should be added before this plugin in indexingfilter.order");
      return doc;
    }
//    LOG.debug(types.toString());
  }

  /**
   * Set the {@link Configuration} object
   */
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  /**
   * Get the {@link Configuration} object
   */
  public Configuration getConf() {
    return this.conf;
  }

}
