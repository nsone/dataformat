package de.sloc.proto;

import java.util.HashMap;
import java.util.Map;

import de.sloc.dataformat.AsNumber;
import de.sloc.dataformat.ImplementorMapped;
import de.sloc.dataformat.PDUSerializable;

public interface Constants
{

	public enum Ethertype implements AsNumber, ImplementorMapped
	{
		IPV4(0x0800, IPv4.class),
		ARP(0x0806, Arp.class),
		IPV6(0x86DD, RawFrame.class),
		MPLS_UNICAST(0x8847, RawFrame.class),
		MPLS_MULTICAST(0x8847, RawFrame.class),
		VLAN(0x8100, RawFrame.class),
		LLDP(0x88CC, RawFrame.class),

		OF_LINK_DISCOVERY(0x0F1D, OFLinkDiscovery.class),
		UNKNOWN(0xFFFF, RawFrame.class);

		protected static Map<Integer, Ethertype> reverseMap = new HashMap<>();

		static
		{
			for (Ethertype type : Ethertype.values())
			{
				reverseMap.put(type.getValue(), type);
			}
		}

		protected int value;
		protected Class<? extends FrameHeader> implementor;

		Ethertype(int value, Class<? extends FrameHeader> implementor)
		{
			this.value = value;
			this.implementor = implementor;
		}

		public int getValue()
		{
			return value;
		}

		@Override
		public Class<? extends PDUSerializable> getImplementor(String... args)
		{
			return implementor;
		}

		@Override
		public Number getNumberValue()
		{
			return value;
		}

		public static Ethertype getByValue(int value)
		{
			Ethertype result = reverseMap.get(value);

			if (result == null)
			{
				result = UNKNOWN;
				result.value = value;
			}

			return result;
		}

	}

	public static enum ArpAddressType implements AsNumber
	{
		ETHERNET(1);

		protected int value;
		protected static Map<Integer, ArpAddressType> reverse = new HashMap<>();

		static
		{
			for (ArpAddressType type : ArpAddressType.values())
			{
				reverse.put(type.getValue(), type);
			}
		}

		ArpAddressType(int value)
		{
			this.value = value;
		}

		@Override
		public Number getNumberValue()
		{
			return value;
		}

		public int getValue()
		{
			return value;
		}

		public static ArpAddressType getByValue(int value)
		{
			return reverse.get(value);
		}

	}

	public static enum ArpOperation implements AsNumber
	{
		REQUEST(1), REPLY(2), REQUEST_REVERSE(3), REPLY_REVERSE(4);

		protected int value;
		protected static Map<Integer, ArpOperation> reverse = new HashMap<>();

		static
		{
			for (ArpOperation type : ArpOperation.values())
			{
				reverse.put(type.getValue(), type);
			}
		}

		ArpOperation(int value)
		{
			this.value = value;
		}

		@Override
		public Number getNumberValue()
		{
			return value;
		}

		public int getValue()
		{
			return value;
		}

		public static ArpOperation getByValue(int value)
		{
			return reverse.get(value);
		}
	}

	public static enum IPProtocol implements AsNumber, ImplementorMapped
	{
		ICMP(1, ICMPv4.class), TCP(0x06, IPv4TCP.class), UNKNOWN(0xFF, IPv4Unknown.class);

		protected int value;
		protected static Map<Integer, IPProtocol> reverse = new HashMap<>();
		protected Class<? extends PDUSerializable> klass;

		static
		{
			for (IPProtocol type : IPProtocol.values())
			{
				reverse.put(type.getValue(), type);
			}
		}

		IPProtocol(int value, Class<? extends PDUSerializable> klass)
		{
			this.value = value;
			this.klass = klass;
		}

		@Override
		public Number getNumberValue()
		{
			return value;
		}

		public int getValue()
		{
			return value;
		}

		public static IPProtocol getByValue(int value)
		{
			IPProtocol result = reverse.get(value);
			if (result == null)
			{
				result = UNKNOWN;
				result.value = value;
			}

			return result;
		}

		@Override
		public Class<? extends PDUSerializable> getImplementor(String... args)
		{
			return klass;
		}
	}

}
