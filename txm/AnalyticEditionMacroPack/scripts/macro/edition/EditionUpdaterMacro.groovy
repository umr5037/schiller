package edition
// STANDARD DECLARATIONS

import org.kohsuke.args4j.*
import groovy.transform.Field
import org.txm.rcpapplication.swt.widget.parameters.*
import org.txm.objects.*
import org.txm.searchengine.cqp.corpus.*;
import org.w3c.dom.*
import org.txm.importer.*
import org.txm.Toolbox
import java.io.*

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.txm.rcpapplication.commands.*
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.txm.utils.FileCopy


if (!(corpusViewSelection instanceof MainCorpus)) {
	println "This marcro works with a MainCorpus selection. Aborting"
	return;
}
println "Working on $corpusViewSelection corpus"
def corpus = corpusViewSelection

// BEGINNING OF PARAMETERS

@Field @Option(name="xslEdition",usage="XSL to build the HTML edition - if empty uses 'txm-edition-xtz.xsl'", widget="File", required=false, def="")
def xslEdition

@Field @Option(name="xslPages",usage="XSL to build the edition pages - if empty uses 'txm-edition-page-split.xsl'", widget="File", required=false, def="")
def xslPages

@Field @Option(name="editionName",usage="The edition name to produce", widget="String", required=false, def="default")
String editionName

@Field @Option(name="useTokenizedDirectory",usage="Use the 'XML/w' of the 'tokenized' directory instead of the 'XML-TXM' files", widget="Boolean", required=false, def="false")
def useTokenizedDirectory = false

@Field @Option(name="debug",usage="Enable debug mode: temporary files are not deleted", widget="Boolean", required=false, def="false")
def debug = false
if (!ParametersDialog.open(this)) return;
// END OF PARAMETERS

if (editionName == null || editionName.length() == 0) {
	editionName = corpus.getDefaultEdition()
}
println "Edition name: $editionName"

File TXMHOME = new File(Toolbox.getParam(Toolbox.USER_TXM_HOME))
File xslDirectory = new File(TXMHOME, "xsl")
if (xslEdition == null || xslEdition.getName() == "txm-edition-xtz.xsl")
	xslEdition = new File(xslDirectory, "txm-edition-xtz.xsl")
if (xslPages == null || xslPages.getName() == "txm-edition-page-split.xsl")
	xslPages = new File(xslDirectory, "txm-edition-page-split.xsl")

println "XSLs: "+xslEdition.getName()+" & "+ xslPages.getName()
println ""

File binDirectory = corpus.getBase().getBaseDirectory()
File txmDirectory = new File(binDirectory, "txm/"+corpus.getName().toUpperCase())
if (useTokenizedDirectory) {
	println "Using the 'tokenized' directory instead of the 'txm' directory to get XML files"
	txmDirectory = new File(binDirectory, "tokenized")
}
File HTMLDirectory = new File(binDirectory, "HTML")
File HTMLCorpusDirectory = new File(HTMLDirectory, corpus.getName().toUpperCase())
File defaultEditionDirectory = new File(HTMLCorpusDirectory, editionName)

File cssDirectory = new File(TXMHOME, "css")
File cssDefaultEditionDirectory = new File(defaultEditionDirectory, "css")
File cssTXM = new File(cssDirectory, "txm.css")
File cssTEI = new File(cssDirectory, "tei.css")
boolean newEdition = false;

if (!txmDirectory.exists()) {
	println "ERROR: can't find this corpus 'txm' directory: $txmDirectory. Aborting"
	return false;
}
if (!defaultEditionDirectory.exists()) {
	println "This is a new edition"
	newEdition = true;
	defaultEditionDirectory.mkdir()
	if (!defaultEditionDirectory.exists()) {
		println "HTML directory could be created: $defaultEditionDirectory. Aborting"
		return false
	}
}
if (!cssTXM.exists()) {
	println "ERROR: can't find the $cssTXM CSS file. Aborting"
	return false;
}
if (!cssTEI.exists()) {
	println "ERROR: can't find the $cssTEI CSS file. Aborting"
	return false;
}
if (!xslEdition.exists()) {
	println "Error: can't find $xslEdition XSL file"
	return false;
}
if (!xslPages.exists()) {
	println "Error: can't find $xslPages XSL file"
	return false;
}

