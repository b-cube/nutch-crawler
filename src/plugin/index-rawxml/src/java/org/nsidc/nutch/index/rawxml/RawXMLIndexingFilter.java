package org.nsidc.nutch.index.rawxml;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.parse.Parse;

/**
 * Adds raw_content from the org.nsidc.nutch.parse.rawxml.RawXmlParseFilter to a
 * Nutch document ready to be indexed in Solr
 */
public class RawXMLIndexingFilter implements IndexingFilter {

	private Configuration conf;

	public NutchDocument filter(NutchDocument doc, Parse parse, Text url,
			CrawlDatum datum, Inlinks inlinks) throws IndexingException {

		doc.add("raw_content", parse.getData().getMeta("raw_content"));

		return doc;

	}

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
}
