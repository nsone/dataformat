package de.sloc.dataformat;

import java.io.Serializable;

public interface AsNumber extends Serializable
{
	public Number getNumberValue();

	public static final String FACTORY_METHOD_NAME = "getByValue";


}
