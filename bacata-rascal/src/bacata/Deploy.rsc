module bacata::Deploy

import IO;
import List;
import String;
import bacata::Notebook;

void generateDockerFile(loc path, str projectName, str languageName){
	writeFile(path, dockerFileContent);
}

str dockerFileContent(str projectName, str languageName){
return
	"FROM maveme/bacata:beta as bacataDSL
	'
	'ADD . root/<projectName>
	'
	'RUN jupyter kernelspec install root/<projectName>/kernel/<languageName>
	'
	'# Copies DSL codeMirror (e.g. halide-codemirror/halide)
	'RUN cp -a root/<projectName>/kernel/codemirror/. /notebook/notebook/static/components/codemirror/mode/
	'
	'RUN mkdir workspace
	'
	'WORKDIR /workspace
	'
	";
}

str dockerLanguageKernelContent(Kernel kernel){
return "{
	'	\"argv\": [
	'		\"java\",
	'		\"-jar\",
	'		\"/root/bacata/bacata-rascal/target/bacata-dsl.jar\",
	'		\"{connection_file}\",
	'		\"home:///<last(split("/", kernel.projectPath.parent.path))>/src\",
	'		\"<kernel.moduleName>\",
	'		\"<kernel.variableName>\",
	'		\"<kernel.languageName>\",
	'		\"home:///salix/src\"
	'	],
	'	\"display_name\": \"<firstUpperCase(kernel.languageName)>\",
	'	\"language\": \"<kernel.languageName>\"
	'}
	";
}