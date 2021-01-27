package bacata.dslNotebook;

import java.io.IOException;
import java.net.URISyntaxException;

//import org.rascalmpl.bacata.repl.BacataREPL;
//import org.rascalmpl.bacata.repl.replization.REPLize;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.library.util.TermREPL;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.values.ValueFactoryFactory;
import org.rascalmpl.values.functions.IFunction;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.IWithKeywordParameters;
import server.JupyterServer;

public class DSLNotebook {

	public static void main(String[] args) {
		try {
			JupyterServer server = new JupyterServer(args[0], makeInterpreter(args[1], args[2]));
			server.startServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ILanguageProtocol makeInterpreter(String source, String replQualifiedName) throws IOException, URISyntaxException {
		String[] tmp = replQualifiedName.split("::");
		String variableName = tmp[tmp.length-1];
		String moduleName = replQualifiedName.replaceFirst("::"+variableName, "");
		GlobalEnvironment heap = new GlobalEnvironment();
		ModuleEnvironment root = heap.addModule(new ModuleEnvironment("$"+moduleName+"$", heap));
		IValueFactory vf = ValueFactoryFactory.getValueFactory();
		Evaluator eval = new Evaluator(vf, System.in, System.err, System.out, root, heap);
		
		// for the standard library
		eval.addRascalSearchPathContributor(StandardLibraryContributor.getInstance());

		// for salix which is included in the fat jar:
		eval.addRascalSearchPath(URIUtil.correctLocation("lib",  "rascal-notebook", "src"));

		eval.doImport(null, moduleName);
		
		ModuleEnvironment module = eval.getHeap().getModule(moduleName);
		Result<IValue> var = module.getSimpleVariable(variableName);
		IConstructor repl = (var != null ? (IConstructor) var.getValue() : (IConstructor) eval.call(variableName, new IValue[]{}));

		IWithKeywordParameters<? extends IConstructor> repl2 = repl.asWithKeywordParameters();
		
		IFunction handler = (IFunction) repl2.getParameter("handler");
		IFunction completor = (IFunction) repl2.getParameter("completor");
		
		return new TermREPL.TheREPL(vf, vf.string("Bacatá"), vf.string("Welcome"), vf.string("IN"),  vf.string("quit"), vf.sourceLocation(""), handler, completor, completor, eval.getInput(), eval.getStdErr(), eval.getStdOut());
	}
}
