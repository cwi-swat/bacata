package bacata.dslNotebook;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.rascalmpl.bacata.repl.replization.REPLize;
//import org.rascalmpl.bacata.repl.replization.REPLize2;
//import org.rascalmpl.bacata.repl.BacataREPL;
//import org.rascalmpl.bacata.repl.replization.REPLize;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.utils.RascalManifest;
import org.rascalmpl.library.util.TermREPL;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.values.ValueFactoryFactory;

import communication.Header;

import entities.ContentExecuteInput;
import entities.ContentStream;
import entities.Message;
import entities.reply.ContentCompleteReply;
import entities.reply.ContentDisplayData;
import entities.reply.ContentExecuteReplyOk;
import entities.reply.ContentExecuteResult;
import entities.reply.ContentIsCompleteReply;
import entities.reply.ContentKernelInfoReply;
import entities.reply.ContentShutdownReply;
import entities.request.ContentCompleteRequest;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.Content;
import entities.util.LanguageInfo;
import entities.util.MessageType;
import entities.util.Status;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.IWithKeywordParameters;
import server.JupyterServer;

public class DSLNotebook extends JupyterServer {


	// -----------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------

	private String languageName;
	
	private static long a;

	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------

	public DSLNotebook(String connectionDetails, String source, String replQualifiedName, String pLanguageName, String... salixPath) throws Exception {
		super(connectionDetails);
		System.out.println("Connection: " + (System.currentTimeMillis() -  a));
		languageName = pLanguageName;
		stdout = new StringWriter();
		stderr = new StringWriter();
		long start = System.currentTimeMillis();
		this.language = makeInterpreter(source, replQualifiedName, salixPath);
		System.out.println("MAKE INTERPRETER: " + (System.currentTimeMillis() -  start));
		
		startServer();
	}
	
	// -----------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------

