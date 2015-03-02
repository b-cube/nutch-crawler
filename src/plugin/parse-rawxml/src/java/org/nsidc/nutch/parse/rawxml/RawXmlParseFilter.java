package org.nsidc.nutch.parse.rawxml;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;

/**
 * Makes full raw content of XML (and HTML) documents available; since Solr is a
 * likely repository for the content, the whole document is wrapped in CDATA
 * tags.
 */
public class RawXmlParseFilter implements HtmlParseFilter {

	public final static String RAW_CONTENT = "raw_content";

	public static final Logger LOG = LoggerFactory
			.getLogger(RawXmlParseFilter.class);

	private Configuration conf;

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	@Override
	public ParseResult filter(Content content, ParseResult parseResult,
			HTMLMetaTags metaTags, DocumentFragment doc) {

		String url = content.getUrl();
		LOG.info("Getting rawxml content for " + url);

		Metadata metadata = parseResult.get(url).getData().getParseMeta();

		String rawContent = new String(content.getContent());
		String defangedContent = removeCdataSections(rawContent);
		metadata.add(RAW_CONTENT, wrapInCdata(defangedContent));

		return parseResult;
	}

	private String removeCdataSections(final String xml) {
		String regexp = "<!\\[CDATA\\[(^\\]\\]>)\\]\\]>";
		return xml.replaceAll(regexp, "");
	}

	private String wrapInCdata(String rawContent) {
		StringBuilder sb = new StringBuilder("<![CDATA[");
		sb.append(rawContent);
		sb.append("]]>");
		return sb.toString();
	}

}
