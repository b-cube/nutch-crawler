package org.nsidc.nutch.index.rawxml;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.NutchField;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Test;

public class RawXMLIndexingFilterTest {

	@Test
	public void testIndexingFilterAdds_raw_content_Field() throws Exception {
		Configuration conf = NutchConfiguration.create();
		conf.setBoolean("moreIndexingFilter.indexMimeTypeParts", false);
		RawXMLIndexingFilter filter = new RawXMLIndexingFilter();
		filter.setConf(conf);

		NutchDocument doc = new NutchDocument();

		Parse parse = mock(Parse.class);
		Metadata metadata = new Metadata();
		metadata.set("raw_content", "Some value");
		ParseData parseData = new ParseData();
		parseData.setParseMeta(metadata);

		// Mock parser response
		when(parse.getData()).thenReturn(parseData);

		filter.filter(doc, parse, null, null, null);
		
		assertTrue(doc.getFieldNames().contains("raw_content"));
		
		NutchField rawContentField = doc.getField("raw_content");
		String rawContentValue = rawContentField.getValues().get(0).toString();
		assertTrue(rawContentValue.equals("Some value"));
	}
}
