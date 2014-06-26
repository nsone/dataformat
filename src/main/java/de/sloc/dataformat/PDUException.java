package de.sloc.dataformat;

import java.io.IOException;

public class PDUException extends IOException
{
	private static final long serialVersionUID = -3845728142339357291L;

	public PDUException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public PDUException(String message)
	{
		super(message);
	}

}
