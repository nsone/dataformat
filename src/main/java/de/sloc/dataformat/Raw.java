package de.sloc.dataformat;

public class Raw implements BinaryType
{
	protected byte[] data;

	public Raw(Byte data, int length, String[] args)
	{
		this.data = new byte[] { data };
	}

	public Raw(byte[] data, int length, String[] args)
	{
		this.data = data;
	}

	@Override
	public byte[] getValue()
	{
		return data;
	}

	@Override
	public int getLength()
	{
		return data.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Class<T> klass)
	{
		// convert to byte[]
		if (klass.isArray() && klass.getComponentType() == byte.class)
		{
			return (T) data;
		}
		else if (klass.isAssignableFrom(byte.class) || klass.isAssignableFrom(Byte.class))
		{
			return (T) (Byte) data[0];
		}

		throw new IllegalArgumentException("Cannot convert RAW to " + klass.getCanonicalName());
	}

}
