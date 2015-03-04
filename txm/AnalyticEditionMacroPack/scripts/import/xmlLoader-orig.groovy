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
// $LastChangedDate: 2014-01-29 11:29:45 +0100 (Wed, 29 Jan 2014) $
// $LastChangedRevision: 2627 $
// $LastChangedBy: mdecorde $
//
package org.txm.importer.xml;

import javax.xml.stream.XMLStreamReader;

import org.txm.sw.RemoveTag;
import org.txm.importer.ApplyXsl2;
import org.txm.importer.ValidateXml;
import org.txm.importer.xml.importer;
import org.txm.importer.xml.compiler;
import org.txm.importer.xml.pager_old;
import org.txm.objects.*;
import org.txm.tokenizer.TokenizerClasses;
import org.txm.utils.*;
import org.txm.*;
import org.txm.scripts.teitxm.*;
import org.txm.utils.i18n.*;
import org.txm.metadatas.*;
import javax.xml.stream.*;
import org.w3c.dom.Element
import org.txm.importer.DomUtils;

String userDir = System.getProperty("user.home");

def MONITOR;
boolean debug = org.txm.utils.logger.Log.isPrintingErrors();
BaseParameters params;
try {params = paramsBinding;MONITOR=monitor} catch (Exception)
{	println "DEV MODE";//exception means we debug
	debug = true
	params = new BaseParameters(new File(userDir, "xml/bvh/bugnames/import.xml"))
	params.load()
	if (!org.txm.Toolbox.isInitialized()) {

		TokenizerClasses.loadFromNode(params.getTokenizerElement(params.getCorpusElement()));
		Toolbox.setParam(Toolbox.INSTALL_DIR,new File("/usr/lib/TXM"));
		//Toolbox.setParam(Toolbox.INSTALL_DIR,new File("C:\\Program Files\\TXM"));//For Windows
		Toolbox.setParam(Toolbox.TREETAGGER_INSTALL_PATH,new File(userDir,"treetagger"));
		//Toolbox.setParam(Toolbox.TREETAGGER_INSTALL_PATH,new File("C:\\Program Files\\treetagger"));//for Windows
		Toolbox.setParam(Toolbox.TREETAGGER_MODELS_PATH,new File(userDir,"treetagger/models"));
		Toolbox.setParam(Toolbox.METADATA_ENCODING, "UTF-8");
		Toolbox.setParam(Toolbox.METADATA_COLSEPARATOR, ",");
		Toolbox.setParam(Toolbox.METADATA_TXTSEPARATOR, "\"");
		//Toolbox.setParam(Toolbox.TREETAGGER_MODELS_PATH,new File("C:\\Program Files\\treetagger\\models"));//for Windows
		Toolbox.setParam(Toolbox.USER_TXM_HOME, new File(System.getProperty("user.home"), "TXM"));
	}
}
if (params == null) { println "no parameters. Aborting"; return; }

String corpusname = params.getCorpusName();
Element corpusElem = params.corpora.get(corpusname);
String basename = params.name;
String rootDir = params.rootDir;
String lang = corpusElem.getAttribute("lang");
String model = lang
String encoding = corpusElem.getAttribute("encoding");
boolean annotate = "true" == corpusElem.getAttribute("annotate");
String xsl = params.getXsltElement(corpusElem).getAttribute("xsl")
def xslParams = params.getXsltParams(corpusElem);

File srcDir = new File(rootDir);
File binDir = new File(Toolbox.getParam(Toolbox.USER_TXM_HOME), "corpora/"+basename);
binDir.deleteDir();
binDir.mkdirs();
if (!binDir.exists()) {
	println "Could not create binDir "+binDir
	return;
}

File txmDir = new File(binDir, "txm/$corpusname");
txmDir.deleteDir();
txmDir.mkdirs();

File propertyFile = new File(rootDir, "import.properties")//default
Properties props = new Properties();
String[] metadatasToKeep;

String textSortAttribute = null;
String paginationElement = null;
boolean normalizeMetadata = false;
String ignoredElements = null;
boolean stopIfMalformed = false;

println "Trying to read import properties file: "+propertyFile
if (propertyFile.exists() && propertyFile.canRead()) {
	InputStreamReader input = new InputStreamReader(new FileInputStream(propertyFile) , "UTF-8");
	props.load(input);
	input.close();
	if(props.getProperty("sortmetadata") != null)
		textSortAttribute = props.get("sortmetadata").toString();
	if (props.getProperty("editionpage") != null)
		paginationElement = props.get("editionpage").toString();
	if (props.getProperty("normalizemetadata") != null)
		normalizeMetadata = Boolean.parseBoolean(props.get("normalizemetadata").toString());
	if (props.getProperty("ignoredelements") != null)
		ignoredElements = props.get("ignoredelements").toString();
	if (props.getProperty("stopifmalformed") != null)
		stopIfMalformed = Boolean.parseBoolean(props.get("stopifmalformed").toString());

	println "import properties: "
	println " sort metadata: "+textSortAttribute
	println " edition page tag: "+paginationElement
	println " normalize attributes: "+normalizeMetadata
	println " ignored elements: "+ignoredElements
	println " stop if a XML source is malformed: "+stopIfMalformed
}

File allmetadatasfile = new File(srcDir, "metadata.csv");

// Apply XSL
if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (MONITOR != null) MONITOR.worked(1, "APPLYING XSL")
if (xsl != null && xslParams != null && xsl.trim().length() > 0) {
	if (ApplyXsl2.processImportSources(new File(xsl), srcDir, new File(binDir, "src"), xslParams))
	// return; // error during process
	srcDir = new File(binDir, "src");
	println ""
}

