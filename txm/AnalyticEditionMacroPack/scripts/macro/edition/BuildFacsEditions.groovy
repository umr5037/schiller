package edition

import org.txm.importer.*
import org.xml.sax.Attributes
import org.txm.importer.filters.*
import java.util.ArrayList
import javax.xml.parsers.*
import javax.xml.stream.*
import java.net.URL
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

class BuildFacsEditions {

	private def url
	private def inputData
	private def factory
	private XMLStreamReader parser
	OutputStreamWriter writer
	StaxStackWriter pagedWriter = null

	File editionDir
	File xmlFile
	File htmlFile
	def pages = []
	def tag, attribute, txtname, corpusname
	boolean firstWord
	boolean cutBefore = true;
	boolean debug = false;
	
	public BuildFacsEditions(File xmlFile, File editionDir, String corpusname, String txtname, String tag, String attribute, boolean debug) {
		inputData = xmlFile.toURI().toURL().openStream()
		factory = XMLInputFactory.newInstance()
		parser = factory.createXMLStreamReader(inputData)
		
		this.xmlFile = xmlFile
		this.editionDir = editionDir
		this.tag = tag
		this.attribute = attribute
		this.txtname = txtname
		this.debug = debug
	}
	
	int n = 0;
	private boolean createNextOutput()
	{
		try {
			def tags = closeMultiWriter();
			for (int i = 0 ; i < tags.size() ; i++) {
				String tag = tags[i]
				if ("body" != tag) {
					tags.remove(i--)
				} else {
					tags.remove(i--) // remove "body"
					break; // remove elements until "body tag
				}
			}
			if (wordid == null) wordid = "w_0";
			println " add page $n $wordid"
			pages << ["$n", wordid]
			
			// Page suivante
			
			htmlFile = new File(editionDir, "${txtname}_${n}.html")
			firstWord = true

			pagedWriter = new StaxStackWriter(new OutputStreamWriter(new FileOutputStream(htmlFile) , "UTF-8"));
			if (debug) println "Create file $htmlFile"
			pagedWriter.writeStartDocument("UTF-8", "1.0")
			pagedWriter.writeStartElement("html");
			pagedWriter.writeEmptyElement("meta", ["http-equiv":"Content-Type", "content":"text/html","charset":"UTF-8"]);
			pagedWriter.writeEmptyElement("link", ["rel":"stylesheet", "type":"text/css","href":"txm.css"]);
			pagedWriter.writeEmptyElement("link", ["rel":"stylesheet", "type":"text/css","href":"${corpusname}.css"]);
			pagedWriter.writeStartElement("head");
			pagedWriter.writeStartElement("title")
			pagedWriter.writeCharacters(corpusname+" Edition - Page "+n)
			pagedWriter.writeEndElement(); // </title>
			pagedWriter.writeEndElement() // </head>
			pagedWriter.writeStartElement("body") //<body>

			pagedWriter.writeStartElements(tags);
			
			n++
			return true;
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
	}
	
	private def closeMultiWriter()
	{
		if (pagedWriter != null) {
			def tags = pagedWriter.getTagStack().clone();

			if (firstWord) { // there was no words
				pagedWriter.writeCharacters("");
				pagedWriter.write("<span id=\"w_0\"/>");
			}
			pagedWriter.writeEndElements();
			pagedWriter.close();
			return tags;
		} else {
			return [];
		}
	}
	
	private writeImg(String src) {
		pagedWriter.writeStartElement("div");
		pagedWriter.writeEmptyElement("img", ["src":src, "width":"100%"]);
		pagedWriter.writeEndElement(); // </div>
	}
	
	String wordid = "w_0"
	public def process() {
		
		boolean start = false
		String localname
		
		createNextOutput();
		for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
			switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					localname = parser.getLocalName();
					switch (localname) {
						case "text":
							start = true
						break;
						case "w":
							if (firstWord) {
								wordid = parser.getAttributeValue(null, "id");
								firstWord = false;
							}
						break;
						case tag:
							if (debug) println "** TAG $tag $attribute"
							String imgPath = parser.getAttributeValue(null, attribute);
							if (imgPath == null) {
								println "ERROR in $xmlFile no value found for $tag@$attribute at location "+parser.getLocation().getLineNumber()
							} else {
								if (cutBefore) {
									if (debug) println " cut before"
									createNextOutput()
									if (debug) println " write img $imgPath"
									writeImg(imgPath)
								} else {
									if (debug) println " write img $imgPath"
									writeImg(imgPath)
									if (debug) println " cut after"
									createNextOutput()
								}
							}
						break;
					}
				break;
			}
		}
		closeMultiWriter()
		return pages
	}
}