/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package anthologiser;
import java.io.File;
/**
 * Manage an TEXT format folder
 * @author desmond
 */
public class TextFolder extends Folder
{
    TextFolder( File src, File dst ) throws Exception
    {
        super( src, dst );
    }
    protected void internalise( File dir ) throws Exception
    {
    }
    protected void externalise( File dir ) throws Exception
    {
    }
    /**
     * Add a file to the folder
     * @param data the data the file contains
     * @param relPath the relative path from the folder to it
     * @param name the name of the file minus the suffix
     * @param suffix the suffix without the dot
     */
    protected void add( byte[] data, String relPath, String name, 
        String hVersion, String suffix )
    {
        // implement later
    }
}
