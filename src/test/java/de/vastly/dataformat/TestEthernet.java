package de.vastly.dataformat;

import static org.junit.Assert.*;

import java.net.InetAddress;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;

import de.sloc.dataformat.PDU;
import de.sloc.proto.ethernet.FrameHeader;
import de.sloc.proto.ethernet.IPv4;
import de.sloc.proto.ethernet.IPv4TCP;
import de.sloc.proto.ethernet.Constants.Ethertype;

public class TestEthernet
{

	@Test
	public void testSimple() throws Exception
	{
		byte[] ethernetFrame = DatatypeConverter.parseHexBinary("24651141c9c3d0e782f0bb750800450000bdfae840004006f3c4c0a86523c0a865191f49ddada00403fe491ef95950180294f73b000017030200909ab804c5ae4c32eca24fdab9282cd08d57170c61ce6322daf4cf04d1b970a4826760738791e03b16221f6dff0878dfc54d7dbc8809db9013fe91f7b08c78bfb88529208d8b02ea07956be8035265befc0c0ca0a24aa8e9e72bf40ce364b99dcaf9a0755ccb2589518ca2a88334c6a89fc84718007a051a1615fd470257824401278ff67b568973fd6e58db92dd30a2b2");

		FrameHeader frame = PDU.decode(ethernetFrame, FrameHeader.class, 0);

		assertTrue(frame instanceof IPv4TCP);

		IPv4TCP tcp = (IPv4TCP) frame;

		assertEquals(Ethertype.IPV4, tcp.getEthertype());
		assertEquals(64, tcp.getTtl());

		assertEquals(InetAddress.getByName("192.168.101.35"), tcp.getSourceIPAddress().getInetAddress());
		assertEquals(InetAddress.getByName("192.168.101.25"), tcp.getDestinationIPAddress().getInetAddress());
		assertEquals(8009, tcp.getSourcePort());
		assertEquals(56749, tcp.getDestinationPort());

		System.out.println(PDU.dump(tcp));
	}
}
