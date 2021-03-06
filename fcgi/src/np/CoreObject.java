package np;

import java.util.HashMap;

import np.Interpreter.InterpreterException;

/*
 * CoreObject is the basic atomic runtime unit of np. Everything
 * evaluates to some subclass of CoreObject. Its main purpose is
 * to provide an untyped box for variables as well as a container
 * for object properties.
 */
public class CoreObject
{
	/*
	 * members can by any kind of field (both variable or executable)
	 * as long as they are CoreObjects as well
	 */
	public HashMap<String, CoreObject> members = new HashMap<String, CoreObject>();
	//public HashMap<String, Boolean> publicMembers = new HashMap<String, Boolean>();
	/*
	 * the actual storage unit for primitive types is here inside the
	 * value field. in value, Java native objects are stored, such
	 * as strings, doubles, method pointers and so on. when an object
	 * is called, this is the value that gets returned or executed.
	 */
	public Object value = null;
	public String name = null;
	
	public boolean isExecutable()
	{
		return false;
	}

	public CoreObject execute(CoreCall cc) throws InterpreterException
	{
	   return(this);	
	}
	
	public CoreObject getMember(String identifier)
	{
		return getMember(identifier, false);
	}

	public CoreObject getMember(String identifier, boolean lookForPublicOnly)
	{
		CoreObject result = members.get(identifier);
		
		if(Interpreter.instance.assignmentMode && result != null)
			new AssignmentTag(result, this, identifier);

		//if(result != null)
		//	Interpreter.instance.debugTrace.append("object "+this+".getMember("+identifier+") result="+result+"\n");
		
		if(result == null && members.get("parent") != null)
			return members.get("parent").getMember(identifier, true);
		
		return result;
	}
	
	public void putMember(String identifier, CoreObject member, boolean pblic)
	{
		members.put(identifier, member);
		/*if(pblic)
			publicMembers.put(identifier, true);
		else
			publicMembers.remove(identifier);*/
	}
	
	public String getType()
	{
		return("NullObject");
	}
	
	public Double toDouble()
	{
		try
		{
			return Double.parseDouble(value.toString());
		}
		catch (Exception e)
		{
			return new Double(0);
		}
	}
	
	public boolean toBoolean()
	{
		if(value == null) return false;
		return !(value.toString().equals("0") || value.toString().isEmpty() || members.size() == 0);
	}

	public CoreObject init() throws InterpreterException
	{
		return null;
	}
	
	protected CoreObject getGlobalCoreClass(String typeIdentifier)
	{
		return Interpreter.instance.rootContext.getMember(typeIdentifier);
	}
	
	protected CoreObject setGlobalCoreClass(String typeIdentifier, CoreObject o)
	{
		Interpreter.instance.rootContext.members.put(typeIdentifier, o);
		return o;
	}
	
	protected CoreObject getOuterCore() throws InterpreterException
	{
		CoreObject o = getGlobalCoreClass(getType());		
		if(o == null)
			o = setGlobalCoreClass(getType(), init());
		return o;
	}
	
	public String toString()
	{
		if(value != null)
			return value.toString();
		if(members.size() < 2)
			return "";
		return "Object "+members.keySet().toString();
	}

}
