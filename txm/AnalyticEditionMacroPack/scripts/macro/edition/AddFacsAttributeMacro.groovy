package edition

import org.kohsuke.args4j.*
import groovy.transform.Field
import org.txm.rcpapplication.swt.widget.parameters.*

// BEGINNING OF PARAMETERS
@Field @Option(name="sourceDirectory",usage="Directory containig XML-TXM or XML SRC files", widget="Folder", required=true, def="/home/mdecorde/xml/macroeditions/src")
def sourceDirectory

@Field @Option(name="imageDirectory",usage="directory containing the ordered images files in subdirectories", widget="Folder", required=true, def="/home/mdecorde/xml/macroeditions/img")
def imageDirectory

@Field @Option(name="outputDirectory",usage="Output directory", widget="Folder", required=true, def="/home/mdecorde/xml/macroeditions/out")
def outputDirectory

@Field @Option(name="element",usage="The element to upgrade", widget="String", required=true, def="pb")
def element

@Field @Option(name="attribute",usage="The attribute to add", widget="String", required=true, def="facs")
def attribute

@Field @Option(name="prefix",usage="The image path prefix, if empty then the absolute file path is used", widget="String", required=true, def="../img/")
def prefix

// Open the parameters input dialog box
if (!ParametersDialog.open(this)) return;
// END OF PARAMETERS

println "Parameters: "
println " sourceDirectory: $sourceDirectory"
println " imageDirectory: $imageDirectory"
println " outputDirectory: $outputDirectory"
println " element: $element"
println " attribute: $attribute"

assert(sourceDirectory.exists())
assert(imageDirectory.exists())

outputDirectory.deleteDir()
outputDirectory.mkdir()

if (!outputDirectory.exists()) {
	println "Error: failed to create $outputDirectory"
	return;
}

def srcFiles = []
sourceDirectory.eachFile() { file ->
	if (!file.isDirectory() && !file.isHidden() && file.getName().endsWith(".xml") && file.getName() != "import.xml") srcFiles << file
}
println "srcFiles=$srcFiles"

for (def subdir : imageDirectory.listFiles()) {
	if (!subdir.isDirectory()) continue;
	
	String name = subdir.getName();
	File srcFile = new File(sourceDirectory, name+".xml")
	if (!srcFile.exists()) { println "Warning: missing source file: $srcFile"; continue }
	
	def imgFiles = subdir.listFiles().sort()
	if (imgFiles.size() == 0) { println "Warning: no image in $subdir"; continue }
	
	def imgPaths = []
	for (def img : imgFiles) {
		if (img.isFile() && !img.isHidden()) {
			if (prefix != null && prefix.length() > 0)
				imgPaths << prefix+name+"/"+img.getName()
			else
				imgPaths << img.getAbsolutePath()
		}
	}
	
	println "Processing '$name' directory with "+imgPaths.size()+" images."
	
	AddAttributeValuesInXML builder = new AddAttributeValuesInXML(srcFile, element, attribute, imgPaths);
	builder.process(new File(outputDirectory, srcFile.getName()));
}