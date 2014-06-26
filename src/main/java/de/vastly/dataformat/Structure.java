package de.vastly.dataformat;

public class Structure implements BinaryType
{
	protected byte[] structure;
	protected int offset;

	public Structure(PDUSerializable packetSerializable, int length, String[] args) throws PDUException
	{
		if (length != -1)
		{
			throw new IllegalArgumentException("length given: structure length is dynamically determined by subtype");
		}

		this.structure = PDU.encode(packetSerializable);
		this.offset = args.length > 0 ? Integer.parseInt(args[0]) : 0;
	}

	public Structure(byte[] value, int length, String[] args)
	{
		this.structure = value;
		this.offset = args.length > 0 ? Integer.parseInt(args[0]) : 0;
	}

	@Override
	public byte[] getValue()
	{
		return structure;
	}

	@Override
	public int getLength()
	{
		return structure.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T to(Class<T> klass) throws PDUException
	{
		return (T) PDU.decode(structure, (Class<? extends PDUSerializable>) klass, offset);
	}

}
