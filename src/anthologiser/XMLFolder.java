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
import java.io.FileOutputStream;
import java.io.FileInputStream;
/**
 * Manage an XML format folder
 * @author desmond
 */
public class XMLFolder extends Folder
{
    XMLFolder( File src, File dst ) throws Exception
    {
        super( src, dst );
        if ( src != null )
            internalise( src );
    }
    private void readFile( File f, String relPath ) throws Exception
    {
        boolean success = true;
        File file = null;
        File dstFolder = new File( dst, relPath );
        if ( !dstFolder.exists() )
            success = dstFolder.mkdirs();
        if ( success )
        {
            file = new File( dstFolder, f.getName() );
            success = file.createNewFile();
        }
        if ( success )
        {
            FileInputStream fis = new FileInputStream( f );
            byte[] data = new byte[(int)f.length()];
            fis.read( data );
            FileOutputStream fos = new FileOutputStream( file );
            fos.write( data );
            fos.close();
        }
        else
            throw new Exception("Couldn't create "+dst.getPath());
    }
    private void readDir( File dir, String relPath ) throws Exception
    {
        File[] contents = dir.listFiles();
        for ( int i=0;i<contents.length;i++ )
        {
            if ( contents[i].isDirectory() )
                readDir( contents[i], relPath+"/"+contents[i].getName() );
            else
                readFile( contents[i], relPath );
        }
    }
    protected void internalise( File dir ) throws Exception
    {
        File[] contents = dir.listFiles();
        for ( int i=0;i<contents.length;i++ )
        {
            if ( contents[i].isDirectory() )
                readDir( contents[i], contents[i].getName() );
            else
                readFile( contents[i], "" );
        }
    }
    /**
     * Save an entire directory
     * @param dst the directory to save dir in
     * @param dir the source directory to save in dst
     * @throws Exception 
     */
    private void saveDir( File dst, File dir ) throws Exception
    {
        File[] contents = dir.listFiles();
        for ( int i=0;i<contents.length;i++ )
        {
            if ( contents[i].isDirectory() )
            {
                File newDst = new File( dst, contents[i].getName() );
                saveDir( newDst, contents[i] );
            }
            else
                saveFile( dst, contents[i] );
        }
    }
    /**
     * Save an individual file
     * @param dir the dir to save it in
     * @param src the source file
     * @throws Exception 
     */
    private void saveFile( File dir, File src ) throws Exception
    {
        FileInputStream fis = new FileInputStream(src);
        int len = (int)src.length();
        byte[] data = new byte[len];
        fis.read( data );
        File dst = new File( dir, src.getName() );
        FileOutputStream fos = new FileOutputStream( dst );
        fos.write( data );
        fos.close();
    }
    /**
     * Write out the contents to the chosen folder
     * @param dir the destination docid folder (%foo)
     * @throws Exception 
     */
    protected void externalise( File dir ) throws Exception
    {
        boolean success = true;
        File fmtDir = new File( dir, this.dst.getName() );
        if ( !fmtDir.exists() )
            success = fmtDir.mkdirs();
        if ( success )
        {
            File[] contents = this.dst.listFiles();
            for ( int i=0;i<contents.length;i++ )
            {
                if ( contents[i].isDirectory() )
                    saveDir( new File(fmtDir,contents[i].getName()), contents[i] );
                else
                    saveFile( fmtDir, contents[i] );
            }
        }
        else
            throw new Exception("Couldn't create directory "+fmtDir );
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
