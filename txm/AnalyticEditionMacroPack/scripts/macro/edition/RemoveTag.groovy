// Copyright © 2010-2013 ENS de Lyon.
// Copyright © 2007-2010 ENS de Lyon, CNRS, INRP, University of
// Lyon 2, University of Franche-Comté, University of Nice
// Sophia Antipolis, University of Paris 3.
// 
// The TXM platform is free software: you can redistribute it
// and/or modify it under the terms of the GNU General Public
// License as published by the Free Software Foundation,
// either version 2 of the License, or (at your option) any
// later version.
// 
// The TXM platform is distributed in the hope that it will be
// useful, but WITHOUT ANY WARRANTY; without even the implied
// warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE. See the GNU General Public License for more
// details.
// 
// You should have received a copy of the GNU General
// Public License along with the TXM platform. If not, see
// http://www.gnu.org/licenses.
// 
// 
// 
// $LastChangedDate:$
// $LastChangedRevision:$
// $LastChangedBy:$ 
//
package edition;

import javax.xml.parsers.*
import javax.xml.transform.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.*

import org.txm.metadatas.*
import org.txm.utils.*
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Removes tags of XML file given a XPath. 
 * @author mdecorde
 *
 */
public class RemoveTag {
	File outfile
	String xpath
	Document doc
	
	public RemoveTag(def root, def outfile, def xpath)
	{
		this.doc = root
		this.outfile = outfile
		this.xpath = xpath
		
		process()
	}
	
	/**
	 *
	 * @param xmlfile the xmlfile
	 * @param outfile the outfile
	 * @param xpath the XPath
	 */
	public RemoveTag(File xmlfile, File outfile, String xpath)
	{
		this.outfile = outfile
		this.xpath = xpath
		
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		this.doc = builder.parse(xmlfile);
		
		process()
	}
	
	private void process() {
		def expr = XPathFactory.newInstance().newXPath().compile(xpath);
		def nodes = expr.evaluate(doc, XPathConstants.NODESET);
		
		if (nodes != null)
		for(def node : nodes)
		{
			//println "Remove node "+node
			Element elem = (Element)node;
			elem.getParentNode().removeChild(node);
		}
		save()
		doc = null
	}
	
	/**
	 * Save.
	 *
	 * @return true, if successful
	 */
	private boolean save()
	{
		if (outfile == null) return true;
	
		try {
			// Création de la source DOM
			Source source = new DOMSource(doc);
			
			// Création du fichier de sortie
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")); 
			Result resultat = new StreamResult(writer);
			
			// Configuration du transformer
			TransformerFactory fabrique = TransformerFactory.newInstance();
			Transformer transformer = fabrique.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); 
			
			// Transformation
			transformer.transform(source, resultat);
			writer.close();
			return true;
		} catch (Exception e) {
			org.txm.utils.logger.Log.printStackTrace(e);
			return false;
		}
	}
	
	public static void main(String[] args) {
		RemoveTag rt = new RemoveTag(
			new File("/home/mdecorde/TXM/corpora/graal/import.xml"),
			new File("/home/mdecorde/TXM/corpora/graal/import-o.xml"),
			"//edition[@name='courante']"
			)
	}
}
