/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package anthologiser;

import java.io.File;
import java.io.FileOutputStream;
import calliope.json.JSONDocument;

/**
 * Represent a JSON config file
 * @author desmond
 */
public class JSONFileItem extends FileItem
{
    JSONDocument jdoc;
    JSONFileItem( File parent, String relPath, File file ) throws Exception
    {
        super( parent, relPath, file, Format.JSON );
        parse();
    }
    JSONFileItem( File parent, String relPath, byte[] contents, String name, 
        String suffix ) throws Exception
    {
        super( parent, relPath, contents, name, suffix );
        parse();
    }
    void add( String key, String value ) throws Exception
    {
        try
        {
            if ( jdoc == null )
                jdoc = new JSONDocument();
            jdoc.add( key, value, false );
        }
        catch ( Exception e )
        {
            throw new Exception( e );
        }
    }
    private void parse()
    {
        try
        {
            byte[] data = getContents();
            String jstr = new String(data,"UTF-8");
            jdoc = JSONDocument.internalise( jstr );
        }
        catch ( Exception e )
        {
        }
    }
    void externalise( File dir, String name ) throws Exception
    {
        File out = new File(dir,name);
        if ( !out.exists() )
            out.createNewFile();
        FileOutputStream fos = new FileOutputStream( out );
        fos.write( jdoc.toString().getBytes("UTF-8") );
        fos.close();
    }
}

