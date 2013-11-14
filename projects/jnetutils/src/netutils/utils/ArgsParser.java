/*
 * Created on 17/10/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package netutils.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;



/**
 * Simple class for getting options from args.<br>
 * for examle "-p 9000 -ip 10.1.11.25 -s -l"<br>
 * The class implements methods for getting the values for options or find if<br>
 * they exists...etc<br>
 *   <br>
 * Example:<br> 
 * <code> 
 * public static void main(String []args)<br>
 * {<br>
 *      LineArgs largs = new LineArgs();<br>
 *<br>
 *		largs.addArg("-ip",1,"[the ip] - the listening ip, mandatory");<br>
 *		largs.addArg("-p",1,"[the port] - the listening port, mandatory");<br>
 *		largs.addArg("-t",1,"[number of threads] - the number of threads used, optional (default=1)",new String[]{"1"});<br>
 *		largs.addArg("-s",0,"no parameters - show statistics to string, optional");<br>
 *		<br>
 *		largs.init(args);<br>
 *		if ( !largs.hasOption("-ip") || !largs.hasOption("-p"))<br>
 *		{<br>
 *			System.out.println("Missing parmeters ip or port");<br>
 *			System.out.println(largs.toString());<br>
 *  			System.exit(-1);<br>
 *		}<br>
 *		<br>
 *		<br>
 *		GenericTCPServer g = new GenericTCPServer(largs.getArgAsString("-ip"),largs.getArgAsInt("-p"));<br>
 *		g.setMyNumOfHandlers(largs.getArgAsInt("-t"));<br>
 *		g.startServer();<br>
 *		if (largs.hasOption("-s"))<br>
 *		while(true)<br>
 *		{<br>
 *			Thread.sleep(10000);<br>
 *			System.out.println(g.getStatistics());<br>
 *		}<br>
 *}<br>
 *	</code>	
 * @author roni bar yanai
 */
public class ArgsParser
{
	private HashMap<String,Param> myArgs = new HashMap<String,Param>();

	private HashSet<String> isMandMap = new HashSet<String>();
	
