package org.nsidc.nutch.index.xmlnamespaces;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
//these three are needed for external file reading
/**
 Description: Extracts xml namespaces into a new Nutch field. 
 Requirements:  index-rawxml, index-more

 */

public class NamespaceIndexingFilter implements IndexingFilter {
	private Configuration conf;
	
	public static List<String> getAllMatches(String text, String regex) {
        List<String> matches = new ArrayList<String>();
        Matcher m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while(m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }

	
	public List<String> getNameSpaces(String xml)
	{
		List<String> matches = new ArrayList<String>();
		ArrayList<String> namespaces = new ArrayList<String>();		
		matches = getAllMatches(xml, "xmlns(.*?)=(\".*?\")");		
		for (String ns : matches){ // easier than using sets in Java
			if (!namespaces.contains(ns)) {
				namespaces.add(ns);
			}
		}
	
		return namespaces;
	}
	
	@Override
	public NutchDocument filter(NutchDocument doc, Parse parse, Text url,
			CrawlDatum datum, Inlinks inlinks) throws IndexingException {
		// If the raw_content is null or the document type do not contains xml then we just return the document as is.
		if (doc.getField("raw_content") == null || !doc.getField("type").getValues().contains("xml")) {
			return doc;
		}		
		String raw_xml_content = doc.getField("raw_content").getValues().get(0).toString();
		
		List<String> namespaces = getNameSpaces(raw_xml_content);
		for(String namespace : namespaces)
		{
			doc.add("xml_namespaces", namespace);
		}

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
