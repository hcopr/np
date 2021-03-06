package np;

import np.Interpreter.InterpreterException;

public class ClastIdentExpr extends ClastNode
{

	public ClastIdentExpr(Token t)
    {
	    super(t);
	    // TODO Auto-generated constructor stub
    }
	
	public CoreObject run(CoreObject objectContext, CoreObject lookupContainer) throws InterpreterException
	{
		if(child == null)
			return new CoreObject();
			
		CoreObject xres = ClastExp.invoke(objectContext, null, child, child.next);
		String ident = xres.toString();
		
		ClastIdentifier ci = new ClastIdentifier(new Token("Identifier", ident));
		
		return ci.run(objectContext, lookupContainer);
	}

}
