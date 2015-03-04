package edition

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.txm.importer.*

/**
 * Add a attribute value map in a XML file
 * Warning: if an attribute already exists its value won't be changed
 */
class AddAttributeValuesInXML extends StaxIdentityParser {
	File xmlFile;
	String tag, attribute;
	List<String> values;
	HashMap<String, String> attributesMap;
	boolean result;

	public AddAttributeValuesInXML(File xmlFile, String tag, String attribute, List<String> values)
	{
		super(xmlFile.toURI().toURL());
		this.xmlFile = xmlFile;
		this.tag = tag;
		this.attribute = attribute;
		this.values = values;
	}

	public boolean process(File outfile) {
		this.result = false;
		boolean ret = super.process(outfile)
		return this.result & ret;
	}

	
	/**
	 * Rewrite the processStartElement() to update/add attributes
	 */
	int n = 0;
	public void processStartElement()
	{
		if (localname != tag) {
			super.processStartElement()
		} else {
			String prefix = parser.getPrefix();
//TODO: uncomment for TXM 0.7.6
//			if (INCLUDE == localname && XI == prefix) {
//				processingXInclude();
//				return;
//			}

			if (prefix.length() > 0)
				writer.writeStartElement(Nscontext.getNamespaceURI(prefix), localname)
			else
				writer.writeStartElement(localname);

			for (int i = 0 ; i < parser.getNamespaceCount() ; i++) {
				writer.writeNamespace(parser.getNamespacePrefix(i), parser.getNamespaceURI(i));
			}

			// get attributes
			HashMap<String, String> attributes = new HashMap<String, String>();
			for (int i = 0 ; i < parser.getAttributeCount() ; i++) {
				attributes[parser.getAttributeLocalName(i)] = parser.getAttributeValue(i);
			}
			// add/update the value
			if (n < values.size()) {
				attributes[attribute] = values[n]; 
			} else {
				println "ERROR: not enough values to insert for file $xmlFile, at XML parser location: l="+parser.getLocation().getLineNumber()+",c="+parser.getLocation().getColumnNumber()+")."
			}
			n++
			
			// write attributes
			for (def k : attributes.keySet()) {
				writer.writeAttribute(k, attributes[k])
			}
		}
	}
	
	@Override
	public void after() {
		super.after();
		
		if (n != values.size()) {
			println "ERROR: number of $tag ("+n+") missmatch the number of values to insert: "+values.size()
		}
	}
}
