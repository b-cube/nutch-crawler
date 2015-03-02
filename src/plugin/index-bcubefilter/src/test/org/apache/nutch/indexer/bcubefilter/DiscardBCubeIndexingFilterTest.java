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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

public class DiscardBCubeIndexingFilterTest {

	@Test
	public void DiscardBCubeIndexingFilter_wont_discard_xml_documents() throws Exception {
		Configuration conf = NutchConfiguration.create();
		conf.setBoolean("moreIndexingFilter.indexMimeTypeParts", true);
		DiscardBCubeIndexingFilter filter = new DiscardBCubeIndexingFilter();
		filter.setConf(conf);

		NutchDocument doc = new NutchDocument();
		
		doc.add("type", "text/xml");
		doc.add("type", "application/xml");
		doc.add("type", "application/opensearchdescription+xml");

		Parse parse = mock(Parse.class);
		Metadata metadata = new Metadata();
		ParseData parseData = new ParseData();
		parseData.setParseMeta(metadata);

		// Mock parser response
		when(parse.getData()).thenReturn(parseData);

		filter.filter(doc, parse, null, null, null);
		
		assertNotNull(doc);
		
		assertTrue(doc.getFieldNames().contains("type"));
		NutchField contentTypeField = doc.getField("type");
		
		String typeValue = contentTypeField.getValues().get(0).toString();
		assertTrue(typeValue.equals("text/xml"));
	}

	@Test
	public void DiscardBCubeIndexingFilter_will_discard_non_xml_documents() throws Exception {
		Configuration conf = NutchConfiguration.create();
		conf.setBoolean("moreIndexingFilter.indexMimeTypeParts", true);
		DiscardBCubeIndexingFilter filter = new DiscardBCubeIndexingFilter();
		filter.setConf(conf);

		NutchDocument doc = new NutchDocument();
		
		doc.add("type", "text/html");
		doc.add("type", "application/javascript");

		Parse parse = mock(Parse.class);
		Metadata metadata = new Metadata();
		ParseData parseData = new ParseData();
		parseData.setParseMeta(metadata);

		// Mock parser response
		when(parse.getData()).thenReturn(parseData);

		doc = filter.filter(doc, parse, null, null, null);
		
		assertNull(doc);
	}
}
