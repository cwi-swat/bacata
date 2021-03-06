package org.rascalmpl.notebook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.repl.RascalInterpreterREPL;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.rascalmpl.uri.URIUtil;

import entities.util.LanguageInfo;
import server.JupyterServer;

public class RascalNotebook  {

	public static void main(String[] args) {
		try {
			LanguageInfo info = new LanguageInfo("rascal", "0.19.3-SNAPSHOT", "text/plain", ".rsc");
			JupyterServer bacata = new JupyterServer(args[0], makeInterpreter(), info);
			bacata.startServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ILanguageProtocol makeInterpreter() throws IOException, URISyntaxException {
		return new RascalInterpreterREPL(false, false, null) {
			@Override
			protected Evaluator constructEvaluator(InputStream input, OutputStream stdout, OutputStream stderr) {
				Evaluator eval = ShellEvaluatorFactory.getDefaultEvaluator(input, stdout, stderr);
				
				// for the standard library
				eval.addRascalSearchPathContributor(StandardLibraryContributor.getInstance());

				// for salix which is included in the fat jar:
				eval.addRascalSearchPath(URIUtil.correctLocation("lib",  "rascal-notebook", "src"));

				return eval;
			}
		};
	}
}
