package testImplementations;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.library.util.TermREPL;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.values.ValueFactoryFactory;
import org.zeromq.ZMQ.Socket;
import com.google.gson.JsonObject;
import communication.Header;
import entities.ContentExecuteInput;
import entities.ContentStream;
import entities.reply.ContentCompleteReply;
import entities.reply.ContentExecuteReplyOk;
import entities.reply.ContentExecuteResult;
import entities.reply.ContentIsCompleteReply;
import entities.reply.ContentKernelInfoReply;
import entities.reply.ContentShutdownReply;
import entities.request.ContentCompleteRequest;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.MessageType;
import entities.util.Status;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValueFactory;
import server.JupyterServer;

public class TermKernel extends JupyterServer{

	// -----------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------

	private int executionNumber;

	private ILanguageProtocol language;

	private StringWriter stdout;

	private StringWriter stderr;

	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------

	public TermKernel(String connectionFilePath, String moduleName, String variableName) throws Exception {
		super(connectionFilePath);
		executionNumber = 1;
		stdout = new StringWriter();
		stderr = new StringWriter();
		this.language = makeInterpreter(moduleName, variableName);
		this.language.initialize(stdout, stderr);
		startServer();
	}

	// -----------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------

	@Override
	public void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest) {
		if(!contentExecuteRequest.isSilent())
		{
			if(contentExecuteRequest.isStoreHistory())
			{
				sendMessage(getCommunication().getPublish(),createHeader(parentHeader.getSession(), MessageType.EXECUTE_INPUT), parentHeader, new JsonObject(), new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber));

				try {
					Map<String, String> data = new HashMap<>();
					Map<String, String> metadata = new HashMap<>();

					this.language.handleInput(contentExecuteRequest.getCode(), data, metadata);
					sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_REPLY), parentHeader, new JsonObject(), new ContentExecuteReplyOk(executionNumber));

					if(!stdout.toString().trim().equals("")){
						sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.STREAM), parentHeader, new JsonObject(), new ContentStream("stdout", stdout.toString()));
						stdout.getBuffer().setLength(0);
						stdout.flush();
					}

					if(!stderr.toString().trim().equals("")){
						sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.STREAM), parentHeader, new JsonObject(), new ContentStream("stderr", stderr.toString()));
						stderr.getBuffer().setLength(0);
						stderr.flush();
					}

					// sends the result
					if(!data.isEmpty())
					{
						ContentExecuteResult content = new ContentExecuteResult(executionNumber, data, metadata);
						sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_RESULT), parentHeader, new JsonObject(), content);
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else{
				// TODO evaluate user code 
			}
			executionNumber ++;
		}
		else{
			// No broadcast output on the IOPUB channel.
			// Don't have an execute_result.
			sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_REPLY), parentHeader, new JsonObject(), new ContentExecuteReplyOk(executionNumber));
		}
	}

	@Override
	public void processHistoryRequest(Header parentHeader) {
		// TODO This is only for clients to explicitly request history from a kernel
	}
	@Override
	public void processKernelInfoRequest(Header parentHeader){
		sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.KERNEL_INFO_REPLY), parentHeader, new JsonObject(), new ContentKernelInfoReply());
	}

	@Override
	public void processShutdownRequest(Socket socket, Header parentHeader, ContentShutdownRequest contentShutdown) {
		boolean restart = false;
		if(contentShutdown.getRestart())
		{
			restart = true;
			// TODO: how should I restart rascal?
		}
		else{
			this.language.stop();
			getCommunication().getRequests().close();
			getCommunication().getPublish().close();
			getCommunication().getControl().close();
			getCommunication().getContext().close();
			getCommunication().getContext().term();
			System.exit(-1);
		}
		sendMessage(socket, createHeader(parentHeader.getSession(), MessageType.SHUTDOWN_REPLY), parentHeader, new JsonObject(), new ContentShutdownReply(restart));
	}

	/**
	 * This method is executed when the kernel receives a is_complete_request message.
	 */
	@Override
	public void processIsCompleteRequest(Header header, ContentIsCompleteRequest request) {
		//TODO: Rascal supports different statuses? (e.g. complete, incomplete, invalid or unknown?
		String status, indent="";
		if(this.language.isStatementComplete(request.getCode())){
			System.out.println("COMPLETO");
			status = Status.COMPLETE;
		}
		else{
			status = Status.INCOMPLETE;
			indent = "??????";
		}
		sendMessage(getCommunication().getRequests(), createHeader(header.getSession(), MessageType.IS_COMPLETE_REPLY), header, new JsonObject(), new ContentIsCompleteReply(status, indent));
	}

	@Override
	public void processCompleteRequest(Header parentHeader, ContentCompleteRequest request) {
		int cursorStart =0;
		ArrayList<String> sugestions;
		if(request.getCode().startsWith("import ")){
			cursorStart=7;
		}
		CompletionResult result =this.language.completeFragment(request.getCode(), request.getCursorPosition());
		if(result != null)
			sugestions = (ArrayList<String>)result.getSuggestions();
		else 
			sugestions = null;
		ContentCompleteReply content = new ContentCompleteReply(sugestions, cursorStart, request.getCode().length(), new HashMap<String, String>(), Status.OK);
		sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.COMPLETE_REPLY), parentHeader, new JsonObject(), content);
	}

	@Override
	public ILanguageProtocol makeInterpreter(String moduleName, String variableName)  {
		GlobalEnvironment heap = new GlobalEnvironment();
		ModuleEnvironment root = heap.addModule(new ModuleEnvironment("$"+variableName+"$", heap));
		IValueFactory vf = ValueFactoryFactory.getValueFactory();
		Evaluator eval = new Evaluator(vf, new PrintWriter(System.err), new PrintWriter(System.out), root, heap);
		eval.addRascalSearchPathContributor(StandardLibraryContributor.getInstance());
		
//		ISourceLocation ggg = vf.sourceLocation(URI.create("project://amalga/src"));
//		eval.addRascalSearchPath(URIUtil.rootLocation("project://amalga/src/lang/amalga"));
//		eval.addRascalSearchPath(URIUtil.rootLocation("amalga/src/lang/amalga"));
//		eval.addRascalSearchPath(URIUtil.rootLocation("file://amalga"));
		
		eval.doImport(null, moduleName);
		ModuleEnvironment module = eval.getHeap().getModule(moduleName);
		IConstructor repl = (IConstructor) module.getSimpleVariable(variableName).getValue();
		
//		eval.doImport(null, moduleName);
//		ModuleEnvironment module = eval.getHeap().getModule(moduleName);
//		IConstructor repl = (IConstructor) module.getSimpleVariable(variableName).getValue();
		
		//---------------
//		eval.addRascalSearchPathContributor(new URIContributor(URIUtil.rootLocation("project://amalga")));
//		eval.addRascalSearchPath(URIUtil.rootLocation("amalga"));
//		eval.doImport(null, "util::amalga::AmalgaREPL");
//		ModuleEnvironment module = eval.getHeap().getModule("util::amalga::AmalgaREPL");
//		IConstructor repl = (IConstructor) module.getSimpleVariable("amalgaRepl").getValue();
		
		try {
			return new TermREPL.TheREPL(vf, repl, eval);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
	

	// -----------------------------------------------------------------
	// Execution
	// -----------------------------------------------------------------

	public static void main(String[] args) {
		try {
//			module to import, name of the variable
			TermKernel kernel =  new TermKernel(args[0], args[1], args[2]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
