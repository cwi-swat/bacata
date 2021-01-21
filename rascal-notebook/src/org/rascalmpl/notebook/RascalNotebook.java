package org.rascalmpl.notebook;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.output.WriterOutputStream;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.interpreter.utils.RascalManifest;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.repl.RascalInterpreterREPL;
import org.rascalmpl.shell.ShellEvaluatorFactory;
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
import entities.util.MessageType;
import entities.util.Status;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValueFactory;
import server.JupyterServer;

public class RascalNotebook extends JupyterServer {
	private final static String STD_ERR_DIV = "output_stderr";
	private final static String STD_OUT_DIV = "output_stdout";
	private final static Charset UTF8 = Charset.forName("UTF8");
	public final static String MIME_TYPE_HTML = "text/html";
	public final static String MIME_TYPE_PLAIN = "text/plain";
	private static final String JAR_FILE_PREFIX = "jar:file:";
	private final String[] searchPath;

	public RascalNotebook(String connectionFilePath, String... salixPath ) throws Exception {
		super(connectionFilePath);
		this.searchPath = salixPath;
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

		this.language = makeInterpreter(".", "", searchPath);
		this.language.initialize(input, output, errors);
	}

	public void replyRequest(Header parentHeader, String session, Map<String, InputStream> data, Map<String, String> metadata) {
		Map<String, String> res = data.entrySet().stream()
			.collect(Collectors.toMap(e -> e.getKey(), e -> convertStreamToString(e.getValue())));

			// TODO remove debug prints
		res.entrySet().stream().forEach(e -> {
			System.err.print(e.getKey() + ": [");
			System.err.println(e.getValue() + "]");
		});

		// "ok" is not nice in a notebook. The cell with simply be evaluated and the "*" will dissappear
		if (res.get(MIME_TYPE_PLAIN).trim().equals("ok")) {
			res.remove(MIME_TYPE_PLAIN);
		}

		// this means that text/html will contain an iframe
		if (res.get(MIME_TYPE_PLAIN).trim().startsWith("Serving visual content at")) {
			res.remove(MIME_TYPE_PLAIN);
		}

		ContentExecuteResult content = new ContentExecuteResult(executionNumber, res, metadata);
		sendMessage(getCommunication().getIOPubSocket(), createHeader(session, MessageType.EXECUTE_RESULT), parentHeader, content);
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
			else {
				// TODO evaluate user code 
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
		ArrayList<String> sugestions;
		
		if (content.getCode().startsWith("import "))
			cursorStart=7;
		
		CompletionResult result =this.language.completeFragment(content.getCode(), content.getCursorPosition());
		if (result != null)
			sugestions = (ArrayList<String>)result.getSuggestions();
		else 
			sugestions = null;
		
		return new ContentCompleteReply(sugestions, cursorStart, content.getCode().length(), new HashMap<String, String>(), Status.OK);
	}

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
	public ILanguageProtocol makeInterpreter(String source, String replQualifiedName, final String... salixPath) throws IOException, URISyntaxException {
		return new RascalInterpreterREPL(false, false, true, null) {
			@Override
			protected Evaluator constructEvaluator(InputStream input, OutputStream stdout, OutputStream stderr) {
				Evaluator e = ShellEvaluatorFactory.getDefaultEvaluator(input, stdout, stderr);
				try {
					e.addRascalSearchPathContributor(StandardLibraryContributor.getInstance());

					// TODO: use reusable code from Rascal for this?
					IValueFactory vf = ValueFactoryFactory.getValueFactory();
					Enumeration<URL> res = ClassLoader.getSystemClassLoader().getResources(RascalManifest.META_INF_RASCAL_MF);
					RascalManifest mf = new RascalManifest();
					while (res.hasMoreElements()) {
						URL next = res.nextElement();
						List<String> roots = mf.getManifestSourceRoots(next.openStream());
						if (roots != null) {
							ISourceLocation currentRoot = createJarLocation(vf, next);
							currentRoot = URIUtil.getParentLocation(URIUtil.getParentLocation(currentRoot));
							for (String r: roots) {
								e.addRascalSearchPath(URIUtil.getChildLocation(currentRoot, r));
							}
							e.addRascalSearchPath(URIUtil.getChildLocation(currentRoot, RascalManifest.DEFAULT_SRC));
						}
					}
					if (salixPath.length > 0) {
						e.addRascalSearchPath(URIUtil.createFromURI((salixPath[0])));
					}
				} catch (URISyntaxException | IOException e1) {
					e1.printStackTrace();
				}
				return e;
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
			RascalNotebook a = args.length > 1 
				? new RascalNotebook(args[0], args[1]) 
				: new RascalNotebook(args[0]);

			a.startServer();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