	private int myLastInitParametersNum = 0;

	
	/**
	 * Parse arguments from string array.
	 * The function should be called after the attributes were configured.
	 * 
	 * 
	 * @param fields -
	 * @param isCaseSensitive 
	 * @return true on success and false otherwise.
	 */
	public boolean init(String fields[],boolean isCaseSensitive)
	{
		int idx = 0;

		try
		{
			while (idx < fields.length)
			{
				String key = fields[idx];
				if (!isCaseSensitive)
					key = key.toLowerCase();
				if (fields[idx].indexOf("-") != -1 && myArgs.containsKey(key))
				{
					if (!isCaseSensitive)
						fields[idx] = fields[idx].toLowerCase();
					Param p = (Param) myArgs.get(fields[idx++]);

					if (p.getNumberOfParameters() == Param.DYNAMIC)
					{
						ArrayList<String> arrlist = new ArrayList<String>();
						while (idx < fields.length && !fields[idx].startsWith("-"))
						{
							arrlist.add(fields[idx]);
							idx++;
						}
						if (arrlist.size() != 0) idx--;
						p.setValues((String[]) arrlist.toArray(new String[0]));
					}
					else
					{
						String values[] = new String[p.getNumberOfParameters()];
						for (int i = 0; i < p.getNumberOfParameters(); i++, idx++)
						{
							values[i] = fields[idx];
						}
						p.setValues(values);
					}
					myLastInitParametersNum++;
				}
				else
				{
					idx++;
				}
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	public boolean init(String fileds[])
	{
		return init(fileds,true);		
	}

	/**
	 * parse the string. after the init called the option can be read.
	 * @param line - the string to be parsed
	 * @return true on init success and false otherwise.
	 */
	public boolean init(String line)
	{
		String[] fileds = line.split(" ");
		return init(fileds);
	}

	/**
	 * Add arg .
	 * @param theSign - the arg sign for example -ip
	 * @param theNumberOfParameters - number of parameters followed
	 * @param theDescription - the arg description for example " [the ip] - the listening ip"
	 */
	public void addArg(String theSign, int theNumberOfParameters, String theDescription, boolean isMandatory)
	{
		Param p = new Param(theSign, theNumberOfParameters, theDescription);
		myArgs.put(theSign, p);
		if (isMandatory)
		{
			isMandMap.add(theSign);
		}
	}
	
	public void addArg(String theSign, int theNumberOfParameters, String theDescription)
	{
		addArg(theSign,theNumberOfParameters,theDescription,false);
	}

	/**
	 * Add dynamic arg. the number of values will be all values until next "-" encountered.
	 * @param theSign
	 * @param theDescription
	 */
	public void addDynamicArg(String theSign, String theDescription)
	{
		Param p = new Param(theSign, Param.DYNAMIC, theDescription);
		myArgs.put(theSign, p);
	}

	/**
	 * Add arg.
	 * @param theSign - the arg sign for example -ip
	 * @param theNumberOfParameters - number of parameters followed
	 * @param theDescription - the arg description for example " [the ip] - the listening ip"
	 * @param theDefaultValue - the arg default value.
	 */
	public void addArg(String theSign, int theNumberOfParameters, String theDescription, String[] theDefaultValue, boolean isMandatory)
	{
		Param p = new Param(theSign, theNumberOfParameters, theDescription, theDefaultValue);
		myArgs.put(theSign, p);
		
		if(isMandatory)
		{
			isMandMap.add(theSign);
		}
	}
	
	public void addArg(String theSign, int theNumberOfParameters, String theDescription, String[] theDefaultValue)
	{
		addArg(theSign, theNumberOfParameters, theDescription,false);
	}
	
	public boolean isValid(StringBuffer err)
	{
		for(String sign : isMandMap)
		{
			if(!hasOption(sign))
			{
				err.append(sign+" is mandatory\n");
			}
		}
		
		return err.length() == 0;
	}

	/**
	 * Get the arg value.
	 * if arg not exist then 0 will be returned.
	 * (use hasOption to check that value appeard in the line)
	 * @param theSign - the sign for example "-ip"
	 * @return the option
	 * may throw runtime exception if option don't exists.
	 */
	public int getArgAsInt(String theSign)
	{
		if (myArgs.containsKey(theSign))
		{
			return Integer.parseInt(((Param) myArgs.get(theSign)).getValues()[0]);
		}
		else
			throw new OptionNotExistsException();
	}

	/**
	 * Get the arg value.
	 * @param theSign
	 * @return the option as double.
	 * may throw runtime exception if option don't exists.
	 */
	public double getArgAsDouble(String theSign)
	{
		if (myArgs.containsKey(theSign))
		{
			return Double.parseDouble(((Param) myArgs.get(theSign)).getValues()[0]);
		}
		else
			throw new OptionNotExistsException();
	}

	/**
	 * return the parameter as a String.
	 * if parameter don't exists then empty string will be returned.
	 * @param theSign
	 * @return the value
	 * may throw runtime exception if option don't exists.
	 */
	public String getArgAsString(String theSign)
	{
		if (myArgs.containsKey(theSign))
		{
			return ((Param) myArgs.get(theSign)).getValues()[0];
		}
		throw new OptionNotExistsException();
	}
	
	public String getArgAsString(String theSign,int parameterIndex)
	{
		if (myArgs.containsKey(theSign))
		{
			return ((Param) myArgs.get(theSign)).getValues()[parameterIndex];
		}
		throw new OptionNotExistsException();
	}

	public boolean getArgAsBoolean(String theSign)
	{
		if (myArgs.containsKey(theSign))
		{
			return (((Param) myArgs.get(theSign)).getValues()[0].toLowerCase().indexOf("true") != -1);
		}
		throw new OptionNotExistsException();
	}

	/**
	 * @param theSign
	 * @return all values attached to the sign.
	 */
	public String[] getAllArgsValues(String theSign)
	{
		if (myArgs.containsKey(theSign))
		{
			return ((Param) myArgs.get(theSign)).getValues();
		}
		throw new OptionNotExistsException();
	}

	/**
	 * @param theSign
	 * @param thedefault
	 * @return the sign value or the defualt if not exists.
	 */
	public boolean getArgAsBoolean(String theSign, boolean thedefault)
	{
		if (!hasOption(theSign)) return thedefault;
		if (myArgs.containsKey(theSign))
		{
			return (((Param) myArgs.get(theSign)).getValues()[0].toLowerCase().indexOf("true") != -1);
		}
		return thedefault;
	}

	/**
	 * @param theSign
	 * @return true if option exists in the line and else otherwise.
	 */
	public boolean hasOption(String theSign)
	{
		if (myArgs.containsKey(theSign))
		{
			return ((Param) myArgs.get(theSign)).isConfigured();
		}
		return false;
	}
	
	public int getOptionNumOfParameters(String theSign)
	{
		if (myArgs.containsKey(theSign))
		{
			return ((Param) myArgs.get(theSign)).getNumberOfParameters();
		}
		return -1;
	}

	/**
	 * @return true if not known parameters were received in the line args and else otherwise.<br>
	 */
	public boolean isEmpty()
	{
		return (myLastInitParametersNum == 0);
	}

	/**
	 * Return all the posible prameters and thier description
	 */
	public String toString()
	{
		String result = "args:\n";
		for( Param p: myArgs.values())
		{
			result = result + p.getDescription() + "\n";

		}
		return result;
	}
	
	public String[] allSigns()
	{
		String[] result = new String[myArgs.size()];
		int i = 0;
		for (Iterator it = myArgs.keySet().iterator(); it.hasNext();)
		{
			result[i] = (String)it.next();
			i++;
		}
		return result;
	}
}

class OptionNotExistsException extends RuntimeException
{
	public OptionNotExistsException()
	{
		super("Don't have that option");
	}

}

/**
 * Data structure for holding the possible args, thier descripation values..
 * @author ronyb
 *
 */
class Param
{
	public static final int DYNAMIC = -1;

	private String name = null;

	private String sign = null;

	private int numberOfParameters = -2;

	private String descripation = null;

	private String[] values = null;

	private boolean isConfigured = false;

	/**
	 * @param sign
	 * @param numberOfParameters
	 * @param descripation
	 */
	public Param(String sign, int numberOfParameters, String descripation)
	{
		super();
		this.sign = sign;
		this.numberOfParameters = numberOfParameters;
		this.descripation = descripation;
	}

	/**
	 * @param sign
	 * @param numberOfParameters
	 * @param descripation
	 * @param values
	 */
	public Param(String sign, int numberOfParameters, String descripation, String[] values)
	{
		super();
		this.sign = sign;
		this.numberOfParameters = numberOfParameters;
		this.descripation = descripation;
		this.values = values;
	}

	public String getDescription()
	{
		return sign + " " + descripation;
	}

	public String[] getValues()
	{
		return values;
	}

	/**
	 * @param values
	 */
	public void setValues(String[] values)
	{
		this.values = values;
		isConfigured = true;
	}

	/**
	 * @return true if option was configured.
	 */
	public boolean isConfigured()
	{
		return isConfigured;
	}

	protected String getSign() {
		return sign;
	}

	protected int getNumberOfParameters()
	{
		return numberOfParameters;
	}

	
}