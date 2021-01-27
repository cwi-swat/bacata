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

public class RascalNotebook  {

	public static void main(String[] args) {
		try {
			JupyterServer bacata = new JupyterServer(".", "", makeInterpreter());
			bacata.startServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ILanguageProtocol makeInterpreter() throws IOException, URISyntaxException {
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
}
