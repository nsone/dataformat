package de.vastly.dataformat;

public class FixedLengthString implements BinaryType
{
	protected byte[] data;

	public FixedLengthString(byte[] data, int length, String[] args)
	{
		this.data = data;
	}

	public FixedLengthString(String string, int length, String[] args)
	{
		int stringLength = string.length();
		if (stringLength > length)
		{
			throw new IllegalArgumentException("String " + string + " is too long: " + stringLength + " max: " + length);
		}

		this.data = new byte[string.length()];
		System.arraycopy(string.getBytes(), 0, this.data, 0, stringLength);
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
		if (klass.isAssignableFrom(String.class))
		{
			return (T) new String(this.data).trim();
		}

		throw new IllegalArgumentException("Cannot convert FIXED_LENGTH_STRING to " + klass.getCanonicalName());
	}

}
