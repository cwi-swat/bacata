module bacata::Generator

import IO;
import String;
import bacata::Notebook;

// [0] Language name
// [1] rascal project path
// [2] variableName
// [3] moduleName
// [4] Bacata path
void main(list[str] args) {
  path = createKernel(args[0], toLocation(args[1]), args[2], args[3], toLocation(args[4]));
  installKernel(path);
  startJupyterServer();
}

loc createKernel(str languageName, loc projectPath, str variableName, str moduleName, loc bacataJar){
	writeFile(|tmp:///<languageName>|+"kernel.json", kernelContent(languageName, projectPath, variableName, moduleName, bacataJar));
	return |tmp:///<languageName>|;
}

str kernelContent(str languageName, loc projectPath, str variableName, str moduleName, loc bacataJar) = 
	"{
  	'	\"argv\": [
    '		\"java\",
    '		\"-cp\",
    '		\"<resolveLocation(|home:///|).path><"<bacataJar>"[9..-1]>:<resolveLocation(|home:///|).path><"<projectPath>"[9..-4]>\",
    '		\"bacata.TermKernel\",
    '		\"{connection_file}\",
    '		\"<"<projectPath>"[1..-1]>\",
    '		\"<moduleName>\",
    '		\"<variableName>\",
    '		\"<languageName>\"
  	'	],
  	'	\"display_name\": \"<firstUpperCase(languageName)>\",
  	'	\"language\": \"<languageName>\"
	'}";