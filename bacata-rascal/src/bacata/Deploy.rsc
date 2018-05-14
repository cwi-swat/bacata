module bacata::Deploy

import IO;

void deployNotebook(loc projectPath){

}


void generateDockerFile(loc path, str projectName, str languageName){
	writeFile(path, dockerFileContent);
}

str dockerFileContent(str projectName, str languageName){
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