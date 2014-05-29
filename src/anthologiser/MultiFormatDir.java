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
import java.util.ArrayList;
import calliope.json.JSONDocument;
/**
 * An item in a FileMap. A set of equivalent Folders with a single MVD key.
 * @author desmond
 */
public class MultiFormatDir 
{
    /** An array of items within that directory:
     * You can have several equivalent formats */
    ArrayList<Folder> items;
    /** optional config */
    JSONFileItem config;
    /** The parent temporary dir we are inside */
    File parent;
    String relPath;
    JSONDocument newConf;
    /**
     * Read the contents of a directory and record the relative path of items
     * @param parent the temp dir to store it in
     * @param dir the directory to read from
     * @throws Exception 
     */
    final void readDir( File dir ) throws Exception
    {
        File[] files = dir.listFiles();
        for ( int i=0;i<files.length;i++ )
        {
            if ( files[i].isDirectory() )
            {
                // if there is a folder it must be a format
                try
                {
                    Format f = Format.valueOf(files[i].getName());
                    Folder folder = FolderFactory.makeFolder( files[i], 
                        parent, f );
                    items.add( folder );
                }
                catch ( Exception e )
                {
                    throw new Exception( "Invalid format folder "
                        +files[i].getName());
                }
            }
            else
            {
                // there might be a .conf file here
                if ( files[i].getName().endsWith(".conf") )
                {
                    JSONFileItem jfi = new JSONFileItem(parent,"",files[i]);
                    if ( config != null )
                        config.jdoc.merge(jfi.jdoc );
                    else
                        config = jfi;
                }
                // ignore all other files
            }
        }
    }
    public String getTitle()
    {
        String title = "";
        if ( newConf != null && newConf.containsKey(JSONKeys.TITLE) )
            title = (String)newConf.get(JSONKeys.TITLE);
        else if ( config != null && config.jdoc.containsKey(JSONKeys.TITLE) )
            title = (String)config.jdoc.get(JSONKeys.TITLE);
        return title;   
    }
    /**
     * Add a key-value pair to the folder
     * @param key the key such as "title"
     * @param value the value such as the poem's title
     */
    public void addConfigPair( String key, Object value ) throws Exception
    {
        if ( value instanceof String )
            value = Utils.cleanCR((String)value,true);
        if ( newConf != null )
        {
            newConf.put(key,value);
        }
        else if ( config == null )
        {
            newConf = new JSONDocument();
            newConf.put(key,value);
        }
        else
            config.jdoc.put( key, value );
    }
    /**
     * Read in a directory from an existing psef-archive
     * @param parent the parent dir to store it in temporarily
     * @param dir the folder item
     */
    public MultiFormatDir( File parent, File dir, String relPath ) throws Exception
    {
        this.parent = parent;
        this.relPath = relPath;
        items = new ArrayList<Folder>();
        readDir( dir );
    }
    /**
     * Create an empty folder item with a name. For splitting.
     * @param parent the temporary directory in which we reside
     */
    public MultiFormatDir( File parent )
    {
        items = new ArrayList<Folder>();
        this.parent = parent;
        this.relPath = "";
    }
    private Format suffixToFormat( String suffix ) throws Exception
    {
        if ( suffix.equals(".xml") )
            return Format.XML;
        else if ( suffix.equals(".mvd") )
            return Format.MVD;
        else if ( suffix.equals(".txt") )
            return Format.TEXT;
        else if ( suffix.equals(".json") )
            return Format.TEXT;
        else
            throw new Exception("unknown suffix "+suffix);
    }
    /**
     * Choose which folder to put a file into
     * @param f the format
     * @return the chosen folder or null if it doesn't yet exist
     */
    Folder chooseFolder( Format f )
    {
        for ( int i=0;i<items.size();i++ )
        {
            Folder folder = items.get(i);
            switch ( f )
            {
                case XML: 
                    if ( folder instanceof XMLFolder )
                        return folder;
                    break;
                case TEXT:
                    if ( folder instanceof TextFolder )
                        return folder;
                    break;
                case MVD:
                    if ( folder instanceof MVDFolder )
                        return folder;
                    break;
            }
        }
        return null;
    }
    /**
     * Add a virtual file to the folder. This happens when splitting.
     * @param parent the temp dir to store it in
     * @param data the file's data content
     * @param relPath the notional relative (group) path
     * @param name the name of the file
     * @param hVersion the individual version ID
     * @param suffix its suffix
     * @throws Exception 
     */
    public void add( byte[] data, String relPath, 
        String name, String hVersion, String suffix ) throws Exception
    {
        Format f = suffixToFormat( suffix );
        Folder folder = chooseFolder( f );
        if ( folder == null )
        {
            File file = new File( parent, f.toString() );
            folder = FolderFactory.makeFolder(null, file, f);
            items.add( folder );
        }
        folder.add( data, relPath, name, hVersion, suffix );
    }
    /**
     * Save this multi-format item to disk
     * @param dst the destination directory (not parent)
     * @param key the FolderItem's key in the FileMap
     * @throws Exception 
     */
    public void save( File dst, String key ) throws Exception
    {
        File dir = new File( dst, key );
        boolean success = true;
        if ( !dir.exists() )
            success = dir.mkdirs();
        if ( success )
        {
            if ( newConf != null )
            {
                File c = new File(dir,"config.conf");
                if ( !c.exists() )
                {
                   boolean res = c.createNewFile();
                   if ( !res )
                       throw new Exception("Couldn't create config.conf");
                }
                FileOutputStream fos = new FileOutputStream(c);
                fos.write( newConf.toString().getBytes());
            }
            else if ( config != null )
            {
                config.externalise( dir, "config.conf" );
            }
            for ( int i=0;i<items.size();i++ )
            {
                Folder f = items.get( i );
                f.externalise( dir );
            }
        }
        else
            throw new Exception("Couldn't create directory "+dir.getPath());
    }
}
