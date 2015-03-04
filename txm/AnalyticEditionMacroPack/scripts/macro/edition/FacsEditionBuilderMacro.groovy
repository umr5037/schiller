package edition

// STANDARD DECLARATIONS

import org.kohsuke.args4j.*
import groovy.transform.Field
import org.txm.rcpapplication.swt.widget.parameters.*
import org.txm.objects.*
import org.txm.searchengine.cqp.corpus.*;
import org.w3c.dom.*
import org.txm.importer.DomUtils
import org.txm.Toolbox
import java.io.*

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.txm.rcpapplication.commands.*
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


if (!(corpusViewSelection instanceof MainCorpus)) {
	println "This marcro works with a MainCorpus selection. Aborting"
	return;
}
println "Working on $corpusViewSelection corpus"
def corpus = corpusViewSelection


// BEGINNING OF PARAMETERS
@Field @Option(name="editionName",usage="the edition name to create", widget="String", required=true, def="facs")
def editionName

@Field @Option(name="tag",usage="The tag to upgrade", widget="String", required=true, def="pb")
def tag

@Field @Option(name="attribute",usage="The attribute to add", widget="String", required=true, def="facs")
def attribute

@Field @Option(name="debug",usage="Debug mode", widget="Boolean", required=false, def="true")
def debug = false

if (!ParametersDialog.open(this)) return;
// END OF PARAMETERS

File binDirectory = corpus.getBase().getBaseDirectory()
File txmDirectory = new File(binDirectory, "txm/"+corpus.getName().toUpperCase())
File tokenizedDirectory = new File(binDirectory, "tokenized")
File HTMLDirectory = new File(binDirectory, "HTML")
File newEditionDirectory = new File(HTMLDirectory, corpus.getName().toUpperCase()+"/"+editionName)

BaseParameters parameters = corpus.getBase().params

if (!HTMLDirectory.exists()) {
	println "ERROR: can't find this corpus 'HTML' directory: $HTMLDirectory. Aborting"
	return false;
}

File workDirectory = txmDirectory
if (!workDirectory.exists()) {
	println "XML-TXM directory ($txmDirectory) not found. Using XML tokenized directory instead: "+tokenizedDirectory
	workDirectory = tokenizedDirectory
}
if (!workDirectory.exists()) {
	println "XML tokenized directory not found: "+tokenizedDirectory
	println "Aborting."
	return false
}

println "Working directory=$workDirectory"

//0- clean previous edition if any : html files, import.xml
if (newEditionDirectory.exists()) {
	println "** Old version of $editionName edition found."
	println " removing the 'edition' reference from the corpus configuration."
	File tempParam = new File(binDirectory, "import.xml.cpy")
	RemoveTag rt = new RemoveTag(
			parameters.root.getOwnerDocument(), // will be updated
			null, // don't create a new import.xml
			"//edition[@name='$editionName']"
			)
	println " delete $newEditionDirectory"
	newEditionDirectory.deleteDir()
	
	//printDOM(parameters.root.getOwnerDocument())
}

//2- fix import.xml
println "** Updating corpus configuration..."
// for edition list
def corpusElem = parameters.getCorpusElement()
parameters.addEditionDefinition(corpusElem, editionName, "groovy", "FacsEditionBuilderMacro");

//1- create HTML files
println "** Building new edition HTML files..."

println " Creating edition '$editionName' directory: '$newEditionDirectory'"
newEditionDirectory.mkdir()
for (def xmlFile : workDirectory.listFiles()) {
	if (xmlFile.isHidden() || xmlFile.isDirectory()) continue // ignore
	String txtname = xmlFile.getName()
	int idx = txtname.indexOf(".")
	if (idx > 0) txtname = txtname.substring(0,idx)

	println " Building HTML pages of text=$txtname"
	BuildFacsEditions builder = new BuildFacsEditions(xmlFile, newEditionDirectory, parameters.name, txtname, tag, attribute, debug);
	def newPages = builder.process()
	if (newPages == null || newPages.size() == 0) {
		println "WARNING: no edition files created with $xmlFile"
	}
		
	println " Building edition references in corpus configuration"
	
	Element textElem = corpus.getText(txtname).getSelfElement()
	Element editionElem = parameters.addEdition(textElem, editionName, newEditionDirectory.getAbsolutePath(), "html");
	//println "$textElem $editionElem"
	for (def pagedef : newPages) {
		parameters.addPage(editionElem, pagedef[0], pagedef[1]);
	}
}

//printDOM(parameters.root.getOwnerDocument())

//3- Save and reload the corpus
println " Saving corpus configuration..."
File paramFile = new File(binDirectory, "import.xml");
DomUtils.save(parameters.root.getOwnerDocument(), paramFile);


//4- Reload Corpora
Toolbox.restart();
monitor.syncExec(new Runnable() {
			public void run() {
				RestartTXM.reloadViews();
			}
		});

//5- Done
println "New edition created."

//printDOM(parameters.root.getOwnerDocument())

def printDOM(def doc) {
	if (!debug) return;
	try {
		// Création de la source DOM
		Source source = new DOMSource(doc);
		
		// Création du fichier de sortie
		StreamResult resultat = new StreamResult(new PrintWriter(System.out));
		
		// Configuration du transformer
		TransformerFactory fabrique = TransformerFactory.newInstance();
		Transformer transformer = fabrique.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");  //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");  //$NON-NLS-1$
		
		// Transformation
		transformer.transform(source, resultat);
		// writer.close();
		return true;
	} catch (Exception e) {
		e.printStackTrace();
		return false;
	}
}