package org.nsidc.nutch.parse.rawxml;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


public class RawXmlParseFilterTest {

	@Test
	public void filter_should_create_a_metadata_key_for_raw_content() {
		// arrange
		Metadata mockMetadata = mock(Metadata.class);		
		ParseResult mockParseResult = createMockParseResultWithMetadata(mockMetadata);
		Content fakeContent = createFakeContent("");
		
		RawXmlParseFilter parseFilter = new RawXmlParseFilter();
		
		// act
		parseFilter.filter(fakeContent, mockParseResult, null, null);
		
		// assert
		verify(mockMetadata).add(eq(RawXmlParseFilter.RAW_CONTENT), anyString()); 
	}
	
	//TODO: This is very similar to previous - should merge?
	@Test
	public void filter_should_wrap_content_in_CDATA() {
		// arrange
		String contentValue = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><some>xml</some>";
		Metadata mockMetadata = mock(Metadata.class);		
		ParseResult mockParseResult = createMockParseResultWithMetadata(
				mockMetadata);
		Content fakeContent = createFakeContent("", contentValue);
		
		RawXmlParseFilter parseFilter = new RawXmlParseFilter();
		
		// act
		parseFilter.filter(fakeContent, mockParseResult, null, null);
		
		// assert
		verify(mockMetadata).add(anyString(), startsWith("<![CDATA["));
		verify(mockMetadata).add(anyString(), endsWith("]]>"));
	}
	
	@Test
	public void filter_should_remove_CDATA_sections_in_input_documents() {
		// arrange
		String contentValue = "<some><![CDATA[<?blah ?>]]>cdata</some>";
		Metadata mockMetadata = mock(Metadata.class);		
		ParseResult mockParseResult = createMockParseResultWithMetadata(
				mockMetadata);
		Content fakeContent = createFakeContent("", contentValue);
		
		RawXmlParseFilter parseFilter = new RawXmlParseFilter();
		
		// act
		parseFilter.filter(fakeContent, mockParseResult, null, null);
		
		// assert
		ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockMetadata).add(anyString(), argumentCaptor.capture());
		String contentReceived = argumentCaptor.getValue();

		assertTrue(contentReceived.startsWith("<![CDATA"));
		assertTrue(contentReceived.endsWith("]]>"));
		assertTrue(contentReceived.contains("<some><![CDATA[<?blah ?>]]>cdata</some>"));
	}
	
	@Test
	public void filter_should_modify_and_return_the_same_ParseResult() {
		// arrange
		ParseResult mockParseResult = createMockParseResult();
		Content fakeContent = createFakeContent("");
		
		RawXmlParseFilter parseFilter = new RawXmlParseFilter();
		
		// act
		ParseResult returnedParseResult = parseFilter.filter(fakeContent, mockParseResult, null, null);
		
		// assert
		assertTrue(returnedParseResult.equals(mockParseResult));
	}

	@Test
	public void filter_should_get_the_ParseResult_based_on_the_Content_url() {
		// arrange
		final String url = "http://some,document.url/123?abc";
		ParseResult mockParseResult = createMockParseResult();
		Content fakeContent = createFakeContent(url);
		
		RawXmlParseFilter parseFilter = new RawXmlParseFilter();
		
		// act
		parseFilter.filter(fakeContent, mockParseResult, null, null);
		
		// assert
		verify(mockParseResult).get(eq(url));
	}
	
	
	private ParseResult createMockParseResultWithMetadata(Metadata mockMetadata) {
		ParseData parseData = new ParseData();
		Parse mockParse = mock(Parse.class);
		ParseResult mockParseResult = mock(ParseResult.class);
		
		if (mockMetadata != null) {
			parseData.setParseMeta(mockMetadata);
		}
		when(mockParse.getData()).thenReturn(parseData);
		when(mockParseResult.get(anyString())).thenReturn(mockParse);
		return mockParseResult;
	}

	private ParseResult createMockParseResult() {
		return createMockParseResultWithMetadata(null);
	}

	private Content createFakeContent(final String url) {
		return createFakeContent(url, null);
	}
	
	private Content createFakeContent(final String url, final String content) {
		byte[] contentByteArray = {}; 
		if (content != null) {
			contentByteArray = content.getBytes();
		}
		Content fakeContent = new Content(url, "", contentByteArray, null, mock(Metadata.class), mock(Configuration.class));
		return fakeContent;
	}
	
}
