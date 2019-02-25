package bacata.dslNotebook;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.utils.RascalManifest;
import org.rascalmpl.library.util.TermREPL;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.values.ValueFactoryFactory;
import org.zeromq.ZMQ.Socket;
import communication.Header;
import entities.ContentExecuteInput;
import entities.ContentStream;
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
import entities.util.LanguageInfo;
import entities.util.MessageType;
import entities.util.Status;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import server.JupyterServer;

public class DSLNotebook extends JupyterServer{


	// -----------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------

	private String languageName;

	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------

	public DSLNotebook(String connectionFilePath, String source, String replQualifiedName, String pLanguageName, String... salixPath) throws Exception {
		super(connectionFilePath);
		languageName = pLanguageName;
		stdout = new StringWriter();
		stderr = new StringWriter();
		this.language = makeInterpreter(source, replQualifiedName, salixPath);
		this.language.initialize(stdout, stderr);
//		generateKernel();
//		installKernel();
		startServer();
	}

	// -----------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------

	@Override
	public void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest, Map<String, String> metadata) {
		Map<String, InputStream> data = new HashMap<>();
		if (!contentExecuteRequest.isSilent()) {
			if (contentExecuteRequest.isStoreHistory()) {
				sendMessage(getCommunication().getPublish(),createHeader(parentHeader.getSession(), MessageType.EXECUTE_INPUT), parentHeader, metadata, new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber));
				try {
					this.language.handleInput(contentExecuteRequest.getCode(), data, metadata);
					sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_REPLY), parentHeader, metadata, new ContentExecuteReplyOk(executionNumber));

					if(!stdout.toString().trim().equals("")){
						sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.STREAM), parentHeader, metadata, new ContentStream("stdout", stdout.toString()));
						stdout.getBuffer().setLength(0);
						stdout.flush();
					}

					if (!stderr.toString().trim().equals("")) {
						// This message is used to Render locations in html because STREM channel does not support it
						String logs = metadata.get("ERROR-LOG");
						if ( logs != null){
							metadata.remove("ERROR-LOG");
							metadata.put("text/html", logs);
							sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.DISPLAY_DATA), parentHeader, metadata, new ContentDisplayData(metadata, metadata, new HashMap<String, String>()));
						}
						else {
							sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.STREAM), parentHeader, metadata, new ContentStream("stderr", stderr.toString()));
						}
						stderr.getBuffer().setLength(0);
						stderr.flush();
					}

					// sends the result
					if (!data.isEmpty()) {
						replyRequest(parentHeader,data, metadata);
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
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
			sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_REPLY), parentHeader, metadata, new ContentExecuteReplyOk(executionNumber));
		}
	}
	
	public void replyRequest(Header parentHeader, Map<String, InputStream> data, Map<String, String> metadata) {
		InputStream input = data.get("text/html");
		Map<String, String> res= new HashMap<>();
		res.put("text/html", convertStreamToString(input));
		ContentExecuteResult content = new ContentExecuteResult(executionNumber, res, metadata);
		sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_RESULT), parentHeader, metadata, content);
		
	}

	@SuppressWarnings("resource")
	public String convertStreamToString(java.io.InputStream inputStream) {
	    Scanner s = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	

	@Override
	public void processHistoryRequest(Header parentHeader, Map<String, String> metadata) {
		// TODO This is only for clients to explicitly request history from a kernel
	}
	
	@Override
	public void processKernelInfoRequest(Header parentHeader, Map<String, String> metadata){
//		sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.KERNEL_INFO_REPLY), parentHeader, new JsonObject(), new ContentKernelInfoReply());
		sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.KERNEL_INFO_REPLY), parentHeader, new HashMap<String, String>(), new ContentKernelInfoReply(new LanguageInfo(languageName)));
	}

	@Override
	public void processShutdownRequest(Socket socket, Header parentHeader, ContentShutdownRequest contentShutdown, Map<String, String> metadata) {
		boolean restart = false;
		if (contentShutdown.getRestart()) {
			restart = true;
			// TODO: how should I restart rascal?
		}
		else {
			this.language.stop();
			getCommunication().getRequests().close();
			getCommunication().getPublish().close();
			getCommunication().getControl().close();
			getCommunication().getContext().close();
			getCommunication().getContext().term();
			System.exit(-1);
		}
		sendMessage(socket, createHeader(parentHeader.getSession(), MessageType.SHUTDOWN_REPLY), parentHeader, new HashMap<String, String>(), new ContentShutdownReply(restart));
	}

	/**
	 * This method is executed when the kernel receives a is_complete_request message.
	 */
	@Override
	public void processIsCompleteRequest(Header header, ContentIsCompleteRequest request, Map<String, String> metadata) {
		//TODO: Rascal supports different statuses? (e.g. complete, incomplete, invalid or unknown?
		String status, indent="";
		if (this.language.isStatementComplete(request.getCode())) {
			System.out.println("COMPLETO");
			status = Status.COMPLETE;
		}
		else {
			status = Status.INCOMPLETE;
			indent = "??????";
		}
		sendMessage(getCommunication().getRequests(), createHeader(header.getSession(), MessageType.IS_COMPLETE_REPLY), header, new HashMap<String, String>(), new ContentIsCompleteReply(status, indent));
	}

	@Override
	public void processCompleteRequest(Header parentHeader, ContentCompleteRequest request, Map<String, String> metadata) {
		int cursorPosition = request.getCursorPosition();
		ArrayList<String> sugestions = null;
		if (request.getCode().startsWith("import ")) {
			cursorPosition=7;
		}
		CompletionResult result = this.language.completeFragment(request.getCode(), cursorPosition);
		if (result != null)
			sugestions = (ArrayList<String>)result.getSuggestions();
		
		ContentCompleteReply content = new ContentCompleteReply(sugestions, result != null ? result.getOffset() : 0, request.getCode().length(), new HashMap<String, String>(), Status.OK);
		sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.COMPLETE_REPLY), parentHeader, new HashMap<String, String>(), content);
	}
	
	private static final String JAR_FILE_PREFIX = "jar:file:";
	
	private static ISourceLocation createJarLocation(IValueFactory vf, URL u) throws URISyntaxException {
		String full = u.toString();
		if (full.startsWith(JAR_FILE_PREFIX)) {
			full = full.substring(JAR_FILE_PREFIX.length());
			return vf.sourceLocation("jar", null, full);
		}
		else {
			return vf.sourceLocation(URIUtil.fromURL(u));
		}
			
	}
		
	@Override
	public ILanguageProtocol makeInterpreter(String source, String replQualifiedName, String... salixPath)  {
		String[] tmp = replQualifiedName.split("::");
		String variableName = tmp[tmp.length-1];
		String moduleName = replQualifiedName.replaceFirst("::"+variableName, "");
		GlobalEnvironment heap = new GlobalEnvironment();
		ModuleEnvironment root = heap.addModule(new ModuleEnvironment("$"+moduleName+"$", heap));
		IValueFactory vf = ValueFactoryFactory.getValueFactory();
		Evaluator eval = new Evaluator(vf, new PrintWriter(System.err), new PrintWriter(System.out), root, heap);
		
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
		
		try {
			return new TermREPL.TheREPL(vf, repl, eval);
		} catch (IOException| URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// -----------------------------------------------------------------
	// Execution
	// -----------------------------------------------------------------

	public static void main(String[] args) {
		try {
			if (args.length==5)
				new DSLNotebook(args[0], args[1], args[2], args[3], args[4]);
			else
				new DSLNotebook(args[0], args[1], args[2], args[3], args[4], args[5]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
