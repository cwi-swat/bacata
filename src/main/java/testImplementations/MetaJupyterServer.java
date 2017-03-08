package testImplementations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.repl.BaseRascalREPL;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.repl.RascalInterpreterREPL;
import org.rascalmpl.repl.jupyter.RascalJupyterInterpreterREPL;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.zeromq.ZMQ.Socket;
import com.google.gson.JsonObject;
import communication.Header;
import entities.reply.ContentCompleteReply;
import entities.reply.ContentExecuteReplyOk;
import entities.reply.ContentExecuteResult;
import entities.reply.ContentIsCompleteReply;
import entities.reply.ContentShutdownReply;
import entities.request.ContentCompleteRequest;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.MessageType;
import entities.util.Status;
import jline.Terminal;
import server.JupyterServer;

public class MetaJupyterServer extends JupyterServer{

	private int executionNumber;

	private RascalJupyterInterpreterREPL repl;

	private StringWriter stdout;
	private StringReader stdin;
	private StringWriter stderr;


	public MetaJupyterServer(String connectionFilePath) throws Exception {
		super(connectionFilePath);
		executionNumber = 0;
		stdout = new StringWriter();
		stderr = new StringWriter();

		repl = makeInterpreter();
		repl.initialize(stdout, stderr);
		
		startServer();
		System.out.println("WORKS!!!!!!");
	}

	@Override
	public void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest) {
		if(!contentExecuteRequest.isSilent())
		{
			if(contentExecuteRequest.isStoreHistory())
			{
				try {
					// TODO How can we manage the user history?
					// TODO evaluate user code
					// TODO publish result 
					repl.handleInput(contentExecuteRequest.getCode());
					System.out.println("ANSWE: "+ stdout.toString());
					System.out.println("__----------------");
					Map<String, String> data = new HashMap<>();
			        data.put("text/plain", "kernel answer");
			        data.put("text/plain", stdout.toString());
			        stdout.getBuffer().setLength(0);
			        stdout.flush();
			        ContentExecuteResult content = new ContentExecuteResult(executionNumber, data, new HashMap<String, String>());
//					sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.DISPLAY_DATA), parentHeader, new JsonObject(), content);
			        sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_RESULT), parentHeader, new JsonObject(), content);
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
			sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_REPLY), parentHeader, new JsonObject(), new ContentExecuteReplyOk(executionNumber, null, null));
		}

	}

	@Override
	public void processHistoryRequest(Header parentHeader) {
		// TODO This is only for clients to explicitly request history from a kernel
	}

	@Override
	public void processShutdownRequest(Socket socket, Header parentHeader, ContentShutdownRequest contentShutdown) {
		boolean restart = false;
		if(contentShutdown.getRestart())
		{
			restart = true;
			// TODO: how can I tell rascal to restart?
		}
		else{
			repl.stop();
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
	 * This method is executed when the kernel receives a is_complete_request
	 */
	@Override
	public void processIsCompleteRequest(Header header, ContentIsCompleteRequest request) {
		//TODO: Rascal supports different statuses? (e.g. complete, incomplete, invalid or unknown?
		String status, indent="";
		if(repl.isStatementComplete(request.getCode())){
			System.out.println("COMPLETO");
			status = Status.COMPLETE;
		}
		else{
			status = Status.INCOMPLETE;
			indent = "??????";
		}
		sendMessage(getCommunication().getRequests(), createHeader(header.getSession(), MessageType.IS_COMPLETE_REPLY), header, new JsonObject(), new ContentIsCompleteReply(status, indent));
	}

	/**
	 * Publish result
	 */
	@Override
	public void processExecuteResult(Header parentHeader, String code, int executionNumber) {
		// TODO In rascal this is done by the handleInput method
		//		System.out.println("EXECUTE_RESULT");
		//        Map<String, String> data = new HashMap<>();
		//        data.put("text/plain", "kernel answer");
		//        data.put("text/plain", code);
		//        ContentExecuteResult content = new ContentExecuteResult(executionNumber, data, new HashMap<String, String>());
		//        sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.DISPLAY_DATA), parentHeader, new JsonObject(), content);
		//        sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_RESULT), parentHeader, new JsonObject(), content);

	}

	private static RascalJupyterInterpreterREPL makeInterpreter() throws IOException, URISyntaxException {
		return new RascalJupyterInterpreterREPL(false, false, null) {
			@Override
			protected Evaluator constructEvaluator(Writer stdout, Writer stderr) {
				System.out.println("ENTRAAAAAAA");
				return ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(stdout), new PrintWriter(stderr));
			}
		};
	}

	public static void main(String[] args) {

//		StringWriter tmp = new StringWriter();
		try {
			MetaJupyterServer mv =  new MetaJupyterServer(args[0]);
			//			InputStream stdin= new ByteArrayInputStream("hola".getBytes(StandardCharsets.UTF_8));
			//			RascalJupyterInterpreterREPL repl = new RascalJupyterInterpreterREPL(false, false, null) {
			//				@Override
			//				protected Evaluator constructEvaluator(Writer stdout, Writer stderr) {
			//					System.out.println("ENTRAAAAAAA");
			//					return ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(stdout), new PrintWriter(stderr));
			//				}
			//			};
			//
			//			repl.initialize(tmp, tmp);
			//			System.out.println("funcionarasca?: "+ repl.isStatementComplete("ASD"));
			//			repl.handleInput("hola;");
			//			System.out.println("L: "+ tmp.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processCompleteRequest(Header parentHeader, ContentCompleteRequest request) {
		System.out.println("Entered to complete");
		System.out.println("@@@@@@@@@@@@@: ");
		System.out.println(request.getCode());
		System.out.println(request.getCursorPosition());
		System.out.println(repl.completeFragment(request.getCode(), request.getCursorPosition()));
		System.out.println("@@@@@@@@@@@@@");
		System.out.println("writer: "+stdout.toString());
		sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.COMPLETE_REPLY), parentHeader, new JsonObject(), new ContentCompleteReply(new ArrayList<String>(), 0, 2, null, Status.OK));
	}
}
