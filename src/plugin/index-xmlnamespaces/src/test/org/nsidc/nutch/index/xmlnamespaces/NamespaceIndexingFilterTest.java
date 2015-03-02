package org.nsidc.nutch.index.xmlnamespaces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Test;

public class NamespaceIndexingFilterTest {

	public String buildXMLString(ArrayList<String> namespaces)
	{
		//concatinate each thing in ArrayList, to create a valid XML document
		// create a string with opening xml tags, itterate over namespaces, and add each "thing" into the xml tags

		String openingXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed";
		String endingXML = "></feed>";
		String namespaceHolder = "";
		for(String namespace : namespaces)
		{
			namespaceHolder = namespaceHolder + " " + namespace; 
		}
		String s = openingXML + namespaceHolder + endingXML;

		return s;
	}
	
	
	@Test
	public void test_xml_construction() {

		Configuration conf = NutchConfiguration.create();
		NamespaceIndexingFilter filter = new NamespaceIndexingFilter();
		filter.setConf(conf);

		ArrayList<String> namespaces = new ArrayList<String>();
		String validXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed "
				+ "xmlns:oai-identifier=\"http://www.openarchives.org/OAI/2.0/oai-identifier\" "
				+ "xmlns:mingo-identifier=\"http://www.google.com\" "
				+ "xmlns:abeve-identifier=\"http://www.news.ycombinator.org/OAI/2.0/oai-identifier\">"
				+ "</feed>";

		namespaces.add("xmlns:oai-identifier=\"http://www.openarchives.org/OAI/2.0/oai-identifier\"");
		namespaces.add("xmlns:mingo-identifier=\"http://www.google.com\"");
		namespaces.add("xmlns:abeve-identifier=\"http://www.news.ycombinator.org/OAI/2.0/oai-identifier\"");
		String testXML = buildXMLString(namespaces);
		assertEquals("Invalid XMl", testXML, validXML);			
					
	}
	
	@Test
	public void getNameSpaces_can_extract_NameSpaces() throws IndexingException {

		Configuration conf = NutchConfiguration.create();
		NutchDocument doc = new NutchDocument();
		NamespaceIndexingFilter namespaceFilter = new NamespaceIndexingFilter();
		namespaceFilter.setConf(conf);
		
		ArrayList<String> namespaces = new ArrayList<String>();
		namespaces.add("xmlns:oai-identifier=\"http://www.openarchives.org/OAI/2.0/oai-identifier\"");
		namespaces.add("xmlns:some-identifier=\"http://www.google.com\"");
		namespaces.add("xmlns:a-identifier=\"http://www.someurl.com\"");
		//we have a doc with xml content that we would actually if we ran a filter	
		String stringTest = buildXMLString(namespaces);
		doc.add("raw_content", stringTest);
		List<String> namespacesFound = namespaceFilter.getNameSpaces(stringTest);
		ArrayList<String> fieldNamespace = new ArrayList<String>();
		fieldNamespace.add("xmlns:oai-identifier=\"http://www.openarchives.org/OAI/2.0/oai-identifier\"");
		fieldNamespace.add("xmlns:some-identifier=\"http://www.google.com\"");
		fieldNamespace.add("xmlns:a-identifier=\"http://www.someurl.com\"");
		
		assertEquals("Invalid XML", fieldNamespace, namespacesFound);

	}
	
	@Test
	public void filter_dont_crash_with_non_xml_documents() throws IndexingException {

		Configuration conf = NutchConfiguration.create();
		NutchDocument doc = new NutchDocument();
		NamespaceIndexingFilter namespaceFilter = new NamespaceIndexingFilter();
		namespaceFilter.setConf(conf);
		
		doc.add("type", "pdf");
		
		NutchDocument returnedDoc = namespaceFilter.filter(doc, null, null, null, null);
		
		assertNotNull(returnedDoc);
		assertNull(returnedDoc.getField("xml_namespaces"));
	}
	
	

	@Test
	public void filter_does_not_index_duplicated_namespaces() throws IndexingException {

		Configuration conf = NutchConfiguration.create();
		NutchDocument doc = new NutchDocument();
		NamespaceIndexingFilter namespaceFilter = new NamespaceIndexingFilter();
		namespaceFilter.setConf(conf);
		
		ArrayList<String> namespaces = new ArrayList<String>();
		namespaces.add("xmlns:duplicated=\"http://dummy.org\"");
		namespaces.add("xmlns:duplicated=\"http://dummy.org\"");
		namespaces.add("xmlns:random=\"http://www.someurl.com\"");
		//we have a doc with xml content that we would actually if we ran a filter	
		String raw_xml = buildXMLString(namespaces);		
		
		doc.add("type", "xml");
		doc.add("raw_content", raw_xml);
		
		NutchDocument returnedDoc = namespaceFilter.filter(doc, null, null, null, null);
		
		assertNotNull(returnedDoc);
		List<Object> parsed_namespaces = returnedDoc.getField("xml_namespaces").getValues();
		assertTrue(parsed_namespaces.size() == 2);
		
		assertEquals ("xmlns:duplicated=\"http://dummy.org\"", parsed_namespaces.get(0).toString());
		assertEquals ("xmlns:random=\"http://www.someurl.com\"", parsed_namespaces.get(1).toString());
	}
}


