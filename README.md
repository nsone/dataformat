# Dataformat

Dataformat is a binary format to POJO converter

## Examples

### Read from java.io.InputStream

```
PDUInputStream<FrameHeader> is = new PDUInputStream<>(FrameHeader.class, parentIs);
FrameHeader frame = is.readPDU();
if(frame.getEthertype == Ethertype.ARP)
{
   Arp arp = (Arp) frame;
}
```

### Write to java.io.OutputStream

```
PDUOutputStream<FrameHeader> os = new PDUOutputStream<>(parentOs);
os.write(arp);
```