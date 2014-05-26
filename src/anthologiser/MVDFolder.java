/*
 * This file is part of Anthologiser.
 * Anthologiser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Anthologiser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Anthologiser.  If not, see <http://www.gnu.org/licenses/>.
 */
package anthologiser;
import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
/**
 * Represent an MVD directory and create/read its structure on disk
 * @author desmond
 */
public class MVDFolder extends Folder
{
    HashMap<String,String> corcodes;
    String cortex;
    MVDFolder( File src, File dst ) throws Exception
    {
        super( src, dst );
        corcodes = new HashMap<String,String>();
        configName = "cortex.conf";
        internalise( src );
    }
    /**
     * Write an MVD folder and its contents to disk
     * @param dir the destination directory
     * @throws Exception 
     */
    public void externalise( File dir ) throws Exception
    {
        File mvdDir = new File( dir, Format.MVD.toString() );
        if ( !mvdDir.exists() )
            mvdDir.mkdir();
        String config = composeConfig();
        writeFile( mvdDir, configName, config, null );
        writeFile( mvdDir, "cortex.mvd", cortex, "UTF-8" );
        File corcodeDir = new File( mvdDir, "corcode" );
        if ( !corcodeDir.exists() )
            corcodeDir.mkdir();
        Set<String> keys = corcodes.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            String key = iter.next();
            writeFile( corcodeDir, key, corcodes.get(key), "UTF-8" );
        }
    }
    /**
     * Write out a file
     * @param parent the directory to write it in
     * @param name the name of the file
     * @param content the content as a String
     * @param enc the encoding or null for the default
     */
    void writeFile( File parent, String name, String content, String enc )
        throws Exception
    {
        if ( content != null )
        {
            File dst = new File( parent, name );
            FileOutputStream fos = new FileOutputStream( dst );
            byte[] data = (enc==null)?content.getBytes():content.getBytes(enc);
            fos.write( data );
            fos.close();
        }
    }
    /**
     * Read a file's content
     * @param file the directory to write it in
     * @param enc the file's encoding
     * @return the content as a String encoded in enc
     */
    String readFile( File file, String enc ) throws Exception
    {
        int len = (int)file.length();
        FileInputStream fis = new FileInputStream( file );
        byte[] data = new byte[len];
        fis.read( data );
        String str = (enc==null)?new String(data):new String(data,enc);
        fis.close();
        return str;
    }
    /**
     * @throws Exception 
     */
    public void internalise( File dir ) throws Exception
    {
        File[] contents = dir.listFiles();
        for ( int i=0;i<contents.length;i++ )
        {
            if ( contents[i].getName().equals("cortex.mvd") )
            {
                cortex = readFile(contents[i], null);
            }
            else if ( contents[i].getName().endsWith(".conf") )
            {
                config = new JSONFileItem( dir, contents[i].getName(), 
                    contents[i] );
            }
            else if ( contents[i].isDirectory() 
                && contents[i].getName().equals("corcode") )
            {
                File[] files = contents[i].listFiles();
                for ( int j=0;j<files.length;j++ )
                {
                    String corcode = readFile( files[j], "UTF-8" );
                    corcodes.put( files[j].getName(), corcode );
                }
            }
        }
    }
    void addCorCode( String name, String corcode )
    {
        corcodes.put( name, corcode );
    }
    /**
     * Add a file to the folder
     * @param data the data the file contains
     * @param relPath the relative path from the folder to it
     * @param name the name of the file minus the suffix
     * @param suffix the suffix without the dot
     */
    protected void add( byte[] data, String relPath, String name, 
        String hVersion, String suffix ) throws Exception
    {
        boolean success = true;
        if ( !dst.exists() )
            success = dst.mkdirs();
        if ( success )
        {
            File f = new File( dst, relPath );
            if ( !f.exists() )
                success = f.mkdirs();
            if ( success )
            {
                if ( hVersion != null )
                    f = new File( f, name+"#"+hVersion+suffix );
                else
                    f = new File( f, name+suffix );
                success = f.createNewFile();
                if ( !success && f.exists() )
                    System.out.println("File already exists!");
                if ( success )
                {
                    FileOutputStream fos = new FileOutputStream( f );
                    fos.write( data );
                    fos.close();
                }
                else
                    throw new Exception("Couldn't create file "+f.getName());
            }
            else
                throw new Exception("Couldn't create directory "+f.getPath());
        }
        else
            throw new Exception("Couldn't create directory "+dst.getPath());
    }
}
