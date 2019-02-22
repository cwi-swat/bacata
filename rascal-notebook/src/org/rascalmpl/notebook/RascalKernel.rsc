module bacata::rascalNotebook::RascalKernel

import IO;
import String;
import bacata::Notebook;


void generateRascalKernel(loc kernelDestPath, loc bacataRascalJar, loc salixPath = |tmp:///|){
	writeFile(kernelDestPath + "kernel.json", rascalKernelContent(bacataRascalJar, salixPath));
	installKernel(kernelDestPath);
}

str rascalKernelContent(loc bacataRascalJar, loc salixPath){
	return 
		"{
		'	\"argv\": [
		'		\"java\",
		'		\"-jar\",
		'		\"<resolveLocation(bacataRascalJar)>\",
		'		\"{connection_file}\",
		' 		<if(salixPath != |tmp:///|){>,\"<"<salixPath>"[1..-1]>\"<}>
		'	],
		'	\"display_name\": \"Rascal\",
		'	\"language\": \"Rascal\"
		'}";
}