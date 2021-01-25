package bacata.dslNotebook;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

//import org.rascalmpl.bacata.repl.BacataREPL;
//import org.rascalmpl.bacata.repl.replization.REPLize;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.library.util.TermREPL;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.values.ValueFactoryFactory;
import org.rascalmpl.values.functions.IFunction;

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

	public DSLNotebook(String connectionDetails, String source, String replQualifiedName, String pLanguageName) throws Exception {
		super(connectionDetails);
		System.out.println("Connection: " + (System.currentTimeMillis() -  a));
		languageName = pLanguageName;
		stdout = new StringWriter();
		stderr = new StringWriter();
		long start = System.currentTimeMillis();
		this.language = makeInterpreter(source, replQualifiedName);
		System.out.println("MAKE INTERPRETER: " + (System.currentTimeMillis() -  start));
	}
	
	// -----------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------

	@Override
	public void processExecuteRequest(ContentExecuteRequest contentExecuteRequest, Message message) {
		Header parentHeader = message.getParentHeader();
		// TODO: Check whether the cellId comes as part of the metadata
		Map<String, String> metadata = message.getMetadata();
		Map<String, InputStream> data = new HashMap<>();
		String session = message.getHeader().getSession();
		
		if (!contentExecuteRequest.isSilent()) {
			if (contentExecuteRequest.isStoreHistory()) {
				sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.EXECUTE_INPUT), parentHeader, new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber));
				try {
					
					this.language.handleInput(contentExecuteRequest.getCode(), data, metadata);
					sendMessage(getCommunication().getShellSocket(), createHeader(session, MessageType.EXECUTE_REPLY), parentHeader, new ContentExecuteReplyOk(executionNumber));

					if (!stdout.toString().trim().equals("")) {
						sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.STREAM), parentHeader, new ContentStream("stdout", stdout.toString()));
						stdout.getBuffer().setLength(0);
						stdout.flush();
					}

					if (!stderr.toString().trim().equals("")) {
						// This message is used to Render locations in html because STREM channel does not support it
						String logs = metadata.get("ERROR-LOG");
						if ( logs != null) {
							metadata.remove("ERROR-LOG");
							metadata.put("text/html", logs);
							sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.DISPLAY_DATA), parentHeader, new ContentDisplayData(metadata, metadata, new HashMap<String, String>()));
						} 
						else {
							sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.STREAM), parentHeader, new ContentStream("stderr", stderr.toString()));
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
			sendMessage(getCommunication().getShellSocket(), createHeader(session, MessageType.EXECUTE_REPLY), parentHeader, new ContentExecuteReplyOk(executionNumber));
		}
	}
	
	public void replyRequest(Header parentHeader, String session, Map<String, InputStream> data, Map<String, String> metadata) {
		InputStream input = data.get("text/plain");
		Map<String, String> res= new HashMap<>();
		res.put("text/html", convertStreamToString(input));
		ContentExecuteResult content = new ContentExecuteResult(executionNumber, res, metadata);
		sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.EXECUTE_RESULT), parentHeader, content);
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
		ContentKernelInfoReply content = new ContentKernelInfoReply(langInf);
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
	
	@Override
	public ILanguageProtocol makeInterpreter(String source, String replQualifiedName) throws IOException, URISyntaxException {
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
	
	// -----------------------------------------------------------------
	// Execution
	// -----------------------------------------------------------------

	public static void main(String[] args) {
		try {
			new DSLNotebook(args[0], args[1], args[2], args[3]).startServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