defaultEditionDirectory.mkdir()
cssDefaultEditionDirectory.mkdir()
FileCopy.copy(cssTXM, new File(cssDefaultEditionDirectory, cssTXM.getName()))
FileCopy.copy(cssTEI, new File(cssDefaultEditionDirectory, cssTEI.getName()))

//1- Back up current "HTML" directory
if (!newEdition) {
	File backupDirectory = new File(binDirectory, "HTML-"+defaultEditionDirectory.getName()+"-back")
	backupDirectory.mkdir()
	println "Backup of $defaultEditionDirectory directory to $backupDirectory..."
	for (File f : defaultEditionDirectory.listFiles()) {
		String name = f.getName()
		if (f.isDirectory() || f.isHidden()) continue
		
		File rez = new File(backupDirectory, f.getName())
		
		if (debug) println " file $f >> $rez"
		else print "."
		
		if (!FileCopy.copy(f, rez)) {
			println "Error: failed to backup $f"
			return false;
		}
	}
	println ""
}

//2- Apply edition XSL
println "Applying XSL 1: $xslEdition..."
ApplyXsl2 applier = new ApplyXsl2(xslEdition);
def htmlFiles = []
for (File f : txmDirectory.listFiles()) {
	String name = f.getName()
	String txtname = name.substring(0, name.indexOf("."));
	File rez = new File(HTMLCorpusDirectory, txtname+".html")

	if (!f.isDirectory() && !f.isHidden() && name.endsWith(".xml") && !name.equals("import.xml")) {
	
		if (debug) println " file $f >> $rez"
		else print "."
	
		if (!applier.process(f, rez)) {
			println "Error: failed to process $f"
			return false
		} else {
			htmlFiles << rez
		}
	}
}
println ""

//3- Apply pages XSL
println "Applying XSL 2: $xslPages..."
ApplyXsl2 applier2 = new ApplyXsl2(xslPages);
applier2.SetParam("editionname", editionName)
applier2.SetParam("cssname", corpus.getName())
for (File f : htmlFiles) {
	String name = f.getName()
	String txtname = name.substring(0, name.indexOf("."));
	File rez = new File(defaultEditionDirectory, txtname+"-pages.html")
	
	if (debug) println " file $f >> $rez"
	else print "."
	
	if (!applier2.process(f, rez)) {
		println "Error: failed to process $f"
		return false
	} else {
		if (!debug) rez.delete()
	}
}
println ""

// clean temp files
if (!debug) {
	for (File f : htmlFiles) {
		f.delete()
	}
}

//4- register new edition if any (copy edition)

if (editionName != corpus.getDefaultEdition()) {
	println "Update corpus configuration"
	BaseParameters parameters = corpus.getBase().params
	RemoveTag rt = new RemoveTag(
			parameters.root.getOwnerDocument(), // will be updated
			null, // don't create a new import.xml
			"//edition[@name='$editionName']"
			)

	def corpusElem = parameters.getCorpusElement()
	parameters.addEditionDefinition(corpusElem, editionName, "xsl", "XSLEditionBuilder");
	
	for (def text : corpus.getTexts()) {
		Element textElem = text.getSelfElement()
		def defaultEdition = text.getEdition(corpus.getDefaultEdition())
		if (defaultEdition == null) { println "Error: no default edition with name="+corpus.getDefaultEdition(); return false} 
		Element editionElem = parameters.addEdition(textElem, editionName, defaultEditionDirectory.getAbsolutePath(), "html");
		def pages = defaultEdition.getPages()
		for (int i = 1 ; i <= pages.size() ; i++) {
			def page = pages[i-1]
			parameters.addPage(editionElem, "$i", page.getWordId());
		}
	}
	
	File paramFile = new File(binDirectory, "import.xml");
	DomUtils.save(parameters.root.getOwnerDocument(), paramFile);
}

//5- Reload Corpora
Toolbox.restart();
monitor.syncExec(new Runnable() {
			public void run() {
				RestartTXM.reloadViews();
			}
		});
