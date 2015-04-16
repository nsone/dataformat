package de.sloc.dataformat;

public interface Converter
{

    public byte[] export();

    public static final String FACTORY_METHOD_NAME = "rawImport";


}
