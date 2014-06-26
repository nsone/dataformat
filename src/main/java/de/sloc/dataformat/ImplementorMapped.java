package de.sloc.dataformat;

public interface ImplementorMapped
{
	public Class<? extends PDUSerializable> getImplementor(String... args);
}
