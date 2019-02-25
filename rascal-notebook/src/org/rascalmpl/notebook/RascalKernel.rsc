module org::rascalmpl::notebook::RascalKernel

import IO;
import List;
import String;
import util::Resources;
import util::ShellExec;


str JUPYTER_HOME = "";
str BACATA_HOME = "";


void generateRascalKernel(loc kernelDestPath = |tmp:///rascal-noteboook/|, loc salixPath = |tmp:///|){
	verifyPreRequisites();
	copyLogoToKernel();
	writeFile(kernelDestPath + "kernel.json", rascalKernelContent(salixPath));
	installKernel(kernelDestPath);
}

str rascalKernelContent(loc salixPath){
	return 
		"{
		'	\"argv\": [
		'		\"java\",
		'		\"-jar\",
		'		\"<BACATA_HOME>\",
		'		\"{connection_file}\"
		' 		<if(salixPath != |tmp:///|){>,\"<"<salixPath>"[1..-1]>\"<}>
		'	],
		'	\"display_name\": \"Rascal\",
		'	\"language\": \"Rascal\"
		'}";
}

void verifyBacataInstallation() {
	BACATA_HOME = getNotebookPluginLocation();
	if (BACATA_HOME == "")
		throw "BACATA_HOME is not defined as environment variable";
}

str getNotebookPluginLocation() {
	pluginsFolder = resolveLocation(|cwd:///|).parent + "Eclipse/plugins/";
	list[loc] notebookPlugins = [ plugin | plugin <- pluginsFolder.ls, startsWith(plugin.file, "rascal-notebook_")];
	return isEmpty(notebookPlugins) ? "" : getLatestVersion(notebookPlugins);
}

/*
* This function verifies the definition of the JUPYTER_HOME
*/
void verifyJupyterInstallation() {
	JUPYTER_HOME = readEnvVariable("JUPYTER_HOME");
	if (JUPYTER_HOME == "")
		throw "JUPYTER_HOME is not defined as environment variable";
}

void verifyPreRequisites() {
	verifyJupyterInstallation();
	verifyBacataInstallation();
}

void installKernel(loc kernelPath) {
	try {
		pid= createProcess(JUPYTER_HOME, args = ["kernelspec", "install", resolveLocation(kernelPath).path]);
	 	printErrTrace(pid);
	 }
	 catch Exc: 
	 	throw "Error while installing the kernel <Exc>";
}

/*
* This function reads the environment variable received as parameter.
*/
str readEnvVariable(str key) {
	f= createProcess("printenv", args=[key]);
	return replaceAll(readEntireStream(f),"\n","");
}

void printErrTrace(PID pid) {
 	line = readFromErr(pid);
    while (line!= "") {
    	println("<line>");
    	line = readFromErr(pid);
    }
}

str getLatestVersion(list[loc] versions) {
	latest = |tmp:///|;
	for (a <- versions) {
		for (b <- versions) {
			if (a >= b) {
				latest = a;
			}
		}
	}
	return resolveLocation(latest).path;
}

/*
* This function takes the url of a logo image (64x64) for the language to be displayed in the browser when the kernel is loaded
*/
void copyLogoToKernel(loc urlLogo, loc destPath) {
	list[int] imgBytes= readFileBytes(urlLogo);
	writeFileBytes(destPath + "logo-64x64.png", imgBytes);
}