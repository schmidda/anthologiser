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
import java.util.ArrayList;
/**
 * An item in a FileMap. A set of files with a single MVD key.
 * @author desmond
 */
public class FolderItem 
{
    /** An array of items within that directory */
    ArrayList<FileItem> items;
    /**
     * Read the contents of a directory and record its relative path
     * @param relPath the relative path to, and including this dir
     * @param parent the temp dir to store it in
     * @param dir the directory itself
     * @throws Exception 
     */
    final void readDir( String relPath, File parent, File dir ) throws Exception
    {
        File[] files = dir.listFiles();
        for ( int i=0;i<files.length;i++ )
        {
            if ( files[i].isDirectory() )
            {
                relPath += "/";
                relPath += files[i].getName();
                readDir( relPath, parent, files[i] );
            }
            else
            {
                FileItem fi = new FileItem( parent, relPath, files[i] );
                items.add( fi );
            }
        }
    }
    /**
     * Read in a directory
     * @param parent the parent dir to store it in temporarily
     * @param dir the folder item
     */
    public FolderItem( File parent, File dir ) throws Exception
    {
        items = new ArrayList<FileItem>();
        File[] files = dir.listFiles();
        for ( int i=0;i<files.length;i++ )
        {
            if ( files[i].isDirectory() )
            {
                readDir( files[i].getName(), parent, files[i] );
            }
            else
            {
                FileItem fi = new FileItem( parent,"",files[i] );
                items.add( fi );
            }
        }
    }
    /**
     * Create an empty folder item with a name
     */
    public FolderItem()
    {
        items = new ArrayList<FileItem>();
    }
    /**
     * Add a virtual file to the folder
     * @param parent the temp dir to store it in
     * @param data the file's data content
     * @param relPath the notional relative (group) path
     * @param name the name of the file
     * @param suffix its suffix
     * @throws Exception 
     */
    public void add( File parent, byte[] data, String relPath, 
        String name, String suffix ) throws Exception
    {
        String path = FileItem.makePath( relPath, name+suffix );
        int i;
        for ( i=0;i<items.size();i++ )
        {
            if ( items.get(i).path.equals(path) )
                break;
        }
        if ( i < items.size() )
            items.remove( i );
        FileItem item = new FileItem( parent, relPath, data, name, suffix );
        items.add( item );
    }
    /**
     * Save this poem to disk
     * @param folder the folder to put it in
     * @param key the FolderItem's key in the FileMap
     * @throws Exception 
     */
    public void save( File folder, String key ) throws Exception
    {
        File dst = new File( folder, key );
        if ( !dst.exists() )
            dst.mkdir();
        for ( int i=0;i<items.size();i++ )
        {
            FileItem fi = items.get( i );
            fi.save( dst );
        }
    }
}
