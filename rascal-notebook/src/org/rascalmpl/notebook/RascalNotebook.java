package org.rascalmpl.notebook;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.output.WriterOutputStream;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.repl.RascalInterpreterREPL;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.rascalmpl.uri.URIUtil;

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
import entities.util.MessageType;
import entities.util.Status;
import server.JupyterServer;

public class RascalNotebook extends JupyterServer {
	private final static String STD_ERR_DIV = "output_stderr";
	private final static String STD_OUT_DIV = "output_stdout";
	private final static Charset UTF8 = Charset.forName("UTF8");
	public final static String MIME_TYPE_HTML = "text/html";

	public RascalNotebook(String connectionFilePath) throws Exception {
		super(connectionFilePath);
	}

	@Override
	public void startServer() throws JsonSyntaxException, JsonIOException, FileNotFoundException, RuntimeException {
		try {
			registerRascalServer();
			super.startServer();
		}
		catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private void registerRascalServer() throws IOException, URISyntaxException {
		this.stdout = new StringWriter();
		this.stderr = new StringWriter();

		OutputStream output = new WriterOutputStream(stdout, UTF8, 4096, true);
		OutputStream errors = new WriterOutputStream(stderr, UTF8, 4096, true);
		InputStream input = new ByteArrayInputStream(new byte[4096]);

		this.language = makeInterpreter(".", "");
		this.language.initialize(input, output, errors);
	}

	public void replyRequest(Header parentHeader, String session, Map<String, InputStream> data, Map<String, String> metadata) {
		Map<String, String> res = data.entrySet().stream()
			.collect(Collectors.toMap(e -> e.getKey(), e -> convertStreamToString(e.getValue())));

		filterResults(res);

		ContentExecuteResult content = new ContentExecuteResult(executionNumber, res, metadata);
		sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.EXECUTE_RESULT), parentHeader, content);
	}

	private void filterResults(Map<String,String> res) {
		String resultString = res.get(MIME_TYPE_HTML).trim();

		if (resultString != null) {
			res.put(MIME_TYPE_HTML, pre(res.get(MIME_TYPE_HTML)));
		}
	}

	private String pre(String body) {
		return "<pre>\n" + body + "\n<pre>";
	}

	@Override
	public void processExecuteRequest(ContentExecuteRequest contentExecuteRequest, Message message) {
		Header header, parentHeader = message.getHeader();
		Map<String, String> metadata = message.getMetadata();
		Map<String, InputStream> data = new HashMap<>();
		String session = message.getHeader().getSession();
		
		if (!contentExecuteRequest.isSilent()) {
			if (contentExecuteRequest.isStoreHistory()) {
				header = new Header(MessageType.EXECUTE_INPUT, parentHeader);
				sendMessage(getCommunication().getIOPubSocket(), header, parentHeader, new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber));
				try {
					this.language.handleInput(contentExecuteRequest.getCode(), data, metadata); // Execute user's code
					
					sendMessage(getCommunication().getShellSocket(), new Header(MessageType.EXECUTE_REPLY, parentHeader), parentHeader, new ContentExecuteReplyOk(executionNumber));

					processStreams(parentHeader, data, metadata, session); // stdout writing
					
					if(!data.isEmpty()) {
						replyRequest(parentHeader, session, data, metadata); // // Returns the result
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			executionNumber ++;
		}
		else {
			// No broadcast output on the IOPUB channel.
			// Don't have an execute_result.
			header = new Header(MessageType.EXECUTE_REPLY, parentHeader);
			sendMessage(getCommunication().getShellSocket(), header, parentHeader, new ContentExecuteReplyOk(executionNumber));
		}
	}
	
	@SuppressWarnings("resource")
	public String convertStreamToString(java.io.InputStream inputStream) {
	    Scanner s = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}

	private void processStreams(Header parentHeader, Map<String, InputStream> data, Map<String, String> metadata, String session) {
		if (!stdout.toString().trim().equals("")) {
			processStreamsReply(ContentStream.STD_OUT, parentHeader, data, metadata, session);
		}
		if (!stderr.toString().trim().equals("")) {
			processStreamsReply(ContentStream.STD_ERR, parentHeader, data, metadata, session);
		}
	}

	public void processStreamsReply(String stream, Header parentHeader, Map<String, InputStream> data, Map<String, String> metadata, String session) {
		String logs = stream.equals(ContentStream.STD_OUT) ? stdout.toString() : stderr.toString();
		if (logs.contains("http://")) {
			metadata.put(MIME_TYPE_HTML, stream.equals(ContentStream.STD_OUT) ? createDiv(STD_OUT_DIV, replaceLocs2html(logs)) : createDiv(STD_ERR_DIV, replaceLocs2html(logs)));
			sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.DISPLAY_DATA), parentHeader, new ContentDisplayData(metadata, metadata, new HashMap<String, String>()));
		}
		else {
			sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.STREAM), parentHeader, new ContentStream(stream, stdout.toString()));
		}
		flushStreams();
	}
	
	public String createDiv(String clazz, String body){
		return "<div class = \""+ clazz +"\">"+ (body.equals("")||body==null ? "</div>" : body+"</div>");
	}

	public void flushStreams() {
		stdout.getBuffer().setLength(0);
		stdout.flush();
		stderr.getBuffer().setLength(0);
		stderr.flush();
	}

	@Override
	public void processHistoryRequest(Message message) {
		// TODO This is only for clients to explicitly request history from a kernel
	}
	
	@Override
	public ContentKernelInfoReply processKernelInfoRequest(Message message) {
//		Header parentHeader = message.getParentHeader();
//		Header header =createHeader(parentHeader.getSession(), MessageType.KERNEL_INFO_REPLY);
//		sendMessage(getCommunication().getShellSocket(), header, parentHeader, message.getMetadata(), new ContentKernelInfoReply());
		return new ContentKernelInfoReply();
	}

	@Override
	public ContentShutdownReply processShutdownRequest(ContentShutdownRequest contentShutdown) {
		boolean restart = false;
		if (contentShutdown.getRestart()){
			restart = true;
			try {
				registerRascalServer();
			}
			catch (IOException | URISyntaxException e) {
				this.language.stop();
				return new ContentShutdownReply(false);
			}
		} else {
			this.language.stop();
		}
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
			status = Status.COMPLETE;
		} else {
			status = Status.INCOMPLETE;
			indent = "??????";
		}
		return new ContentIsCompleteReply(status, indent);
	}

	@Override
	public Content processCompleteRequest(ContentCompleteRequest content) {
		int cursorStart = 0;
		ArrayList<String> suggestions;
		
		if (content.getCode().startsWith("import "))
			cursorStart=7;
		
		CompletionResult result =this.language.completeFragment(content.getCode(), content.getCursorPosition());
		if (result != null) {
			suggestions = (ArrayList<String>)result.getSuggestions();
		}
		else {
			suggestions = null;
		}
		
		return new ContentCompleteReply(suggestions, cursorStart, content.getCode().length(), new HashMap<String, String>(), Status.OK);
	}

	public ILanguageProtocol makeInterpreter(String source, String replQualifiedName) throws IOException, URISyntaxException {
		return new RascalInterpreterREPL(false, false, true, null) {
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

	public String replaceLocs2html(String logs){
		String pattern = "(?s)(.*)(\\|)(.+)(\\|)(.*$)";
		if (logs.matches(pattern)){
			logs = logs.replaceAll("\n", "<br>");
			String prefix = logs.replaceAll(pattern,"$1");
			String url = logs.replaceAll(pattern,"$3");
			String suffix = logs.replaceAll(pattern,"$5");
			return prefix + "<a href=\""+ url +"\" target=\"_blank\">"+url+"</a>" + suffix;
		}
		return logs;
	}

	public static void main(String[] args) {
		try {
			new RascalNotebook(args[0]).startServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
