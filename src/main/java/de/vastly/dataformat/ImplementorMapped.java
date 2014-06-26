package de.vastly.dataformat;

public interface ImplementorMapped
{
	public Class<? extends PDUSerializable> getImplementor(String... args);
}
