/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package anthologiser;

import java.io.File;

/**
 * Abstract Folder class
 * @author desmond
 */
abstract class Folder 
{
    String configName;
    JSONFileItem config;
    /** the src format directory to read */
    File dst;
    /**
     * Called by factory method
     * @param src the folder we are reading whose name is this Format
     * @param relPath the relative path from the MVD folder to the format folder
     * @throws Exception 
     */
    protected Folder( File src, File dst ) throws Exception
    {
        this.dst = dst;
        configName = "config.conf";
        if ( !dst.exists() )
        {
            boolean success = dst.mkdirs();
            if ( !success )
                throw new Exception("Couldn't create type folder");
        }
    }
    protected abstract void internalise( File dir ) throws Exception;
    /**
     * Externalise the folder's content
     * @throws Exception 
     */
    protected abstract void externalise( File dir ) throws Exception;
    /**
     * Add one key to the config contents
     * @param sb the string being built of the config file
     * @param key the JSON key
     * @param value the JSON value
     */
    void addConfigKey( String key, String value ) throws Exception
    {
        if ( config == null )
        {
            File file = new File( dst, configName );
            config = new JSONFileItem( dst, "", file );
        }
        config.add( key, value );
    }
    /**
     * Turn this folder's current options into a config file's contents
     * @return the contents of the config file
     */
    protected String composeConfig()
    {
        if ( config != null )
            return config.toString();
        else
            return null;
    }
    /**
     * Add a file to the folder
     * @param data the data the file contains
     * @param relPath the relative path from the folder to it
     * @param name the name of the file minus the suffix
     * @param hVersion the individual version ID
     * @param suffix the suffix without the dot
     */
    protected abstract void add( byte[] data, String relPath, String name, 
        String hVersion, String suffix ) throws Exception;
    /**
     * Get our intrinsic format
     * @return the Format
     */
    Format getFormat() throws Exception
    {
        return Format.valueOf(this.dst.getName());
    }
}