	@Override
	public void processExecuteRequest(ContentExecuteRequest contentExecuteRequest, Message message) {
		Header parentHeader = message.getParentHeader();
		// TODO: Check whether the cellId comes as part of the metadata
		HashMap<String, String> metadata = message.getMetadata();
		Map<String, InputStream> data = new HashMap<>();
		String session = message.getHeader().getSession();
		
		if (!contentExecuteRequest.isSilent()) {
			if (contentExecuteRequest.isStoreHistory()) {
				sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.EXECUTE_INPUT), parentHeader, new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber), (HashMap<String, String>) metadata);
				try {
					// add meta data info e.g., origin: salix-notebook
					metadata.put("origin", "notebook");
					this.language.handleInput(contentExecuteRequest.getCode(), data, metadata);
					sendMessage(getCommunication().getShellSocket(), createHeader(session, MessageType.EXECUTE_REPLY), parentHeader, new ContentExecuteReplyOk(executionNumber), metadata);

					if (!stdout.toString().trim().equals("")) {
						sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.STREAM), parentHeader, new ContentStream("stdout", stdout.toString()), metadata);
						stdout.getBuffer().setLength(0);
						stdout.flush();
					}

					if (!stderr.toString().trim().equals("")) {
						// This message is used to Render locations in html because STREM channel does not support it
						String logs = metadata.get("ERROR-LOG");
						if ( logs != null) {
							metadata.remove("ERROR-LOG");
							metadata.put("text/html", logs);
							sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.DISPLAY_DATA), parentHeader, new ContentDisplayData(metadata, metadata, new HashMap<String, String>()), (HashMap<String, String>) metadata);
						} else {
							sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.STREAM), parentHeader, new ContentStream("stderr", stderr.toString()), metadata);
						}
						stderr.getBuffer().setLength(0);
						stderr.flush();
					}

					// sends the result
					if (!data.isEmpty()) {
						replyRequest(message.getHeader(), session, data, metadata);
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
					// TODO: Return error messages.
				}
			}
			else {
				// TODO evaluate user code 
			}
			executionNumber ++;
		}
		else {
			// No broadcast output on the IOPUB channel.
			// Don't have an execute_result.
			sendMessage(getCommunication().getShellSocket(), createHeader(session, MessageType.EXECUTE_REPLY), parentHeader, new ContentExecuteReplyOk(executionNumber), metadata);
		}
	}
	
	public void replyRequest(Header parentHeader, String session, Map<String, InputStream> data, HashMap<String, String> metadata) {
		InputStream input = data.get("text/html");
		Map<String, String> res= new HashMap<>();
		res.put("text/html", convertStreamToString(input));
		ContentExecuteResult content = new ContentExecuteResult(executionNumber, res, metadata);
		sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.EXECUTE_RESULT), parentHeader, content, metadata);
	}

	@SuppressWarnings("resource")
	public String convertStreamToString(java.io.InputStream inputStream) {
	    Scanner s = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	

	@Override
	public void processHistoryRequest(Message message) {
		// TODO This is only for clients to explicitly request history from a kernel
	}
	
	@Override
	public Content processKernelInfoRequest(Message message) {
		LanguageInfo langInf = new LanguageInfo(languageName);
		ContentKernelInfoReply content = new ContentKernelInfoReply(langInf, "ok");
		return content;
	}

	@Override
	public Content processShutdownRequest(ContentShutdownRequest contentShutdown) {
		boolean restart = false;
		if (contentShutdown.getRestart()) {
			restart = true;
			// TODO: how can I restart rascal from here?
		} else 
			this.language.stop();
		return new ContentShutdownReply(restart);
	}

	/**
	 * This method is executed when the kernel receives a is_complete_request message.
	 */
	@Override
	public Content processIsCompleteRequest(ContentIsCompleteRequest request) {
		//TODO: Rascal supports different statuses? (e.g. complete, incomplete, invalid or unknown?
		String status, indent="";
		if (this.language.isStatementComplete(request.getCode())) {
			System.out.println("COMPLETO");
			status = Status.COMPLETE;
		} else {
			status = Status.INCOMPLETE;
			indent = "??????";
		}
		return new ContentIsCompleteReply(status, indent);
	}

	@Override
	public Content processCompleteRequest(ContentCompleteRequest contentCompleteRequest) {
		int cursorPosition = contentCompleteRequest.getCursorPosition();
		ArrayList<String> sugestions = null;
		
		CompletionResult result = this.language.completeFragment(contentCompleteRequest.getCode(), cursorPosition);
		sugestions = (ArrayList<String>) result.getSuggestions();
		
		return new ContentCompleteReply(sugestions, result.getOffset(), contentCompleteRequest.getCode().length(), new HashMap<String, String>(), Status.OK);
	}
	
	private static final String JAR_FILE_PREFIX = "jar:file:";
	
	private static ISourceLocation createJarLocation(IValueFactory vf, URL u) throws URISyntaxException {
		String full = u.toString();
		if (full.startsWith(JAR_FILE_PREFIX)) {
			full = full.substring(JAR_FILE_PREFIX.length());
			return vf.sourceLocation("jar", null, full);
		} else {
			return vf.sourceLocation(URIUtil.fromURL(u));
		}
	}
		
	@Override
	public ILanguageProtocol makeInterpreter(String source, String replQualifiedName, String... salixPath) throws IOException, URISyntaxException {
		String[] tmp = replQualifiedName.split("::");
		String variableName = tmp[tmp.length-1];
		String moduleName = replQualifiedName.replaceFirst("::"+variableName, "");
		GlobalEnvironment heap = new GlobalEnvironment();
		ModuleEnvironment root = heap.addModule(new ModuleEnvironment("$"+moduleName+"$", heap));
		IValueFactory vf = ValueFactoryFactory.getValueFactory();
		Evaluator eval = new Evaluator(vf, System.in, System.err, System.out, root, heap);
		
		eval.addRascalSearchPathContributor(StandardLibraryContributor.getInstance());
		try {
			Enumeration<URL> res = ClassLoader.getSystemClassLoader().getResources(RascalManifest.META_INF_RASCAL_MF);
			RascalManifest mf = new RascalManifest();
			while (res.hasMoreElements()) {
				URL next = res.nextElement();
				List<String> roots = mf.getManifestSourceRoots(next.openStream());
				if (roots != null) {
					ISourceLocation currentRoot = createJarLocation(vf, next);
					currentRoot = URIUtil.getParentLocation(URIUtil.getParentLocation(currentRoot));
					for (String r: roots) {
						eval.addRascalSearchPath(URIUtil.getChildLocation(currentRoot, r));
					}
					eval.addRascalSearchPath(URIUtil.getChildLocation(currentRoot, RascalManifest.DEFAULT_SRC));
				}
			}
			if (salixPath!=null && salixPath.length!=0)
				eval.addRascalSearchPath(URIUtil.createFromURI((salixPath[0])));
			eval.addRascalSearchPath(URIUtil.createFromURI(source));
		} catch (URISyntaxException | IOException e1) {
			throw new RuntimeException(e1);
		} 
		eval.doImport(null, moduleName);
		
		ModuleEnvironment module = eval.getHeap().getModule(moduleName);
		Result<IValue> var = module.getSimpleVariable(variableName);
		IConstructor repl = (var != null ? (IConstructor) var.getValue() : (IConstructor) eval.call(variableName, new IValue[]{}));

		IWithKeywordParameters<? extends IConstructor> repl2 = repl.asWithKeywordParameters();
		
		IValue handler = (ICallableValue) repl2.getParameter("newHandler");
		IValue completor = (ICallableValue) repl2.getParameter("completor");
		
//		ICallableValue defaultConfig = (ICallableValue) repl2.getParameter("initConfig");
		
		// if printer ->
//		ICallableValue printer = (ICallableValue) repl.get("printer");
		
		
//		return new REPLize2(vf, vf.string("Bacatá"), vf.string("Welcome"), vf.string("IN"),  vf.string("quit"), vf.sourceLocation(""), handler, completor, completor, defaultConfig, printer, eval.getInput(), eval.getStdErr(), eval.getStdOut());
		
		IString title = vf.string("Bacata");
		IString welcome = vf.string("Welcome");
		IString prompt = vf.string("In");
		IString quit = vf.string("quit");
		ISourceLocation history = vf.sourceLocation("");
		
		InputStream inpu = eval.getInput();
		OutputStream stderr= eval.getStdErr();
		OutputStream stdout = eval.getStdOut();
		
		return new REPLize(vf, repl,  inpu, stderr, stdout);
		
//		return new TermREPL.TheREPL(vf, title , welcome, prompt, quit, history, handler, completor, completor, inpu, stderr, stdout);
	}
	
	// -----------------------------------------------------------------
	// Execution
	// -----------------------------------------------------------------

	public static void main(String[] args) {
		try {
			if (args.length == 5)
				new DSLNotebook(args[0], args[1], args[2], args[3], args[4]);
			else 
				new DSLNotebook(args[0], args[1], args[2], args[3], args[4], args[5]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
