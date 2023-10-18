package sk.iway.iwcm.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.doc.DocDetails;

/**
 *  MapUtilsJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.10.2010 15:49:15
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MapUtilsJUnit
{
	@Test
	public void shouldCreateMap()
	{
		Map<String, String> map = MapUtils.asMap(
			"key", "value",
			"key2", "value2"
		);

		assertThat(map.keySet().contains("key"), is(true));
		assertThat(map.keySet().contains("key2"), is(true));
		assertThat(map.keySet().size(), is(2));

		assertThat(map.get("key"), is("value"));
		assertThat(map.get("key2"), is("value2"));
		assertThat(map.values().size(), is(2));
	}

	@Test
	public void propertyMap()
	{
		Map<String, DocDetails> pathsToDocuments = MapUtils.toMapWithPropertyAsKey(documents(), "virtualPath");

		assertThatIsPopulatedWell(pathsToDocuments);
	}

	@Test
	public void pairedMap()
	{
		Map<String, DocDetails> pathsToDocuments = MapUtils.toMap(documents(), new PairMaker<String, DocDetails, DocDetails>(){
			@Override
			public Pair<String, DocDetails> makePair(DocDetails source){
				return new Pair<String, DocDetails>(source.getVirtualPath(), source);
			}
		});

		assertThatIsPopulatedWell(pathsToDocuments);
	}

	private void assertThatIsPopulatedWell(Map<String, DocDetails> pathsToDocuments)
	{
		assertThat(pathsToDocuments.keySet().size(), is(2));
		assertThat(pathsToDocuments.values().size(), is(2));

		assertThat(pathsToDocuments.get("/sk/"), is(not(nullValue())));
		assertThat(pathsToDocuments.get("/sk/vedlajsia"), is(not(nullValue())));

		assertThat(pathsToDocuments.get("/sk/").getTitle(), is("Hlavna stranka"));
	}

	private List<DocDetails> documents()
	{
		List<DocDetails> documents = new ArrayList<DocDetails>();
		DocDetails doc = new DocDetails();
		doc.setTitle("Hlavna stranka");
		doc.setVirtualPath("/sk/");
		documents.add(doc);
		DocDetails doc2 = new DocDetails();
		doc2.setTitle("Vedlajsia stranka");
		doc2.setVirtualPath("/sk/vedlajsia");
		documents.add(doc2);
		return documents;
	}

	@Test
	public void shouldBeAbleToMergeTwoMaps()
	{
		Map<String, String> jeefsSettings = MapUtils.asMap("OS", "Mac", "browser", "Safari");
		Map<String, String> defaults = MapUtils.asMap("OS", "Win", "browser", "Internet Explorer", "type", "desktop");
		Map<String, String> merged = MapUtils.merge(jeefsSettings, defaults);
		assertThat(merged.get("OS"), is("Mac"));
		assertThat(merged.get("browser"), is("Safari"));
		assertThat(merged.get("type"), is("desktop"));
	}
}