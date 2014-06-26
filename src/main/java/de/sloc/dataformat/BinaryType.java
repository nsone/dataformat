package de.sloc.dataformat;

public interface BinaryType
{
	public byte[] getValue();

	public int getLength();

	public <T> T to(Class<T> klass) throws PDUException;

}
