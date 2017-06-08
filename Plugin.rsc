module lang::Plugin

import lang::Syntax;
import lang::Checker;
import util::IDE;
import ParseTree;
import IO;

private str LANGUAGE_NAME="Savvas";
private str LANGUAGE_EXTENSION="savvas";

void main() {
  registerLanguage(LANGUAGE_NAME, LANGUAGE_EXTENSION, parseAlg);
}

start[Program] parseAlg(str input, loc src) {
   return parse(#start[Program], input, src, allowAmbiguity=true);
}

void tests(){
	p = parse(#start[Program], |project://Savvas/src/test/test.savvas|);
	println(evalProgram(p.top));
}