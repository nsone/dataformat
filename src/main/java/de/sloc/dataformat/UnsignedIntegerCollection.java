package de.sloc.dataformat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UnsignedIntegerCollection implements BinaryType
{
	protected byte[] numberCollection;
	protected Class<?> elementType;
	protected int unsignedIntegerLength;

	public UnsignedIntegerCollection(Collection<?> numbers, int length, String[] args) throws PDUException
	{
		if (length != -1)
		{
			throw new IllegalArgumentException("length given: UnsignedIntegerCollection length is determined by component type");
		}

		parseArgs(args);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try
		{
			for (Object numberObject : numbers)
			{
				Number number = null;
				if (numberObject instanceof Number)
				{
					number = (Number) numberObject;
				}
				else if (numberObject instanceof AsNumber)
				{
					number = ((AsNumber) numberObject).getNumberValue();
				}
				else
				{
					throw new IllegalArgumentException("Unsupported number type: " + numberObject.getClass());
				}

				baos.write(new UnsignedInteger(number, this.unsignedIntegerLength).getValue());
			}
		}
		catch (IOException e)
		{
			// shouldn't happen
			e.printStackTrace();
		}

		this.numberCollection = baos.toByteArray();
	}

	public UnsignedIntegerCollection(byte[] data, int length, String[] args)
	{
		parseArgs(args);

		this.numberCollection = data;
	}

	@SuppressWarnings("unchecked")
	protected void parseArgs(String[] args)
	{
		if (args == null || args.length < 2)
		{
			throw new IllegalArgumentException("Please provide the element type as args[0] and the UnsignedInteger length as args[1]");
		}
		this.elementType = (Class<? extends Number>) PDU.resolveElementType(args[0]);
		this.unsignedIntegerLength = Integer.parseInt(args[1]);
	}

	@Override
	public byte[] getValue()
	{
		return numberCollection;
	}

	@Override
	public int getLength()
	{
		return numberCollection.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Class<T> klass) throws PDUException
	{
		Collection<Object> result;

		if (Set.class.isAssignableFrom(klass))
		{
			result = new HashSet<>();
		}
		else if (List.class.isAssignableFrom(klass))
		{
			result = new ArrayList<>();
		}
		else
		{
			throw new PDUException("Cannot convert from UNSIGNED_INTEGER_COLLECTION to " + klass);
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(numberCollection);

		try
		{
			for (int i = 0; i < numberCollection.length; i += unsignedIntegerLength)
			{
				byte[] slice = new byte[unsignedIntegerLength];
				bais.read(slice);
				result.add(new UnsignedInteger(slice, unsignedIntegerLength).to(elementType));
			}
		}
		catch (IOException e)
		{
			// shouldn't happen
			e.printStackTrace();
		}

		return (T) result;
	}

}
