package de.vastly.dataformat;

public class Padding implements BinaryType
{
	protected byte[] padding;

	public Padding(int length)
	{
		this.padding = new byte[length];
	}

	public Padding(byte[] data, int length, String[] args)
	{
		this.padding = data;
	}

	@Override
	public byte[] getValue()
	{
		return padding;
	}

	@Override
	public int getLength()
	{
		return padding.length;
	}

	@Override
	public <T> T to(Class<T> klass)
	{
		return null;
	}

}
