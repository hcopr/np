package np;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import com.fastcgi.FCGIInterface;
import com.fastcgi.FCGIRequest;

/*
 * the interpreter is the heart of np, it bundles together the entire
 * runtime. it's responsible for handing raw code off to the Lexer and
 * then to the Parser, before executing the resulting classed AST tree.
 */
public class Interpreter
{
	public static Interpreter instance = null;
	
	public Lexer lexer = new Lexer();
	public Parser tree = new Parser();
	public FCGIRequest req;
	public com.fastcgi.Response response = new com.fastcgi.Response();
	
	public StringBuilder output = new StringBuilder();
	public StringBuilder debugTrace = new StringBuilder();
	public RTErrorMessage fatalError = null;
	public CoreObject rootContext = new CoreObject();
	public CoreCall rootCall = null;
	
	public long execTimeStart = System.currentTimeMillis();
	public long execTimeOut = System.currentTimeMillis()+500;
	public long execTimeOutChecks = 0;
	
	public HashMap<CoreObject, AssignmentTag> assignmentList = new HashMap<CoreObject, AssignmentTag>();
	public HashMap<CoreObject, String> assignmentNameHints = new HashMap<CoreObject, String>();
	public boolean assignmentMode = false;
	
	public Interpreter(FCGIRequest r) 
	{
		req = r;
	}
	
	public void beginAssignmentMode()
	{
		assignmentMode = true;
	}
	
	public AssignmentTag endAssignmentMode(CoreObject o)
	{
		AssignmentTag result = assignmentList.get(o);
		assignmentList.clear();
		assignmentNameHints.clear();
		assignmentMode = false;
		return result;
	}
	
	public static class InterpreterException extends Throwable {
        private static final long serialVersionUID = 2840895767933757339L;
        public String msg;
        public Token token;
        InterpreterException(String m, Token t)
        {
        	msg = m;
        	token = t;
        }
	}
	
	public void checkForTimeout() throws InterpreterException
	{
		/*
		 * execTimeOutChecks is simply there to provide some granularity so
		 * we don't have to make this system call every time something gets
		 * executed. 
		 */
		execTimeOutChecks++;
		if(execTimeOutChecks < 1000) return;
		execTimeOutChecks = 0;
		if(System.currentTimeMillis() > execTimeOut)
			throw new InterpreterException("interpreter time out", new Token());
	}
	
	public String getInternalStackTrace(Exception e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	public CoreObject run(ClastNode node) throws InterpreterException
	{
		new LibNP();
		
		if(rootContext.members.get("request") == null)
		{
			CoreMap param = new CoreMap();
			CoreMap env = new CoreMap(FCGIInterface.request.params);
			CoreMap get = HttpTools.getQueryParameters(param, env.item("QUERY_STRING").toString(), this);
			CoreMap post = HttpTools.getPostParameters(param, HttpTools.inputStreamToString(this.req.inStream), this);
			param.putMember("env", env, false);
			param.putMember("get", get, false);
			param.putMember("post", post, false);
			rootContext.putMember("request", param, true);
		}
		rootContext.putMember("global", new CoreObject(), true);
		
		rootCall = new CoreCall(rootContext, rootContext, null, null);
		return node.run(rootCall, null);
	}
	
	public void load(String fileName)
	{
		try
		{
			ClastNode cnode = ClastFactory.getClastFromCache(fileName);
			if(cnode == null)
			{
				RTFile srcFile = new RTFile(fileName);
				if(srcFile.error != null)
				{
					output.append(srcFile.error+"\n");
					req.appStatus = 404;
					return;
				}		
				lexer.readText(srcFile.content);
				tree.parse(lexer.tokens, fileName);
				cnode = ClastFactory.makeClastFromTree(tree.root, srcFile);
			}
			run(cnode);
		}
		catch (InterpreterException e)
		{
			fatalError = new RTErrorMessage(e.msg, e.token);
			output.append("Error: "+fatalError);
		}
	}
	
	public CoreObject eval(String src) throws InterpreterException
	{
		CoreObject result = new CoreObject();
		try
		{
			lexer.readText(src);
			tree.parse(lexer.tokens, "(eval)");
			ClastNode cnode = ClastFactory.makeClastFromTree(tree.root, null);
			result.members.put("result", run(cnode));
			result.members.put("root", rootCall);
		}
		catch (InterpreterException e)
		{
			fatalError = new RTErrorMessage(e.msg, e.token);
			output.append("Error: "+fatalError);
		}
		result.members.put("output", new CoreString(output.toString()));
		return result;
	}

}
