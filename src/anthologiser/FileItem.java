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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
/**
 * An individual file and its content stored externally.
 * @author desmond
 */
public class FileItem 
{
    /** the file's content (don't store it in memory) */
    File cache;
    /** relative path from the FolderItem to here, including file name */
    String path;
    final byte[] readFile( File src ) throws Exception
    {
        FileInputStream fis = new FileInputStream( src );
        byte[] content = new byte[(int)src.length()];
        fis.read( content );
        fis.close();
        return content;
    }
    /**
     * Write the file item to temporary or permanent storage
     * @param dst the destination file
     * @param content its content
     * @throws Exception 
     */
    final void writeFile( File dst, byte[] content ) throws Exception
    {
        FileOutputStream fos = new FileOutputStream( dst );
        fos.write( content );
        fos.close();
    }
    /**
     * Make a temporary file when extracting data from a larger file
     * @param parent the temporary directory to store it in
     * @param relPath the relative path
     * @param contents the contents to store there
     * @param name the name of the "file"
     * @param suffix its desired suffix
     * @throws Exception 
     */
    public FileItem( File parent, String relPath, byte[] contents, String name, 
        String suffix ) throws Exception
    {
        this.path = makePath( relPath, name+suffix );
        cache = File.createTempFile( "ANTH", suffix, parent );
        writeFile( cache, contents );
    }
    /**
     * Make the path from the relPath (often empty) and the file name+suffix
     * @param relPath the relative path from the folderItem to the file
     * @param name the name including the suffix
     * @return the relative path to the file
     */
    public static String makePath( String relPath, String name )
    {
        if ( relPath.length()>0 )
            relPath += "/";
        return relPath+name;
    }
    /**
     * Create a temporary file when we have a file in an existing directory
     * @param parent the place to store it temporarily
     * @param relPath the relative path from the folder item
     * @param file the original file to copy
     * @throws Exception 
     */
    public FileItem( File parent, String relPath, File file ) throws Exception
    {
        // copy the file to temporary store
        this.path = makePath( relPath, file.getName() );
        byte[] content = readFile( file );
        int index = file.getName().lastIndexOf(".");
        String suffix = "";
        if ( index != -1 )
            suffix = file.getName().substring(index);
        cache = File.createTempFile( "ANTH", suffix, parent );
        writeFile( cache, content );
        file.delete();
    }
    /**
     * Save the file in a new parent directory
     * @param dst the new destination dir (not temporary)
     */
    public void save( File dst ) throws Exception
    {
        File newLoc = new File( dst, path );
        byte[] content = readFile( cache );
        writeFile( newLoc, content );
    }
}
