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

/**
 *
 * @author desmond
 */
public class Utils {
    /**
     * Compute a unique prefix for one string compared to another: as 
     * many characters shared between both, plus the next unmatched char of 
     * the first string
     * @param one the first string
     * @param two the second string
     * @return the prefix
     */
    static String uniquePrefix( String one, String two )
    {
        StringBuilder sb = new StringBuilder();
        int len1 = one.length();
        int len2 = two.length();
        for ( int i=0;i<len1&&i<len2;i++ )
        {
            if ( one.charAt(i)==two.charAt(i) )
                sb.append( one.charAt(i) );
            else
            {
                sb.append( one.charAt(i) );
                break;
            }
        }
        if ( sb.charAt(0)=='%' )
            sb.deleteCharAt(0);
        return sb.toString();
    }
    /**
     * Get a file name's suffix
     * @param file the file name
     * @return the suffix or the empty string
     */
    static String fileSuffix( String file )
    {
        int index = file.lastIndexOf(".");
        if ( index!= -1 )
            return file.substring(index);
        else
            return "";
    }
    /**
     * Get the file name minus its suffix
     * @param file the file name
     * @return the name minus any suffix
     */
    static String fileName( String file )
    {
        int index = file.lastIndexOf(".");
        if ( index!= -1 )
            return file.substring(0,index);
        else
            return file;
    }
    /**
     * Delete a non-empty directory recursively
     * @param dir the directory to empty
     * @throws Exception 
     */
    static void removeDir( File dir ) throws Exception
    {
        File[] files = dir.listFiles();
        for ( int i=0;i<files.length;i++ )
        {
            if ( files[i].isDirectory() )
                removeDir( files[i] );
            else
                files[i].delete();
        }
        dir.delete();
    }
    /**
     * Turn a raw path into a functional docID
     * @param path the path to escape
     * @return the escaped path
     */
    static String makeDocID( String path )
    {
        String spaced = path.replace( " ", "%20" );
        return spaced.replace("/","%2F");
    }
}
