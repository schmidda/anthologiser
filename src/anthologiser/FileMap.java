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
import java.util.HashMap;
import java.io.File;
import java.util.Set;
import java.util.Arrays;

/**
 * A collection of files that can be allocated to a set of sub-directories
 * @author desmond
 */
public class FileMap extends HashMap<String,MultiFormatDir>
{
    static int MAX_LEN = 8;
    File tempDir;
    /**
     * Read a directory and store all its contents
     * @param folder the folder to recurse into
     */
    public FileMap( File folder ) throws Exception
    {
        StringBuilder sb = new StringBuilder(System.getProperty("java.io.tmpdir"));
        if ( sb.charAt(sb.length()-1)!='/' )
            sb.append("/");
        sb.append("FILEMAP");
        tempDir = new File( sb.toString() );
        if ( tempDir.exists() )
            Utils.removeDir( tempDir );
        tempDir.mkdir();
        scanDir( folder, "" );
    }
    /**
     * Get the temporary directory so other classes can write files to it
     * @return the temporary directory
     */
    File getTempDir()
    {
        return tempDir;
    }
    /**
     * Scan an existing directory for folders
     * @param folder the folder we are to scan
     * @param relPath the relativePath to the item
     * @throws Exception 
     */
    final void scanDir( File folder, String relPath ) throws Exception
    {
        File[] files = folder.listFiles();
        for ( int i=0;i<files.length;i++ )
        {
            if ( files[i].isDirectory() )
            {
                String fname = files[i].getName();
                if ( fname.startsWith("%") )
                {
                    ingest( files[i], relPath );
                    Utils.removeDir( files[i] );
                }
                else
                {
                    String newRelPath = (relPath.length()>0)?relPath + File.separator 
                        + files[i].getName():files[i].getName();
                    scanDir( files[i], newRelPath );
                    Utils.removeDir( files[i] );
                }
            }
            else
                files[i].delete();
            // ignore files
        }
    }
    /**
     * Add a dir already checked for % in its name
     * @param dir a dir starting with "%"
     */
    void ingest( File dir, String relPath ) throws Exception
    {
        String name = (relPath.length()>0)?relPath+File.separator+dir.getName():dir.getName();
        File dstDir = new File( tempDir, name );
        // make relpath a property of multiformatdir
        MultiFormatDir mfd = new MultiFormatDir( dstDir, dir, relPath );
        put( name, mfd );
    }
    /**
     * Compute a unique prefix for one string compared to another: as 
     * many characters shared between both, plus the next unmatched char of 
     * the first string. Ignore leading %s.
     * @param one the first string
     * @param two the second string
     * @param prev the previous trailing prefix or null
     * @return the prefix of the first string
     */
    String uniquePrefix( String one, String two, String prev )
    {
        StringBuilder sb = new StringBuilder();
        if ( one.startsWith("%") )
            one = one.substring(1);
        if ( two.startsWith("%") )
            two = two.substring(1);
        int len1 = one.length();
        int i,len2 = two.length();
        for ( i=0;i<len1&&i<len2;i++ )
        {
            char tok1 = one.charAt(i);
            char tok2 = two.charAt(i);
            sb.append( tok1 );
            if ( one.charAt(i)!=two.charAt(i)
                &&Character.isLetter(tok1)
                &&Character.isLetter(tok2)
                &&(prev==null||prev.length()<=sb.length()) )
                break;
        }
        if ( i==len2&& i<len1 )
            sb.append( one.charAt(i) );
        // check that the string isn't too long
        if ( sb.length()> MAX_LEN )
        {
            int end = sb.length()-(MAX_LEN-6);
            sb.delete( 3, end );
            sb.insert( 3, "..." );
        }
        return sb.toString();
    }
    /**
     * Save an entire filemap
     * @param dst the destination folder
     * @param anthology the anthology that needs our subdirectories
     * @param usingSubFolders true if we split up into subfolders
     */
    public void save( File dst, Anthology anthology, boolean usingSubFolders ) 
            throws Exception
    {
        int numBuckets = 2*(int)Math.round(Math.log(size()));
        if ( numBuckets != 0 )
        {
            int bucketSize = (size()/numBuckets)+1;
            String prev = null;
            Set<String> keys = keySet();
            String[] array = new String[size()];
            keys.toArray( array );
            // need to sort ignoring punctuation
            Arrays.sort(array);
            String subFolder = "";
            for ( int i=0;i<array.length;i+=bucketSize )
            {
                File dstFolder = dst;
                if ( usingSubFolders )
                {
                    String first = array[i];
                    String last = (i+bucketSize-1>=array.length)
                        ?array[array.length-1]:array[i+bucketSize-1];
                    String leading = uniquePrefix(first,last,prev);
                    String trailing = uniquePrefix(last,first,null);
                    prev = trailing;
                    subFolder = " "+leading+"-"+trailing;
                    dstFolder = new File(dst,subFolder);
                }
                boolean success = true;
                if ( !dstFolder.exists() )
                    success = dstFolder.mkdir();
                if ( success )
                {
                    for ( int j=i;j<i+bucketSize&&j<array.length;j++ )
                    {
                        MultiFormatDir mfd = get( array[j] );
                        mfd.save( dstFolder, array[j] );
                        String poem = array[j].substring(1);
                        String base = Utils.makeDocID(anthology.getLinkBase());
                        String path = (usingSubFolders)?base+subFolder+"/"+poem:base+poem;
                        anthology.addItem( mfd.getTitle(), path );
                    }
                }
                else
                    throw new Exception("Couldn't create folder "
                        +dstFolder.getPath());
            }
        }
        else
            System.out.println("No poems to save");
    }
}