// copy xml+dtd files
if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
List<File> srcfiles = srcDir.listFiles();
if (srcfiles != null)
for (int i = 0 ; i < srcfiles.size() ; i++) {// check XML format, and copy file into binDir
	File f = srcfiles.get(i)
	if (f.getName().equals("import.xml") || f.getName().equals("metadata.csv") || f.getName().endsWith(".properties")) {
		srcfiles.remove(i);
		i--;
		continue;
	}
	if (ValidateXml.test(f)) {
		FileCopy.copy(f, new File(txmDir, f.getName()));
	} else {
		println "Won't process file "+f;
	}
}

if (txmDir.listFiles() == null) {
	println "No txm file to process"
	return;
}

// filtering
/*def xpaths = params.getExcludeXpaths()
if (xpaths != null) {
	println "Filtering XML files with xpaths: $xpaths"
	for (File infile : txmDir.listFiles()) {
		print "."
		if (!RemoveTag.xpath(infile, xpaths)) {
			println "Failed to filter $infile"
			return
		}
	}
	println ""
}*/

//get metadatas values from CSV
Metadatas metadatas; // text metadatas

println "Trying to read metadatas from: "+allmetadatasfile
if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (allmetadatasfile.exists()) {
	File copy = new File(binDir, "metadata.csv")
	if (!FileCopy.copy(allmetadatasfile, copy)) {
		println "Error: could not create a copy of metadata file "+allmetadatasfile.getAbsoluteFile();
		return;
	}
	metadatas = new Metadatas(copy, Toolbox.getParam(Toolbox.METADATA_ENCODING), Toolbox.getParam(Toolbox.METADATA_COLSEPARATOR), Toolbox.getParam(Toolbox.METADATA_TXTSEPARATOR), 1)
} else {
	println "no metadata file: "+allmetadatasfile
}

if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (MONITOR != null) MONITOR.worked(5, "IMPORTER")
println "-- IMPORTER - Reading source files"
def imp = new importer();
imp.doValidation(true) // change this to not validate xml
imp.doTokenize(true) // change this, to not tokenize xml
imp.setStopIfMalformed(stopIfMalformed);
if (!imp.run( srcDir, binDir, txmDir, basename, ignoredElements)) {
	println "import process stopped";
	return;
}

if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (MONITOR != null) MONITOR.worked(20, "INJECTING METADATA")
if (metadatas != null) {
	println("-- INJECTING METADATA - "+metadatas.getHeadersList()+" in texts of directory "+new File(binDir,"txm"))
	for (File infile : txmDir.listFiles()) {
		print "."
		File outfile = File.createTempFile("temp", ".xml", infile.getParentFile());
		if (!metadatas.injectMetadatasInXml(infile, outfile, "text", null)) {
			outfile.delete();
		} else {
			if (!(infile.delete() && outfile.renameTo(infile))) println "Warning can't rename file "+outfile+" to "+infile
			if (!infile.exists()) {
				println "Error: could not replace $infile by $outfile"
				return false;
			}
		}
	}
	println ""
}
List<File> files = txmDir.listFiles()
if (files == null || files.size() == 0) {
	return;
}

if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (MONITOR != null) MONITOR.worked(20, "ANNOTATE")
println "-- ANNOTATE - Running NLP tools"
boolean annotationSuccess = false;
if (annotate && new Annotate().run(binDir, txmDir, model+".par")) {
	annotationSuccess = true;
}

if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (MONITOR != null) MONITOR.worked(25, "COMPILING")
println "-- COMPILING - Building Search Engine indexes"
def c = new compiler();
if(debug) c.setDebug();
//c.setCwbPath("~/TXM/cwb/bin");
c.setOptions(textSortAttribute, normalizeMetadata);
c.setAnnotationSuccess(annotationSuccess)
c.setLang(lang);
if (!c.run(binDir, txmDir, corpusname, null, srcfiles, metadatas)) {
	println "import process stopped";
	return;
}

if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (MONITOR != null) MONITOR.worked(25, "EDITION")
println "-- EDITION - Building edition"
new File(binDir,"HTML/$corpusname").deleteDir();
new File(binDir,"HTML/$corpusname").mkdirs();
File outdir = new File(binDir,"/HTML/$corpusname/default/");
outdir.mkdirs();
List<File> filelist = txmDir.listFiles();
Collections.sort(filelist);
def second = 0

println "Paginating texts: "+filelist
for (File txmFile : filelist) {
	print "."
	String txtname = txmFile.getName();
	int i = txtname.lastIndexOf(".");
	if(i > 0) txtname = txtname.substring(0, i);

	List<String> NoSpaceBefore = LangFormater.getNoSpaceBefore(lang);
	List<String> NoSpaceAfter = LangFormater.getNoSpaceAfter(lang);

	Element text = params.addText(corpusElem, txtname, txmFile);

	def ed = new pager(txmFile, outdir, txtname, NoSpaceBefore, NoSpaceAfter, 300, basename, "pb");
	Element edition = params.addEdition(text, "default", outdir.getAbsolutePath(), "html");

	for (i = 0 ; i < ed.getPageFiles().size();) {
		File f = ed.getPageFiles().get(i);
		String wordid = ed.getIdx().get(i);
		params.addPage(edition, ""+(++i), wordid);
	}
}

if (MONITOR != null && MONITOR.isCanceled()) { return MONITOR.done(); }
if (MONITOR != null) MONITOR.worked(20, "FINALIZING")
File paramFile = new File(binDir, "import.xml");
DomUtils.save(params.root.getOwnerDocument(), paramFile);readyToLoad = true;