package wiki_search;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

class UserHandler extends DefaultHandler {

	boolean bId = false;
	boolean bTitle = false;
	boolean bText = false;
	boolean idFound = false;
	String id, title;
	StringBuilder text;

	@Override
	public void startElement(
			String uri, String localName, String qName, Attributes attributes)
					throws SAXException {

		if (qName.equalsIgnoreCase("id") && !idFound) {
			bId = true;
			idFound = true;
		} else if (qName.equalsIgnoreCase("title")) {
			bTitle = true;
		}
		else if(qName.equalsIgnoreCase("redirect")) {
			title = attributes.getValue("title");
		}
		else if (qName.equalsIgnoreCase("text")) {
			text = new StringBuilder();
			bText = true;
		} 
	}

	@Override
	public void endElement(String uri, 
			String localName, String qName) throws SAXException {
		
		//to handle last few articles (not multiple of 100)
		if(qName.equalsIgnoreCase("mediawiki")) {
			PreProcess pObj = new PreProcess();
			pObj.processPage(id, title, text.toString(), true);
		}
		else if (qName.equalsIgnoreCase("page")) {
			//System.out.println("End Element :" + qName);
			idFound = false;
			//System.out.println(title);
			PreProcess pObj = new PreProcess();
			pObj.processPage(id, title, text.toString(), false);
		}
		else if(qName.equalsIgnoreCase("text")) {
			bText = false;
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {

		if (bId) {
			//System.out.println("ID: " + new String(ch, start, length));
			id = new String(ch, start, length);
			bId = false;
		} else if (bTitle) {
			//System.out.println("Title: " + new String(ch, start, length));
			title = new String(ch, start, length);
			bTitle = false;
		} else if (bText) {
			text.append(new String(ch, start, length));
		} 
	}
}